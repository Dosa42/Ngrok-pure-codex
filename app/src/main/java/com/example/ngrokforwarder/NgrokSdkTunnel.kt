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

    @Volatile
    private var nativeLoaded = false

    fun initNative(context: android.content.Context) {
        if (nativeLoaded) return
        try {
            val cacheDir = context.cacheDir
            System.setProperty("java.io.tmpdir", cacheDir.absolutePath)
        } catch (t: Throwable) {
            Log.w(TAG, "Could not set java.io.tmpdir: ${t.message}")
        }

        try {
            System.loadLibrary("ngrok_java")
            nativeLoaded = true
            Log.i(TAG, "Successfully loaded libngrok_java via System.loadLibrary")
        } catch (t: Throwable) {
            Log.e(TAG, "System.loadLibrary(ngrok_java) failed: ${t.message}", t)
        }
    }

    suspend fun connect(authtoken: String, domain: String, targetPort: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!nativeLoaded) {
                try {
                    System.loadLibrary("ngrok_java")
                    nativeLoaded = true
                } catch (t: Throwable) {
                    Log.w(TAG, "Late loadLibrary(ngrok_java) attempt: ${t.message}")
                }
            }
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
