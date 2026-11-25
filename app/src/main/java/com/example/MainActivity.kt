package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.BatteryInspectorSheet
import com.example.ui.components.GameBoosterSheet
import com.example.ui.components.SecurityScanSheet
import com.example.ui.components.ShizukuAndPermissionsSheet
import com.example.ui.screens.AppManagerScreen
import com.example.ui.screens.CleaningScreen
import com.example.ui.screens.DeepCleanScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.JfCleanerTheme
import com.example.ui.theme.PureBlack
import com.example.viewmodel.CleanerViewModel

object CleanerRoutes {
    const val HOME = "home"
    const val CLEANING = "cleaning"
    const val DEEP_CLEAN = "deep_clean"
    const val APP_MANAGER = "app_manager"
}

private enum class ActiveToolSheet {
    NONE,
    BATTERY,
    SECURITY,
    GAME_BOOSTER,
    ENGINE_SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JfCleanerTheme {
                JfCleanerApp()
            }
        }
    }
}

@Composable
fun JfCleanerApp(
    viewModel: CleanerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
    var activeSheet by remember { mutableStateOf(ActiveToolSheet.NONE) }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addSafTreeUri(uri)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemTelemetry()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NavHost(
        navController = navController,
        startDestination = CleanerRoutes.HOME,
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        enterTransition = {
            fadeIn(animationSpec = tween(260)) + slideInHorizontally(
                animationSpec = tween(260),
                initialOffsetX = { it / 10 }
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(220)) + slideOutHorizontally(
                animationSpec = tween(220),
                targetOffsetX = { -it / 12 }
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(260)) + slideInHorizontally(
                animationSpec = tween(260),
                initialOffsetX = { -it / 10 }
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220)) + slideOutHorizontally(
                animationSpec = tween(220),
                targetOffsetX = { it / 10 }
            )
        }
    ) {
        composable(CleanerRoutes.HOME) {
            HomeScreen(
                uiState = uiState,
                shizukuStatus = shizukuStatus,
                onSphereClick = { viewModel.runDeviceAnalysisScan() },
                onOptimizeClick = {
                    viewModel.startOptimizationCleaning()
                    navController.navigate(CleanerRoutes.CLEANING)
                },
                onBatteryClick = {
                    viewModel.refreshSystemTelemetry()
                    activeSheet = ActiveToolSheet.BATTERY
                },
                onSecurityScanClick = {
                    activeSheet = ActiveToolSheet.SECURITY
                    if (!uiState.securityScanResult.hasScanned) {
                        viewModel.runSecurityScan()
                    }
                },
                onDeepCleanClick = {
                    navController.navigate(CleanerRoutes.DEEP_CLEAN)
                },
                onGameBoosterClick = {
                    viewModel.refreshSystemTelemetry()
                    activeSheet = ActiveToolSheet.GAME_BOOSTER
                },
                onManageAppsClick = {
                    navController.navigate(CleanerRoutes.APP_MANAGER)
                },
                onEngineSettingsClick = {
                    viewModel.refreshSystemTelemetry()
                    activeSheet = ActiveToolSheet.ENGINE_SETTINGS
                }
            )
        }

        composable(CleanerRoutes.CLEANING) {
            CleaningScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
                onEngineSettingsClick = {
                    viewModel.refreshSystemTelemetry()
                    activeSheet = ActiveToolSheet.ENGINE_SETTINGS
                }
            )
        }

        composable(CleanerRoutes.DEEP_CLEAN) {
            DeepCleanScreen(
                uiState = uiState,
                shizukuStatus = shizukuStatus,
                onBackClick = { navController.popBackStack() },
                onRefreshScan = { viewModel.runDeviceAnalysisScan() },
                onOptimizeCachesClick = {
                    viewModel.startOptimizationCleaning()
                    navController.navigate(CleanerRoutes.CLEANING)
                },
                onDeleteFile = { item -> viewModel.deleteDetectedFile(item) },
                onDeleteCategory = { category -> viewModel.deleteCategoryFiles(category) },
                onUninstallApp = { pkg -> viewModel.uninstallPackage(pkg) },
                onGrantUsageAccess = { viewModel.openUsageAccessSettings() },
                onAddSafFolder = { safFolderLauncher.launch(null) },
                onOpenEngineSettings = {
                    viewModel.refreshSystemTelemetry()
                    activeSheet = ActiveToolSheet.ENGINE_SETTINGS
                }
            )
        }

        composable(CleanerRoutes.APP_MANAGER) {
            AppManagerScreen(
                uiState = uiState,
                shizukuStatus = shizukuStatus,
                onBackClick = { navController.popBackStack() },
                onSearchChange = { viewModel.updateAppSearchQuery(it) },
                onToggleShowSystem = { viewModel.toggleShowSystemApps(it) },
                onSortModeChange = { viewModel.setAppSortMode(it) },
                onToggleSelectPackage = { viewModel.toggleSelectPackage(it) },
                onToggleSelectAll = { viewModel.toggleSelectAllVisible() },
                onCleanCacheClick = { viewModel.cleanSelectedAppsCache() },
                onCleanStorageClick = { viewModel.clearSelectedAppsStorage() },
                onOpenAppInfo = { viewModel.openAppDetails(it) },
                onGrantUsageAccess = { viewModel.openUsageAccessSettings() },
                onDismissMessage = { viewModel.clearAppActionMessage() }
            )
        }
    }

    when (activeSheet) {
        ActiveToolSheet.BATTERY -> {
            BatteryInspectorSheet(
                battery = uiState.batteryTelemetry,
                onRefresh = { viewModel.refreshSystemTelemetry() },
                onDismiss = { activeSheet = ActiveToolSheet.NONE }
            )
        }
        ActiveToolSheet.SECURITY -> {
            SecurityScanSheet(
                scanResult = uiState.securityScanResult,
                isScanning = uiState.isScanningSecurity,
                onRunScan = { viewModel.runSecurityScan() },
                onOpenAppInfo = { pkg -> viewModel.openAppDetails(pkg) },
                onDismiss = { activeSheet = ActiveToolSheet.NONE }
            )
        }
        ActiveToolSheet.GAME_BOOSTER -> {
            GameBoosterSheet(
                memory = uiState.memoryOverview,
                boostResult = uiState.gameBoostResult,
                isBoosting = uiState.isBoostingMemory,
                shizukuReady = shizukuStatus.isReady,
                onBoostNow = { viewModel.runGameBooster() },
                onDismiss = { activeSheet = ActiveToolSheet.NONE }
            )
        }
        ActiveToolSheet.ENGINE_SETTINGS -> {
            ShizukuAndPermissionsSheet(
                shizukuStatus = shizukuStatus,
                usageAccessGranted = uiState.usageAccessGranted,
                safTreeUris = uiState.safTreeUris,
                onRequestShizuku = { viewModel.requestShizukuPermission() },
                onOpenShizukuApp = { viewModel.openShizukuApp() },
                onOpenUsageAccess = { viewModel.openUsageAccessSettings() },
                onAddSafFolder = { safFolderLauncher.launch(null) },
                onRefresh = { viewModel.refreshSystemTelemetry() },
                onDismiss = { activeSheet = ActiveToolSheet.NONE }
            )
        }
        ActiveToolSheet.NONE -> Unit
    }
}
