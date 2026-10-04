package com.focusgrowing.app.domain.model

import java.time.LocalDate

enum class StatsRange(val days: Int, val label: String, val isPremium: Boolean) {
    WEEK(7, "7D", false),
    MONTH(30, "30D", true),
    QUARTER(90, "3M", true),
    YEAR(365, "1Y", true),
}

data class DayBucket(val date: LocalDate, val label: String, val focusSeconds: Long)

data class HourSlotShare(val startHour: Int, val endHour: Int, val share: Float, val focusSeconds: Long)

data class FocusStatistics(
    val range: StatsRange,
    val totalFocusSeconds: Long,
    val sessionCount: Int,
    val completedMissions: Int,
    /** Percent change vs the previous period of equal length; null when no previous data. */
    val focusChangePercent: Int?,
    val sessionChangePercent: Int?,
    val buckets: List<DayBucket>,
    val topHourSlots: List<HourSlotShare>,
    val averageSessionSeconds: Long,
    /** 0..1 share of started focus sessions that were completed. */
    val completionRate: Float,
    /** Positive = missions take longer than estimated. Null when not enough data. */
    val estimationErrorPercent: Int?,
    val todayFocusSeconds: Long,
    val weekFocusSeconds: Long,
) {
    companion object {
        fun empty(range: StatsRange) = FocusStatistics(
            range, 0, 0, 0, null, null, emptyList(), emptyList(), 0, 0f, null, 0, 0,
        )
    }
}

data class StreakInfo(
    val currentDays: Int,
    val isActiveToday: Boolean,
    /** Mon..Sun of the current week: true when there was a completed focus session that day. */
    val weekActivity: List<Boolean>,
) {
    companion object { val Empty = StreakInfo(0, false, List(7) { false }) }
}
