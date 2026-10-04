package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StreakCalculatorTest {
    private val zone = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 9, 24) // Thursday

    private fun at(date: LocalDate, hour: Int = 10) =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `consecutive days ending today`() {
        val times = listOf(at(today), at(today.minusDays(1)), at(today.minusDays(2)), at(today.minusDays(4)))
        val streak = StreakCalculator.calculate(times, zone, today)
        assertEquals(3, streak.currentDays)
        assertTrue(streak.isActiveToday)
    }

    @Test
    fun `streak survives until the user focuses today`() {
        val times = listOf(at(today.minusDays(1)), at(today.minusDays(2)))
        val streak = StreakCalculator.calculate(times, zone, today)
        assertEquals(2, streak.currentDays)
        assertFalse(streak.isActiveToday)
    }

    @Test
    fun `a missed day resets the streak`() {
        val times = listOf(at(today.minusDays(2)), at(today.minusDays(3)))
        assertEquals(0, StreakCalculator.calculate(times, zone, today).currentDays)
    }

    @Test
    fun `days are counted in the user's time zone`() {
        // 23:30 in Istanbul is still "today" even though it's a different UTC hour.
        val late = LocalDateTime.of(today, java.time.LocalTime.of(23, 30)).atZone(zone).toInstant().toEpochMilli()
        val streak = StreakCalculator.calculate(listOf(late), zone, today)
        assertTrue(streak.isActiveToday)
    }

    @Test
    fun `week activity is monday to sunday`() {
        val times = listOf(at(LocalDate.of(2026, 9, 21)), at(today)) // Monday + Thursday
        val week = StreakCalculator.calculate(times, zone, today).weekActivity
        assertEquals(listOf(true, false, false, true, false, false, false), week)
    }
}
