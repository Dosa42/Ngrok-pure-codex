package com.example.ngrokforwarder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ForwarderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForwarderRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ForwarderUiState())
    val uiState: StateFlow<ForwarderUiState> = _uiState.asStateFlow()

    init {
        // Load stored token or fallback to BuildConfig token if configured
        val savedToken = repository.getSavedAuthtoken().ifBlank {
            val buildToken = try {
                BuildConfig.NGROK_AUTHTOKEN
            } catch (e: Exception) {
                ""
            }
            if (buildToken != "DEFAULT_TOKEN") buildToken else ""
        }

        _uiState.update { it.copy(authtoken = savedToken) }

        // Initial port probe on launch
        probeTargetPort()

        // Background poller for metrics and events while ViewModel is active
        viewModelScope.launch {
            while (isActive) {
                refreshTelemetry()
                delay(1500)
            }
        }

        // Periodic port health checker (every 4 seconds)
        viewModelScope.launch {
            while (isActive) {
                probeTargetPort()
                delay(4000)
            }
        }
    }

    fun onAuthtokenChanged(newToken: String) {
        _uiState.update { it.copy(authtoken = newToken) }
        repository.saveAuthtoken(newToken)
    }

    fun toggleAuthtokenVisibility() {
        _uiState.update { it.copy(isAuthtokenVisible = !it.isAuthtokenVisible) }
    }

    fun probeTargetPort() {
        viewModelScope.launch {
            val (status, message) = repository.probePort(
                _uiState.value.targetHost,
                _uiState.value.targetPort
            )
            _uiState.update {
                it.copy(
                    portStatus = status,
                    portStatusMessage = message
                )
            }
        }
    }

    fun toggleTunnel() {
        viewModelScope.launch {
            val currentStatus = _uiState.value.tunnelStatus
            if (currentStatus == TunnelStatus.ONLINE || currentStatus == TunnelStatus.CONNECTING) {
                repository.stopTunnel()
                _uiState.update {
                    it.copy(
                        tunnelStatus = TunnelStatus.STOPPED,
                        publicUrl = null,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        tunnelStatus = TunnelStatus.CONNECTING,
                        errorMessage = null
                    )
                }
                val success = repository.startTunnel(
                    _uiState.value.targetHost,
                    _uiState.value.targetPort,
                    _uiState.value.authtoken.trim()
                )

                if (success) {
                    val publicUrl = repository.getPublicUrl()
                    _uiState.update {
                        it.copy(
                            tunnelStatus = TunnelStatus.ONLINE,
                            publicUrl = publicUrl,
                            errorMessage = null
                        )
                    }
                } else {
                    val err = repository.getLastError().ifBlank { "Failed to establish tunnel" }
                    _uiState.update {
                        it.copy(
                            tunnelStatus = TunnelStatus.ERROR,
                            errorMessage = err
                        )
                    }
                }
            }
            refreshTelemetry()
        }
    }

    private fun refreshTelemetry() {
        val currentTunnelStatus = repository.getTunnelStatus()
        val metrics = repository.getMetrics()
        val events = repository.getRecentEvents(50)
        val publicUrl = if (currentTunnelStatus == TunnelStatus.ONLINE) repository.getPublicUrl() else null

        _uiState.update {
            it.copy(
                tunnelStatus = if (it.tunnelStatus == TunnelStatus.CONNECTING && currentTunnelStatus == TunnelStatus.STOPPED) {
                    TunnelStatus.CONNECTING
                } else {
                    currentTunnelStatus
                },
                publicUrl = publicUrl ?: it.publicUrl,
                metrics = metrics,
                trafficLogs = events
            )
        }
    }

    fun selectTrafficItem(item: TrafficItem?) {
        _uiState.update { it.copy(selectedTrafficItem = item) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(trafficLogs = emptyList()) }
    }

    fun sendTestPing(path: String = "/") {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingTestPing = true, testPingResult = null) }
            val result = repository.sendTestRequest(path)
            _uiState.update {
                it.copy(
                    isSendingTestPing = false,
                    testPingResult = result
                )
            }
            refreshTelemetry()
        }
    }

    fun dismissTestPingResult() {
        _uiState.update { it.copy(testPingResult = null) }
    }
}
