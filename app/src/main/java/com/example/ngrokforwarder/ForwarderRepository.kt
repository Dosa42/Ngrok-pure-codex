package com.example.ngrokforwarder

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.nio.charset.StandardCharsets

class ForwarderRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ngrok_prefs", Context.MODE_PRIVATE)
    private val bridge: NativeTunnelBridge = NativeTunnelBridge()

    fun getSavedAuthtoken(): String {
        return prefs.getString("authtoken", "") ?: ""
    }

    fun saveAuthtoken(token: String) {
        prefs.edit().putString("authtoken", token).apply()
    }

    suspend fun probePort(host: String = "127.0.0.1", port: Int = 8080): Pair<PortStatus, String> =
        withContext(Dispatchers.IO) {
            val res = bridge.nativeProbePort(host, port)
            when (res) {
                0 -> PortStatus.LISTENING to "Active & Listening on $host:$port"
                -1 -> PortStatus.REFUSED to "Connection Refused: No service on $host:$port"
                else -> PortStatus.ERROR to "Socket error ($res) on $host:$port"
            }
        }

    suspend fun startTunnel(host: String = "127.0.0.1", port: Int = 8080, token: String): Boolean =
        withContext(Dispatchers.IO) {
            saveAuthtoken(token)
            bridge.nativeStart(host, port, token)
        }

    suspend fun stopTunnel(): Unit = withContext(Dispatchers.IO) {
        bridge.nativeStop()
    }

    fun getTunnelStatus(): TunnelStatus {
        return when (bridge.nativeGetStatus()) {
            1 -> TunnelStatus.CONNECTING
            2 -> TunnelStatus.ONLINE
            -1 -> TunnelStatus.ERROR
            else -> TunnelStatus.STOPPED
        }
    }

    fun getPublicUrl(): String? {
        return bridge.nativeGetPublicUrl()
    }

    fun getLastError(): String {
        return bridge.nativeGetLastError()
    }

    fun getMetrics(): ForwarderMetrics {
        val stats: LongArray? = bridge.nativeGetStats()
        return if (stats != null && stats.size >= 4) {
            ForwarderMetrics(
                bytesIn = stats[0],
                bytesOut = stats[1],
                activeConnections = stats[2],
                totalRequests = stats[3]
            )
        } else {
            ForwarderMetrics()
        }
    }

    fun getRecentEvents(maxCount: Int = 50): List<TrafficItem> {
        val jsonStr = bridge.nativeGetRecentEventsJson(maxCount)
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<TrafficItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TrafficItem(
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        method = obj.optString("method", "GET"),
                        path = obj.optString("path", "/"),
                        statusCode = obj.optInt("statusCode", 200),
                        durationMs = obj.optInt("durationMs", 0),
                        bytesReceived = obj.optInt("bytesReceived", 0),
                        bytesSent = obj.optInt("bytesSent", 0),
                        clientIp = obj.optString("clientIp", "127.0.0.1"),
                        statusMessage = obj.optString("statusMessage", "")
                    )
                )
            }
            list.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendTestRequest(path: String = "/"): String = withContext(Dispatchers.IO) {
        val rawRequest = "GET $path HTTP/1.1\r\n" +
                "Host: 127.0.0.1:8080\r\n" +
                "User-Agent: NgrokForwarder-NativeJNI/1.0\r\n" +
                "Accept: */*\r\n" +
                "Connection: close\r\n\r\n"

        val responseBytes: ByteArray? = bridge.nativeForwardBytes(
            rawRequest.toByteArray(StandardCharsets.UTF_8),
            "127.0.0.1"
        )

        if (responseBytes != null && responseBytes.isNotEmpty()) {
            String(responseBytes, StandardCharsets.UTF_8)
        } else {
            "No response received from 127.0.0.1:8080"
        }
    }
}
