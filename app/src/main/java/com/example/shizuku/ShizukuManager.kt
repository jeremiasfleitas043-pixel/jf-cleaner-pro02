package com.example.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import com.example.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

enum class ShizukuConnectionState {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY
}

data class ShizukuStatus(
    val state: ShizukuConnectionState = ShizukuConnectionState.NOT_RUNNING,
    val uid: Int? = null,
    val apiVersion: Int? = null,
    val isServiceBound: Boolean = false
) {
    val isReady: Boolean get() = state == ShizukuConnectionState.READY
}

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

class ShizukuManager(private val context: Context) {

    companion object {
        private const val SHIZUKU_MANAGER_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private val _status = MutableStateFlow(ShizukuStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val bindMutex = Mutex()
    private var userServiceBinder: IBinder? = null
    private var pendingBindDeferred: CompletableDeferred<IBinder?>? = null

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShizukuUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shizuku_worker")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            userServiceBinder = service
            pendingBindDeferred?.complete(service)
            pendingBindDeferred = null
            refreshStatus()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userServiceBinder = null
            pendingBindDeferred?.complete(null)
            pendingBindDeferred = null
            refreshStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        userServiceBinder = null
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                refreshStatus()
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    // Service will be bound lazily on first command
                }
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
            // Safe fallback if Shizuku provider isn't available in test environment
        }
        refreshStatus()
    }

    fun release() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            if (userServiceBinder != null) {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        } catch (_: Throwable) {
        }
    }

    fun refreshStatus() {
        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

        val installed = binderAlive || isPackageInstalled(SHIZUKU_MANAGER_PACKAGE)
        val state = when {
            !installed -> ShizukuConnectionState.NOT_INSTALLED
            !binderAlive -> ShizukuConnectionState.NOT_RUNNING
            else -> {
                val granted = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (_: Throwable) {
                    false
                }
                if (granted) ShizukuConnectionState.READY else ShizukuConnectionState.PERMISSION_REQUIRED
            }
        }

        val uid = if (state == ShizukuConnectionState.READY) {
            try {
                Shizuku.getUid()
            } catch (_: Throwable) {
                null
            }
        } else null

        val version = if (binderAlive) {
            try {
                Shizuku.getVersion()
            } catch (_: Throwable) {
                null
            }
        } else null

        _status.value = ShizukuStatus(
            state = state,
            uid = uid,
            apiVersion = version,
            isServiceBound = userServiceBinder?.isBinderAlive == true
        )
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } else {
                refreshStatus()
            }
        } catch (_: Throwable) {
            refreshStatus()
        }
    }

    fun openShizukuApp(): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_MANAGER_PACKAGE)
            ?: return false
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    private suspend fun ensureUserServiceBound(): IBinder? {
        refreshStatus()
        if (!_status.value.isReady) return null

        userServiceBinder?.let { binder ->
            if (binder.isBinderAlive) return binder
        }

        return bindMutex.withLock {
            userServiceBinder?.let { binder ->
                if (binder.isBinderAlive) return@withLock binder
            }

            val deferred = CompletableDeferred<IBinder?>()
            pendingBindDeferred = deferred

            withContext(Dispatchers.Main) {
                try {
                    Shizuku.bindUserService(userServiceArgs, serviceConnection)
                } catch (t: Throwable) {
                    deferred.complete(null)
                }
            }

            withTimeoutOrNull(4500L) {
                deferred.await()
            }
        }
    }

    suspend fun execShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        val binder = ensureUserServiceBound()
            ?: return@withContext ShellResult(-1, "", "Shizuku service unavailable")

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(ShizukuUserService.DESCRIPTOR)
            data.writeString(command)
            val ok = binder.transact(ShizukuUserService.TRANSACTION_EXEC_SHELL, data, reply, 0)
            if (!ok) {
                return@withContext ShellResult(-1, "", "Shizuku binder transaction failed")
            }
            reply.readException()
            val exitCode = reply.readInt()
            val stdout = reply.readString().orEmpty()
            val stderr = reply.readString().orEmpty()
            ShellResult(exitCode, stdout, stderr)
        } catch (t: Throwable) {
            ShellResult(-1, "", t.message ?: "Shizuku exec error")
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
