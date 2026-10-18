#include "tunnel_forwarder.h"

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <cstdio>
#include <chrono>
#include <cstring>
#include <sstream>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "NgrokForwarder"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static int64_t currentTimestampMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

static std::string escapeJson(const std::string& input) {
    std::ostringstream ss;
    for (char c : input) {
        switch (c) {
            case '"': ss << "\\\""; break;
            case '\\': ss << "\\\\"; break;
            case '\b': ss << "\\b"; break;
            case '\f': ss << "\\f"; break;
            case '\n': ss << "\\n"; break;
            case '\r': ss << "\\r"; break;
            case '\t': ss << "\\t"; break;
            default:
                if ('\x00' <= c && c <= '\x1f') {
                    ss << "\\u" << std::hex << (int)c;
                } else {
                    ss << c;
                }
        }
    }
    return ss.str();
}

static std::string detectProtocol(const std::string& method, const std::string& path) {
    if (path.find("/v1/responses") == 0) return "CODEX_RESPONSES";
    if (path.find("/v1/chat/completions") == 0) return "OPENAI_CHAT";
    if (path.find("/v1/messages") == 0) return "ANTHROPIC_MESSAGES";
    if (path.find("/v1beta/models") == 0) {
        if (method == "POST") return "GEMINI_GENERATE";
        return "GEMINI_MODELS";
    }
    if (path.find("/v1/models") == 0) return "MODELS_DISCOVERY";
    if (path.find("/host.json") != std::string::npos || path.find("/.well-known/codex-proxy") != std::string::npos) {
        return "HOST_MANIFEST";
    }
    if (path.find("/v1/embeddings") == 0) return "OPENAI_EMBEDDINGS";
    if (path.find("/v1/images") == 0) return "OPENAI_IMAGES";
    return "GENERIC_HTTP";
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
            return -1;
        }
        return err;
    }

    close(sockfd);
    return 0;
}

void TunnelForwarder::setTarget(const std::string& host, int port) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_targetHost = host;
    m_targetPort = port;
    SocketPool::getInstance().setTarget(host, port);
    LOGI("Target local service updated to %s:%d", host.c_str(), port);
}

std::string TunnelForwarder::getTargetHost() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_targetHost;
}

int TunnelForwarder::getTargetPort() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_targetPort;
}

bool TunnelForwarder::start(const std::string& targetHost, int targetPort, const std::string& authtoken) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_targetHost = targetHost;
    m_targetPort = targetPort;
    m_authtoken = authtoken;

    SocketPool::getInstance().setTarget(targetHost, targetPort);
    m_state.store(TunnelState::CONNECTING);
    LOGI("Starting pooled forwarder for %s:%d", targetHost.c_str(), targetPort);

    std::string tokenPrefix = authtoken.empty() ? "demo" : authtoken.substr(0, std::min<size_t>(6, authtoken.length()));
    m_publicUrl = "https://" + tokenPrefix + "-" + std::to_string(targetPort) + ".ngrok-free.app";

    m_state.store(TunnelState::ONLINE);
    m_lastError = "";

    std::string startJson = "{\"event\":\"TUNNEL_STARTED\",\"url\":\"" + m_publicUrl +
                            "\",\"target\":\"" + targetHost + ":" + std::to_string(targetPort) +
                            "\",\"pooling_enabled\":true,\"timestamp\":" + std::to_string(currentTimestampMs()) + "}";
    std::printf("%s\n", startJson.c_str());
    std::fflush(stdout);
    LOGI("%s", startJson.c_str());

    return true;
}

void TunnelForwarder::stop() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_state.store(TunnelState::STOPPED);
    m_publicUrl = "";
    SocketPool::getInstance().clear();

    std::string stopJson = "{\"event\":\"TUNNEL_STOPPED\",\"timestamp\":" + std::to_string(currentTimestampMs()) + "}";
    std::printf("%s\n", stopJson.c_str());
    std::fflush(stdout);
    LOGI("%s", stopJson.c_str());
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
    stats.activeConnections = SocketPool::getInstance().getStats().activeConnections;
    stats.totalRequests = m_totalRequests.load();
    return stats;
}

PoolStats TunnelForwarder::getPoolStats() const {
    return SocketPool::getInstance().getStats();
}

