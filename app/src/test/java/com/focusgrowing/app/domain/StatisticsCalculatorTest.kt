package com.focusgrowing.app.domain

import com.focusgrowing.app.domain.logic.StatisticsCalculator
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.StatsRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

class StatisticsCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 9, 24)

    private fun session(daysAgo: Long, hour: Int, minutes: Long, completed: Boolean = true) = FocusSession(
        missionId = null,
        type = SessionType.FOCUS,
        startedAt = LocalDateTime.of(today.minusDays(daysAgo), LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli(),
        endedAt = null,
        plannedDurationSeconds = 25 * 60,
        actualDurationSeconds = minutes * 60,
        completed = completed,
        interruptionCount = 0,
        xpEarned = 0,
    )

    @Test
    fun `week totals, sessions and change vs previous week`() {
        val sessions = listOf(
            session(0, 9, 25), session(1, 9, 25), session(2, 15, 25), session(3, 20, 10, completed = false),
            session(8, 9, 25), session(9, 9, 25), // previous week: 50 minutes
        )
        val stats = StatisticsCalculator.calculate(StatsRange.WEEK, sessions, emptyList(), zone, today, Locale.US)
        assertEquals(85 * 60L, stats.totalFocusSeconds)
        assertEquals(3, stats.sessionCount)
        assertEquals(70, stats.focusChangePercent) // 85 vs 50 minutes
        assertEquals(7, stats.buckets.size)
        assertEquals(0.75f, stats.completionRate, 0.001f)
        assertEquals(25 * 60L, stats.averageSessionSeconds)
        assertEquals(8, stats.topHourSlots.first().startHour)
    }

    @Test
    fun `no previous data means no percent change`() {
        val stats = StatisticsCalculator.calculate(StatsRange.WEEK, listOf(session(0, 9, 25)), emptyList(), zone, today, Locale.US)
        assertNull(stats.focusChangePercent)
    }

    @Test
    fun `estimation error needs at least two finished missions`() {
        val now = today.atStartOfDay(zone).toInstant().toEpochMilli()
        fun mission(est: Int, done: Int) = Mission(
            title = "m", estimatedPomodoros = est, completedPomodoros = done,
            status = MissionStatus.COMPLETED, createdAt = now, completedAt = now,
        )
        assertNull(StatisticsCalculator.estimationError(listOf(mission(4, 5)), 0))
        assertEquals(25, StatisticsCalculator.estimationError(listOf(mission(4, 5), mission(4, 5)), 0))
    }

    @Test
    fun `year range groups by month`() {
        val stats = StatisticsCalculator.calculate(StatsRange.YEAR, listOf(session(0, 9, 25)), emptyList(), zone, today, Locale.US)
        assertEquals(13, stats.buckets.size)
        assertEquals(25 * 60L, stats.buckets.last().focusSeconds)
    }
}
