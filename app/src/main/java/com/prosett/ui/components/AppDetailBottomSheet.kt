package com.prosett.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prosett.data.model.InstalledApp
import com.prosett.ui.theme.StatusAllowed
import com.prosett.ui.theme.StatusBlocked
import com.prosett.ui.theme.StatusRestricted
import com.prosett.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: InstalledApp,
    isRecentlyStopped: Boolean = false,
    onDismiss: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
    onToggleBoth: () -> Unit,
    onToggleBackground: () -> Unit,
    onKillProcess: () -> Unit,
    onRefreshProcess: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onLaunchApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("app_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    packageName = app.packageName,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v${app.versionName} • UID ${app.uid} • ${if (app.isSystemApp) "System App" else "User App"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sheet"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Network Control Card
            Text(
                text = "NETWORK ACCESS CONTROL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Wi-Fi Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Wi-Fi",
                                tint = if (app.isWifiBlocked) StatusBlocked else StatusAllowed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Wi-Fi Access",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (app.isWifiBlocked) "Blocked on Wi-Fi" else "Allowed on Wi-Fi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (app.isWifiBlocked) StatusBlocked else StatusAllowed
                                )
                            }
                        }

                        Switch(
                            checked = !app.isWifiBlocked,
                            onCheckedChange = { onToggleWifi() },
                            modifier = Modifier.testTag("detail_toggle_wifi"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusAllowed,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = StatusBlocked
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mobile Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = "Cellular",
                                tint = if (app.isMobileBlocked) StatusBlocked else StatusAllowed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mobile Cellular Data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (app.isMobileBlocked) "Blocked on Mobile" else "Allowed on Mobile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (app.isMobileBlocked) StatusBlocked else StatusAllowed
                                )
                            }
                        }

                        Switch(
                            checked = !app.isMobileBlocked,
                            onCheckedChange = { onToggleMobile() },
                            modifier = Modifier.testTag("detail_toggle_mobile"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = StatusAllowed,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = StatusBlocked
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Toggle Both
                    OutlinedButton(
                        onClick = onToggleBoth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_toggle_both_network"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Toggle Both",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (app.isBothNetworkBlocked) "Restore All Network Access" else "Block Both (Wi-Fi + Mobile)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Background Activity Control Card
            Text(
                text = "BACKGROUND ACTIVITY CONTROL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Background Activity",
                                tint = if (app.isBackgroundRestricted) StatusRestricted else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Background Activity",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (app.isBackgroundRestricted) "Turned OFF (Restricted)" else "Turned ON (Allowed)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (app.isBackgroundRestricted) StatusRestricted else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Switch(
                            checked = !app.isBackgroundRestricted,
                            onCheckedChange = { onToggleBackground() },
                            modifier = Modifier.testTag("detail_toggle_background"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = StatusRestricted
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "When turned OFF, background processes are terminated and prevented from executing tasks when the app is not in the foreground.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val buttonColor = if (isRecentlyStopped) StatusAllowed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                    val iconTint = if (isRecentlyStopped) StatusAllowed else MaterialTheme.colorScheme.onErrorContainer
                    val iconVector = if (isRecentlyStopped) Icons.Default.CheckCircle else Icons.Default.FlashOn
                    val buttonText = if (isRecentlyStopped) "Refresh Process State" else "Stop Background Process Now"

                    Button(
                        onClick = if (isRecentlyStopped) onRefreshProcess else onKillProcess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_kill_process_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = iconTint
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = if (isRecentlyStopped) "Refresh Process State" else "Stop Now",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(buttonText, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // System Actions
            Text(
                text = "SYSTEM ACTIONS & SHORTCUTS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_app_settings_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "App Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("App Info", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_battery_settings_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Battery Settings",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Battery", fontSize = 12.sp)
                }

                Button(
                    onClick = onLaunchApp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_app_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = "Launch",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
