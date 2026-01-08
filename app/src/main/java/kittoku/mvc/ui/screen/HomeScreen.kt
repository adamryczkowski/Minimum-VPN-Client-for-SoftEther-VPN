package kittoku.mvc.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kittoku.mvc.R
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStats
import kittoku.mvc.ui.theme.VpnConnected
import kittoku.mvc.ui.theme.VpnConnecting
import kittoku.mvc.ui.theme.VpnDisconnected
import kittoku.mvc.ui.theme.VpnError
import kittoku.mvc.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val connectionState by viewModel.connectionState.collectAsState()

    HomeScreenContent(
        connectionState = connectionState,
        onConnectClick = {
            viewModel.onConnectionToggled(true)
            onConnectClick()
        },
        onDisconnectClick = {
            viewModel.onConnectionToggled(false)
            onDisconnectClick()
        },
        modifier = modifier,
    )
}

@Composable
fun HomeScreenContent(
    connectionState: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Connection status indicator
        ConnectionStatusIndicator(
            connectionState = connectionState,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        // Status text
        Text(
            text = getStatusText(connectionState),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        // Status description
        Text(
            text = getStatusDescription(connectionState),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        // Connect/Disconnect button
        ConnectionButton(
            connectionState = connectionState,
            onConnectClick = onConnectClick,
            onDisconnectClick = onDisconnectClick,
        )

        // Connection stats (when connected)
        if (connectionState is ConnectionState.Connected) {
            Spacer(modifier = Modifier.height(32.dp))
            ConnectionStatsCard(stats = connectionState.stats)
        }
    }
}

@Composable
private fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val statusColor =
        when (connectionState) {
            is ConnectionState.Connected -> VpnConnected
            is ConnectionState.Connecting -> VpnConnecting
            is ConnectionState.Disconnecting -> VpnConnecting
            is ConnectionState.Error -> VpnError
            is ConnectionState.Disconnected -> VpnDisconnected
        }

    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(durationMillis = 300),
        label = "statusColor",
    )

    val scale by animateFloatAsState(
        targetValue = if (connectionState is ConnectionState.Connecting) 1.1f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "scale",
    )

    Box(
        modifier =
            modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    color = animatedColor.copy(alpha = 0.2f),
                    shape = CircleShape,
                )
                .semantics {
                    contentDescription = "Connection status: ${getStatusText(connectionState)}"
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .background(
                        color = animatedColor,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            when (connectionState) {
                is ConnectionState.Connected -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                is ConnectionState.Connecting,
                is ConnectionState.Disconnecting,
                -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                    )
                }
                is ConnectionState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                is ConnectionState.Disconnected -> {
                    // Empty circle for disconnected state
                }
            }
        }
    }
}

@Composable
private fun ConnectionButton(
    connectionState: ConnectionState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting
    val isDisconnecting = connectionState is ConnectionState.Disconnecting

    when {
        isConnected -> {
            OutlinedButton(
                onClick = onDisconnectClick,
                modifier =
                    modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(stringResource(R.string.disconnect))
            }
        }
        isConnecting || isDisconnecting -> {
            Button(
                onClick = { },
                enabled = false,
                modifier =
                    modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        else -> {
            Button(
                onClick = onConnectClick,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = VpnConnected,
                    ),
                modifier =
                    modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Text(stringResource(R.string.connect))
            }
        }
    }
}

@Composable
private fun ConnectionStatsCard(
    stats: ConnectionStats,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.connection_statistics),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            stats.assignedIpAddress?.let { ip ->
                StatRow(
                    label = stringResource(R.string.ip_address),
                    value = ip,
                )
            }

            stats.serverHostname?.let { server ->
                StatRow(
                    label = stringResource(R.string.server),
                    value = server,
                )
            }

            StatRow(
                label = stringResource(R.string.data_sent),
                value = formatBytes(stats.bytesSent),
            )

            StatRow(
                label = stringResource(R.string.data_received),
                value = formatBytes(stats.bytesReceived),
            )

            StatRow(
                label = stringResource(R.string.duration),
                value = formatDuration(stats.durationMs),
            )

            if (stats.isUdpAccelerated) {
                StatRow(
                    label = stringResource(R.string.udp_acceleration),
                    value = stringResource(R.string.enabled),
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun getStatusText(state: ConnectionState): String =
    when (state) {
        is ConnectionState.Connected -> "Connected"
        is ConnectionState.Connecting -> "Connecting"
        is ConnectionState.Disconnecting -> "Disconnecting"
        is ConnectionState.Error -> "Error"
        is ConnectionState.Disconnected -> "Disconnected"
    }

private fun getStatusDescription(state: ConnectionState): String =
    when (state) {
        is ConnectionState.Connected -> "Your connection is secure"
        is ConnectionState.Connecting -> state.step
        is ConnectionState.Disconnecting -> "Please wait..."
        is ConnectionState.Error -> state.message
        is ConnectionState.Disconnected -> "Tap Connect to start VPN"
    }

private fun formatBytes(bytes: Long): String =
    when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60)
        else -> String.format("%d:%02d", minutes, seconds % 60)
    }
}
