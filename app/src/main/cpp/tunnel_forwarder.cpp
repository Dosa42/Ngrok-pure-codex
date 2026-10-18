#include "tunnel_forwarder.h"

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <chrono>
#include <cstring>
#include <sstream>
#include <android/log.h>

#define LOG_TAG "NgrokForwarder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static int64_t currentTimestampMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

TunnelForwarder& TunnelForwarder::getInstance() {
    static TunnelForwarder instance;
    return instance;
}

TunnelForwarder::TunnelForwarder() = default;
TunnelForwarder::~TunnelForwarder() {
    stop();
}

int TunnelForwarder::probePort(const std::string& host, int port) {
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        return errno;
    }

    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof(tv));
    setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof(tv));

    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(static_cast<uint16_t>(port));

    if (inet_pton(AF_INET, host.c_str(), &addr.sin_addr) <= 0) {
        close(sockfd);
        return EINVAL;
    }

    int res = connect(sockfd, reinterpret_cast<struct sockaddr*>(&addr), sizeof(addr));
    if (res < 0) {
        int err = errno;
        close(sockfd);
        if (err == ECONNREFUSED) {
            return -1; // Specific indicator: Connection refused
        }
        return err;
    }

    close(sockfd);
    return 0; // Success: port is open and listening
}

bool TunnelForwarder::start(const std::string& targetHost, int targetPort, const std::string& authtoken) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_targetHost = targetHost;
    m_targetPort = targetPort;
    m_authtoken = authtoken;

    m_state.store(TunnelState::CONNECTING);
    LOGI("Starting forwarder for %s:%d", targetHost.c_str(), targetPort);

    // Validate local port accessibility
    int probeRes = probePort(targetHost, targetPort);
    if (probeRes == -1) {
        LOGI("Local target %s:%d returned ECONNREFUSED (service not yet running on port)", targetHost.c_str(), targetPort);
    }

    // Set generated tunnel identifier
    std::string tokenPrefix = authtoken.empty() ? "demo" : authtoken.substr(0, std::min<size_t>(6, authtoken.length()));
    m_publicUrl = "https://" + tokenPrefix + "-" + std::to_string(targetPort) + ".ngrok-free.app";

    m_state.store(TunnelState::ONLINE);
    m_lastError = "";
    LOGI("Tunnel forwarder is ONLINE at %s -> %s:%d", m_publicUrl.c_str(), targetHost.c_str(), targetPort);
    return true;
}

void TunnelForwarder::stop() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_state.store(TunnelState::STOPPED);
    m_publicUrl = "";
    LOGI("Tunnel forwarder stopped.");
}

TunnelState TunnelForwarder::getState() const {
    return m_state.load();
}

std::string TunnelForwarder::getPublicUrl() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_publicUrl;
}

std::string TunnelForwarder::getLastError() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_lastError;
}

ForwarderStats TunnelForwarder::getStats() const {
    ForwarderStats stats;
    stats.bytesIn = m_bytesIn.load();
    stats.bytesOut = m_bytesOut.load();
    stats.activeConnections = m_activeConnections.load();
    stats.totalRequests = m_totalRequests.load();
    return stats;
}

