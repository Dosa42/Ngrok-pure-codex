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

data class CodexProxyInfo(
    val isDetected: Boolean = false,
    val manifestFormat: String = "",
    val version: String = "",
    val modelCount: Int = 0,
    val models: List<String> = emptyList(),
    val defaultModel: String = "",
    val description: String = "",
    val baseUrl: String = "http://127.0.0.1:8080"
)

data class PoolMetrics(
    val idleCount: Long = 0L,
    val poolHits: Long = 0L,
    val poolMisses: Long = 0L,
    val activeConnections: Long = 0L,
    val maxIdle: Long = 8L
)

data class JsonLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val event: String = "FORWARD_HTTP",
    val protocol: String = "GENERIC_HTTP",
    val client: String = "127.0.0.1",
    val target: String = "127.0.0.1:8080",
    val method: String = "GET",
    val path: String = "/",
    val status: Int = 200,
    val durationMs: Int = 0,
    val bytesIn: Long = 0L,
    val bytesOut: Long = 0L,
    val pooled: Boolean = false,
    val error: String = "",
    val rawJson: String = ""
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
    val isAuthtokenEmbeddedFromEnv: Boolean = false,
    val targetHost: String = "127.0.0.1",
    val targetPort: Int = 8080,
    val targetPortInput: String = "8080",
    val tunnelStatus: TunnelStatus = TunnelStatus.STOPPED,
    val portStatus: PortStatus = PortStatus.CHECKING,
    val portStatusMessage: String = "Probing target service...",
    val codexInfo: CodexProxyInfo = CodexProxyInfo(),
    val devUrl: String = "https://ais-dev-t7ylyxqor5zeetjs5fxfv3-571187916303.europe-west3.run.app",
    val isDevUrlEmbedded: Boolean = true,
    val publicUrl: String? = null,
    val errorMessage: String? = null,
    val metrics: ForwarderMetrics = ForwarderMetrics(),
    val poolMetrics: PoolMetrics = PoolMetrics(),
    val jsonLogs: List<JsonLogEntry> = emptyList(),
    val rawStdoutString: String = "[]",
    val selectedJsonLog: JsonLogEntry? = null,
    val isSendingTestPing: Boolean = false,
    val testPingResult: String? = null,
    val testPingTitle: String = "Test Response",
    val jsonSearchQuery: String = "",
    val dexState: DexState = DexState()
)
