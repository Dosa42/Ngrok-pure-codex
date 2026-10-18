package com.example.ngrokforwarder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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

    private var probeJob: Job? = null

    init {
        val envToken = repository.getEmbeddedEnvToken()
        val savedToken = repository.getSavedAuthtoken()
        val initialToken = savedToken.ifBlank { envToken }
        val envDevUrl = repository.getEmbeddedDevUrl()
        val savedDevUrl = repository.getSavedDevUrl()
        val initialDevUrl = savedDevUrl.ifBlank { envDevUrl }
        val savedHost = repository.getSavedTargetHost()
        val savedPort = repository.getSavedTargetPort()

        _uiState.update {
            it.copy(
                authtoken = initialToken,
                isAuthtokenEmbeddedFromEnv = initialToken == envToken && envToken.isNotBlank(),
                devUrl = initialDevUrl,
                isDevUrlEmbedded = initialDevUrl == envDevUrl,
                targetHost = savedHost,
                targetPort = savedPort,
                targetPortInput = savedPort.toString()
            )
        }

        repository.saveTarget(savedHost, savedPort)
        repository.saveDevUrl(initialDevUrl)
        probeCurrentTarget()

        // Telemetry loop
        viewModelScope.launch {
            while (isActive) {
                refreshTelemetry()
                delay(1200)
            }
        }

        // Port health checker loop
        viewModelScope.launch {
            while (isActive) {
                probeCurrentTarget()
                delay(4000)
            }
        }

        // Real Samsung DeX & Desktop mode observation
        viewModelScope.launch {
            SamsungDexManager.dexState.collect { dex ->
                _uiState.update { it.copy(dexState = dex) }
            }
        }
    }

    fun onAuthtokenChanged(newToken: String) {
        val envToken = repository.getEmbeddedEnvToken()
        _uiState.update {
            it.copy(
                authtoken = newToken,
                isAuthtokenEmbeddedFromEnv = newToken == envToken && envToken.isNotBlank()
            )
        }
        repository.saveAuthtoken(newToken)
    }

    fun toggleAuthtokenVisibility() {
        _uiState.update { it.copy(isAuthtokenVisible = !it.isAuthtokenVisible) }
    }

    fun onDevUrlChanged(newUrl: String) {
        val envDevUrl = repository.getEmbeddedDevUrl()
        _uiState.update {
            it.copy(
                devUrl = newUrl,
                isDevUrlEmbedded = newUrl == envDevUrl
            )
        }
        repository.saveDevUrl(newUrl)
    }

    fun resetDevUrlToDefault() {
        val envDevUrl = repository.getEmbeddedDevUrl()
        onDevUrlChanged(envDevUrl)
    }

    fun onTargetHostChanged(newHost: String) {
        _uiState.update { it.copy(targetHost = newHost) }
        repository.saveTarget(newHost, _uiState.value.targetPort)
        debounceProbe()
    }

    fun onTargetPortInputChanged(newPortInput: String) {
        val digitsOnly = newPortInput.filter { it.isDigit() }.take(5)
        _uiState.update { it.copy(targetPortInput = digitsOnly) }

        val portNum = digitsOnly.toIntOrNull()
        if (portNum != null && portNum in 1..65535) {
            _uiState.update { it.copy(targetPort = portNum) }
            repository.saveTarget(_uiState.value.targetHost, portNum)
            debounceProbe()
        }
    }

    fun selectPortPreset(presetPort: Int) {
        _uiState.update {
            it.copy(
                targetPort = presetPort,
                targetPortInput = presetPort.toString()
            )
        }
        repository.saveTarget(_uiState.value.targetHost, presetPort)
        probeCurrentTarget()
    }

    private fun debounceProbe() {
        probeJob?.cancel()
        probeJob = viewModelScope.launch {
            delay(500)
            probeCurrentTarget()
        }
    }

    fun probeCurrentTarget() {
        viewModelScope.launch {
            val host = _uiState.value.targetHost.trim().ifBlank { "127.0.0.1" }
            val port = _uiState.value.targetPort
            val (status, message) = repository.probePort(host, port)
            _uiState.update {
                it.copy(
                    portStatus = status,
                    portStatusMessage = message
                )
            }

            // If active and listening, attempt Codex Proxy discovery
            if (status == PortStatus.LISTENING) {
                val codex = repository.discoverCodexProxy(host, port)
                _uiState.update {
                    it.copy(codexInfo = codex ?: CodexProxyInfo(isDetected = false))
                }
            } else {
                _uiState.update {
                    it.copy(codexInfo = CodexProxyInfo(isDetected = false))
                }
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
                    _uiState.value.targetHost.trim().ifBlank { "127.0.0.1" },
                    _uiState.value.targetPort,
                    _uiState.value.authtoken.trim(),
                    _uiState.value.devUrl.trim().ifBlank { repository.getEmbeddedDevUrl() }
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
        val poolMetrics = repository.getPoolMetrics()
        val (rawStdout, jsonLogs) = repository.getStdoutJsonLogs(50)
        val publicUrl = if (currentTunnelStatus == TunnelStatus.ONLINE) repository.getPublicUrl() else null

        val err = repository.getLastError()
        _uiState.update {
            it.copy(
                tunnelStatus = if (it.tunnelStatus == TunnelStatus.CONNECTING && currentTunnelStatus == TunnelStatus.STOPPED) {
                    TunnelStatus.CONNECTING
                } else {
                    currentTunnelStatus
                },
                publicUrl = if (currentTunnelStatus == TunnelStatus.ONLINE) (publicUrl ?: it.publicUrl) else null,
                errorMessage = if (currentTunnelStatus == TunnelStatus.ERROR) (err.ifBlank { it.errorMessage }) else it.errorMessage,
                metrics = metrics,
                poolMetrics = poolMetrics,
                rawStdoutString = rawStdout,
                jsonLogs = jsonLogs
            )
        }
    }

    fun onJsonSearchQueryChanged(query: String) {
        _uiState.update { it.copy(jsonSearchQuery = query) }
    }

    fun selectJsonLog(log: JsonLogEntry?) {
        _uiState.update { it.copy(selectedJsonLog = log) }
    }

    fun runCodexTest(testType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingTestPing = true, testPingResult = null) }
            val (title, result) = repository.executeCodexTest(
                _uiState.value.targetHost,
                _uiState.value.targetPort,
                testType
            )
            _uiState.update {
                it.copy(
                    isSendingTestPing = false,
                    testPingTitle = title,
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
