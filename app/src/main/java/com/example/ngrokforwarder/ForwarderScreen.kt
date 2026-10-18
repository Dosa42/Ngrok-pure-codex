package com.example.ngrokforwarder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberYellow
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwarderScreen(
    viewModel: ForwarderViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isDesktopDeX = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ngrok Forwarder",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                            TunnelStatusBadge(state.tunnelStatus)
                            if (isDesktopDeX) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF1E1B4B), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DesktopWindows,
                                            contentDescription = null,
                                            tint = Color(0xFFA5B4FC),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "DeX Desktop",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC7D2FE)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "POOL: ON",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                        IconButton(
                            onClick = { viewModel.probeCurrentTarget() },
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon.Hand)
                                .testTag("refresh_probe_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Probe Socket",
                                tint = NeonCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            }
        ) { innerPadding ->
            val filteredLogs = if (state.jsonSearchQuery.isBlank()) {
                state.jsonLogs
            } else {
                state.jsonLogs.filter {
                    it.path.contains(state.jsonSearchQuery, ignoreCase = true) ||
                            it.method.contains(state.jsonSearchQuery, ignoreCase = true) ||
                            it.protocol.contains(state.jsonSearchQuery, ignoreCase = true) ||
                            it.rawJson.contains(state.jsonSearchQuery, ignoreCase = true)
                }
            }

            if (isDesktopDeX) {
                // Expanded Dual-Pane DevTools Layout for Samsung DeX
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Control Pane (Fixed width ~ 400dp)
                    Column(
                        modifier = Modifier
                            .width(400.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        DynamicTargetServiceCard(
                            host = state.targetHost,
                            portInput = state.targetPortInput,
                            activePort = state.targetPort,
                            portStatus = state.portStatus,
                            statusMessage = state.portStatusMessage,
                            onHostChanged = { viewModel.onTargetHostChanged(it) },
                            onPortChanged = { viewModel.onTargetPortInputChanged(it) },
                            onPresetSelected = { viewModel.selectPortPreset(it) },
                            onProbeClicked = { viewModel.probeCurrentTarget() }
                        )

                        if (state.codexInfo.isDetected) {
                            CodexProxyBannerCard(
                                codexInfo = state.codexInfo,
                                onTestClicked = { viewModel.runCodexTest(it) }
                            )
                        }

                        CodexTestSuiteCard(
                            isSendingPing = state.isSendingTestPing,
                            targetPort = state.targetPort,
                            onTestTriggered = { viewModel.runCodexTest(it) }
                        )

                        CredentialsCard(
                            authtoken = state.authtoken,
                            isAuthtokenVisible = state.isAuthtokenVisible,
                            isEmbeddedFromEnv = state.isAuthtokenEmbeddedFromEnv,
                            tunnelStatus = state.tunnelStatus,
                            errorMessage = state.errorMessage,
                            onAuthtokenChanged = { viewModel.onAuthtokenChanged(it) },
                            onToggleVisibility = { viewModel.toggleAuthtokenVisibility() },
                            onToggleTunnel = { viewModel.toggleTunnel() }
                        )

                        if (state.tunnelStatus == TunnelStatus.ONLINE && state.publicUrl != null) {
                            PublicTunnelUrlCard(
                                publicUrl = state.publicUrl!!,
                                targetHost = state.targetHost,
                                targetPort = state.targetPort,
                                isSendingPing = state.isSendingTestPing,
                                onCopyClicked = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Ngrok URL", state.publicUrl))
                                    Toast.makeText(context, "Public URL copied", Toast.LENGTH_SHORT).show()
                                },
                                onShareClicked = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, state.publicUrl)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Tunnel URL"))
                                },
                                onTestPingClicked = { viewModel.runCodexTest("CHAT") }
                            )
                        }

                        TelemetryAndPoolSection(
                            metrics = state.metrics,
                            poolMetrics = state.poolMetrics
                        )
                    }

                    VerticalDivider(color = DarkOutline, thickness = 1.dp)

                    // Right Live Terminal Pane (Flex 1)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        JsonTerminalHeader(
                            eventCount = state.jsonLogs.size,
                            searchQuery = state.jsonSearchQuery,
                            onSearchQueryChanged = { viewModel.onJsonSearchQueryChanged(it) },
                            onCopyFullStdout = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Ngrok Stdout Logs", state.rawStdoutString))
                                Toast.makeText(context, "Full stdout JSON copied", Toast.LENGTH_SHORT).show()
                            },
                            onExportStdout = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, state.rawStdoutString)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Stdout JSON"))
                            }
                        )

                        if (filteredLogs.isEmpty()) {
                            EmptyJsonTerminalCard(isTunnelOnline = state.tunnelStatus == TunnelStatus.ONLINE)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = filteredLogs,
                                    key = { it.timestamp.toString() + it.path + it.durationMs }
                                ) { logItem ->
                                    JsonLogCard(
                                        log = logItem,
                                        onClick = { viewModel.selectJsonLog(logItem) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Compact / Mobile Layout
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        DynamicTargetServiceCard(
                            host = state.targetHost,
                            portInput = state.targetPortInput,
                            activePort = state.targetPort,
                            portStatus = state.portStatus,
                            statusMessage = state.portStatusMessage,
                            onHostChanged = { viewModel.onTargetHostChanged(it) },
                            onPortChanged = { viewModel.onTargetPortInputChanged(it) },
                            onPresetSelected = { viewModel.selectPortPreset(it) },
                            onProbeClicked = { viewModel.probeCurrentTarget() }
                        )
                    }

                    if (state.codexInfo.isDetected) {
                        item {
                            CodexProxyBannerCard(
                                codexInfo = state.codexInfo,
                                onTestClicked = { viewModel.runCodexTest(it) }
                            )
                        }
                    }

                    item {
                        CodexTestSuiteCard(
                            isSendingPing = state.isSendingTestPing,
                            targetPort = state.targetPort,
                            onTestTriggered = { viewModel.runCodexTest(it) }
                        )
                    }

                    item {
                        CredentialsCard(
                            authtoken = state.authtoken,
                            isAuthtokenVisible = state.isAuthtokenVisible,
                            isEmbeddedFromEnv = state.isAuthtokenEmbeddedFromEnv,
                            tunnelStatus = state.tunnelStatus,
                            errorMessage = state.errorMessage,
                            onAuthtokenChanged = { viewModel.onAuthtokenChanged(it) },
                            onToggleVisibility = { viewModel.toggleAuthtokenVisibility() },
                            onToggleTunnel = { viewModel.toggleTunnel() }
                        )
                    }

                    if (state.tunnelStatus == TunnelStatus.ONLINE && state.publicUrl != null) {
                        item {
                            PublicTunnelUrlCard(
                                publicUrl = state.publicUrl!!,
                                targetHost = state.targetHost,
                                targetPort = state.targetPort,
                                isSendingPing = state.isSendingTestPing,
                                onCopyClicked = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Ngrok URL", state.publicUrl))
                                    Toast.makeText(context, "Public URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onShareClicked = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Ngrok Tunnel URL")
                                        putExtra(Intent.EXTRA_TEXT, state.publicUrl)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Tunnel URL"))
                                },
                                onTestPingClicked = { viewModel.runCodexTest("CHAT") }
                            )
                        }
                    }

                    item {
                        TelemetryAndPoolSection(
                            metrics = state.metrics,
                            poolMetrics = state.poolMetrics
                        )
                    }

                    item {
                        JsonTerminalHeader(
                            eventCount = state.jsonLogs.size,
                            searchQuery = state.jsonSearchQuery,
                            onSearchQueryChanged = { viewModel.onJsonSearchQueryChanged(it) },
                            onCopyFullStdout = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Ngrok Stdout Logs", state.rawStdoutString))
                                Toast.makeText(context, "Full stdout JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onExportStdout = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "Ngrok Forwarder Stdout JSON Logs")
                                    putExtra(Intent.EXTRA_TEXT, state.rawStdoutString)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Stdout JSON"))
                            }
                        )
                    }

                    if (filteredLogs.isEmpty()) {
                        item {
                            EmptyJsonTerminalCard(isTunnelOnline = state.tunnelStatus == TunnelStatus.ONLINE)
                        }
                    } else {
                        items(
                            items = filteredLogs,
                            key = { it.timestamp.toString() + it.path + it.durationMs }
                        ) { logItem ->
                            JsonLogCard(
                                log = logItem,
                                onClick = { viewModel.selectJsonLog(logItem) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (state.selectedJsonLog != null) {
        JsonLogDetailSheet(
            log = state.selectedJsonLog!!,
            onDismiss = { viewModel.selectJsonLog(null) },
            onCopySingleJson = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Forwarded JSON", state.selectedJsonLog!!.rawJson))
                Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (state.testPingResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTestPingResult() },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = state.testPingTitle,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.testPingResult!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (state.testPingResult!!.contains("502") || state.testPingResult!!.contains("Refused")) CoralRed else EmeraldGreen
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissTestPingResult() },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
fun CodexProxyBannerCard(
    codexInfo: CodexProxyInfo,
    onTestClicked: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172C)),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF818CF8))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "CODEX PROXY DETECTED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC),
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF312E81), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${codexInfo.modelCount} Models Online",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC7D2FE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Default Model: ${codexInfo.defaultModel} • Manifest: v${codexInfo.version}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )

            if (codexInfo.models.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    codexInfo.models.forEach { modelId ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E1B4B), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = modelId,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE0E7FF)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodexTestSuiteCard(
    isSendingPing: Boolean,
    targetPort: Int,
    onTestTriggered: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PRE-CONFIGURED ENDPOINT TESTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestChip(
                    label = "GET /host.json",
                    enabled = !isSendingPing,
                    onClick = { onTestTriggered("MANIFEST") }
                )
                TestChip(
                    label = "GET /v1/models",
                    enabled = !isSendingPing,
                    onClick = { onTestTriggered("MODELS") }
                )
                TestChip(
                    label = "POST /v1/chat/completions",
                    enabled = !isSendingPing,
                    onClick = { onTestTriggered("CHAT") }
                )
                TestChip(
                    label = "GET /v1/models/catalog",
                    enabled = !isSendingPing,
                    onClick = { onTestTriggered("CATALOG") }
                )
            }
        }
    }
}

