package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.StreakInfo
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A streak is the number of consecutive local days (in the user's time zone) with at least
 * one completed focus session, ending today — or yesterday, so the streak is not lost
 * before the user had a chance to focus today.
 */
object StreakCalculator {

    fun calculate(completedFocusStartTimes: List<Long>, zone: ZoneId, today: LocalDate): StreakInfo {
        val days: Set<LocalDate> = completedFocusStartTimes
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSet()

        val activeToday = today in days
        var cursor = if (activeToday) today else today.minusDays(1)
        var count = 0
        while (cursor in days) {
            count++
            cursor = cursor.minusDays(1)
        }

        val monday = today.with(DayOfWeek.MONDAY)
        val week = (0 until 7).map { monday.plusDays(it.toLong()) in days }

        return StreakInfo(currentDays = count, isActiveToday = activeToday, weekActivity = week)
    }
}
