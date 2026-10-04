package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.TimerCalculator
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.TimerPhase
import com.focusgrowing.app.domain.model.TimerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerCalculatorTest {
    private val planned = 25 * 60_000L
    private val running = TimerState(
        phase = TimerPhase.RUNNING,
        startedAt = 0L,
        endAt = planned,
        plannedDurationMillis = planned,
    )

    @Test
    fun `remaining time is derived from the end timestamp`() {
        assertEquals(planned - 60_000L, TimerCalculator.remainingMillis(running, now = 60_000L))
        assertEquals(0L, TimerCalculator.remainingMillis(running, now = planned + 5_000L))
    }

    @Test
    fun `paused state keeps its remaining time regardless of the clock`() {
        val paused = running.copy(phase = TimerPhase.PAUSED, remainingWhenPausedMillis = 90_000L)
        assertEquals(90_000L, TimerCalculator.remainingMillis(paused, now = 10_000_000L))
    }

    @Test
    fun `elapsed detection only for running sessions`() {
        assertTrue(TimerCalculator.isElapsed(running, planned))
        assertFalse(TimerCalculator.isElapsed(running, planned - 1))
        assertFalse(TimerCalculator.isElapsed(running.copy(phase = TimerPhase.PAUSED), planned + 1))
    }

    @Test
    fun `progress goes from 0 to 1`() {
        assertEquals(0f, TimerCalculator.progress(running, 0L), 0.0001f)
        assertEquals(0.5f, TimerCalculator.progress(running, planned / 2), 0.0001f)
        assertEquals(1f, TimerCalculator.progress(running, planned * 2), 0.0001f)
    }

    @Test
    fun `every fourth focus session is followed by a long break`() {
        assertEquals(SessionType.SHORT_BREAK, TimerCalculator.nextBreakType(1, 4))
        assertEquals(SessionType.SHORT_BREAK, TimerCalculator.nextBreakType(3, 4))
        assertEquals(SessionType.LONG_BREAK, TimerCalculator.nextBreakType(4, 4))
        assertEquals(SessionType.LONG_BREAK, TimerCalculator.nextBreakType(8, 4))
    }

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("25:00", TimerCalculator.formatMmSs(planned))
        assertEquals("24:33", TimerCalculator.formatMmSs(24 * 60_000L + 32_100L))
        assertEquals("00:00", TimerCalculator.formatMmSs(0L))
    }
}
