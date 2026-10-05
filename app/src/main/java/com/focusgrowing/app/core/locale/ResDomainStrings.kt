package com.focusgrowing.app.core.locale

import com.focusgrowing.app.R
import com.focusgrowing.app.domain.model.WorldItemType
import com.focusgrowing.app.domain.repository.DayPart
import com.focusgrowing.app.domain.repository.DomainStrings
import javax.inject.Inject
import javax.inject.Singleton

/** The domain layer's texts, read from the translated string resources. */
@Singleton
class ResDomainStrings @Inject constructor(
    private val strings: StringProvider,
) : DomainStrings {

    private fun DayPart.titleRes() = when (this) {
        DayPart.MORNING -> R.string.insight_time_of_day_title_morning
        DayPart.AFTERNOON -> R.string.insight_time_of_day_title_afternoon
        DayPart.EVENING -> R.string.insight_time_of_day_title_evening
        DayPart.NIGHT -> R.string.insight_time_of_day_title_night
    }

    private fun DayPart.messageRes() = when (this) {
        DayPart.MORNING -> R.string.insight_time_of_day_message_morning
        DayPart.AFTERNOON -> R.string.insight_time_of_day_message_afternoon
        DayPart.EVENING -> R.string.insight_time_of_day_message_evening
        DayPart.NIGHT -> R.string.insight_time_of_day_message_night
    }

    override fun focusCompletedTitle() = strings.get(R.string.inbox_focus_completed_title)
    override fun focusCompletedMessage(minutes: Int) = strings.get(R.string.inbox_focus_completed_message, minutes)
    override fun xpDetail(xp: Int, missionTitle: String?) =
        if (missionTitle == null) strings.get(R.string.inbox_xp_detail, xp)
        else strings.get(R.string.inbox_xp_detail_with_mission, xp, missionTitle)
    override fun missionCompletedTitle() = strings.get(R.string.inbox_mission_completed_title)
    override fun quoted(text: String) = strings.get(R.string.format_quoted, text)
    override fun bonusXpDetail(xp: Int) = strings.get(R.string.inbox_bonus_xp_detail, xp)
    override fun streakTitle() = strings.get(R.string.inbox_streak_title)
    override fun streakMessage(days: Int) = strings.get(R.string.inbox_streak_message, days)
    override fun streakDetail() = strings.get(R.string.inbox_streak_detail)
    override fun missionCreatedTitle() = strings.get(R.string.inbox_mission_created_title)
    override fun estimatedPomodoros(count: Int) = strings.quantity(R.plurals.inbox_estimated_pomodoros, count, count)
    override fun levelUpTitle() = strings.get(R.string.inbox_level_up_title)
    override fun levelUpMessage(level: Int) = strings.get(R.string.inbox_level_up_message, level)
    override fun levelUpUnlocked(items: List<WorldItemType>) =
        strings.get(R.string.inbox_level_up_unlocked, items.joinToString { strings.get(it.nameRes()) })
    override fun imageAddFailed() = strings.get(R.string.bg_error_image_add_failed)

    override fun insightGettingStartedTitle() = strings.get(R.string.insight_getting_started_title)
    override fun insightGettingStartedMessage(sessionsLeft: Int) =
        strings.quantity(R.plurals.insight_getting_started_message, sessionsLeft, sessionsLeft)
    override fun insightTimeOfDayTitle(part: DayPart) = strings.get(part.titleRes())
    override fun insightTimeOfDayMessage(sharePercent: Int, part: DayPart) = strings.get(part.messageRes(), sharePercent)
    override fun insightStreakTitle(days: Int) = strings.get(R.string.insight_streak_title, days)
    override fun insightStreakMessage(days: Int) = strings.get(R.string.insight_streak_message, days)
    override fun insightDailyStepsTitle() = strings.get(R.string.insight_daily_steps_title)
    override fun insightDailyStepsMessage(activeDays: Int) =
        strings.quantity(R.plurals.insight_daily_steps_message, activeDays, activeDays)
    override fun insightShortSessionsTitle() = strings.get(R.string.insight_short_sessions_title)
    override fun insightShortSessionsMessage(averageMinutes: Int) = strings.get(R.string.insight_short_sessions_message, averageMinutes)
    override fun insightLongInterruptedTitle() = strings.get(R.string.insight_long_interrupted_title)
    override fun insightLongInterruptedMessage() = strings.get(R.string.insight_long_interrupted_message)
    override fun insightAverageSessionTitle(averageMinutes: Int) = strings.get(R.string.insight_average_session_title, averageMinutes)
    override fun insightAverageSessionMessage(averageMinutes: Int) = strings.get(R.string.insight_average_session_message, averageMinutes)
    override fun insightEstimationOverTitle() = strings.get(R.string.insight_estimation_over_title)
    override fun insightEstimationOverMessage(percent: Int) = strings.get(R.string.insight_estimation_over_message, percent)
    override fun insightEstimationUnderTitle() = strings.get(R.string.insight_estimation_under_title)
    override fun insightEstimationUnderMessage(percent: Int) = strings.get(R.string.insight_estimation_under_message, percent)
    override fun insightEarlyStopsTitle() = strings.get(R.string.insight_early_stops_title)
    override fun insightEarlyStopsMessage(percent: Int) = strings.get(R.string.insight_early_stops_message, percent)
    override fun insightPausesTitle() = strings.get(R.string.insight_pauses_title)
    override fun insightPausesMessage() = strings.get(R.string.insight_pauses_message)
    override fun insightBestDayTitle(dayName: String) = strings.get(R.string.insight_best_day_title, dayName)
    override fun insightBestDayMessage(dayName: String) = strings.get(R.string.insight_best_day_message, dayName)
}