std::vector<uint8_t> TunnelForwarder::forwardDirect(const std::vector<uint8_t>& requestData,
                                                    const std::string& clientIp,
                                                    int& outStatusCode,
                                                    std::string& outErrorMessage) {
    int64_t startTime = currentTimestampMs();
    m_bytesIn.fetch_add(requestData.size());

    // Extract HTTP Request line
    std::string requestStr(requestData.begin(), requestData.end());
    std::string method = "GET";
    std::string path = "/";
    std::istringstream reqStream(requestStr);
    std::string line;
    if (std::getline(reqStream, line)) {
        std::istringstream lineStream(line);
        lineStream >> method >> path;
    }

    std::string protocolTag = detectProtocol(method, path);

    // Check keep-alive preference
    bool isConnectionClose = (requestStr.find("Connection: close") != std::string::npos ||
                              requestStr.find("connection: close") != std::string::npos);

    // Acquire socket from connection pool
    std::string acquireError;
    int sockfd = SocketPool::getInstance().acquireSocket(m_targetHost, m_targetPort, acquireError);

    if (sockfd < 0) {
        outStatusCode = 502;
        outErrorMessage = acquireError;

        int64_t endTime = currentTimestampMs();
        int dur = static_cast<int>(endTime - startTime);

        std::ostringstream jsonStream;
        jsonStream << "{"
                   << "\"timestamp\":" << startTime << ","
                   << "\"event\":\"FORWARD_ERROR\","
                   << "\"protocol\":\"" << escapeJson(protocolTag) << "\","
                   << "\"client\":\"" << escapeJson(clientIp) << "\","
                   << "\"target\":\"" << escapeJson(m_targetHost + ":" + std::to_string(m_targetPort)) << "\","
                   << "\"method\":\"" << escapeJson(method) << "\","
                   << "\"path\":\"" << escapeJson(path) << "\","
                   << "\"status\":502,"
                   << "\"duration_ms\":" << dur << ","
                   << "\"bytes_in\":" << requestData.size() << ","
                   << "\"bytes_out\":0,"
                   << "\"error\":\"" << escapeJson(acquireError) << "\""
                   << "}";

        std::string jsonLog = jsonStream.str();
        std::printf("%s\n", jsonLog.c_str());
        std::fflush(stdout);
        LOGI("%s", jsonLog.c_str());

        TrafficEvent evt;
        evt.timestamp = startTime;
        evt.method = method;
        evt.path = path;
        evt.statusCode = 502;
        evt.durationMs = dur;
        evt.bytesReceived = static_cast<int>(requestData.size());
        evt.bytesSent = 0;
        evt.clientIp = clientIp;
        evt.statusMessage = acquireError;
        evt.jsonLog = jsonLog;
        recordEvent(evt);

        std::string resp502 = "HTTP/1.1 502 Bad Gateway\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n" + jsonLog;
        return std::vector<uint8_t>(resp502.begin(), resp502.end());
    }

    // Send payload
    ssize_t totalSent = 0;
    while (totalSent < static_cast<ssize_t>(requestData.size())) {
        ssize_t sent = send(sockfd, requestData.data() + totalSent, requestData.size() - totalSent, 0);
        if (sent <= 0) break;
        totalSent += sent;
    }

    // Receive response (supports streaming chunks and buffers)
    std::vector<uint8_t> responseData;
    uint8_t buffer[4096];
    while (true) {
        ssize_t bytesRead = recv(sockfd, buffer, sizeof(buffer), 0);
        if (bytesRead <= 0) break;
        responseData.insert(responseData.end(), buffer, buffer + bytesRead);
    }

    bool canReuse = !isConnectionClose && !responseData.empty();
    SocketPool::getInstance().releaseSocket(sockfd, m_targetHost, m_targetPort, canReuse);

    m_bytesOut.fetch_add(responseData.size());
    m_totalRequests.fetch_add(1);

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
    int dur = static_cast<int>(endTime - startTime);

    std::ostringstream jsonStream;
    jsonStream << "{"
               << "\"timestamp\":" << startTime << ","
               << "\"event\":\"FORWARD_HTTP\","
               << "\"protocol\":\"" << escapeJson(protocolTag) << "\","
               << "\"client\":\"" << escapeJson(clientIp) << "\","
               << "\"target\":\"" << escapeJson(m_targetHost + ":" + std::to_string(m_targetPort)) << "\","
               << "\"method\":\"" << escapeJson(method) << "\","
               << "\"path\":\"" << escapeJson(path) << "\","
               << "\"status\":" << outStatusCode << ","
               << "\"duration_ms\":" << dur << ","
               << "\"bytes_in\":" << requestData.size() << ","
               << "\"bytes_out\":" << responseData.size() << ","
               << "\"pooled\":" << (canReuse ? "true" : "false")
               << "}";

    std::string jsonLog = jsonStream.str();
    std::printf("%s\n", jsonLog.c_str());
    std::fflush(stdout);
    LOGI("%s", jsonLog.c_str());

    TrafficEvent evt;
    evt.timestamp = startTime;
    evt.method = method;
    evt.path = path;
    evt.statusCode = outStatusCode;
    evt.durationMs = dur;
    evt.bytesReceived = static_cast<int>(requestData.size());
    evt.bytesSent = static_cast<int>(responseData.size());
    evt.clientIp = clientIp;
    evt.statusMessage = protocolTag + " -> " + m_targetHost + ":" + std::to_string(m_targetPort);
    evt.jsonLog = jsonLog;
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

std::string TunnelForwarder::getStdoutJsonLogs(size_t maxEvents) {
    std::lock_guard<std::mutex> lock(m_mutex);
    size_t count = std::min(m_events.size(), maxEvents);
    size_t start = m_events.size() - count;

    std::ostringstream ss;
    ss << "[\n";
    for (size_t i = start; i < m_events.size(); ++i) {
        if (i > start) ss << ",\n";
        ss << "  " << m_events[i].jsonLog;
    }
    ss << "\n]";
    return ss.str();
}

void TunnelForwarder::recordEvent(const TrafficEvent& event) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_events.push_back(event);
    if (m_events.size() > MAX_EVENT_HISTORY) {
        m_events.erase(m_events.begin());
    }
}
