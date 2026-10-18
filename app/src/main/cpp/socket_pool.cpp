#include "socket_pool.h"

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <cstring>
#include <chrono>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "NgrokSocketPool"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static int64_t currentPoolTimeMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()
    ).count();
}

SocketPool& SocketPool::getInstance() {
    static SocketPool instance;
    return instance;
}

SocketPool::SocketPool() = default;

SocketPool::~SocketPool() {
    clear();
}

void SocketPool::setConfig(uint32_t maxIdle, int64_t idleTimeoutMs) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_maxIdle = maxIdle;
    m_idleTimeoutMs = idleTimeoutMs;
}

void SocketPool::setTarget(const std::string& host, int port) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_currentTargetHost != host || m_currentTargetPort != port) {
        LOGI("Target changed from %s:%d to %s:%d. Draining socket pool.",
             m_currentTargetHost.c_str(), m_currentTargetPort, host.c_str(), port);
        m_currentTargetHost = host;
        m_currentTargetPort = port;
        for (auto& s : m_idleSockets) {
            if (s.fd >= 0) close(s.fd);
        }
        m_idleSockets.clear();
    }
}

bool SocketPool::isSocketAlive(int sockfd) const {
    if (sockfd < 0) return false;
    char peekBuf[1];
    ssize_t ret = recv(sockfd, peekBuf, 1, MSG_PEEK | MSG_DONTWAIT);
    if (ret == 0) {
        return false; // Connection closed by peer
    }
    if (ret < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            return true; // Still open and healthy
        }
        return false; // Socket error
    }
    return true; // Bytes available, still connected
}

int SocketPool::acquireSocket(const std::string& host, int port, std::string& outError) {
    int64_t now = currentPoolTimeMs();

    // 1. Try to find an idle socket from the pool
    {
        std::lock_guard<std::mutex> lock(m_mutex);
        while (!m_idleSockets.empty()) {
            PooledSocket pooled = m_idleSockets.back();
            m_idleSockets.pop_back();

            if (pooled.host == host && pooled.port == port) {
                if (now - pooled.lastUsedTimestamp < m_idleTimeoutMs && isSocketAlive(pooled.fd)) {
                    m_poolHits.fetch_add(1);
                    m_activeConnections.fetch_add(1);
                    LOGI("Socket pool HIT: Reusing socket fd %d for %s:%d", pooled.fd, host.c_str(), port);
                    return pooled.fd;
                }
            }
            // Expired or dead socket
            if (pooled.fd >= 0) {
                close(pooled.fd);
            }
        }
    }

    // 2. Pool miss: create fresh socket connection
    m_poolMisses.fetch_add(1);

    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        outError = "socket() failed: " + std::string(strerror(errno));
        return -1;
    }

    // Configure 5s socket timeouts
    struct timeval tv;
    tv.tv_sec = 5;
    tv.tv_usec = 0;
    setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof(tv));
    setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, (const char*)&tv, sizeof(tv));

    struct sockaddr_in addr;
    std::memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(static_cast<uint16_t>(port));
    if (inet_pton(AF_INET, host.c_str(), &addr.sin_addr) <= 0) {
        close(sockfd);
        outError = "Invalid target host address: " + host;
        return -1;
    }

    if (connect(sockfd, reinterpret_cast<struct sockaddr*>(&addr), sizeof(addr)) < 0) {
        int err = errno;
        close(sockfd);
        if (err == ECONNREFUSED) {
            outError = "Connection refused at " + host + ":" + std::to_string(port);
        } else {
            outError = "Connect failed: " + std::string(strerror(err));
        }
        return -1;
    }

    m_activeConnections.fetch_add(1);
    LOGI("Socket pool MISS: Created fresh connection fd %d to %s:%d", sockfd, host.c_str(), port);
    return sockfd;
}

void SocketPool::releaseSocket(int sockfd, const std::string& host, int port, bool canReuse) {
    if (sockfd < 0) return;
    m_activeConnections.fetch_sub(1);

    if (canReuse) {
        std::lock_guard<std::mutex> lock(m_mutex);
        if (m_idleSockets.size() < m_maxIdle && isSocketAlive(sockfd)) {
            m_idleSockets.push_back({sockfd, host, port, currentPoolTimeMs()});
            LOGI("Returned socket fd %d to pool for %s:%d (pool size: %zu)",
                 sockfd, host.c_str(), port, m_idleSockets.size());
            return;
        }
    }

    close(sockfd);
}

void SocketPool::clear() {
    std::lock_guard<std::mutex> lock(m_mutex);
    for (auto& s : m_idleSockets) {
        if (s.fd >= 0) close(s.fd);
    }
    m_idleSockets.clear();
}

PoolStats SocketPool::getStats() const {
    std::lock_guard<std::mutex> lock(m_mutex);
    PoolStats stats;
    stats.currentIdleSize = static_cast<uint32_t>(m_idleSockets.size());
    stats.poolHits = m_poolHits.load();
    stats.poolMisses = m_poolMisses.load();
    stats.activeConnections = m_activeConnections.load();
    stats.maxIdleLimit = m_maxIdle;
    return stats;
}
