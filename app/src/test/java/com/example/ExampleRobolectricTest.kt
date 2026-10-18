package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ngrokforwarder.CodexProxyInfo
import com.example.ngrokforwarder.ForwarderMetrics
import com.example.ngrokforwarder.ForwarderUiState
import com.example.ngrokforwarder.JsonLogEntry
import com.example.ngrokforwarder.PoolMetrics
import com.example.ngrokforwarder.PortStatus
import com.example.ngrokforwarder.TunnelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertEquals("8080", defaultState.targetPortInput)
        assertEquals(TunnelStatus.STOPPED, defaultState.tunnelStatus)
        assertEquals(PortStatus.CHECKING, defaultState.portStatus)
        assertEquals(0L, defaultState.poolMetrics.poolHits)
        assertEquals(false, defaultState.codexInfo.isDetected)
    }

    @Test
    fun `verify codex proxy info model`() {
        val info = CodexProxyInfo(
            isDetected = true,
            manifestFormat = "codex-proxy-host",
            version = "1.0.0",
            modelCount = 11,
            models = listOf("gpt-6.1-sol", "gpt-6-astra", "gpt-5.6-sol"),
            defaultModel = "gpt-5.6-sol",
            description = "Self-contained client discovery contract"
        )
        assertTrue(info.isDetected)
        assertEquals("codex-proxy-host", info.manifestFormat)
        assertEquals(11, info.modelCount)
        assertEquals("gpt-5.6-sol", info.defaultModel)
    }

    @Test
    fun `verify json log entry protocol tagging`() {
        val entry = JsonLogEntry(
            method = "POST",
            path = "/v1/chat/completions",
            protocol = "OPENAI_CHAT",
            target = "127.0.0.1:8080",
            status = 200,
            durationMs = 12,
            bytesIn = 320,
            bytesOut = 1024,
            pooled = true,
            rawJson = "{\"protocol\": \"OPENAI_CHAT\"}"
        )
        assertEquals("POST", entry.method)
        assertEquals("/v1/chat/completions", entry.path)
        assertEquals("OPENAI_CHAT", entry.protocol)
        assertTrue(entry.pooled)
    }
}
