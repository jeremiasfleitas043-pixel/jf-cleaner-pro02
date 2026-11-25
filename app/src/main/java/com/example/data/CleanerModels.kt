package com.example.data

import androidx.compose.ui.graphics.ImageBitmap

data class StorageOverview(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val usedPercent: Int = 0
)

data class MemoryOverview(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val usedPercent: Int = 0,
    val lowMemory: Boolean = false
)

data class BatteryTelemetry(
    val levelPercent: Int = 0,
    val isCharging: Boolean = false,
    val chargingSource: String = "Battery",
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val healthLabel: String = "Good",
    val estimatedSummary: String = ""
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val cacheBytes: Long,
    val storageBytes: Long,
    val codeBytes: Long,
    val dataBytes: Long,
    val hasAccurateUsageStats: Boolean,
    val lastUsedTimestamp: Long,
    val dangerousPermissions: List<String>,
    val isDebuggable: Boolean,
    val isUnknownSource: Boolean,
    val iconBitmap: ImageBitmap? = null
)

enum class FileCategory {
    BIG_FILE,
    TEMP_FILE,
    APK_FILE,
    RESIDUAL
}

data class DetectedFileItem(
    val id: String,
    val name: String,
    val pathOrUri: String,
    val sizeBytes: Long,
    val category: FileCategory,
    val isSafUri: Boolean
)

data class OptimizationBreakdown(
    val isCalculated: Boolean = false,
    val cacheBytes: Long = 0L,
    val reclaimableMemoryBytes: Long = 0L,
    val packageCount: Int = 0,
    val apkBytes: Long = 0L,
    val obsoleteBytes: Long = 0L,
    val residualBytes: Long = 0L
) {
    val totalOptimizableBytes: Long
        get() = cacheBytes + apkBytes + obsoleteBytes + residualBytes
}

data class CleaningOperationResult(
    val completed: Boolean = false,
    val freedStorageBytes: Long = 0L,
    val freedMemoryBytes: Long = 0L,
    val trimmedAppsCount: Int = 0,
    val deletedFilesCount: Int = 0,
    val usedShizuku: Boolean = false,
    val summaryMessage: String = ""
)

data class SecurityFlaggedApp(
    val packageName: String,
    val appName: String,
    val reasons: List<String>,
    val riskLevel: SecurityRiskLevel
)

enum class SecurityRiskLevel {
    INFO,
    REVIEW
}

data class SecurityScanResult(
    val hasScanned: Boolean = false,
    val scannedPackagesCount: Int = 0,
    val flaggedApps: List<SecurityFlaggedApp> = emptyList(),
    val shizukuActive: Boolean = false,
    val usageAccessGranted: Boolean = false
)

data class GameBoostResult(
    val boosted: Boolean = false,
    val beforeAvailableBytes: Long = 0L,
    val afterAvailableBytes: Long = 0L,
    val stoppedPackagesCount: Int = 0,
    val usedShizuku: Boolean = false
) {
    val reclaimedBytes: Long get() = (afterAvailableBytes - beforeAvailableBytes).coerceAtLeast(0L)
}
