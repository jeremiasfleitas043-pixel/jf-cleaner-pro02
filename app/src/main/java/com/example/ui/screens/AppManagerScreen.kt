package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InstalledAppInfo
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CardDarkGray
import com.example.ui.theme.CardElevatedGray
import com.example.ui.theme.CyberMint
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PureBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.FormatUtils
import com.example.viewmodel.AppSortMode
import com.example.viewmodel.CleanerUiState

@Composable
fun AppManagerScreen(
    uiState: CleanerUiState,
    shizukuStatus: ShizukuStatus,
    onBackClick: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleShowSystem: (Boolean) -> Unit,
    onSortModeChange: (AppSortMode) -> Unit,
    onToggleSelectPackage: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onCleanCacheClick: () -> Unit,
    onCleanStorageClick: () -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStorageConfirmDialog by remember { mutableStateOf(false) }

    val filteredApps = uiState.filteredApps
    val allVisibleSelected = filteredApps.isNotEmpty() &&
        filteredApps.all { it.packageName in uiState.selectedPackages }

    val selectedApps = remember(uiState.allApps, uiState.selectedPackages) {
        uiState.allApps.filter { it.packageName in uiState.selectedPackages }
    }
    val selectedCacheTotal = selectedApps.sumOf { it.cacheBytes }
    val selectedStorageTotal = selectedApps.sumOf { it.storageBytes }

    if (showStorageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStorageConfirmDialog = false },
            containerColor = CardDarkGray,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Confirm Clean Storage") },
            text = {
                Text(
                    if (shizukuStatus.isReady) {
                        "Clearing app storage will erase all local app data, logins, and settings for ${selectedApps.size} selected app(s) via Shizuku. Continue?"
                    } else {
                        "Without Shizuku, Android requires manual confirmation in App Info for each app's storage reset. Open App Info now?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStorageConfirmDialog = false
                        onCleanStorageClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerCoral,
                        contentColor = PureBlack
                    )
                ) {
                    Text("CLEAN STORAGE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageConfirmDialog = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("app_manager_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.screen_app_manager_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Search & Filter Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = uiState.appSearchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_input"),
                    placeholder = { Text("Search installed apps...", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = CyberMint
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardDarkGray,
                        unfocusedContainerColor = CardDarkGray,
                        focusedBorderColor = NeonEmerald,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AppSortChip(
                            label = "Storage",
                            selected = uiState.appSortMode == AppSortMode.STORAGE,
                            onClick = { onSortModeChange(AppSortMode.STORAGE) }
                        )
                    }
                    item {
                        AppSortChip(
                            label = "Cache",
                            selected = uiState.appSortMode == AppSortMode.CACHE,
                            onClick = { onSortModeChange(AppSortMode.CACHE) }
                        )
                    }
                    item {
                        AppSortChip(
                            label = "Name",
                            selected = uiState.appSortMode == AppSortMode.NAME,
                            onClick = { onSortModeChange(AppSortMode.NAME) }
                        )
                    }
                    item {
                        AppSortChip(
                            label = if (uiState.showSystemApps) "System: On" else "System: Off",
                            selected = uiState.showSystemApps,
                            onClick = { onToggleShowSystem(!uiState.showSystemApps) }
                        )
                    }
                }

                if (!uiState.usageAccessGranted) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardElevatedGray,
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onGrantUsageAccess)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grant Usage Access for exact per-app cache & data sizes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarningAmber,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Grant",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberMint
                            )
                        }
                    }
                }

                // Select All Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardDarkGray,
                    border = BorderStroke(1.dp, CardBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_all_bar")
                        .clickable(onClick = onToggleSelectAll)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allVisibleSelected,
                                onCheckedChange = { onToggleSelectAll() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonEmerald,
                                    uncheckedColor = TextSecondary,
                                    checkmarkColor = PureBlack
                                )
                            )
                            Text(
                                text = stringResource(R.string.action_select_all),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (selectedApps.isEmpty()) {
                                "${filteredApps.size} apps"
                            } else {
                                "${selectedApps.size} selected · ${FormatUtils.formatBytesCompact(selectedStorageTotal)}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedApps.isNotEmpty()) CyberMint else TextSecondary
                        )
                    }
                }

                uiState.appActionMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardElevatedGray,
                        border = BorderStroke(1.dp, EmeraldDeep),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDismissMessage)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberMint,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Installed Apps List
            if (uiState.isLoadingApps && filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonEmerald)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        InstalledAppRowCard(
                            app = app,
                            isSelected = app.packageName in uiState.selectedPackages,
                            onToggleSelect = { onToggleSelectPackage(app.packageName) },
                            onOpenAppInfo = { onOpenAppInfo(app.packageName) }
                        )
                    }
                }
            }

            // Bottom Action Bar: CLEAN CACHE & CLEAN STORAGE
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CardDarkGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedApps.isNotEmpty()) {
                        Text(
                            text = "Selected cache: ${FormatUtils.formatBytes(selectedCacheTotal)} · " +
                                "Storage: ${FormatUtils.formatBytes(selectedStorageTotal)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onCleanCacheClick,
                            enabled = selectedApps.isNotEmpty() && !uiState.isAppActionRunning,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("clean_cache_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = PureBlack,
                                disabledContainerColor = CardElevatedGray,
                                disabledContentColor = TextSecondary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.action_clean_cache),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showStorageConfirmDialog = true },
                            enabled = selectedApps.isNotEmpty() && !uiState.isAppActionRunning,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("clean_storage_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selectedApps.isNotEmpty()) WarningAmber.copy(alpha = 0.7f) else CardBorderSubtle
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WarningAmber,
                                disabledContentColor = TextSecondary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.action_clean_storage),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CardDarkGray,
            labelColor = TextSecondary,
            selectedContainerColor = EmeraldDeep.copy(alpha = 0.35f),
            selectedLabelColor = CyberMint
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = CardBorderSubtle,
            selectedBorderColor = NeonEmerald.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun InstalledAppRowCard(
    app: InstalledAppInfo,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}")
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onToggleSelect),
        shape = RoundedCornerShape(18.dp),
        color = CardDarkGray,
        border = BorderStroke(
            1.dp,
            if (isSelected) NeonEmerald.copy(alpha = 0.6f) else CardBorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = NeonEmerald,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = PureBlack
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CardElevatedGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberMint
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cacheLabel = if (app.hasAccurateUsageStats) {
                        "Cache: ${FormatUtils.formatBytesCompact(app.cacheBytes)}"
                    } else {
                        "Cache: --"
                    }
                    Text(
                        text = cacheLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (app.cacheBytes > 0L) CyberMint else TextSecondary
                    )
                    Text(
                        text = "Storage: ${FormatUtils.formatBytesCompact(app.storageBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }

            IconButton(
                onClick = onOpenAppInfo,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "App Info ${app.appName}",
                    tint = TextSecondary
                )
            }
        }
    }
}
