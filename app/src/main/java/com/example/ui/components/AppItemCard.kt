package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import com.example.data.model.InstalledApp
import com.example.ui.theme.StatusAllowed
import com.example.ui.theme.StatusBlocked
import com.example.ui.theme.StatusRestricted
import com.example.ui.theme.StatusWarning

@Composable
fun AppItemCard(
    app: InstalledApp,
    onToggleWifi: () -> Unit,
    onToggleMobile: () -> Unit,
    onToggleBackground: () -> Unit,
    onKillProcess: () -> Unit,
    onAppClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnyBlocked = app.isWifiBlocked || app.isMobileBlocked || app.isBackgroundRestricted

    val borderColor by animateColorAsState(
        targetValue = when {
            app.isBothNetworkBlocked -> StatusBlocked.copy(alpha = 0.5f)
            app.isWifiBlocked || app.isMobileBlocked -> StatusWarning.copy(alpha = 0.4f)
            app.isBackgroundRestricted -> StatusRestricted.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        label = "borderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}")
            .border(
                width = if (isAnyBlocked) 1.2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onAppClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: App Icon, Name, Package, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    packageName = app.packageName,
                    size = 46.dp
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
                    onClick = onAppClick,
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

            Spacer(modifier = Modifier.height(12.dp))

            // Action Control Strip: Wi-Fi, Mobile Data, Background Activity, Quick Stop
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Wi-Fi Control
                NetworkControlButton(
                    label = "Wi-Fi",
                    icon = if (app.isWifiBlocked) Icons.Default.WifiOff else Icons.Default.Wifi,
                    isBlocked = app.isWifiBlocked,
                    onClick = onToggleWifi,
                    testTag = "toggle_wifi_${app.packageName}"
                )

                // Mobile Data Control
                NetworkControlButton(
                    label = "Cellular",
                    icon = if (app.isMobileBlocked) Icons.Outlined.SignalCellularConnectedNoInternet0Bar else Icons.Default.SignalCellularAlt,
                    isBlocked = app.isMobileBlocked,
                    onClick = onToggleMobile,
                    testTag = "toggle_mobile_${app.packageName}"
                )

                // Background Activity Control
                BackgroundControlButton(
                    isRestricted = app.isBackgroundRestricted,
                    onClick = onToggleBackground,
                    testTag = "toggle_bg_${app.packageName}"
                )

                // Force Kill button
                IconButton(
                    onClick = onKillProcess,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                        .testTag("kill_btn_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Stop App Background Process",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkControlButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBlocked: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (isBlocked) StatusBlocked.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isBlocked) StatusBlocked else StatusAllowed

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = if (isBlocked) "Blocked" else label,
            fontSize = 12.sp,
            fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun BackgroundControlButton(
    isRestricted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (isRestricted) StatusRestricted.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isRestricted) StatusRestricted else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = "Background Activity",
            tint = contentColor,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = if (isRestricted) "BG Off" else "BG On",
            fontSize = 12.sp,
            fontWeight = if (isRestricted) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun BadgeLabel(
    text: String,
    bgColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
