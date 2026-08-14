package com.rudi.audioplayer.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbRepeatLogicTest {

    @Test
    fun `isActive false when either point is null`() {
        assertFalse(AbRepeatLogic.isActive(null, 5000L))
        assertFalse(AbRepeatLogic.isActive(5000L, null))
        assertFalse(AbRepeatLogic.isActive(null, null))
    }

    @Test
    fun `isActive false when B is before or equal to A`() {
        assertFalse(AbRepeatLogic.isActive(5000L, 5000L))
        assertFalse(AbRepeatLogic.isActive(5000L, 4999L))
    }

    @Test
    fun `isActive true when B strictly after A`() {
        assertTrue(AbRepeatLogic.isActive(1000L, 5000L))
    }

    @Test
    fun `shouldLoopBack false when not active`() {
        assertFalse(AbRepeatLogic.shouldLoopBack(9000L, null, 5000L))
        assertFalse(AbRepeatLogic.shouldLoopBack(9000L, 5000L, null))
        assertFalse(AbRepeatLogic.shouldLoopBack(9000L, 5000L, 5000L))
    }

    @Test
    fun `shouldLoopBack false while position is still before B`() {
        assertFalse(AbRepeatLogic.shouldLoopBack(4999L, 1000L, 5000L))
    }

    @Test
    fun `shouldLoopBack true exactly at B`() {
        assertTrue(AbRepeatLogic.shouldLoopBack(5000L, 1000L, 5000L))
    }

    @Test
    fun `shouldLoopBack true past B`() {
        assertTrue(AbRepeatLogic.shouldLoopBack(5001L, 1000L, 5000L))
    }

    @Test
    fun `shouldLoopBack true at position zero when A is zero`() {
        // Guards against an off-by-one that would treat pointA = 0L as "unset" — 0L is a
        // legitimate bookmark/A-point (start of song), distinct from the null sentinel.
        assertTrue(AbRepeatLogic.shouldLoopBack(3000L, 0L, 3000L))
    }
}
