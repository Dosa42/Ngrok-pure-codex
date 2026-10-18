#pragma once

#include <string>
#include <vector>
#include <mutex>
#include <atomic>
#include <cstdint>

struct PooledSocket {
    int fd;
    std::string host;
    int port;
    int64_t lastUsedTimestamp;
};

struct PoolStats {
    uint32_t currentIdleSize;
    uint32_t poolHits;
    uint32_t poolMisses;
    uint32_t activeConnections;
    uint32_t maxIdleLimit;
};

class SocketPool {
public:
    static SocketPool& getInstance();

    void setConfig(uint32_t maxIdle, int64_t idleTimeoutMs);
    void setTarget(const std::string& host, int port);

    // Acquire a connected socket to host:port (from pool or freshly created)
    int acquireSocket(const std::string& host, int port, std::string& outError);

    // Return a healthy socket to the pool, or close if pool is full/stale
    void releaseSocket(int sockfd, const std::string& host, int port, bool canReuse);

    // Drain and close all pooled sockets (e.g. when target changes or tunnel stops)
    void clear();

    PoolStats getStats() const;

private:
    SocketPool();
    ~SocketPool();

    bool isSocketAlive(int sockfd) const;

    mutable std::mutex m_mutex;
    std::vector<PooledSocket> m_idleSockets;
    uint32_t m_maxIdle{8};
    int64_t m_idleTimeoutMs{30000}; // 30 seconds

    std::string m_currentTargetHost{"127.0.0.1"};
    int m_currentTargetPort{8080};

    std::atomic<uint32_t> m_poolHits{0};
    std::atomic<uint32_t> m_poolMisses{0};
    std::atomic<uint32_t> m_activeConnections{0};
};
