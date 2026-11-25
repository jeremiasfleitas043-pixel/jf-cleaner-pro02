package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BatteryTelemetry
import com.example.data.GameBoostResult
import com.example.data.MemoryOverview
import com.example.data.SecurityRiskLevel
import com.example.data.SecurityScanResult
import com.example.shizuku.ShizukuConnectionState
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CardDarkGray
import com.example.ui.theme.CardElevatedGray
import com.example.ui.theme.CyberMint
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PureBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryInspectorSheet(
    battery: BatteryTelemetry,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianBg,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BatteryChargingFull,
                    contentDescription = "Battery telemetry",
                    tint = NeonEmerald,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text("Battery Diagnostics", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = battery.estimatedSummary.ifBlank { "Live hardware telemetry" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberMint
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${battery.levelPercent}%",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = battery.chargingSource,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (battery.isCharging) CyberMint else TextSecondary
                        )
                    }
                    NeonProgressBar(progress = battery.levelPercent / 100f, heightDp = 6)
                    TelemetryRow("Temperature", "${"%.1f".format(battery.temperatureCelsius)} °C")
                    TelemetryRow("Voltage", "${battery.voltageMv} mV")
                    TelemetryRow("Health", battery.healthLabel)
                }
            }

            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("battery_refresh_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonEmerald,
                    contentColor = PureBlack
                )
            ) {
                Text("REFRESH TELEMETRY", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScanSheet(
    scanResult: SecurityScanResult,
    isScanning: Boolean,
    onRunScan: () -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianBg,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = "Security scan",
                    tint = NeonEmerald,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text("Security Scan", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (scanResult.hasScanned) {
                            "${scanResult.scannedPackagesCount} packages inspected"
                        } else {
                            "Analyze permissions, debug flags & app sources"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (isScanning) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardDarkGray,
                    border = BorderStroke(1.dp, CardBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = NeonEmerald,
                            strokeWidth = 2.5.dp
                        )
                        Text("Inspecting installed packages...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else if (scanResult.hasScanned) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardDarkGray,
                    border = BorderStroke(
                        1.dp,
                        if (scanResult.flaggedApps.isEmpty()) EmeraldDeep else CardBorderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (scanResult.flaggedApps.isEmpty()) {
                                Icons.Outlined.VerifiedUser
                            } else {
                                Icons.Outlined.WarningAmber
                            },
                            contentDescription = null,
                            tint = if (scanResult.flaggedApps.isEmpty()) CyberMint else WarningAmber
                        )
                        Column {
                            Text(
                                text = if (scanResult.flaggedApps.isEmpty()) {
                                    "No elevated risks detected"
                                } else {
                                    "${scanResult.flaggedApps.size} app(s) flagged for review"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Checked sideloading, debuggable flags & sensitive permissions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (scanResult.flaggedApps.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scanResult.flaggedApps, key = { it.packageName }) { flagged ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = CardElevatedGray,
                                border = BorderStroke(1.dp, CardBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = flagged.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = flagged.reasons.joinToString(" · "),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (flagged.riskLevel == SecurityRiskLevel.REVIEW) {
                                                WarningAmber
                                            } else {
                                                TextSecondary
                                            }
                                        )
                                    }
                                    TextButton(onClick = { onOpenAppInfo(flagged.packageName) }) {
                                        Text("Inspect", color = CyberMint)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onRunScan,
                enabled = !isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("run_security_scan_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonEmerald,
                    contentColor = PureBlack
                )
            ) {
                Text(
                    text = if (scanResult.hasScanned) "SCAN AGAIN" else "START SECURITY SCAN",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoosterSheet(
    memory: MemoryOverview,
    boostResult: GameBoostResult?,
    isBoosting: Boolean,
    shizukuReady: Boolean,
    onBoostNow: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianBg,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = "Game Booster",
                    tint = NeonEmerald,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text("Game Booster", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (shizukuReady) {
                            "Shizuku accelerated memory trimming"
                        } else {
                            "Standard background process trimmer"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (shizukuReady) CyberMint else TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${memory.usedPercent}% RAM used",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${FormatUtils.formatBytes(memory.availableRamBytes)} free",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberMint
                        )
                    }
                    NeonProgressBar(progress = memory.usedPercent / 100f, heightDp = 6)
                    TelemetryRow("Total RAM", FormatUtils.formatBytes(memory.totalRamBytes))
                    TelemetryRow("Used RAM", FormatUtils.formatBytes(memory.usedRamBytes))
                    TelemetryRow("Available RAM", FormatUtils.formatBytes(memory.availableRamBytes), CyberMint)
                }
            }

            if (boostResult != null && boostResult.boosted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardElevatedGray,
                    border = BorderStroke(1.dp, EmeraldDeep)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Memory, contentDescription = null, tint = CyberMint)
                        Column {
                            Text(
                                text = "Processed ${boostResult.stoppedPackagesCount} background apps",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (boostResult.reclaimedBytes > 0L) {
                                    "Reclaimed ${FormatUtils.formatBytes(boostResult.reclaimedBytes)} RAM"
                                } else {
                                    "Available RAM: ${FormatUtils.formatBytes(boostResult.afterAvailableBytes)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberMint
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onBoostNow,
                enabled = !isBoosting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("boost_memory_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonEmerald,
                    contentColor = PureBlack
                )
            ) {
                if (isBoosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PureBlack,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BOOSTING...", fontWeight = FontWeight.Bold)
                } else {
                    Text("BOOST PERFORMANCE", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuAndPermissionsSheet(
    shizukuStatus: ShizukuStatus,
    usageAccessGranted: Boolean,
    safTreeUris: List<Uri>,
    onRequestShizuku: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onAddSafFolder: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ObsidianBg,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("System & Shizuku Engine", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onRefresh) {
                    Text("Refresh", color = CyberMint)
                }
            }

            // Shizuku Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, if (shizukuStatus.isReady) EmeraldDeep else CardBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = "Shizuku",
                            tint = if (shizukuStatus.isReady) CyberMint else TextSecondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shizuku Integration", style = MaterialTheme.typography.titleMedium)
                            val desc = when (shizukuStatus.state) {
                                ShizukuConnectionState.READY ->
                                    "Connected (UID ${shizukuStatus.uid ?: "-"} · API v${shizukuStatus.apiVersion ?: "-"})"
                                ShizukuConnectionState.PERMISSION_REQUIRED ->
                                    "Shizuku running — permission required for cache & storage ops"
                                ShizukuConnectionState.NOT_RUNNING ->
                                    "Shizuku installed, service not started"
                                ShizukuConnectionState.NOT_INSTALLED ->
                                    "Shizuku app not detected (Standard Android APIs active)"
                            }
                            Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }

                    when (shizukuStatus.state) {
                        ShizukuConnectionState.PERMISSION_REQUIRED -> {
                            Button(
                                onClick = onRequestShizuku,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonEmerald,
                                    contentColor = PureBlack
                                )
                            ) {
                                Text("Authorize Shizuku", fontWeight = FontWeight.Bold)
                            }
                        }
                        ShizukuConnectionState.NOT_RUNNING -> {
                            OutlinedButton(
                                onClick = onOpenShizukuApp,
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, EmeraldDeep)
                            ) {
                                Text("Open Shizuku Manager", color = CyberMint)
                            }
                        }
                        else -> Unit
                    }
                }
            }

            // Usage Stats Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, if (usageAccessGranted) EmeraldDeep else CardBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (usageAccessGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = if (usageAccessGranted) CyberMint else WarningAmber
                        )
                        Column {
                            Text("Usage Access", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (usageAccessGranted) {
                                    "Active — reading real app cache & usage stats"
                                } else {
                                    "Required to measure real per-app cache & rarely used apps"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    if (!usageAccessGranted) {
                        TextButton(onClick = onOpenUsageAccess) {
                            Text("Grant", color = CyberMint)
                        }
                    }
                }
            }

            // SAF Storage Folders Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = CyberMint
                        )
                        Column {
                            Text("Folder Scanner (SAF)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (safTreeUris.isEmpty()) {
                                    "Select Downloads/Storage folders to scan big & temp files"
                                } else {
                                    "${safTreeUris.size} folder(s) authorized for real file scanning"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    TextButton(onClick = onAddSafFolder) {
                        Text("Add", color = CyberMint)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TelemetryRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
