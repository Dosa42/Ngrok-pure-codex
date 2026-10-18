package com.example.ngrokforwarder

import android.util.Log
import com.ngrok.Session
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NgrokSdkTunnel {
    private const val TAG = "NgrokSdkTunnel"

    @Volatile
    private var activeSession: Session? = null

    suspend fun connect(authtoken: String, domain: String, targetPort: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Attempting native Ngrok SDK connection with domain=$domain targetPort=$targetPort")
            val sessionBuilder = if (authtoken.isNotBlank()) {
                Session.withAuthtoken(authtoken)
            } else {
                Session.withAuthtokenFromEnv()
            }
            val session = sessionBuilder.connect()
            activeSession = session

            val endpointBuilder = session.httpEndpoint()
            if (domain.isNotBlank()) {
                val cleanDomain = domain.removePrefix("https://").removePrefix("http://").trimEnd('/')
                endpointBuilder.domain(cleanDomain)
            }

            val forwarder = endpointBuilder.forward(URL("http://127.0.0.1:$targetPort"))
            val url = forwarder.url
            Log.i(TAG, "Ngrok SDK tunnel established: $url")
            Result.success(url.toString())
        } catch (t: Throwable) {
            Log.e(TAG, "Ngrok SDK connection failed: ${t.message}", t)
            Result.failure(t)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            activeSession?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "Error closing session: ${t.message}", t)
        } finally {
            activeSession = null
        }
    }

    fun isConnected(): Boolean {
        return activeSession != null
    }
}
