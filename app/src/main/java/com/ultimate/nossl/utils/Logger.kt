
package com.ultimate.nossl.utils

import de.robv.android.xposed.XposedBridge

object Logger {
    private const val TAG = "[ULTIMATE]"

    enum class Level { DEBUG, INFO, WARN, ERROR }
    private var minLevel: Level = Level.INFO
    private val logCounts = java.util.concurrent.ConcurrentHashMap<Level, Int>()

    @Volatile
    var isDebug: Boolean = false
        private set

    init {
        logCounts[Level.DEBUG] = 0
        logCounts[Level.INFO] = 0
        logCounts[Level.WARN] = 0
        logCounts[Level.ERROR] = 0
    }

    fun setDebug(enabled: Boolean) { isDebug = enabled }
    fun setMinLevel(level: Level) { minLevel = level }

    fun d(msg: String) {
        if (!isDebug) return
        log(Level.DEBUG, msg)
    }

    fun i(msg: String) = log(Level.INFO, msg)

    fun w(msg: String) = log(Level.WARN, msg)

    fun e(msg: String, t: Throwable? = null) {
        log(Level.ERROR, msg)
        t?.let { XposedBridge.log(it) }
    }

    fun hook(cls: String, method: String) = i("HOOKED: $cls.$method")
    fun native(func: String) = i("NATIVE: $func")
    fun scan(msg: String) = i("SCAN: $msg")

    fun getLogCount(level: Level): Int = logCounts[level] ?: 0
    fun getTotalLogCount(): Int = logCounts.values.sum()

    fun resetCounts() {
        logCounts.keys.forEach { logCounts[it] = 0 }
    }

    fun getFormattedCounts(): String =
        "D:${logCounts[Level.DEBUG] ?: 0} I:${logCounts[Level.INFO] ?: 0} W:${logCounts[Level.WARN] ?: 0} E:${logCounts[Level.ERROR] ?: 0} T:${getTotalLogCount()}"

    private fun log(level: Level, msg: String) {
        logCounts.merge(level, 1) { old, _ -> old + 1 }
        if (level.ordinal < minLevel.ordinal) return
        val prefix = when (level) {
            Level.DEBUG -> "D"
            Level.INFO -> "I"
            Level.WARN -> "W"
            Level.ERROR -> "E"
        }
        XposedBridge.log("$TAG [$prefix] $msg")
    }
}
