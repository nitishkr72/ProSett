package com.prosett.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prosett.data.model.InstalledApp
import com.prosett.ui.theme.StatusAllowed
import com.prosett.ui.theme.StatusBlocked
import com.prosett.ui.theme.StatusRestricted
import com.prosett.ui.theme.StatusWarning

@Composable
fun AppItemCard(
    app: InstalledApp,
    isRecentlyStopped: Boolean = false,
    onToggleWifi: (InstalledApp) -> Unit,
    onToggleMobile: (InstalledApp) -> Unit,
    onToggleBackground: (InstalledApp) -> Unit,
    onKillProcess: (InstalledApp) -> Unit,
    onRefreshProcess: (InstalledApp) -> Unit = {},
    onAppClick: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnyBlocked = app.isWifiBlocked || app.isMobileBlocked || app.isBackgroundRestricted

    val borderColor by animateColorAsState(
        targetValue = when {
            app.isBothNetworkBlocked -> StatusBlocked.copy(alpha = 0.6f)
            app.isWifiBlocked || app.isMobileBlocked -> StatusWarning.copy(alpha = 0.5f)
            app.isBackgroundRestricted -> StatusRestricted.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}")
            .border(
                width = if (isAnyBlocked) 1.5.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: App Icon, Name, Package, Badges, Details chevron (Clickable to open details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = { onAppClick(app) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    packageName = app.packageName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (app.isSystemApp) {
                            BadgeLabel(text = "SYS", bgColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onAppClick(app) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("details_button_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Control Strip: Wi-Fi, Mobile Data, Background Activity, Quick Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Wi-Fi Button
                ActionToggleChip(
                    title = "Wi-Fi",
                    stateText = if (app.isWifiBlocked) "Off" else "On",
                    icon = if (app.isWifiBlocked) Icons.Default.WifiOff else Icons.Default.Wifi,
                    isBlocked = app.isWifiBlocked,
                    activeColor = StatusBlocked,
                    inactiveColor = StatusAllowed,
                    onClick = { onToggleWifi(app) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_wifi_${app.packageName}")
                )

                // Cellular Button
                ActionToggleChip(
                    title = "Data",
                    stateText = if (app.isMobileBlocked) "Off" else "On",
                    icon = if (app.isMobileBlocked) Icons.Outlined.SignalCellularConnectedNoInternet0Bar else Icons.Default.SignalCellularAlt,
                    isBlocked = app.isMobileBlocked,
                    activeColor = StatusBlocked,
                    inactiveColor = StatusAllowed,
                    onClick = { onToggleMobile(app) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_mobile_${app.packageName}")
                )

                // Background Activity Button
                ActionToggleChip(
                    title = "BG",
                    stateText = if (app.isBackgroundRestricted) "Off" else "On",
                    icon = if (app.isBackgroundRestricted) Icons.Default.SyncDisabled else Icons.Default.Sync,
                    isBlocked = app.isBackgroundRestricted,
                    activeColor = StatusRestricted,
                    inactiveColor = MaterialTheme.colorScheme.primary,
                    onClick = { onToggleBackground(app) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_bg_${app.packageName}")
                )

                // Quick Kill Process button
                val buttonColor = if (isRecentlyStopped) StatusAllowed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                val iconTint = if (isRecentlyStopped) StatusAllowed else MaterialTheme.colorScheme.error
                val iconVector = if (isRecentlyStopped) Icons.Default.CheckCircle else Icons.Default.StopCircle
                
                Surface(
                    onClick = { if (isRecentlyStopped) onRefreshProcess(app) else onKillProcess(app) },
                    shape = RoundedCornerShape(12.dp),
                    color = buttonColor,
                    modifier = Modifier
                        .size(height = 44.dp, width = 44.dp)
                        .testTag("kill_btn_${app.packageName}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = if (isRecentlyStopped) "Refresh Process State" else "Stop Process",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionToggleChip(
    title: String,
    stateText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBlocked: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isBlocked) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val contentColor = if (isBlocked) activeColor else inactiveColor
    val borderStrokeColor = if (isBlocked) activeColor.copy(alpha = 0.6f) else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
            .height(44.dp)
            .border(width = 1.dp, color = borderStrokeColor, shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 11.sp
                )
                Text(
                    text = stateText,
                    fontSize = 12.sp,
                    fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun BadgeLabel(text: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
