package com.cinnamon.app.data.startup

import android.os.SystemClock
import android.util.Log

/**
 * Low-overhead, opt-in startup timing for local performance diagnosis.
 *
 * The elapsed-time records are emitted only when the `CinnamonStartup` log tag
 * is explicitly enabled at DEBUG level, without recording user content or
 * identifiers.
 */
internal object StartupPerformanceTrace {
    private const val TAG = "CinnamonStartup"

    suspend fun <T> measure(stage: String, block: suspend () -> T): T {
        val startedAtMillis = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                val elapsedMillis = SystemClock.elapsedRealtime() - startedAtMillis
                Log.d(TAG, "$stage=${elapsedMillis}ms")
            }
        }
    }

    fun <T> measureBlocking(stage: String, block: () -> T): T {
        val startedAtMillis = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                val elapsedMillis = SystemClock.elapsedRealtime() - startedAtMillis
                Log.d(TAG, "$stage=${elapsedMillis}ms")
            }
        }
    }
}
