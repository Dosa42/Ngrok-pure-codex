package com.example.ngrokforwarder

class NativeTunnelBridge {

    companion object {
        init {
            System.loadLibrary("ngrokforwarder")
        }
    }

    external fun nativeProbePort(host: String, port: Int): Int
    external fun nativeStart(host: String, port: Int, authtoken: String): Boolean
    external fun nativeStop()
    external fun nativeGetStatus(): Int
    external fun nativeGetPublicUrl(): String?
    external fun nativeGetLastError(): String
    external fun nativeGetStats(): LongArray?
    external fun nativeForwardBytes(requestData: ByteArray, clientIp: String): ByteArray?
    external fun nativeGetRecentEventsJson(maxCount: Int): String
}
