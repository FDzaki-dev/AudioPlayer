package com.rudi.audioplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PinLockoutPolicyTest {

    @Test
    fun `first four wrong attempts cost nothing`() {
        for (fails in 0..4) {
            assertEquals("failCount=$fails should not lock out", 0L, PinLockoutPolicy.lockoutDurationMillis(fails))
        }
    }

    @Test
    fun `fifth attempt locks out for 30 seconds`() {
        assertEquals(30_000L, PinLockoutPolicy.lockoutDurationMillis(5))
    }

    @Test
    fun `lockout escalates on each further attempt`() {
        assertEquals(30_000L, PinLockoutPolicy.lockoutDurationMillis(5))
        assertEquals(60_000L, PinLockoutPolicy.lockoutDurationMillis(6))
        assertEquals(120_000L, PinLockoutPolicy.lockoutDurationMillis(7))
        assertEquals(240_000L, PinLockoutPolicy.lockoutDurationMillis(8))
    }

    @Test
    fun `lockout duration is capped at four minutes no matter how many further attempts`() {
        assertEquals(240_000L, PinLockoutPolicy.lockoutDurationMillis(9))
        assertEquals(240_000L, PinLockoutPolicy.lockoutDurationMillis(20))
        assertEquals(240_000L, PinLockoutPolicy.lockoutDurationMillis(1000))
    }
}
