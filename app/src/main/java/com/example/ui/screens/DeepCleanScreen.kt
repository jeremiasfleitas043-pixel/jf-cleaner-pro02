package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DetectedFileItem
import com.example.data.FileCategory
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.NeonProgressBar
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CardDarkGray
import com.example.ui.theme.CardElevatedGray
import com.example.ui.theme.CyberMint
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PureBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.FormatUtils
import com.example.viewmodel.CleanerUiState
import java.util.concurrent.TimeUnit

@Composable
fun DeepCleanScreen(
    uiState: CleanerUiState,
    shizukuStatus: ShizukuStatus,
    onBackClick: () -> Unit,
    onRefreshScan: () -> Unit,
    onOptimizeCachesClick: () -> Unit,
    onDeleteFile: (DetectedFileItem) -> Unit,
    onDeleteCategory: (FileCategory) -> Unit,
    onUninstallApp: (String) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onAddSafFolder: () -> Unit,
    onOpenEngineSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }
    val storage = uiState.storageOverview
    val rarelyUsed = uiState.rarelyUsedApps
    val bigFiles = uiState.bigFiles
    val tempFiles = uiState.tempFiles
    val apkFiles = uiState.apkFiles
    val cacheAndResidualBytes = uiState.breakdown.cacheBytes + uiState.breakdown.residualBytes

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar matching reference
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("deep_clean_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.screen_deep_clean_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = onRefreshScan,
                    enabled = !uiState.isScanning,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("deep_clean_refresh_button")
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CyberMint,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Rescan storage",
                            tint = CyberMint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Header
            Text(
                text = "${storage.usedPercent}% used",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            NeonProgressBar(
                progress = storage.usedPercent / 100f,
                heightDp = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${FormatUtils.formatBytes(storage.availableBytes)} storage space available" +
                    if (storage.totalBytes > 0L) " · ${FormatUtils.formatBytes(storage.totalBytes)} total" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 1. Rarely Used Apps Card
            DeepCleanCategoryCard(
                title = if (uiState.usageAccessGranted) {
                    "${rarelyUsed.size} rarely used apps"
                } else {
                    "Rarely used apps"
                },
                description = if (uiState.usageAccessGranted) {
                    if (rarelyUsed.isEmpty()) {
                        "No unused apps detected in the last 30 days"
                    } else {
                        "You can uninstall unused apps to free up space"
                    }
                } else {
                    "Grant Usage Access to inspect app activity over the last 30 days"
                },
                actionIcon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                onCardClick = {
                    if (!uiState.usageAccessGranted) {
                        onGrantUsageAccess()
                    } else {
                        expandedSection = if (expandedSection == "rare") null else "rare"
                    }
                },
                onActionClick = {
                    if (!uiState.usageAccessGranted) {
                        onGrantUsageAccess()
                    } else {
                        expandedSection = if (expandedSection == "rare") null else "rare"
                    }
                },
                testTag = "deep_clean_rare_apps_card"
            )

            AnimatedVisibility(visible = expandedSection == "rare" && rarelyUsed.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rarelyUsed.take(15).forEach { app ->
                        val daysAgo = TimeUnit.MILLISECONDS.toDays(
                            System.currentTimeMillis() - app.lastUsedTimestamp
                        ).coerceAtLeast(30)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardElevatedGray,
                            border = BorderStroke(1.dp, CardBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.appName,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${FormatUtils.formatBytes(app.storageBytes)} · Last used ${daysAgo}d ago",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                TextButton(onClick = { onUninstallApp(app.packageName) }) {
                                    Text("Uninstall", color = WarningAmber)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Big Files Card
            val bigTotal = bigFiles.sumOf { it.sizeBytes }
            DeepCleanCategoryCard(
                title = "Big files",
                description = if (bigFiles.isNotEmpty()) {
                    "${bigFiles.size} file(s) totaling ${FormatUtils.formatBytes(bigTotal)}"
                } else {
                    "No files over 50 MB found in scanned directories"
                },
                actionIcon = Icons.Outlined.DeleteOutline,
                onCardClick = {
                    if (bigFiles.isNotEmpty()) {
                        expandedSection = if (expandedSection == "big") null else "big"
                    }
                },
                onActionClick = {
                    if (bigFiles.isNotEmpty()) {
                        expandedSection = if (expandedSection == "big") null else "big"
                    }
                },
                testTag = "deep_clean_big_files_card"
            )

            DetectedFilesList(
                visible = expandedSection == "big",
                files = bigFiles,
                onDeleteFile = onDeleteFile
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Temporary Files Card
            val tempTotal = tempFiles.sumOf { it.sizeBytes }
            DeepCleanCategoryCard(
                title = "Temporary files",
                description = if (tempFiles.isNotEmpty()) {
                    "Found ${tempFiles.size} temporary file(s) (${FormatUtils.formatBytes(tempTotal)})"
                } else {
                    "No temporary .tmp/.log/.bak files detected"
                },
                actionIcon = Icons.Outlined.DeleteOutline,
                onCardClick = {
                    if (tempFiles.isNotEmpty()) {
                        expandedSection = if (expandedSection == "temp") null else "temp"
                    }
                },
                onActionClick = {
                    if (tempFiles.isNotEmpty()) onDeleteCategory(FileCategory.TEMP_FILE)
                },
                testTag = "deep_clean_temp_files_card"
            )

            DetectedFilesList(
                visible = expandedSection == "temp",
                files = tempFiles,
                onDeleteFile = onDeleteFile
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. APK Files Card
            val apkTotal = apkFiles.sumOf { it.sizeBytes }
            DeepCleanCategoryCard(
                title = "APK files",
                description = if (apkFiles.isNotEmpty()) {
                    "You can delete ${apkFiles.size} APK(s), consuming ${FormatUtils.formatBytes(apkTotal)}"
                } else {
                    "No standalone APK installers detected"
                },
                actionIcon = Icons.Outlined.DeleteOutline,
                onCardClick = {
                    if (apkFiles.isNotEmpty()) {
                        expandedSection = if (expandedSection == "apk") null else "apk"
                    }
                },
                onActionClick = {
                    if (apkFiles.isNotEmpty()) onDeleteCategory(FileCategory.APK_FILE)
                },
                testTag = "deep_clean_apk_files_card"
            )

            DetectedFilesList(
                visible = expandedSection == "apk",
                files = apkFiles,
                onDeleteFile = onDeleteFile
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. App Cache & Residual Cleanup Card
            DeepCleanCategoryCard(
                title = "App cache & residuals",
                description = if (uiState.breakdown.isCalculated) {
                    "Detected ${FormatUtils.formatBytes(cacheAndResidualBytes)} across apps & caches"
                } else {
                    "Tap trash to run system cache optimization"
                },
                actionIcon = Icons.Outlined.DeleteOutline,
                onCardClick = onOptimizeCachesClick,
                onActionClick = onOptimizeCachesClick,
                testTag = "deep_clean_cache_card"
            )

            // Shared Storage Authorization Prompt (only shown when neither Shizuku nor SAF folder is set)
            if (!shizukuStatus.isReady || uiState.safTreeUris.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardElevatedGray,
                    border = BorderStroke(1.dp, EmeraldDeep.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Shared Storage Deep Scan",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberMint
                        )
                        Text(
                            text = if (shizukuStatus.isReady) {
                                "Shizuku is scanning /sdcard. You can also authorize specific SAF folders."
                            } else {
                                "Select a folder (e.g. Downloads) or connect Shizuku to scan real big files, temp files, and APKs."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onAddSafFolder,
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, EmeraldDeep),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberMint)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Folder")
                            }
                            if (!shizukuStatus.isReady) {
                                OutlinedButton(
                                    onClick = onOpenEngineSettings,
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, CardBorderSubtle),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Terminal,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shizuku")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DeepCleanCategoryCard(
    title: String,
    description: String,
    actionIcon: ImageVector,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        color = CardDarkGray,
        border = BorderStroke(1.dp, CardBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onActionClick),
                shape = CircleShape,
                color = CardElevatedGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = title,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedFilesList(
    visible: Boolean,
    files: List<DetectedFileItem>,
    onDeleteFile: (DetectedFileItem) -> Unit
) {
    AnimatedVisibility(visible = visible && files.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            files.take(20).forEach { file ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardElevatedGray,
                    border = BorderStroke(1.dp, CardBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = FormatUtils.formatBytes(file.sizeBytes),
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberMint
                            )
                        }
                        IconButton(
                            onClick = { onDeleteFile(file) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete ${file.name}",
                                tint = NeonEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}
