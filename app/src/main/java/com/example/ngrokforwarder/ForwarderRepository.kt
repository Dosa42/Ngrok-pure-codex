package com.example.ngrokforwarder

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class ForwarderRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("ngrok_prefs", Context.MODE_PRIVATE)
    private val bridge: NativeTunnelBridge = NativeTunnelBridge()

    fun getEmbeddedEnvToken(): String {
        return try {
            val token = BuildConfig.NGROK_AUTHTOKEN
            if (token.isNotBlank() && token != "DEFAULT_TOKEN") token else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getSavedAuthtoken(): String {
        val stored = prefs.getString("authtoken", "") ?: ""
        return if (stored.isNotBlank()) stored else getEmbeddedEnvToken()
    }

    fun saveAuthtoken(token: String) {
        prefs.edit().putString("authtoken", token).apply()
    }

    fun getSavedTargetHost(): String {
        return prefs.getString("target_host", "127.0.0.1") ?: "127.0.0.1"
    }

    fun getSavedTargetPort(): Int {
        return prefs.getInt("target_port", 8080)
    }

    fun saveTarget(host: String, port: Int) {
        prefs.edit().putString("target_host", host).putInt("target_port", port).apply()
        bridge.nativeSetTarget(host, port)
    }

    fun getEmbeddedDevUrl(): String {
        return try {
            val url = BuildConfig.DEV_URL
            if (url.isNotBlank() && url != "DEFAULT_DEV_URL") url else "https://fernlike-profusely-stunner.ngrok-free.dev"
        } catch (e: Exception) {
            "https://fernlike-profusely-stunner.ngrok-free.dev"
        }
    }

    fun getSavedDevUrl(): String {
        val stored = prefs.getString("dev_url", "") ?: ""
        return if (stored.isNotBlank()) stored else getEmbeddedDevUrl()
    }

    fun saveDevUrl(url: String) {
        prefs.edit().putString("dev_url", url).apply()
        bridge.nativeSetPublicUrl(url)
    }

    suspend fun probePort(host: String, port: Int): Pair<PortStatus, String> =
        withContext(Dispatchers.IO) {
            val res = bridge.nativeProbePort(host, port)
            when (res) {
                0 -> PortStatus.LISTENING to "Active & Listening on $host:$port"
                -1 -> PortStatus.REFUSED to "Connection Refused: No service on $host:$port"
                else -> PortStatus.ERROR to "Socket error ($res) on $host:$port"
            }
        }

    suspend fun discoverCodexProxy(host: String, port: Int): CodexProxyInfo? =
        withContext(Dispatchers.IO) {
            try {
                val req = "GET /host.json HTTP/1.1\r\n" +
                        "Host: $host:$port\r\n" +
                        "Accept: application/json\r\n" +
                        "Connection: close\r\n\r\n"

                val bytes = bridge.nativeForwardBytes(req.toByteArray(StandardCharsets.UTF_8), "127.0.0.1")
                if (bytes == null || bytes.isEmpty()) return@withContext null

                val respStr = String(bytes, StandardCharsets.UTF_8)
                val bodyIndex = respStr.indexOf("\r\n\r\n")
                if (bodyIndex == -1) return@withContext null

                val jsonBody = respStr.substring(bodyIndex + 4).trim()
                if (!jsonBody.startsWith("{")) return@withContext null

                val root = JSONObject(jsonBody)
                if (root.optString("manifest_format") != "codex-proxy-host") return@withContext null

                val version = root.optString("manifest_version", "1.0.0")
                val desc = root.optString("description", "")
                val endpointObj = root.optJSONObject("endpoint")
                val baseUrl = endpointObj?.optString("base_url") ?: "http://$host:$port"

                val runtime = root.optJSONObject("runtime_snapshot")
                val modelCount = runtime?.optInt("listed_model_count") ?: 0
                val defaultModel = runtime?.optString("default_model") ?: "gpt-5.6-sol"

                val modelList = mutableListOf<String>()
                val modelsArr = runtime?.optJSONArray("models")
                if (modelsArr != null) {
                    for (i in 0 until modelsArr.length()) {
                        val m = modelsArr.optJSONObject(i)
                        val id = m?.optString("id")
                        if (!id.isNullOrBlank()) modelList.add(id)
                    }
                }

                CodexProxyInfo(
                    isDetected = true,
                    manifestFormat = "codex-proxy-host",
                    version = version,
                    modelCount = if (modelList.isNotEmpty()) modelList.size else modelCount,
                    models = modelList,
                    defaultModel = defaultModel,
                    description = desc,
                    baseUrl = baseUrl
                )
            } catch (e: Exception) {
                null
            }
        }

    @Volatile
    private var sdkErrorDetail: String? = null

    suspend fun startTunnel(host: String, port: Int, token: String, devUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            saveAuthtoken(token)
            saveDevUrl(devUrl)
            saveTarget(host, port)
            sdkErrorDetail = null

            // Authentically invoke the ngrok Java SDK
            val sdkResult = NgrokSdkTunnel.connect(token, devUrl, port)
            sdkResult.onSuccess { activeSdkUrl ->
                bridge.nativeSetPublicUrl(activeSdkUrl)
                saveDevUrl(activeSdkUrl)
            }.onFailure { sdkError ->
                sdkErrorDetail = "${sdkError::class.java.simpleName}: ${sdkError.message}"
                android.util.Log.e("NgrokForwarder", "ngrok Java SDK error: ${sdkError.message}", sdkError)
            }

            bridge.nativeStart(host, port, token, devUrl)
        }

    suspend fun stopTunnel(): Unit = withContext(Dispatchers.IO) {
        NgrokSdkTunnel.disconnect()
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
        val nativeErr = bridge.nativeGetLastError()
        val sdkErr = sdkErrorDetail
        return when {
            !sdkErr.isNullOrBlank() && nativeErr.isNotBlank() -> "$sdkErr | $nativeErr"
            !sdkErr.isNullOrBlank() -> sdkErr
            else -> nativeErr
        }
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

    fun getPoolMetrics(): PoolMetrics {
        val stats: LongArray? = bridge.nativeGetPoolStats()
        return if (stats != null && stats.size >= 5) {
            PoolMetrics(
                idleCount = stats[0],
                poolHits = stats[1],
                poolMisses = stats[2],
                activeConnections = stats[3],
                maxIdle = stats[4]
            )
        } else {
            PoolMetrics()
        }
    }

    fun getStdoutJsonLogs(maxCount: Int = 50): Pair<String, List<JsonLogEntry>> {
        val jsonStr = bridge.nativeGetStdoutJsonLogs(maxCount)
        if (jsonStr.isBlank() || jsonStr == "[\n\n]" || jsonStr == "[]") {
            return "[]" to emptyList()
        }

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<JsonLogEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj: JSONObject = jsonArray.getJSONObject(i)
                list.add(
                    JsonLogEntry(
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        event = obj.optString("event", "FORWARD_HTTP"),
                        protocol = obj.optString("protocol", "GENERIC_HTTP"),
                        client = obj.optString("client", "127.0.0.1"),
                        target = obj.optString("target", "127.0.0.1:8080"),
                        method = obj.optString("method", "GET"),
                        path = obj.optString("path", "/"),
                        status = obj.optInt("status", 200),
                        durationMs = obj.optInt("duration_ms", 0),
                        bytesIn = obj.optLong("bytes_in", 0L),
                        bytesOut = obj.optLong("bytes_out", 0L),
                        pooled = obj.optBoolean("pooled", false),
                        error = obj.optString("error", ""),
                        rawJson = obj.toString(2)
                    )
                )
            }
            jsonStr to list.reversed()
        } catch (e: Exception) {
            jsonStr to emptyList()
        }
    }

    suspend fun executeCodexTest(host: String, port: Int, testType: String): Pair<String, String> =
        withContext(Dispatchers.IO) {
            when (testType.uppercase()) {
                "MANIFEST" -> {
                    val req = "GET /host.json HTTP/1.1\r\n" +
                            "Host: $host:$port\r\n" +
                            "User-Agent: NgrokForwarder-NativeJNI/1.0\r\n" +
                            "Accept: application/json\r\n" +
                            "Connection: close\r\n\r\n"
                    val resp = bridge.nativeForwardBytes(req.toByteArray(StandardCharsets.UTF_8), "127.0.0.1")
                    "Codex Host Manifest (/host.json)" to (resp?.let { String(it, StandardCharsets.UTF_8) } ?: "No response")
                }
                "MODELS" -> {
                    val req = "GET /v1/models HTTP/1.1\r\n" +
                            "Host: $host:$port\r\n" +
                            "User-Agent: NgrokForwarder-NativeJNI/1.0\r\n" +
                            "Accept: application/json\r\n" +
                            "Connection: close\r\n\r\n"
                    val resp = bridge.nativeForwardBytes(req.toByteArray(StandardCharsets.UTF_8), "127.0.0.1")
                    "OpenAI Models List (/v1/models)" to (resp?.let { String(it, StandardCharsets.UTF_8) } ?: "No response")
                }
                "CATALOG" -> {
                    val req = "GET /v1/models/catalog HTTP/1.1\r\n" +
                            "Host: $host:$port\r\n" +
                            "User-Agent: NgrokForwarder-NativeJNI/1.0\r\n" +
                            "Accept: application/json\r\n" +
                            "Connection: close\r\n\r\n"
                    val resp = bridge.nativeForwardBytes(req.toByteArray(StandardCharsets.UTF_8), "127.0.0.1")
                    "Codex Catalog (/v1/models/catalog)" to (resp?.let { String(it, StandardCharsets.UTF_8) } ?: "No response")
                }
                "CHAT" -> {
                    val body = "{\"model\":\"gpt-6.1-sol\",\"messages\":[{\"role\":\"user\",\"content\":\"Ping from Ngrok Forwarder\"}],\"stream\":false}"
                    val req = "POST /v1/chat/completions HTTP/1.1\r\n" +
                            "Host: $host:$port\r\n" +
                            "User-Agent: NgrokForwarder-NativeJNI/1.0\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: ${body.toByteArray(StandardCharsets.UTF_8).size}\r\n" +
                            "Connection: close\r\n\r\n" + body
                    val resp = bridge.nativeForwardBytes(req.toByteArray(StandardCharsets.UTF_8), "127.0.0.1")
                    "OpenAI Chat Completion (/v1/chat/completions)" to (resp?.let { String(it, StandardCharsets.UTF_8) } ?: "No response")
                }
                else -> {
                    val req = "GET / HTTP/1.1\r\n" +
                            "Host: $host:$port\r\n" +
                            "Connection: close\r\n\r\n"
                    val resp = bridge.nativeForwardBytes(req.toByteArray(StandardCharsets.UTF_8), "127.0.0.1")
                    "Ping :$port" to (resp?.let { String(it, StandardCharsets.UTF_8) } ?: "No response")
                }
            }
        }
}
