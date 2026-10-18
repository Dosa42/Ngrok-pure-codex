package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ngrokforwarder.ForwarderMetrics
import com.example.ngrokforwarder.ForwarderUiState
import com.example.ngrokforwarder.PortStatus
import com.example.ngrokforwarder.TrafficItem
import com.example.ngrokforwarder.TunnelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ngrok Forwarder", appName)
    }

    @Test
    fun `verify initial forwarder state defaults`() {
        val defaultState = ForwarderUiState()
        assertEquals("127.0.0.1", defaultState.targetHost)
        assertEquals(8080, defaultState.targetPort)
        assertEquals(TunnelStatus.STOPPED, defaultState.tunnelStatus)
        assertEquals(PortStatus.CHECKING, defaultState.portStatus)
    }

    @Test
    fun `verify traffic item data model`() {
        val item = TrafficItem(
            method = "POST",
            path = "/api/webhook",
            statusCode = 200,
            durationMs = 15,
            bytesReceived = 256,
            bytesSent = 512,
            clientIp = "192.168.1.50"
        )
        assertEquals("POST", item.method)
        assertEquals("/api/webhook", item.path)
        assertEquals(200, item.statusCode)
        assertEquals(15, item.durationMs)
    }
}
