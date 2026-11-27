package com.erikraft.soundstickers

import kotlin.math.abs

internal object NativeExportRules {
    val supportedFps = listOf(30, 25, 20, 15, 12, 10, 8, 6, 5)

    fun chooseFps(sourceFps: Double?, isImage: Boolean): Int {
        if (isImage) return 5
        val measured = sourceFps
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?.coerceAtMost(30.0)
            ?: 30.0

        return supportedFps.minWithOrNull(
            compareBy<Int>({ abs(it - measured) }, { -it })
        ) ?: 30
    }

    fun resolveFps(selection: String?, sourceFps: Double?, isImage: Boolean): Int? {
        if (selection == null || selection == "auto") {
            return chooseFps(sourceFps, isImage)
        }
        val parsed = selection.toIntOrNull() ?: return null
        return parsed.takeIf { it in supportedFps }
    }

    fun isValidClip(startSeconds: Double, endSeconds: Double, durationSeconds: Double): Boolean {
        if (!startSeconds.isFinite() || !endSeconds.isFinite() || !durationSeconds.isFinite()) {
            return false
        }
        if (startSeconds < 0.0 || endSeconds - startSeconds < 0.1 || durationSeconds <= 0.0) {
            return false
        }
        if (endSeconds > durationSeconds + 0.001) return false
        return endSeconds - startSeconds <= 10.0 + 0.001
    }
}
