package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.DayBucket
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.FocusStatistics
import com.focusgrowing.app.domain.model.HourSlotShare
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.StatsRange
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** Pure statistics math. Everything is computed on-device from local sessions. */
object StatisticsCalculator {

    private const val SLOT_HOURS = 2

    fun calculate(
        range: StatsRange,
        sessions: List<FocusSession>,
        missions: List<Mission>,
        zone: ZoneId,
        today: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): FocusStatistics {
        val periodStart = today.minusDays((range.days - 1).toLong())
        val previousStart = periodStart.minusDays(range.days.toLong())
        val periodStartMillis = periodStart.startMillis(zone)
        val previousStartMillis = previousStart.startMillis(zone)

        val focus = sessions.filter { it.type == SessionType.FOCUS }
        val current = focus.filter { it.startedAt >= periodStartMillis }
        val previous = focus.filter { it.startedAt in previousStartMillis until periodStartMillis }
        val currentCompleted = current.filter { it.completed }

        val totalSeconds = current.sumOf { it.actualDurationSeconds }
        val previousSeconds = previous.sumOf { it.actualDurationSeconds }
        val sessionCount = currentCompleted.size
        val previousCount = previous.count { it.completed }

        val completedMissions = missions.count { m ->
            val done = m.completedAt ?: return@count false
            done >= periodStartMillis
        }

        val todayStart = today.startMillis(zone)
        val weekStart = today.with(DayOfWeek.MONDAY).startMillis(zone)

        return FocusStatistics(
            range = range,
            totalFocusSeconds = totalSeconds,
            sessionCount = sessionCount,
            completedMissions = completedMissions,
            focusChangePercent = percentChange(totalSeconds, previousSeconds),
            sessionChangePercent = percentChange(sessionCount.toLong(), previousCount.toLong()),
            buckets = buckets(range, current, zone, periodStart, today, locale),
            topHourSlots = topHourSlots(current, zone),
            averageSessionSeconds = if (currentCompleted.isEmpty()) 0L
            else currentCompleted.sumOf { it.actualDurationSeconds } / currentCompleted.size,
            completionRate = if (current.isEmpty()) 0f else currentCompleted.size.toFloat() / current.size,
            estimationErrorPercent = estimationError(missions, periodStartMillis),
            todayFocusSeconds = focus.filter { it.startedAt >= todayStart }.sumOf { it.actualDurationSeconds },
            weekFocusSeconds = focus.filter { it.startedAt >= weekStart }.sumOf { it.actualDurationSeconds },
        )
    }

    fun percentChange(current: Long, previous: Long): Int? {
        if (previous <= 0L) return null
        return (((current - previous).toDouble() / previous) * 100).roundToInt()
    }

    /** Positive when missions needed more pomodoros than estimated. */
    fun estimationError(missions: List<Mission>, sinceMillis: Long): Int? {
        val finished = missions.filter { m ->
            val done = m.completedAt ?: return@filter false
            done >= sinceMillis && m.estimatedPomodoros > 0
        }
        if (finished.size < 2) return null
        val avg = finished.map {
            (it.completedPomodoros - it.estimatedPomodoros).toDouble() / it.estimatedPomodoros
        }.average()
        return (avg * 100).roundToInt()
    }

    private fun buckets(
        range: StatsRange,
        sessions: List<FocusSession>,
        zone: ZoneId,
        periodStart: LocalDate,
        today: LocalDate,
        locale: Locale,
    ): List<DayBucket> {
        val byDate: Map<LocalDate, Long> = sessions
            .groupBy { it.startedAt.toLocalDate(zone) }
            .mapValues { (_, list) -> list.sumOf { it.actualDurationSeconds } }

        return when {
            range.days <= 31 -> (0 until range.days).map { offset ->
                val date = periodStart.plusDays(offset.toLong())
                val label = if (range.days <= 7) date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                else date.dayOfMonth.toString()
                DayBucket(date, label, byDate[date] ?: 0L)
            }
            range.days <= 120 -> {
                val weeks = (ChronoUnit.DAYS.between(periodStart, today) / 7 + 1).toInt()
                (0 until weeks).map { w ->
                    val start = periodStart.plusWeeks(w.toLong())
                    val sum = (0 until 7).sumOf { d -> byDate[start.plusDays(d.toLong())] ?: 0L }
                    DayBucket(start, "${start.dayOfMonth}/${start.monthValue}", sum)
                }
            }
            else -> {
                val firstMonth = periodStart.withDayOfMonth(1)
                val months = (ChronoUnit.MONTHS.between(firstMonth, today.withDayOfMonth(1)) + 1).toInt()
                (0 until months).map { m ->
                    val month = firstMonth.plusMonths(m.toLong())
                    val sum = byDate.filterKeys { it.year == month.year && it.month == month.month }.values.sum()
                    DayBucket(month, month.month.getDisplayName(TextStyle.SHORT, locale), sum)
                }
            }
        }
    }

    private fun topHourSlots(sessions: List<FocusSession>, zone: ZoneId): List<HourSlotShare> {
        val total = sessions.sumOf { it.actualDurationSeconds }
        if (total <= 0L) return emptyList()
        return sessions
            .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).hour / SLOT_HOURS }
            .map { (slot, list) ->
                val seconds = list.sumOf { it.actualDurationSeconds }
                HourSlotShare(
                    startHour = slot * SLOT_HOURS,
                    endHour = slot * SLOT_HOURS + SLOT_HOURS,
                    share = seconds.toFloat() / total,
                    focusSeconds = seconds,
                )
            }
            .sortedByDescending { it.focusSeconds }
            .take(3)
    }

    private fun LocalDate.startMillis(zone: ZoneId): Long = atStartOfDay(zone).toInstant().toEpochMilli()
    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
