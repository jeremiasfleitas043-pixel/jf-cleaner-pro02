package com.example.util

import java.util.Locale

data class FormattedSizeParts(
    val value: String,
    val unit: String
)

object FormatUtils {
    fun splitSizeParts(bytes: Long): FormattedSizeParts {
        if (bytes <= 0L) return FormattedSizeParts("0", "B")
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> FormattedSizeParts(String.format(Locale.US, "%.2f", bytes / gb), "GB")
            bytes >= mb -> {
                val value = bytes / mb
                val pattern = if (value >= 100.0) "%.0f" else "%.1f"
                FormattedSizeParts(String.format(Locale.US, pattern, value), "MB")
            }
            bytes >= kb -> FormattedSizeParts(String.format(Locale.US, "%.0f", bytes / kb), "KB")
            else -> FormattedSizeParts(bytes.toString(), "B")
        }
    }

    fun formatBytes(bytes: Long): String {
        val parts = splitSizeParts(bytes)
        return "${parts.value} ${parts.unit}"
    }

    fun formatBytesCompact(bytes: Long): String {
        val parts = splitSizeParts(bytes)
        return "${parts.value}${parts.unit}"
    }
}
