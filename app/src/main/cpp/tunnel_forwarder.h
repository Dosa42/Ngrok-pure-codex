#pragma once

#include <string>
#include <vector>
#include <mutex>
#include <atomic>
#include <cstdint>
#include "socket_pool.h"

struct TrafficEvent {
    int64_t timestamp;
    std::string method;
    std::string path;
    int statusCode;
    int durationMs;
    int bytesReceived;
    int bytesSent;
    std::string clientIp;
    std::string statusMessage;
    std::string jsonLog;
};

struct ForwarderStats {
    uint64_t bytesIn;
    uint64_t bytesOut;
    uint32_t activeConnections;
    uint32_t totalRequests;
};

enum class TunnelState {
    STOPPED = 0,
    CONNECTING = 1,
    ONLINE = 2,
    ERROR = -1
};

class TunnelForwarder {
public:
    static TunnelForwarder& getInstance();

    int probePort(const std::string& host, int port);

    // Dynamic target service configuration
    void setTarget(const std::string& host, int port);
    std::string getTargetHost() const;
    int getTargetPort() const;

    // Tunnel control
    bool start(const std::string& targetHost, int targetPort, const std::string& authtoken);
    void stop();

    // State & telemetry
    TunnelState getState() const;
    std::string getPublicUrl() const;
    std::string getLastError() const;
    ForwarderStats getStats() const;
    PoolStats getPoolStats() const;

    // Direct forwarding relay using the SocketPool and emitting stdout JSON
    std::vector<uint8_t> forwardDirect(const std::vector<uint8_t>& requestData,
                                       const std::string& clientIp,
                                       int& outStatusCode,
                                       std::string& outErrorMessage);

    // Event & stdout JSON log buffer
    std::vector<TrafficEvent> getRecentEvents(size_t maxEvents);
    std::string getStdoutJsonLogs(size_t maxEvents);
    void recordEvent(const TrafficEvent& event);

private:
    TunnelForwarder();
    ~TunnelForwarder();

    std::atomic<TunnelState> m_state{TunnelState::STOPPED};
    std::string m_targetHost{"127.0.0.1"};
    int m_targetPort{8080};
    std::string m_authtoken;
    std::string m_publicUrl;
    std::string m_lastError;

    mutable std::mutex m_mutex;
    std::atomic<uint64_t> m_bytesIn{0};
    std::atomic<uint64_t> m_bytesOut{0};
    std::atomic<uint32_t> m_totalRequests{0};

    std::vector<TrafficEvent> m_events;
    static constexpr size_t MAX_EVENT_HISTORY = 100;
};