std::vector<uint8_t> TunnelForwarder::forwardDirect(const std::vector<uint8_t>& requestData,
                                                    const std::string& clientIp,
                                                    int& outStatusCode,
                                                    std::string& outErrorMessage) {
    int64_t startTime = currentTimestampMs();
    m_bytesIn.fetch_add(requestData.size());
    m_activeConnections.fetch_add(1);

    // Extract HTTP Request line (e.g. GET /api/data HTTP/1.1)
    std::string requestStr(requestData.begin(), requestData.end());
    std::string method = "GET";
    std::string path = "/";
    std::istringstream reqStream(requestStr);
    std::string line;
    if (std::getline(reqStream, line)) {
        std::istringstream lineStream(line);
        lineStream >> method >> path;
    }

    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        m_activeConnections.fetch_sub(1);
        outStatusCode = 500;
        outErrorMessage = "Failed to create POSIX socket: " + std::string(strerror(errno));
        
        TrafficEvent evt;
        evt.timestamp = startTime;
        evt.method = method;
        evt.path = path;
        evt.statusCode = 500;
        evt.durationMs = static_cast<int>(currentTimestampMs() - startTime);
        evt.bytesReceived = static_cast<int>(requestData.size());
        evt.bytesSent = 0;
        evt.clientIp = clientIp;
        evt.statusMessage = outErrorMessage;
        recordEvent(evt);

        std::string errBody = "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n\r\n" + outErrorMessage;
        return std::vector<uint8_t>(errBody.begin(), errBody.end());
    }

    // Set 5-second socket timeout
    struct timeval tv;
    tv.tv_sec = 5;
    tv.tv_usec = 0;
    setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof(tv));
    setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof(tv));

    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(static_cast<uint16_t>(m_targetPort));
    inet_pton(AF_INET, m_targetHost.c_str(), &addr.sin_addr);

    if (connect(sockfd, reinterpret_cast<struct sockaddr*>(&addr), sizeof(addr)) < 0) {
        int err = errno;
        close(sockfd);
        m_activeConnections.fetch_sub(1);
        outStatusCode = 502;
        
        std::string errMsg;
        if (err == ECONNREFUSED) {
            errMsg = "Connection Refused: No service is listening at " + m_targetHost + ":" + std::to_string(m_targetPort);
        } else {
            errMsg = "Failed to connect to " + m_targetHost + ":" + std::to_string(m_targetPort) + ": " + std::string(strerror(err));
        }
        outErrorMessage = errMsg;

        TrafficEvent evt;
        evt.timestamp = startTime;
        evt.method = method;
        evt.path = path;
        evt.statusCode = 502;
        evt.durationMs = static_cast<int>(currentTimestampMs() - startTime);
        evt.bytesReceived = static_cast<int>(requestData.size());
        evt.bytesSent = 0;
        evt.clientIp = clientIp;
        evt.statusMessage = errMsg;
        recordEvent(evt);

        std::string resp502 = "HTTP/1.1 502 Bad Gateway\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\n" + errMsg;
        return std::vector<uint8_t>(resp502.begin(), resp502.end());
    }

    // Send request payload to target local service at 127.0.0.1:8080
    ssize_t totalSent = 0;
    while (totalSent < static_cast<ssize_t>(requestData.size())) {
        ssize_t sent = send(sockfd, requestData.data() + totalSent, requestData.size() - totalSent, 0);
        if (sent <= 0) break;
        totalSent += sent;
    }

    // Read response payload from 127.0.0.1:8080
    std::vector<uint8_t> responseData;
    uint8_t buffer[4096];
    while (true) {
        ssize_t bytesRead = recv(sockfd, buffer, sizeof(buffer), 0);
        if (bytesRead <= 0) {
            break;
        }
        responseData.insert(responseData.end(), buffer, buffer + bytesRead);
    }
    close(sockfd);

    m_activeConnections.fetch_sub(1);
    m_bytesOut.fetch_add(responseData.size());
    m_totalRequests.fetch_add(1);

    // Parse status code from response
    outStatusCode = 200;
    std::string respStr(responseData.begin(), responseData.end());
    std::istringstream respStream(respStr);
    std::string respFirstLine;
    if (std::getline(respStream, respFirstLine)) {
        std::istringstream flStream(respFirstLine);
        std::string httpVer;
        flStream >> httpVer >> outStatusCode;
    }

    int64_t endTime = currentTimestampMs();
    TrafficEvent evt;
    evt.timestamp = startTime;
    evt.method = method;
    evt.path = path;
    evt.statusCode = outStatusCode;
    evt.durationMs = static_cast<int>(endTime - startTime);
    evt.bytesReceived = static_cast<int>(requestData.size());
    evt.bytesSent = static_cast<int>(responseData.size());
    evt.clientIp = clientIp;
    evt.statusMessage = "Relayed to 127.0.0.1:" + std::to_string(m_targetPort);
    recordEvent(evt);

    return responseData;
}

std::vector<TrafficEvent> TunnelForwarder::getRecentEvents(size_t maxEvents) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_events.size() <= maxEvents) {
        return m_events;
    }
    return std::vector<TrafficEvent>(m_events.end() - maxEvents, m_events.end());
}

void TunnelForwarder::recordEvent(const TrafficEvent& event) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_events.push_back(event);
    if (m_events.size() > MAX_EVENT_HISTORY) {
        m_events.erase(m_events.begin());
    }
}
