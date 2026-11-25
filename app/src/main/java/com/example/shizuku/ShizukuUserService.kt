package com.example.shizuku

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

@Keep
class ShizukuUserService : Binder() {

    companion object {
        const val DESCRIPTOR = "com.example.shizuku.IShizukuShellService"
        const val TRANSACTION_EXEC_SHELL = IBinder.FIRST_CALL_TRANSACTION
        const val TRANSACTION_DESTROY = IBinder.FIRST_CALL_TRANSACTION + 1
        private const val MAX_OUTPUT_CHARS = 512_000
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return when (code) {
            TRANSACTION_EXEC_SHELL -> {
                data.enforceInterface(DESCRIPTOR)
                val command = data.readString().orEmpty()
                val result = executeCommand(command)
                reply?.writeNoException()
                reply?.writeInt(result.exitCode)
                reply?.writeString(result.stdout)
                reply?.writeString(result.stderr)
                true
            }
            TRANSACTION_DESTROY -> {
                data.enforceInterface(DESCRIPTOR)
                reply?.writeNoException()
                exitProcess(0)
            }
            else -> super.onTransact(code, data, reply, flags)
        }
    }

    private fun executeCommand(command: String): ShellExecOutput {
        if (command.isBlank()) return ShellExecOutput(-1, "", "Empty command")
        return try {
            val process = ProcessBuilder("sh", "-c", command).start()
            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val outThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null && stdoutBuilder.length < MAX_OUTPUT_CHARS) {
                        stdoutBuilder.append(line).append('\n')
                        line = reader.readLine()
                    }
                }
            }
            val errThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null && stderrBuilder.length < MAX_OUTPUT_CHARS) {
                        stderrBuilder.append(line).append('\n')
                        line = reader.readLine()
                    }
                }
            }

            outThread.start()
            errThread.start()
            val exitCode = process.waitFor()
            outThread.join(2000)
            errThread.join(2000)

            ShellExecOutput(
                exitCode = exitCode,
                stdout = stdoutBuilder.toString().trimEnd(),
                stderr = stderrBuilder.toString().trimEnd()
            )
        } catch (e: Throwable) {
            ShellExecOutput(-1, "", e.message ?: "Shell execution failed")
        }
    }

    private data class ShellExecOutput(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )
}
