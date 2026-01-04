package kittoku.mvc.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kittoku.mvc.R

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Connection Settings
        SettingsSection(title = stringResource(R.string.connection_settings)) {
            SettingsItem(
                title = stringResource(R.string.server_hostname),
                subtitle = stringResource(R.string.server_hostname_description),
                onClick = { /* TODO: Open server hostname dialog */ },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.port_number),
                subtitle = stringResource(R.string.port_number_description),
                onClick = { /* TODO: Open port dialog */ },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.hub_name),
                subtitle = stringResource(R.string.hub_name_description),
                onClick = { /* TODO: Open hub name dialog */ },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.username),
                subtitle = stringResource(R.string.username_description),
                onClick = { /* TODO: Open username dialog */ },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.password),
                subtitle = stringResource(R.string.password_description),
                onClick = { /* TODO: Open password dialog */ },
            )
        }

        // Advanced Settings
        SettingsSection(title = stringResource(R.string.advanced_settings)) {
            var udpAcceleration by remember { mutableStateOf(true) }
            SettingsToggleItem(
                title = stringResource(R.string.udp_acceleration),
                subtitle = stringResource(R.string.udp_acceleration_description),
                checked = udpAcceleration,
                onCheckedChange = { udpAcceleration = it },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.ssl_settings),
                subtitle = stringResource(R.string.ssl_settings_description),
                onClick = { /* TODO: Open SSL settings */ },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.split_tunneling),
                subtitle = stringResource(R.string.split_tunneling_description),
                onClick = { /* TODO: Open split tunneling settings */ },
            )
        }

        // Auto-Connect Settings
        SettingsSection(title = stringResource(R.string.auto_connect_settings)) {
            var autoConnectOnBoot by remember { mutableStateOf(false) }
            SettingsToggleItem(
                title = stringResource(R.string.auto_connect_on_boot),
                subtitle = stringResource(R.string.auto_connect_on_boot_description),
                checked = autoConnectOnBoot,
                onCheckedChange = { autoConnectOnBoot = it },
            )
            HorizontalDivider()
            var autoConnectOnWifi by remember { mutableStateOf(false) }
            SettingsToggleItem(
                title = stringResource(R.string.auto_connect_on_wifi),
                subtitle = stringResource(R.string.auto_connect_on_wifi_description),
                checked = autoConnectOnWifi,
                onCheckedChange = { autoConnectOnWifi = it },
            )
        }

        // Debug Settings
        SettingsSection(title = stringResource(R.string.debug_settings)) {
            var enableLogging by remember { mutableStateOf(false) }
            SettingsToggleItem(
                title = stringResource(R.string.enable_logging),
                subtitle = stringResource(R.string.enable_logging_description),
                checked = enableLogging,
                onCheckedChange = { enableLogging = it },
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.log_directory),
                subtitle = stringResource(R.string.log_directory_description),
                onClick = { /* TODO: Open log directory picker */ },
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
