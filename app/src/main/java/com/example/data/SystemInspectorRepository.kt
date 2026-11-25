package com.example.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.shizuku.ShizukuManager
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SystemInspectorRepository(
    private val context: Context,
    val shizukuManager: ShizukuManager
) {
    private val packageManager: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    companion object {
        private const val BIG_FILE_THRESHOLD_BYTES = 50L * 1024L * 1024L // 50 MB
        private val TEMP_EXTENSIONS = setOf("tmp", "temp", "log", "bak", "exo", "dmp")
        private val SENSITIVE_PERMISSIONS = mapOf(
            "android.permission.READ_SMS" to "Read SMS",
            "android.permission.RECEIVE_SMS" to "Receive SMS",
            "android.permission.CAMERA" to "Camera",
            "android.permission.RECORD_AUDIO" to "Microphone",
            "android.permission.ACCESS_FINE_LOCATION" to "Precise Location",
            "android.permission.READ_CONTACTS" to "Contacts",
            "android.permission.SYSTEM_ALERT_WINDOW" to "Display Over Apps"
        )
        private val TRUSTED_INSTALLERS = setOf(
            "com.android.vending",
            "com.google.android.feedback",
            "com.sec.android.app.samsungapps",
            "com.amazon.venezia"
        )
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getStorageOverview(): StorageOverview {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val available = stat.availableBytes
            val used = (total - available).coerceAtLeast(0L)
            val percent = if (total > 0L) ((used * 100L) / total).toInt().coerceIn(0, 100) else 0
            StorageOverview(
                totalBytes = total,
                availableBytes = available,
                usedBytes = used,
                usedPercent = percent
            )
        } catch (_: Throwable) {
            StorageOverview()
        }
    }

    fun getMemoryOverview(): MemoryOverview {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return MemoryOverview()
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val total = info.totalMem
        val avail = info.availMem
        val used = (total - avail).coerceAtLeast(0L)
        val percent = if (total > 0L) ((used * 100L) / total).toInt().coerceIn(0, 100) else 0
        return MemoryOverview(
            totalRamBytes = total,
            availableRamBytes = avail,
            usedRamBytes = used,
            usedPercent = percent,
            lowMemory = info.lowMemory
        )
    }

    fun getBatteryTelemetry(): BatteryTelemetry {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryTelemetry()
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val percent = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charge"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Power"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "Discharging"
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC = tempTenths / 10f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val healthLabel = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Degraded"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val chargeTimeMs = if (isCharging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                batteryManager?.computeChargeTimeRemaining() ?: -1L
            } catch (_: Throwable) {
                -1L
            }
        } else -1L

        val summary = when {
            status == BatteryManager.BATTERY_STATUS_FULL -> "100% Charged"
            isCharging && chargeTimeMs > 0L -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(chargeTimeMs).coerceAtLeast(1L)
                if (mins >= 60) "${mins / 60}h ${mins % 60}m until full" else "$mins min until full"
            }
            isCharging -> "$percent% · $source"
            else -> "$percent% · ${"%.1f".format(tempC)}°C"
        }

        return BatteryTelemetry(
            levelPercent = percent,
            isCharging = isCharging,
            chargingSource = source,
            temperatureCelsius = tempC,
            voltageMv = voltage,
            healthLabel = healthLabel,
            estimatedSummary = summary
        )
    }

    suspend fun loadInstalledApps(includeSystemApps: Boolean = false): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val usageGranted = hasUsageStatsPermission()
            val usageMap = if (usageGranted) queryLastUsedTimestamps() else emptyMap()
            val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
            } else null

            val packages = getInstalledPackageInfos()
            val results = mutableListOf<InstalledAppInfo>()

            for (pkgInfo in packages) {
                val appInfo = pkgInfo.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                if (!includeSystemApps && isSystem) continue

                val pkgName = pkgInfo.packageName
                val appName = try {
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Throwable) {
                    pkgName
                }

                val canQueryStats = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    (usageGranted || pkgName == context.packageName)

                var cacheBytes = 0L
                var codeBytes = 0L
                var dataBytes = 0L
                var accurateStats = false

                if (canQueryStats && storageStatsManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val stats = storageStatsManager.queryStatsForPackage(
                            StorageManager.UUID_DEFAULT,
                            pkgName,
                            Process.myUserHandle()
                        )
                        cacheBytes = stats.cacheBytes
                        codeBytes = stats.appBytes
                        dataBytes = (stats.dataBytes - stats.cacheBytes).coerceAtLeast(0L)
                        accurateStats = true
                    } catch (_: Throwable) {
                    }
                }

                if (!accurateStats) {
                    codeBytes = try {
                        val source = appInfo.sourceDir
                        if (source != null) File(source).length() else 0L
                    } catch (_: Throwable) {
                        0L
                    }
                }

                val totalStorage = if (accurateStats) {
                    codeBytes + dataBytes + cacheBytes
                } else {
                    codeBytes
                }

                val requestedPerms = pkgInfo.requestedPermissions?.toList().orEmpty()
                val dangerousPerms = requestedPerms.mapNotNull { SENSITIVE_PERMISSIONS[it] }.distinct()
                val isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val installer = getInstallerPackage(pkgName)
                val isUnknownSource = !isSystem && installer != null && installer !in TRUSTED_INSTALLERS

                val icon = iconCache[pkgName] ?: loadAppIconBitmap(appInfo)?.also {
                    iconCache[pkgName] = it
                }

                results.add(
                    InstalledAppInfo(
                        packageName = pkgName,
                        appName = appName,
                        versionName = pkgInfo.versionName ?: "",
                        isSystemApp = isSystem,
                        cacheBytes = cacheBytes,
                        storageBytes = totalStorage,
                        codeBytes = codeBytes,
                        dataBytes = dataBytes,
                        hasAccurateUsageStats = accurateStats,
                        lastUsedTimestamp = usageMap[pkgName] ?: 0L,
                        dangerousPermissions = dangerousPerms,
                        isDebuggable = isDebuggable,
                        isUnknownSource = isUnknownSource,
                        iconBitmap = icon
                    )
                )
            }

            results.sortedByDescending { it.storageBytes }
        }

    private fun getInstalledPackageInfos(): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun getInstallerPackage(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryLastUsedTimestamps(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(90)
        return try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, start, end)
            val map = mutableMapOf<String, Long>()
            for (stat in stats) {
                val existing = map[stat.packageName] ?: 0L
                if (stat.lastTimeUsed > existing) {
                    map[stat.packageName] = stat.lastTimeUsed
                }
            }
            map
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun loadAppIconBitmap(appInfo: ApplicationInfo): ImageBitmap? {
        return try {
            val drawable: Drawable = packageManager.getApplicationIcon(appInfo)
            val size = 96
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                Bitmap.createScaledBitmap(drawable.bitmap, size, size, true)
            } else {
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                bmp
            }
            bitmap.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun scanDetectedFiles(safTreeUris: List<Uri>): List<DetectedFileItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<DetectedFileItem>()
            val seenPaths = HashSet<String>()

            // 1. Scan own app cache/codeCache/externalCache dirs (always accessible)
            listOfNotNull(
                context.cacheDir,
                context.codeCacheDir,
                context.externalCacheDir
            ).forEach { dir ->
                scanLocalDirectory(dir, items, seenPaths)
            }

            // 2. If Shizuku is active, run real shell find scan on /sdcard
            if (shizukuManager.status.value.isReady) {
                scanViaShizuku(items, seenPaths)
            }

            // 3. Scan any user-authorized SAF document trees
            for (treeUri in safTreeUris) {
                scanSafTree(treeUri, items, seenPaths)
            }

            items.sortedByDescending { it.sizeBytes }
        }

    private fun scanLocalDirectory(
        dir: File,
        out: MutableList<DetectedFileItem>,
        seen: MutableSet<String>
    ) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.walkTopDown().maxDepth(5).forEach { file ->
            if (file.isFile && file.length() > 0L && seen.add(file.absolutePath)) {
                val ext = file.extension.lowercase()
                val category = when {
                    ext == "apk" -> FileCategory.APK_FILE
                    ext in TEMP_EXTENSIONS -> FileCategory.TEMP_FILE
                    file.length() >= BIG_FILE_THRESHOLD_BYTES -> FileCategory.BIG_FILE
                    else -> FileCategory.RESIDUAL
                }
                out.add(
                    DetectedFileItem(
                        id = file.absolutePath,
                        name = file.name,
                        pathOrUri = file.absolutePath,
                        sizeBytes = file.length(),
                        category = category,
                        isSafUri = false
                    )
                )
            }
        }
    }

    private suspend fun scanViaShizuku(
        out: MutableList<DetectedFileItem>,
        seen: MutableSet<String>
    ) {
        // Big files (>50MB), temp files, and APK files in shared storage
        val cmd = """
            find /sdcard -maxdepth 5 -type f \( -size +50M -o -iname "*.apk" -o -iname "*.tmp" -o -iname "*.temp" -o -iname "*.log" -o -iname "*.bak" -o -iname "*.exo" \) -exec stat -c "%s|%n" {} + 2>/dev/null | head -n 250
        """.trimIndent()

        val result = shizukuManager.execShell(cmd)
        if (!result.isSuccess && result.stdout.isBlank()) return

        result.stdout.lineSequence().forEach { line ->
            val sep = line.indexOf('|')
            if (sep <= 0) return@forEach
            val size = line.substring(0, sep).trim().toLongOrNull() ?: return@forEach
            val path = line.substring(sep + 1).trim()
            if (path.isEmpty() || !seen.add(path)) return@forEach

            val name = path.substringAfterLast('/')
            val ext = name.substringAfterLast('.', "").lowercase()
            val category = when {
                ext == "apk" -> FileCategory.APK_FILE
                ext in TEMP_EXTENSIONS -> FileCategory.TEMP_FILE
                size >= BIG_FILE_THRESHOLD_BYTES -> FileCategory.BIG_FILE
                else -> FileCategory.RESIDUAL
            }
            out.add(
                DetectedFileItem(
                    id = path,
                    name = name,
                    pathOrUri = path,
                    sizeBytes = size,
                    category = category,
                    isSafUri = false
                )
            )
        }
    }

    private fun scanSafTree(
        treeUri: Uri,
        out: MutableList<DetectedFileItem>,
        seen: MutableSet<String>
    ) {
        val rootDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Throwable) {
            return
        }
        walkSafFolder(treeUri, rootDocId, 0, out, seen)
    }

    private fun walkSafFolder(
        treeUri: Uri,
        parentDocId: String,
        depth: Int,
        out: MutableList<DetectedFileItem>,
        seen: MutableSet<String>
    ) {
        if (depth > 4 || out.size >= 400) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext() && out.size < 400) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val size = cursor.getLong(sizeIdx)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        walkSafFolder(treeUri, docId, depth + 1, out, seen)
                    } else if (size > 0L) {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        val category = when {
                            ext == "apk" -> FileCategory.APK_FILE
                            ext in TEMP_EXTENSIONS -> FileCategory.TEMP_FILE
                            size >= BIG_FILE_THRESHOLD_BYTES -> FileCategory.BIG_FILE
                            else -> null
                        }
                        if (category != null) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
                            if (seen.add(docUri)) {
                                out.add(
                                    DetectedFileItem(
                                        id = docUri,
                                        name = name,
                                        pathOrUri = docUri,
                                        sizeBytes = size,
                                        category = category,
                                        isSafUri = true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun runFullOptimization(
        apps: List<InstalledAppInfo>,
        detectedFiles: List<DetectedFileItem>
    ): CleaningOperationResult = withContext(Dispatchers.IO) {
        val beforeStorage = getStorageOverview()
        val beforeMemory = getMemoryOverview()
        val shizukuActive = shizukuManager.status.value.isReady

        var freedBytes = 0L
        var trimmedApps = 0
        var deletedFiles = 0

        // 1. Clean own app cache dirs
        freedBytes += clearOwnCache()

        // 2. Delete detected temp/obsolete files (never auto-delete user big files or APKs without asking)
        val autoCleanableTemps = detectedFiles.filter {
            it.category == FileCategory.TEMP_FILE || it.category == FileCategory.RESIDUAL
        }
        for (item in autoCleanableTemps) {
            if (deleteDetectedFile(item)) {
                freedBytes += item.sizeBytes
                deletedFiles++
            }
        }

        // 3. Shizuku system cache trim + external cache cleanup
        if (shizukuActive) {
            val trimResult = shizukuManager.execShell("pm trim-caches 999999999999")
            if (trimResult.isSuccess) {
                trimmedApps = apps.count { !it.isSystemApp && it.cacheBytes > 0L }
                    .coerceAtLeast(1)
            }
            // Clear external app cache directories accessible via shell
            for (app in apps.filter { !it.isSystemApp && it.packageName != context.packageName }) {
                if (app.cacheBytes > 0L) {
                    val escaped = shellQuote(app.packageName)
                    shizukuManager.execShell("rm -rf /sdcard/Android/data/$escaped/cache/* 2>/dev/null")
                }
            }
        }

        // 4. Trim background processes RAM
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        for (app in apps) {
            if (!app.isSystemApp &&
                app.packageName != context.packageName &&
                app.packageName != "moe.shizuku.privileged.api"
            ) {
                try {
                    am?.killBackgroundProcesses(app.packageName)
                } catch (_: Throwable) {
                }
            }
        }

        val afterStorage = getStorageOverview()
        val afterMemory = getMemoryOverview()
        val storageDelta = (afterStorage.availableBytes - beforeStorage.availableBytes).coerceAtLeast(0L)
        val totalFreed = maxOf(freedBytes, storageDelta)
        val memDelta = (afterMemory.availableRamBytes - beforeMemory.availableRamBytes).coerceAtLeast(0L)

        val message = buildString {
            if (shizukuActive) {
                append("Shizuku trimmed system caches and background processes. ")
            } else {
                append("Standard Android cleanup completed. Connect Shizuku to trim third-party app caches system-wide. ")
            }
            if (totalFreed > 0L) {
                append("Reclaimed ${FormatUtils.formatBytes(totalFreed)} storage.")
            } else {
                append("No additional reclaimable cache bytes reported by system.")
            }
        }

        CleaningOperationResult(
            completed = true,
            freedStorageBytes = totalFreed,
            freedMemoryBytes = memDelta,
            trimmedAppsCount = trimmedApps,
            deletedFilesCount = deletedFiles,
            usedShizuku = shizukuActive,
            summaryMessage = message
        )
    }

    suspend fun cleanSelectedAppsCache(packages: List<InstalledAppInfo>): CleaningOperationResult =
        withContext(Dispatchers.IO) {
            val shizukuActive = shizukuManager.status.value.isReady
            val beforeStorage = getStorageOverview()
            var clearedCount = 0
            var estimatedFreed = 0L

            for (app in packages) {
                if (app.packageName == context.packageName) {
                    val ownFreed = clearOwnCache()
                    estimatedFreed += ownFreed
                    clearedCount++
                    continue
                }
                if (shizukuActive) {
                    val pkg = shellQuote(app.packageName)
                    val cacheBefore = app.cacheBytes
                    // Try Android 12+ cache-only clear or external cache removal
                    val res1 = shizukuManager.execShell("pm clear --cache-only $pkg 2>/dev/null || rm -rf /sdcard/Android/data/$pkg/cache/* 2>/dev/null")
                    if (res1.isSuccess) {
                        clearedCount++
                        estimatedFreed += cacheBefore
                    }
                }
            }

            if (shizukuActive && packages.size > 1) {
                shizukuManager.execShell("pm trim-caches 999999999999")
            }

            val afterStorage = getStorageOverview()
            val storageDelta = (afterStorage.availableBytes - beforeStorage.availableBytes).coerceAtLeast(0L)
            val finalFreed = maxOf(estimatedFreed, storageDelta)

            val summary = if (shizukuActive) {
                "Cleared cache for $clearedCount selected app(s) via Shizuku."
            } else {
                "Cleared internal app cache. Enable Shizuku to clear third-party app caches automatically, or open App Info."
            }

            CleaningOperationResult(
                completed = true,
                freedStorageBytes = finalFreed,
                trimmedAppsCount = clearedCount,
                usedShizuku = shizukuActive,
                summaryMessage = summary
            )
        }

    suspend fun clearSelectedAppsStorage(packages: List<InstalledAppInfo>): CleaningOperationResult =
        withContext(Dispatchers.IO) {
            val shizukuActive = shizukuManager.status.value.isReady
            val beforeStorage = getStorageOverview()
            var clearedCount = 0
            var estimatedFreed = 0L

            if (shizukuActive) {
                for (app in packages) {
                    if (app.packageName == context.packageName ||
                        app.packageName == "moe.shizuku.privileged.api"
                    ) continue
                    val pkg = shellQuote(app.packageName)
                    val res = shizukuManager.execShell("pm clear $pkg")
                    if (res.isSuccess && res.stdout.contains("Success", ignoreCase = true)) {
                        clearedCount++
                        estimatedFreed += (app.dataBytes + app.cacheBytes)
                    }
                }
            }

            val afterStorage = getStorageOverview()
            val storageDelta = (afterStorage.availableBytes - beforeStorage.availableBytes).coerceAtLeast(0L)
            val finalFreed = maxOf(estimatedFreed, storageDelta)

            val summary = if (shizukuActive) {
                "Cleared app data & storage for $clearedCount app(s) via Shizuku."
            } else {
                "Clearing third-party app storage requires Shizuku or manual confirmation in Android App Info."
            }

            CleaningOperationResult(
                completed = true,
                freedStorageBytes = finalFreed,
                trimmedAppsCount = clearedCount,
                usedShizuku = shizukuActive,
                summaryMessage = summary
            )
        }

    suspend fun deleteDetectedFile(item: DetectedFileItem): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (item.isSafUri) {
                DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(item.pathOrUri))
            } else {
                val localFile = File(item.pathOrUri)
                if (localFile.exists() && localFile.canWrite()) {
                    localFile.delete()
                } else if (shizukuManager.status.value.isReady) {
                    val escaped = shellQuote(item.pathOrUri)
                    shizukuManager.execShell("rm -f $escaped").isSuccess
                } else {
                    false
                }
            }
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun runSecurityScan(apps: List<InstalledAppInfo>): SecurityScanResult =
        withContext(Dispatchers.IO) {
            val flagged = mutableListOf<SecurityFlaggedApp>()
            for (app in apps) {
                if (app.isSystemApp) continue
                val reasons = mutableListOf<String>()
                if (app.isDebuggable) reasons.add("Debuggable build enabled")
                if (app.isUnknownSource) reasons.add("Installed outside primary app store")
                if (app.dangerousPermissions.size >= 4) {
                    reasons.add("${app.dangerousPermissions.size} sensitive permissions (${app.dangerousPermissions.take(3).joinToString(", ")})")
                }
                if (reasons.isNotEmpty()) {
                    val level = if (app.isDebuggable || app.isUnknownSource) {
                        SecurityRiskLevel.REVIEW
                    } else {
                        SecurityRiskLevel.INFO
                    }
                    flagged.add(
                        SecurityFlaggedApp(
                            packageName = app.packageName,
                            appName = app.appName,
                            reasons = reasons,
                            riskLevel = level
                        )
                    )
                }
            }
            SecurityScanResult(
                hasScanned = true,
                scannedPackagesCount = apps.size,
                flaggedApps = flagged.sortedByDescending { it.riskLevel == SecurityRiskLevel.REVIEW },
                shizukuActive = shizukuManager.status.value.isReady,
                usageAccessGranted = hasUsageStatsPermission()
            )
        }

    suspend fun boostGameMemory(apps: List<InstalledAppInfo>): GameBoostResult =
        withContext(Dispatchers.IO) {
            val before = getMemoryOverview().availableRamBytes
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val shizukuActive = shizukuManager.status.value.isReady
            var stoppedCount = 0

            val targets = apps.filter {
                !it.isSystemApp &&
                    it.packageName != context.packageName &&
                    it.packageName != "moe.shizuku.privileged.api"
            }

            if (shizukuActive) {
                shizukuManager.execShell("am kill-all")
                for (app in targets.take(25)) {
                    val pkg = shellQuote(app.packageName)
                    val res = shizukuManager.execShell("am force-stop $pkg")
                    if (res.isSuccess) stoppedCount++
                }
            } else {
                for (app in targets) {
                    try {
                        am?.killBackgroundProcesses(app.packageName)
                        stoppedCount++
                    } catch (_: Throwable) {
                    }
                }
            }

            val after = getMemoryOverview().availableRamBytes
            GameBoostResult(
                boosted = true,
                beforeAvailableBytes = before,
                afterAvailableBytes = after,
                stoppedPackagesCount = stoppedCount,
                usedShizuku = shizukuActive
            )
        }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAppDetailsSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun uninstallPackage(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun clearOwnCache(): Long {
        var freed = 0L
        listOfNotNull(context.cacheDir, context.codeCacheDir, context.externalCacheDir).forEach { dir ->
            if (dir.exists()) {
                dir.walkBottomUp().forEach { file ->
                    if (file.isFile) {
                        val size = file.length()
                        if (file.delete()) {
                            freed += size
                        }
                    }
                }
            }
        }
        return freed
    }

    private fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\\''") + "'"
    }
}
