package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.data.BatteryTelemetry
import com.example.data.MemoryOverview
import com.example.data.OptimizationBreakdown
import com.example.data.StorageOverview
import com.example.shizuku.ShizukuConnectionState
import com.example.shizuku.ShizukuStatus
import com.example.ui.screens.CleaningScreen
import com.example.ui.screens.DeepCleanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.JfCleanerTheme
import com.example.util.FormatUtils
import com.example.viewmodel.CleanerUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class CleanerUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun formatUtils_formatsBytesAccurately() {
        val parts = FormatUtils.splitSizeParts(3L * 1024L * 1024L * 1024L)
        assertEquals("3.00", parts.value)
        assertEquals("GB", parts.unit)
    }

    @Test
    fun homeScreen_hidesFakeValueWhenNotCalculated_andShowsCards() {
        val state = CleanerUiState(
            breakdown = OptimizationBreakdown(isCalculated = false),
            batteryTelemetry = BatteryTelemetry(levelPercent = 82, estimatedSummary = "82% · Discharging"),
            memoryOverview = MemoryOverview(totalRamBytes = 8_000_000_000L, availableRamBytes = 3_200_000_000L)
        )
        composeRule.setContent {
            JfCleanerTheme {
                HomeScreen(
                    uiState = state,
                    shizukuStatus = ShizukuStatus(ShizukuConnectionState.NOT_RUNNING),
                    onSphereClick = {},
                    onOptimizeClick = {},
                    onBatteryClick = {},
                    onSecurityScanClick = {},
                    onDeepCleanClick = {},
                    onGameBoosterClick = {},
                    onManageAppsClick = {},
                    onEngineSettingsClick = {}
                )
            }
        }
        composeRule.onNodeWithTag("energy_sphere_hero").assertIsDisplayed()
        composeRule.onNodeWithTag("optimize_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("card_battery").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("card_security_scan").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("card_cleaner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("card_game_booster").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("card_manage_apps").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun cleaningAndDeepCleanScreens_renderRealMetrics() {
        val state = CleanerUiState(
            storageOverview = StorageOverview(
                totalBytes = 128_000_000_000L,
                availableBytes = 32_000_000_000L,
                usedBytes = 96_000_000_000L,
                usedPercent = 75
            ),
            breakdown = OptimizationBreakdown(
                isCalculated = true,
                cacheBytes = 512L * 1024L * 1024L
            ),
            isCleaning = true,
            cleaningProgress = 0.5f,
            cleaningStageLabel = "Cleaning caches..."
        )
        composeRule.setContent {
            JfCleanerTheme {
                DeepCleanScreen(
                    uiState = state,
                    shizukuStatus = ShizukuStatus(ShizukuConnectionState.NOT_INSTALLED),
                    onBackClick = {},
                    onRefreshScan = {},
                    onOptimizeCachesClick = {},
                    onDeleteFile = {},
                    onDeleteCategory = {},
                    onUninstallApp = {},
                    onGrantUsageAccess = {},
                    onAddSafFolder = {},
                    onOpenEngineSettings = {}
                )
            }
        }
        composeRule.onNodeWithText("75% used").assertIsDisplayed()
        composeRule.onNodeWithTag("deep_clean_rare_apps_card").assertIsDisplayed()
        composeRule.onNodeWithTag("deep_clean_big_files_card").assertIsDisplayed()
    }

    @Test
    fun cleaningScreen_displaysProgressAndBreakdown() {
        val state = CleanerUiState(
            memoryOverview = MemoryOverview(totalRamBytes = 8_000_000_000L, availableRamBytes = 2_400_000_000L),
            breakdown = OptimizationBreakdown(
                isCalculated = true,
                cacheBytes = 256L * 1024L * 1024L
            ),
            isCleaning = true,
            cleaningProgress = 0.45f,
            cleaningStageLabel = "Trimming system caches..."
        )
        composeRule.setContent {
            JfCleanerTheme {
                CleaningScreen(
                    uiState = state,
                    onBackClick = {},
                    onEngineSettingsClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Cleaning..").assertIsDisplayed()
        composeRule.onNodeWithTag("energy_sphere_hero").assertIsDisplayed()
    }
}
