package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.EnergySphereHero
import com.example.ui.components.FuturisticFeatureCard
import com.example.ui.components.ShizukuStatusBadge
import com.example.ui.components.SphereState
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CardDarkGray
import com.example.ui.theme.CyberMint
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PureBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.CleanerUiState

@Composable
fun HomeScreen(
    uiState: CleanerUiState,
    shizukuStatus: ShizukuStatus,
    onSphereClick: () -> Unit,
    onOptimizeClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onSecurityScanClick: () -> Unit,
    onDeepCleanClick: () -> Unit,
    onGameBoosterClick: () -> Unit,
    onManageAppsClick: () -> Unit,
    onEngineSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breakdown = uiState.breakdown
    val sphereState = when {
        uiState.isScanning -> SphereState.SCANNING
        else -> SphereState.IDLE
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        val sphereSize = (maxHeight * 0.32f).coerceIn(192.dp, 260.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Brand & Engine Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald)
                    )
                    Text(
                        text = stringResource(R.string.brand_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShizukuStatusBadge(
                        status = shizukuStatus,
                        onClick = onEngineSettingsClick
                    )
                    IconButton(
                        onClick = onEngineSettingsClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("engine_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Adjust,
                            contentDescription = "System & Shizuku Settings",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Central 3D Green Energy Sphere
            EnergySphereHero(
                displayBytes = breakdown.totalOptimizableBytes,
                isCalculated = breakdown.isCalculated,
                sphereState = sphereState,
                onClick = onSphereClick,
                size = sphereSize
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Optimization Status Text
            val statusMessage = when {
                uiState.isScanning -> "Analyzing device storage & caches..."
                !breakdown.isCalculated -> "Tap sphere to calculate optimizable space"
                breakdown.totalOptimizableBytes > 0L -> "A few items can be optimized"
                else -> "System storage is optimized"
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Green OPTIMIZE Button
            Button(
                onClick = onOptimizeClick,
                modifier = Modifier
                    .testTag("optimize_button")
                    .minimumInteractiveComponentSize()
                    .height(48.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldDeep.copy(alpha = 0.24f),
                    contentColor = CyberMint
                ),
                border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.55f))
            ) {
                Text(
                    text = stringResource(R.string.action_optimize),
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberMint,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 2x2 Feature Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FuturisticFeatureCard(
                    title = stringResource(R.string.card_battery),
                    subtitle = uiState.batteryTelemetry.estimatedSummary.ifBlank {
                        "${uiState.batteryTelemetry.levelPercent}% battery"
                    },
                    icon = Icons.Outlined.BatteryChargingFull,
                    onClick = onBatteryClick,
                    testTag = "card_battery",
                    accentColor = if (uiState.batteryTelemetry.isCharging) CyberMint else TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                val securitySubtitle = if (uiState.securityScanResult.hasScanned) {
                    val flagged = uiState.securityScanResult.flaggedApps.size
                    if (flagged == 0) "No risks in ${uiState.securityScanResult.scannedPackagesCount} apps"
                    else "$flagged item(s) to review"
                } else {
                    "Scan for risks in your device"
                }

                FuturisticFeatureCard(
                    title = stringResource(R.string.card_security_scan),
                    subtitle = securitySubtitle,
                    icon = Icons.Outlined.VerifiedUser,
                    onClick = onSecurityScanClick,
                    testTag = "card_security_scan",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val cleanerSubtitle = if (breakdown.isCalculated && breakdown.totalOptimizableBytes > 0L) {
                    "Free up ${FormatUtils.formatBytesCompact(breakdown.totalOptimizableBytes)}"
                } else {
                    "${uiState.storageOverview.usedPercent}% storage used"
                }

                FuturisticFeatureCard(
                    title = stringResource(R.string.card_cleaner),
                    subtitle = cleanerSubtitle,
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = onDeepCleanClick,
                    testTag = "card_cleaner",
                    accentColor = CyberMint,
                    modifier = Modifier.weight(1f)
                )

                val boosterSubtitle = if (uiState.memoryOverview.totalRamBytes > 0L) {
                    "${FormatUtils.formatBytesCompact(uiState.memoryOverview.availableRamBytes)} RAM free"
                } else {
                    "Max out the performance"
                }

                FuturisticFeatureCard(
                    title = stringResource(R.string.card_game_booster),
                    subtitle = boosterSubtitle,
                    icon = Icons.Outlined.Bolt,
                    onClick = onGameBoosterClick,
                    testTag = "card_game_booster",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Wide Card: Manage Installed Apps
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_manage_apps")
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onManageAppsClick),
                shape = RoundedCornerShape(20.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = stringResource(R.string.card_manage_apps),
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.card_manage_apps),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (uiState.userAppsCount > 0) {
                                    "${uiState.userAppsCount} apps · Inspect cache & storage"
                                } else {
                                    stringResource(R.string.card_manage_apps_desc)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
