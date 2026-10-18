package com.example.ngrokforwarder

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwarderScreen(
    viewModel: ForwarderViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ngrok JNI Forwarder",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                        TunnelStatusBadge(state.tunnelStatus)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.probeTargetPort() },
                        modifier = Modifier.testTag("refresh_status_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh target status",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Target Service 127.0.0.1:8080/* Status
            item {
                TargetServiceCard(
                    host = state.targetHost,
                    port = state.targetPort,
                    portStatus = state.portStatus,
                    statusMessage = state.portStatusMessage,
                    onProbeClicked = { viewModel.probeTargetPort() }
                )
            }

            // Section 2: Tunnel Control & Authtoken
            item {
                TunnelControlCard(
                    authtoken = state.authtoken,
                    isAuthtokenVisible = state.isAuthtokenVisible,
                    tunnelStatus = state.tunnelStatus,
                    errorMessage = state.errorMessage,
                    onAuthtokenChanged = { viewModel.onAuthtokenChanged(it) },
                    onToggleVisibility = { viewModel.toggleAuthtokenVisibility() },
                    onToggleTunnel = { viewModel.toggleTunnel() }
                )
            }

            // Section 3: Active Tunnel URL Banner (when online)
            if (state.tunnelStatus == TunnelStatus.ONLINE && state.publicUrl != null) {
                item {
                    PublicUrlCard(
                        publicUrl = state.publicUrl!!,
                        isSendingPing = state.isSendingTestPing,
                        onCopyClicked = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Ngrok Tunnel URL", state.publicUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Public URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onShareClicked = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Ngrok Public URL (127.0.0.1:8080)")
                                putExtra(Intent.EXTRA_TEXT, state.publicUrl)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Tunnel URL"))
                        },
                        onTestPingClicked = {
                            viewModel.sendTestPing("/")
                        }
                    )
                }
            }

            // Section 4: Live Telemetry Metrics
            item {
                TelemetryGrid(metrics = state.metrics)
            }

            // Section 5: Real-time Traffic Inspector
            item {
                TrafficInspectorHeader(
                    eventCount = state.trafficLogs.size,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }

            if (state.trafficLogs.isEmpty()) {
                item {
                    EmptyTrafficCard(isTunnelOnline = state.tunnelStatus == TunnelStatus.ONLINE)
                }
            } else {
                items(
                    items = state.trafficLogs,
                    key = { it.timestamp.toString() + it.path + it.durationMs }
                ) { item ->
                    TrafficItemCard(
                        item = item,
                        onClick = { viewModel.selectTrafficItem(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Detail Bottom Sheet
    if (state.selectedTrafficItem != null) {
        TrafficDetailSheet(
            item = state.selectedTrafficItem!!,
            onDismiss = { viewModel.selectTrafficItem(null) }
        )
    }

    // Test Ping Result Dialog
    if (state.testPingResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTestPingResult() },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Raw Response from 127.0.0.1:8080",
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
                TextButton(onClick = { viewModel.dismissTestPingResult() }) {
                    Text("Close", color = NeonCyan)
                }
            }
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
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TargetServiceCard(
    host: String,
    port: Int,
    portStatus: PortStatus,
    statusMessage: String,
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
                        text = "TARGET LOCAL SERVICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$host:$port/*",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = onProbeClicked,
                    modifier = Modifier.testTag("probe_port_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan
                    ),
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkOutline, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Port Status Indicator
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

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Native C++ bridge proxies all incoming requests directly into this loopback socket.",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun TunnelControlCard(
    authtoken: String,
    isAuthtokenVisible: Boolean,
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
            Text(
                text = "NGROK CREDENTIALS & ENGINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = authtoken,
                onValueChange = onAuthtokenChanged,
                label = { Text("Ngrok Authtoken") },
                placeholder = { Text("Paste your ngrok token here...") },
                singleLine = true,
                visualTransformation = if (isAuthtokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector = if (isAuthtokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle token visibility",
                                tint = TextSecondary
                            )
                        }
                        if (authtoken.isNotBlank()) {
                            IconButton(onClick = { onAuthtokenChanged("") }) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            val isOnline = tunnelStatus == TunnelStatus.ONLINE
            val isConnecting = tunnelStatus == TunnelStatus.CONNECTING

            Button(
                onClick = onToggleTunnel,
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("toggle_tunnel_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOnline) CoralRed else NeonCyan,
                    contentColor = if (isOnline) Color.White else Color(0xFF00363D)
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF00363D),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Starting C++ JNI Forwarder...", fontWeight = FontWeight.Bold)
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
fun PublicUrlCard(
    publicUrl: String,
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
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(EmeraldGreen, CircleShape)
                )
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
                        .testTag("copy_url_button"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onShareClicked,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_url_button"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp)
                }

                Button(
                    onClick = onTestPingClicked,
                    enabled = !isSendingPing,
                    modifier = Modifier
                        .weight(1.3f)
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
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Test Ping", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ping 8080", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TelemetryGrid(metrics: ForwarderMetrics) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SOCKET TELEMETRY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                title = "Total Requests",
                value = metrics.totalRequests.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Active Sockets",
                value = metrics.activeConnections.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                title = "Data In (RX)",
                value = formatBytes(metrics.bytesIn),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Data Out (TX)",
                value = formatBytes(metrics.bytesOut),
                modifier = Modifier.weight(1f)
            )
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
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun TrafficInspectorHeader(
    eventCount: Int,
    onClearLogs: () -> Unit
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
            Text(
                text = "TRAFFIC INSPECTOR",
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
                Text(
                    text = eventCount.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        if (eventCount > 0) {
            TextButton(
                onClick = onClearLogs,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Clear", fontSize = 12.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun EmptyTrafficCard(isTunnelOnline: Boolean) {
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
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isTunnelOnline) "Waiting for incoming traffic..." else "Tunnel is offline",
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Requests forwarded to 127.0.0.1:8080/* will appear here in real time.",
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TrafficItemCard(
    item: TrafficItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("traffic_item_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Method Badge
                MethodBadge(item.method)

                Column {
                    Text(
                        text = item.path,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatTime(item.timestamp)} • ${item.durationMs}ms • ${item.clientIp}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Status Badge
            StatusCodeBadge(item.statusCode)
        }
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = statusCode.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficDetailSheet(
    item: TrafficItem,
    onDismiss: () -> Unit
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
                    text = "Request Transaction Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkOutline)
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Endpoint Path", value = item.path, isMonospace = true)
            DetailRow(label = "HTTP Method", value = item.method, isMonospace = true)
            DetailRow(label = "Status Code", value = "${item.statusCode} (${item.statusMessage})", isMonospace = true)
            DetailRow(label = "Duration", value = "${item.durationMs} ms")
            DetailRow(label = "Payload In", value = formatBytes(item.bytesReceived.toLong()))
            DetailRow(label = "Payload Out", value = formatBytes(item.bytesSent.toLong()))
            DetailRow(label = "Client IP", value = item.clientIp, isMonospace = true)
            DetailRow(label = "Timestamp", value = formatFullTime(item.timestamp))

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatFullTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(millis))
}
