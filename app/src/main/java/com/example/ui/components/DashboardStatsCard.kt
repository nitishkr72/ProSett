package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusAllowed
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusRestricted
import com.example.ui.theme.StatusWarning

@Composable
fun DashboardStatsCard(
    isVpnActive: Boolean,
    totalApps: Int,
    wifiBlockedCount: Int,
    mobileBlockedCount: Int,
    bgRestrictedCount: Int,
    onToggleVpn: (Boolean) -> Unit,
    onKillAllRestricted: () -> Unit,
    onBatchWifiToggle: (Boolean) -> Unit,
    onBatchMobileToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBatchActions by remember { mutableStateOf(false) }

    val statusBgColor by animateColorAsState(
        targetValue = if (isVpnActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        label = "statusBg"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_stats_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = statusBgColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Master Firewall Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVpnActive) StatusAllowed.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVpnActive) Icons.Default.Shield else Icons.Default.Security,
                            contentDescription = "Firewall Shield",
                            tint = if (isVpnActive) StatusAllowed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Network Firewall",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isVpnActive) StatusAllowed else StatusBlocked)
                            )
                        }
                        Text(
                            text = if (isVpnActive) "Active • Enforcing Rules" else "Disabled • Traffic Allowed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isVpnActive,
                    onCheckedChange = onToggleVpn,
                    modifier = Modifier.testTag("master_vpn_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StatusAllowed,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.Wifi,
                    label = "Wi-Fi Cut",
                    count = wifiBlockedCount,
                    accentColor = if (wifiBlockedCount > 0) StatusBlocked else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.SignalCellularAlt,
                    label = "Cell Cut",
                    count = mobileBlockedCount,
                    accentColor = if (mobileBlockedCount > 0) StatusBlocked else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.PowerSettingsNew,
                    label = "BG Restricted",
                    count = bgRestrictedCount,
                    accentColor = if (bgRestrictedCount > 0) StatusRestricted else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fast Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onKillAllRestricted,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kill_all_restricted_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bgRestrictedCount > 0) StatusRestricted else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Stop background",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bgRestrictedCount > 0) "Stop Restricted ($bgRestrictedCount)" else "Stop BG Apps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = { showBatchActions = !showBatchActions },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("toggle_batch_actions_button")
                ) {
                    Text(
                        text = if (showBatchActions) "Hide Batch" else "Quick Rules",
                        fontSize = 13.sp
                    )
                }
            }

            // Expandable Batch Actions
            AnimatedVisibility(visible = showBatchActions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Quick Batch Operations",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onBatchWifiToggle(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cut All Wi-Fi", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onBatchWifiToggle(false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Allow All Wi-Fi", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onBatchMobileToggle(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cut All Cellular", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onBatchMobileToggle(false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Allow Cellular", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