@Composable
fun TestChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceVariant,
            contentColor = NeonCyan
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DynamicTargetServiceCard(
    host: String,
    portInput: String,
    activePort: Int,
    portStatus: PortStatus,
    statusMessage: String,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onProbeClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC TARGET LOCAL SERVICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$host:$activePort/*",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onProbeClicked,
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("probe_port_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Probe Socket",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probe", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChanged,
                    label = { Text("Target Host") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("target_host_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = onPortChanged,
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("target_port_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkOutline,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Quick Port Presets:", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(8080, 3000, 8000, 5000, 8888)
                presets.forEach { preset ->
                    val isSelected = activePort == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(":$preset", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) NeonCyan else DarkOutline,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val (statusColor, statusIcon) = when (portStatus) {
                PortStatus.LISTENING -> EmeraldGreen to Icons.Default.CheckCircle
                PortStatus.REFUSED -> CoralRed to Icons.Default.Error
                PortStatus.ERROR -> CoralRed to Icons.Default.Warning
                PortStatus.CHECKING -> AmberYellow to Icons.Default.Refresh
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = statusMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun CredentialsCard(
    authtoken: String,
    isAuthtokenVisible: Boolean,
    isEmbeddedFromEnv: Boolean,
    tunnelStatus: TunnelStatus,
    errorMessage: String?,
    onAuthtokenChanged: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleTunnel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NGROK CREDENTIALS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                if (isEmbeddedFromEnv) {
                    Box(
                        modifier = Modifier
                            .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                            Text(text = "Embedded from Environment", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = authtoken,
                onValueChange = onAuthtokenChanged,
                label = { Text("Ngrok Authtoken") },
                placeholder = { Text("Paste token or use embedded environment token") },
                singleLine = true,
                visualTransformation = if (isAuthtokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleVisibility, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                            Icon(
                                imageVector = if (isAuthtokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle token visibility",
                                tint = TextSecondary
                            )
                        }
                        if (authtoken.isNotBlank()) {
                            IconButton(onClick = { onAuthtokenChanged("") }, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear token",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("authtoken_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkOutline,
                    focusedLabelColor = NeonCyan,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonCyan,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )

            AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                errorMessage?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(CoralRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, CoralRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = it, color = CoralRed, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isOnline = tunnelStatus == TunnelStatus.ONLINE
            val isConnecting = tunnelStatus == TunnelStatus.CONNECTING

            Button(
                onClick = onToggleTunnel,
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .testTag("toggle_tunnel_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOnline) CoralRed else NeonCyan,
                    contentColor = if (isOnline) Color.White else Color(0xFF00363D)
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF00363D), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Starting Pooled Forwarder...", fontWeight = FontWeight.Bold)
                } else if (isOnline) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop In-Process Forwarder", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start In-Process Forwarder", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PublicTunnelUrlCard(
    publicUrl: String,
    targetHost: String,
    targetPort: Int,
    isSendingPing: Boolean,
    onCopyClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onTestPingClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).background(EmeraldGreen, CircleShape))
                Text(
                    text = "PUBLIC TUNNEL ENDPOINT ACTIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = publicUrl,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyClicked,
                    modifier = Modifier
                        .weight(1f)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("copy_url_button"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)
                    )
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onShareClicked,
                    modifier = Modifier
                        .weight(1f)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("share_url_button"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp)
                }

                Button(
                    onClick = onTestPingClicked,
                    enabled = !isSendingPing,
                    modifier = Modifier
                        .weight(1.3f)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("test_ping_button"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = NeonCyan
                    )
                ) {
                    if (isSendingPing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = NeonCyan)
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Ping", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test AI Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TelemetryAndPoolSection(
    metrics: ForwarderMetrics,
    poolMetrics: PoolMetrics
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KEEP-ALIVE SOCKET POOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Max Idle: ${poolMetrics.maxIdle}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PoolStatChip(label = "Idle Sockets", value = poolMetrics.idleCount.toString(), modifier = Modifier.weight(1f))
                    PoolStatChip(label = "Pool Hits", value = poolMetrics.poolHits.toString(), color = EmeraldGreen, modifier = Modifier.weight(1f))
                    PoolStatChip(label = "Pool Misses", value = poolMetrics.poolMisses.toString(), modifier = Modifier.weight(1f))
                    PoolStatChip(label = "Active Sockets", value = poolMetrics.activeConnections.toString(), color = NeonCyan, modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(title = "Total Requests", value = metrics.totalRequests.toString(), modifier = Modifier.weight(1f))
            MetricTile(title = "Data In (RX)", value = formatBytes(metrics.bytesIn), modifier = Modifier.weight(1f))
            MetricTile(title = "Data Out (TX)", value = formatBytes(metrics.bytesOut), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun PoolStatChip(
    label: String,
    value: String,
    color: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(text = label, fontSize = 9.sp, color = TextMuted)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = color)
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 10.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TextPrimary)
        }
    }
}

@Composable
fun JsonTerminalHeader(
    eventCount: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCopyFullStdout: () -> Unit,
    onExportStdout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Text(
                    text = "JSON FORWARDING TERMINAL (STDOUT)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
                        .border(1.dp, DarkOutline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = eventCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onCopyFullStdout,
                    modifier = Modifier
                        .size(32.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("copy_json_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy JSON", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onExportStdout,
                    modifier = Modifier
                        .size(32.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .testTag("export_stdout_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export Stdout", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Filter JSON by protocol, path, status...", fontSize = 12.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("json_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkOutline,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun EmptyJsonTerminalCard(isTunnelOnline: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isTunnelOnline) "Listening on tunnel... stdout JSON streams here" else "Forwarder is stopped",
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Each request relayed through the C++ socket pool outputs plain JSON to stdout.",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun JsonLogCard(
    log: JsonLogEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .testTag("json_log_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MethodBadge(log.method)
                    ProtocolBadge(log.protocol)
                    Text(
                        text = log.path,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (log.pooled) {
                        Box(
                            modifier = Modifier
                                .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(text = "POOLED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                    }
                    StatusCodeBadge(log.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(6.dp))
                    .border(1.dp, DarkOutline.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = log.rawJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (log.status >= 400) CoralRed else TextSecondary,
                    maxLines = 6
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Target: ${log.target}", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text(text = "${log.durationMs}ms • ${formatBytes(log.bytesIn + log.bytesOut)}", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun ProtocolBadge(protocol: String) {
    val (color, label) = when (protocol) {
        "OPENAI_CHAT" -> NeonCyan to "OPENAI"
        "CODEX_RESPONSES" -> Color(0xFFC084FC) to "CODEX"
        "ANTHROPIC_MESSAGES" -> Color(0xFFFDBA74) to "ANTHROPIC"
        "GEMINI_GENERATE" -> EmeraldGreen to "GEMINI"
        "MODELS_DISCOVERY" -> Color(0xFF38BDF8) to "MODELS"
        "HOST_MANIFEST" -> Color(0xFF2DD4BF) to "MANIFEST"
        else -> TextSecondary to "HTTP"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun MethodBadge(method: String) {
    val color = when (method.uppercase()) {
        "GET" -> NeonCyan
        "POST" -> EmeraldGreen
        "PUT" -> AmberYellow
        "DELETE" -> CoralRed
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = method.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun StatusCodeBadge(statusCode: Int) {
    val color = when {
        statusCode in 200..299 -> EmeraldGreen
        statusCode in 300..399 -> NeonCyan
        statusCode in 400..499 -> AmberYellow
        else -> CoralRed
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = statusCode.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun TunnelStatusBadge(status: TunnelStatus) {
    val (color, label) = when (status) {
        TunnelStatus.ONLINE -> EmeraldGreen to "ONLINE"
        TunnelStatus.CONNECTING -> AmberYellow to "CONNECTING"
        TunnelStatus.ERROR -> CoralRed to "ERROR"
        TunnelStatus.STOPPED -> TextMuted to "OFFLINE"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonLogDetailSheet(
    log: JsonLogEntry,
    onDismiss: () -> Unit,
    onCopySingleJson: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Stdout JSON Transaction",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkOutline)
            Spacer(modifier = Modifier.height(12.dp))

            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, DarkOutline, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = log.rawJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCopySingleJson,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerHoverIcon(PointerIcon.Hand),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color(0xFF00363D))
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy JSON Payload", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}
