package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryTelemetry
import com.example.data.CleaningOperationResult
import com.example.data.DetectedFileItem
import com.example.data.FileCategory
import com.example.data.GameBoostResult
import com.example.data.InstalledAppInfo
import com.example.data.MemoryOverview
import com.example.data.OptimizationBreakdown
import com.example.data.SecurityScanResult
import com.example.data.StorageOverview
import com.example.data.SystemInspectorRepository
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class AppSortMode {
    STORAGE,
    CACHE,
    NAME
}

data class CleanerUiState(
    val storageOverview: StorageOverview = StorageOverview(),
    val memoryOverview: MemoryOverview = MemoryOverview(),
    val batteryTelemetry: BatteryTelemetry = BatteryTelemetry(),
    val usageAccessGranted: Boolean = false,
    val safTreeUris: List<Uri> = emptyList(),
    val allApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val isScanning: Boolean = false,
    val breakdown: OptimizationBreakdown = OptimizationBreakdown(isCalculated = false),
    val detectedFiles: List<DetectedFileItem> = emptyList(),
    val isCleaning: Boolean = false,
    val cleaningProgress: Float = 0f,
    val cleaningStageLabel: String = "",
    val cleaningResult: CleaningOperationResult? = null,
    val securityScanResult: SecurityScanResult = SecurityScanResult(),
    val isScanningSecurity: Boolean = false,
    val gameBoostResult: GameBoostResult? = null,
    val isBoostingMemory: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val appSearchQuery: String = "",
    val showSystemApps: Boolean = false,
    val appSortMode: AppSortMode = AppSortMode.STORAGE,
    val isAppActionRunning: Boolean = false,
    val appActionMessage: String? = null
) {
    val userAppsCount: Int
        get() = allApps.count { !it.isSystemApp }

    val rarelyUsedApps: List<InstalledAppInfo>
        get() {
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            return allApps.filter {
                !it.isSystemApp &&
                    it.lastUsedTimestamp in 1 until thirtyDaysAgo
            }.sortedBy { it.lastUsedTimestamp }
        }

    val bigFiles: List<DetectedFileItem>
        get() = detectedFiles.filter { it.category == FileCategory.BIG_FILE }

    val tempFiles: List<DetectedFileItem>
        get() = detectedFiles.filter { it.category == FileCategory.TEMP_FILE }

    val apkFiles: List<DetectedFileItem>
        get() = detectedFiles.filter { it.category == FileCategory.APK_FILE }

    val residualFiles: List<DetectedFileItem>
        get() = detectedFiles.filter { it.category == FileCategory.RESIDUAL }

    val filteredApps: List<InstalledAppInfo>
        get() {
            val query = appSearchQuery.trim().lowercase()
            return allApps
                .asSequence()
                .filter { showSystemApps || !it.isSystemApp }
                .filter {
                    query.isEmpty() ||
                        it.appName.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
                }
                .let { seq ->
                    when (appSortMode) {
                        AppSortMode.STORAGE -> seq.sortedByDescending { it.storageBytes }
                        AppSortMode.CACHE -> seq.sortedByDescending { it.cacheBytes }
                        AppSortMode.NAME -> seq.sortedBy { it.appName.lowercase() }
                    }
                }
                .toList()
        }
}

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val shizukuManager = ShizukuManager(application)
    val repository = SystemInspectorRepository(application, shizukuManager)

    private val prefs = application.getSharedPreferences("jf_cleaner_prefs", Application.MODE_PRIVATE)

    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuManager.status
        .stateIn(viewModelScope, SharingStarted.Eagerly, shizukuManager.status.value)

    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    init {
        loadPersistedSafUris()
        refreshSystemTelemetry()
        loadInstalledAppsAndAnalyze()
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.release()
    }

    private fun loadPersistedSafUris() {
        val saved = prefs.getStringSet("saf_tree_uris", emptySet()).orEmpty()
        val validUris = saved.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        _uiState.update { it.copy(safTreeUris = validUris) }
    }

    fun addSafTreeUri(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            resolver.takePersistableUriPermission(uri, flags)
        } catch (_: Throwable) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {
            }
        }
        val updated = (_uiState.value.safTreeUris + uri).distinctBy { it.toString() }
        prefs.edit().putStringSet("saf_tree_uris", updated.map { it.toString() }.toSet()).apply()
        _uiState.update { it.copy(safTreeUris = updated) }
        runDeviceAnalysisScan()
    }

    fun refreshSystemTelemetry() {
        shizukuManager.refreshStatus()
        val storage = repository.getStorageOverview()
        val memory = repository.getMemoryOverview()
        val battery = repository.getBatteryTelemetry()
        val usageGranted = repository.hasUsageStatsPermission()
        _uiState.update {
            it.copy(
                storageOverview = storage,
                memoryOverview = memory,
                batteryTelemetry = battery,
                usageAccessGranted = usageGranted
            )
        }
    }

    fun loadInstalledAppsAndAnalyze() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingApps = true) }
            val apps = repository.loadInstalledApps(includeSystemApps = true)
            _uiState.update { it.copy(allApps = apps, isLoadingApps = false) }
            runDeviceAnalysisScan()
        }
    }

    fun runDeviceAnalysisScan() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            refreshSystemTelemetry()

            val apps = if (_uiState.value.allApps.isEmpty()) {
                repository.loadInstalledApps(includeSystemApps = true)
            } else {
                repository.loadInstalledApps(includeSystemApps = true)
            }
            val files = repository.scanDetectedFiles(_uiState.value.safTreeUris)
            val memory = repository.getMemoryOverview()
            val storage = repository.getStorageOverview()

            val totalCacheBytes = apps.sumOf { it.cacheBytes }
            val apkBytes = files.filter { it.category == FileCategory.APK_FILE }.sumOf { it.sizeBytes }
            val obsoleteBytes = files.filter { it.category == FileCategory.TEMP_FILE }.sumOf { it.sizeBytes }
            val residualBytes = files.filter { it.category == FileCategory.RESIDUAL }.sumOf { it.sizeBytes }

            val breakdown = OptimizationBreakdown(
                isCalculated = true,
                cacheBytes = totalCacheBytes,
                reclaimableMemoryBytes = memory.availableRamBytes,
                packageCount = apps.count { !it.isSystemApp },
                apkBytes = apkBytes,
                obsoleteBytes = obsoleteBytes,
                residualBytes = residualBytes
            )

            _uiState.update {
                it.copy(
                    storageOverview = storage,
                    memoryOverview = memory,
                    allApps = apps,
                    detectedFiles = files,
                    breakdown = breakdown,
                    isScanning = false
                )
            }
        }
    }

    fun startOptimizationCleaning() {
        if (_uiState.value.isCleaning) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCleaning = true,
                    cleaningProgress = 0.08f,
                    cleaningStageLabel = "Inspecting cache & storage...",
                    cleaningResult = null
                )
            }

            if (!_uiState.value.breakdown.isCalculated) {
                val apps = repository.loadInstalledApps(includeSystemApps = true)
                val files = repository.scanDetectedFiles(_uiState.value.safTreeUris)
                val memory = repository.getMemoryOverview()
                val totalCacheBytes = apps.sumOf { it.cacheBytes }
                val apkBytes = files.filter { it.category == FileCategory.APK_FILE }.sumOf { it.sizeBytes }
                val obsoleteBytes = files.filter { it.category == FileCategory.TEMP_FILE }.sumOf { it.sizeBytes }
                val residualBytes = files.filter { it.category == FileCategory.RESIDUAL }.sumOf { it.sizeBytes }

                _uiState.update {
                    it.copy(
                        allApps = apps,
                        detectedFiles = files,
                        memoryOverview = memory,
                        breakdown = OptimizationBreakdown(
                            isCalculated = true,
                            cacheBytes = totalCacheBytes,
                            reclaimableMemoryBytes = memory.availableRamBytes,
                            packageCount = apps.count { !it.isSystemApp },
                            apkBytes = apkBytes,
                            obsoleteBytes = obsoleteBytes,
                            residualBytes = residualBytes
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    cleaningProgress = 0.35f,
                    cleaningStageLabel = if (shizukuStatus.value.isReady) {
                        "Trimming system caches via Shizuku..."
                    } else {
                        "Cleaning accessible caches & temp files..."
                    }
                )
            }
            delay(350)

            val result = repository.runFullOptimization(
                apps = _uiState.value.allApps,
                detectedFiles = _uiState.value.detectedFiles
            )

            _uiState.update {
                it.copy(
                    cleaningProgress = 0.78f,
                    cleaningStageLabel = "Recomputing device storage & RAM..."
                )
            }
            delay(300)

            val updatedApps = repository.loadInstalledApps(includeSystemApps = true)
            val updatedFiles = repository.scanDetectedFiles(_uiState.value.safTreeUris)
            val updatedStorage = repository.getStorageOverview()
            val updatedMemory = repository.getMemoryOverview()

            val updatedBreakdown = OptimizationBreakdown(
                isCalculated = true,
                cacheBytes = updatedApps.sumOf { it.cacheBytes },
                reclaimableMemoryBytes = updatedMemory.availableRamBytes,
                packageCount = updatedApps.count { !it.isSystemApp },
                apkBytes = updatedFiles.filter { it.category == FileCategory.APK_FILE }.sumOf { it.sizeBytes },
                obsoleteBytes = updatedFiles.filter { it.category == FileCategory.TEMP_FILE }.sumOf { it.sizeBytes },
                residualBytes = updatedFiles.filter { it.category == FileCategory.RESIDUAL }.sumOf { it.sizeBytes }
            )

            _uiState.update {
                it.copy(
                    storageOverview = updatedStorage,
                    memoryOverview = updatedMemory,
                    allApps = updatedApps,
                    detectedFiles = updatedFiles,
                    breakdown = updatedBreakdown,
                    isCleaning = false,
                    cleaningProgress = 1f,
                    cleaningStageLabel = "Optimization complete",
                    cleaningResult = result
                )
            }
        }
    }

    fun deleteDetectedFile(item: DetectedFileItem) {
        viewModelScope.launch {
            val deleted = repository.deleteDetectedFile(item)
            if (deleted) {
                val remaining = _uiState.value.detectedFiles.filterNot { it.id == item.id }
                val storage = repository.getStorageOverview()
                _uiState.update { state ->
                    state.copy(
                        detectedFiles = remaining,
                        storageOverview = storage,
                        breakdown = state.breakdown.copy(
                            apkBytes = remaining.filter { it.category == FileCategory.APK_FILE }.sumOf { it.sizeBytes },
                            obsoleteBytes = remaining.filter { it.category == FileCategory.TEMP_FILE }.sumOf { it.sizeBytes },
                            residualBytes = remaining.filter { it.category == FileCategory.RESIDUAL }.sumOf { it.sizeBytes }
                        )
                    )
                }
            }
        }
    }

    fun deleteCategoryFiles(category: FileCategory) {
        viewModelScope.launch {
            val targets = _uiState.value.detectedFiles.filter { it.category == category }
            for (file in targets) {
                repository.deleteDetectedFile(file)
            }
            runDeviceAnalysisScan()
        }
    }

    fun runSecurityScan() {
        if (_uiState.value.isScanningSecurity) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningSecurity = true) }
            val apps = _uiState.value.allApps.ifEmpty {
                repository.loadInstalledApps(includeSystemApps = true)
            }
            val result = repository.runSecurityScan(apps)
            _uiState.update {
                it.copy(
                    allApps = apps,
                    securityScanResult = result,
                    isScanningSecurity = false
                )
            }
        }
    }

    fun runGameBooster() {
        if (_uiState.value.isBoostingMemory) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBoostingMemory = true) }
            val apps = _uiState.value.allApps.ifEmpty {
                repository.loadInstalledApps(includeSystemApps = true)
            }
            val boostResult = repository.boostGameMemory(apps)
            val updatedMemory = repository.getMemoryOverview()
            _uiState.update {
                it.copy(
                    memoryOverview = updatedMemory,
                    gameBoostResult = boostResult,
                    isBoostingMemory = false
                )
            }
        }
    }

    // App Manager Controls
    fun updateAppSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun toggleShowSystemApps(show: Boolean) {
        _uiState.update { it.copy(showSystemApps = show) }
    }

    fun setAppSortMode(mode: AppSortMode) {
        _uiState.update { it.copy(appSortMode = mode) }
    }

    fun toggleSelectPackage(packageName: String) {
        _uiState.update { state ->
            val next = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = next)
        }
    }

    fun toggleSelectAllVisible() {
        _uiState.update { state ->
            val visiblePkgs = state.filteredApps.map { it.packageName }.toSet()
            val allSelected = visiblePkgs.isNotEmpty() && visiblePkgs.all { it in state.selectedPackages }
            val next = if (allSelected) {
                state.selectedPackages - visiblePkgs
            } else {
                state.selectedPackages + visiblePkgs
            }
            state.copy(selectedPackages = next)
        }
    }

    fun cleanSelectedAppsCache() {
        val selected = _uiState.value.selectedPackages
        if (selected.isEmpty() || _uiState.value.isAppActionRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAppActionRunning = true, appActionMessage = null) }
            val targets = _uiState.value.allApps.filter { it.packageName in selected }
            val result = repository.cleanSelectedAppsCache(targets)
            if (!result.usedShizuku && targets.size == 1) {
                val singlePkg = targets.first().packageName
                if (singlePkg != getApplication<Application>().packageName) {
                    repository.openAppDetailsSettings(singlePkg)
                }
            }
            val refreshedApps = repository.loadInstalledApps(includeSystemApps = true)
            val storage = repository.getStorageOverview()
            _uiState.update {
                it.copy(
                    allApps = refreshedApps,
                    storageOverview = storage,
                    isAppActionRunning = false,
                    appActionMessage = result.summaryMessage
                )
            }
        }
    }

    fun clearSelectedAppsStorage() {
        val selected = _uiState.value.selectedPackages
        if (selected.isEmpty() || _uiState.value.isAppActionRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAppActionRunning = true, appActionMessage = null) }
            val targets = _uiState.value.allApps.filter { it.packageName in selected }
            val result = repository.clearSelectedAppsStorage(targets)
            if (!result.usedShizuku && targets.isNotEmpty()) {
                repository.openAppDetailsSettings(targets.first().packageName)
            }
            val refreshedApps = repository.loadInstalledApps(includeSystemApps = true)
            val storage = repository.getStorageOverview()
            _uiState.update {
                it.copy(
                    allApps = refreshedApps,
                    storageOverview = storage,
                    selectedPackages = emptySet(),
                    isAppActionRunning = false,
                    appActionMessage = result.summaryMessage
                )
            }
        }
    }

    fun clearAppActionMessage() {
        _uiState.update { it.copy(appActionMessage = null) }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun openShizukuApp(): Boolean = shizukuManager.openShizukuApp()

    fun openUsageAccessSettings() = repository.openUsageAccessSettings()

    fun openAppDetails(packageName: String) = repository.openAppDetailsSettings(packageName)

    fun uninstallPackage(packageName: String) = repository.uninstallPackage(packageName)
}
