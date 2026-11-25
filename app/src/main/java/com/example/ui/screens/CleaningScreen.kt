package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.EnergySphereHero
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.SphereState
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CardDarkGray
import com.example.ui.theme.CardElevatedGray
import com.example.ui.theme.CyberMint
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PureBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.viewmodel.CleanerUiState

@Composable
fun CleaningScreen(
    uiState: CleanerUiState,
    onBackClick: () -> Unit,
    onEngineSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breakdown = uiState.breakdown
    val result = uiState.cleaningResult
    val sphereState = if (uiState.isCleaning) SphereState.CLEANING else SphereState.CLEANED

    val sphereDisplayBytes = when {
        result != null && result.freedStorageBytes > 0L -> result.freedStorageBytes
        else -> breakdown.totalOptimizableBytes
    }

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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cleaning_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onEngineSettingsClick,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Adjust,
                        contentDescription = "System Engine",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.isCleaning) {
                    stringResource(R.string.screen_cleaning_title)
                } else {
                    stringResource(R.string.screen_cleaning_done)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = uiState.cleaningStageLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberMint
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Green Sphere
            EnergySphereHero(
                displayBytes = sphereDisplayBytes,
                isCalculated = breakdown.isCalculated || result != null,
                sphereState = sphereState,
                size = 248.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Real Operation Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NeonProgressBar(progress = uiState.cleaningProgress, heightDp = 6)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.isCleaning) "Processing..." else "Verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${(uiState.cleaningProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberMint
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Dark Breakdown Card matching reference
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = CardDarkGray,
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CleaningBreakdownRow(
                        label = stringResource(R.string.label_cache_files),
                        value = if (breakdown.isCalculated) FormatUtils.formatBytes(breakdown.cacheBytes) else "--",
                        valueColor = TextPrimary
                    )
                    CleaningBreakdownRow(
                        label = stringResource(R.string.label_memory),
                        value = FormatUtils.formatBytes(uiState.memoryOverview.availableRamBytes),
                        valueColor = CyberMint
                    )
                    CleaningBreakdownRow(
                        label = stringResource(R.string.label_packages),
                        value = if (breakdown.apkBytes > 0L) {
                            FormatUtils.formatBytes(breakdown.apkBytes)
                        } else {
                            "${breakdown.packageCount} apps"
                        },
                        valueColor = TextPrimary
                    )
                    CleaningBreakdownRow(
                        label = stringResource(R.string.label_obsolete_files),
                        value = if (breakdown.isCalculated) FormatUtils.formatBytes(breakdown.obsoleteBytes) else "--",
                        valueColor = TextPrimary
                    )
                    CleaningBreakdownRow(
                        label = stringResource(R.string.label_residuals),
                        value = if (breakdown.isCalculated) FormatUtils.formatBytes(breakdown.residualBytes) else "--",
                        valueColor = TextPrimary
                    )
                }
            }

            // Real Operation Result Card
            AnimatedVisibility(
                visible = result != null && !uiState.isCleaning,
                enter = fadeIn() + slideInVertically { it / 4 }
            ) {
                result?.let { opResult ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardElevatedGray,
                            border = BorderStroke(1.dp, EmeraldDeep)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Operation result",
                                    tint = CyberMint,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = opResult.summaryMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Storage freed: ${FormatUtils.formatBytes(opResult.freedStorageBytes)} · " +
                                            "RAM delta: +${FormatUtils.formatBytes(opResult.freedMemoryBytes)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberMint
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onBackClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("cleaning_done_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = PureBlack
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.action_done),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CleaningBreakdownRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}
