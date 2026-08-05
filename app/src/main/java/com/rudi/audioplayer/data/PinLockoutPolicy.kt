package com.rudi.audioplayer.data

/**
 * Pure escalating-lockout formula, pulled out of [AppLockStore] specifically so it can be
 * unit-tested without a Context/SharedPreferences: the first 4 wrong PIN attempts cost
 * nothing (an honest typo never locks anyone out), then each further attempt escalates the
 * lockout — 30s, 1m, 2m, capped at 4m — enough to make repeated guessing impractical without
 * permanently locking the owner out.
 */
internal object PinLockoutPolicy {
    fun lockoutDurationMillis(failCount: Int): Long {
        if (failCount < 5) return 0L
        val step = (failCount - 4).coerceAtMost(4)
        val seconds = 30L * (1L shl (step - 1))
        return seconds.coerceAtMost(240L) * 1000L
    }
}
