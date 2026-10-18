package com.example.ngrokforwarder

enum class TunnelStatus {
    STOPPED,
    CONNECTING,
    ONLINE,
    ERROR
}

enum class PortStatus {
    CHECKING,
    LISTENING,
    REFUSED,
    ERROR
}

data class TrafficItem(
    val timestamp: Long = System.currentTimeMillis(),
    val method: String = "GET",
    val path: String = "/",
    val statusCode: Int = 200,
    val durationMs: Int = 0,
    val bytesReceived: Int = 0,
    val bytesSent: Int = 0,
    val clientIp: String = "127.0.0.1",
    val statusMessage: String = ""
)

data class ForwarderMetrics(
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val activeConnections: Long = 0L,
    val totalRequests: Long = 0L
)

data class ForwarderUiState(
    val authtoken: String = "",
    val isAuthtokenVisible: Boolean = false,
    val targetHost: String = "127.0.0.1",
    val targetPort: Int = 8080,
    val tunnelStatus: TunnelStatus = TunnelStatus.STOPPED,
    val portStatus: PortStatus = PortStatus.CHECKING,
    val portStatusMessage: String = "Probing 127.0.0.1:8080...",
    val publicUrl: String? = null,
    val errorMessage: String? = null,
    val metrics: ForwarderMetrics = ForwarderMetrics(),
    val trafficLogs: List<TrafficItem> = emptyList(),
    val selectedTrafficItem: TrafficItem? = null,
    val isSendingTestPing: Boolean = false,
    val testPingResult: String? = null
)
