package com.focusgrowing.app.domain.repository

import com.focusgrowing.app.domain.model.WorldItemType

enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

/**
 * Every piece of user-visible text the domain layer produces (inbox entries, insights).
 * The domain stays free of Android: the app binds an implementation that reads the translated
 * string resources (core/locale/ResDomainStrings); [EnglishDomainStrings] is used in unit tests.
 *
 * Inbox entries are stored as text, so they keep the language they were created in.
 */
interface DomainStrings {
    // Inbox ------------------------------------------------------------------------------------
    fun focusCompletedTitle(): String
    fun focusCompletedMessage(minutes: Int): String
    /** "+50 XP" or "+50 XP · Mission title" */
    fun xpDetail(xp: Int, missionTitle: String?): String
    fun missionCompletedTitle(): String
    /** The mission title in the language's quotation marks. */
    fun quoted(text: String): String
    fun bonusXpDetail(xp: Int): String
    fun streakTitle(): String
    fun streakMessage(days: Int): String
    fun streakDetail(): String
    fun missionCreatedTitle(): String
    fun estimatedPomodoros(count: Int): String
    fun levelUpTitle(): String
    fun levelUpMessage(level: Int): String
    fun levelUpUnlocked(items: List<WorldItemType>): String
    fun imageAddFailed(): String

    // Insights ---------------------------------------------------------------------------------
    fun insightGettingStartedTitle(): String
    fun insightGettingStartedMessage(sessionsLeft: Int): String
    fun insightTimeOfDayTitle(part: DayPart): String
    fun insightTimeOfDayMessage(sharePercent: Int, part: DayPart): String
    fun insightStreakTitle(days: Int): String
    fun insightStreakMessage(days: Int): String
    fun insightDailyStepsTitle(): String
    fun insightDailyStepsMessage(activeDays: Int): String
    fun insightShortSessionsTitle(): String
    fun insightShortSessionsMessage(averageMinutes: Int): String
    fun insightLongInterruptedTitle(): String
    fun insightLongInterruptedMessage(): String
    fun insightAverageSessionTitle(averageMinutes: Int): String
    fun insightAverageSessionMessage(averageMinutes: Int): String
    fun insightEstimationOverTitle(): String
    fun insightEstimationOverMessage(percent: Int): String
    fun insightEstimationUnderTitle(): String
    fun insightEstimationUnderMessage(percent: Int): String
    fun insightEarlyStopsTitle(): String
    fun insightEarlyStopsMessage(percent: Int): String
    fun insightPausesTitle(): String
    fun insightPausesMessage(): String
    fun insightBestDayTitle(dayName: String): String
    fun insightBestDayMessage(dayName: String): String
}

/** English texts, identical to res/values/strings.xml. Used by unit tests and as a safe default. */
object EnglishDomainStrings : DomainStrings {
    private fun DayPart.phrase() = if (this == DayPart.NIGHT) "at night" else "in the ${name.lowercase()}"

    override fun focusCompletedTitle() = "Focus Session Completed"
    override fun focusCompletedMessage(minutes: Int) = "You completed a $minutes minute focus session. Great job!"
    override fun xpDetail(xp: Int, missionTitle: String?) = if (missionTitle == null) "+$xp XP" else "+$xp XP · $missionTitle"
    override fun missionCompletedTitle() = "Mission Completed!"
    override fun quoted(text: String) = "“$text”"
    override fun bonusXpDetail(xp: Int) = "You earned +$xp bonus XP"
    override fun streakTitle() = "Streak Continues!"
    override fun streakMessage(days: Int) = "You're on a $days day streak!"
    override fun streakDetail() = "Keep it up!"
    override fun missionCreatedTitle() = "New Mission Assigned"
    override fun estimatedPomodoros(count: Int) = if (count == 1) "Estimated 1 Pomodoro" else "Estimated $count Pomodoros"
    override fun levelUpTitle() = "World Level Up!"
    override fun levelUpMessage(level: Int) = "Your world has reached Level $level!"
    override fun levelUpUnlocked(items: List<WorldItemType>) = "New elements unlocked: " + items.joinToString { it.displayName }
    override fun imageAddFailed() = "This image could not be added. Please try another one."

    override fun insightGettingStartedTitle() = "Your insights are growing"
    override fun insightGettingStartedMessage(sessionsLeft: Int) =
        if (sessionsLeft == 1) "Complete 1 more focus session to unlock personal insights."
        else "Complete $sessionsLeft more focus sessions to unlock personal insights."
    override fun insightTimeOfDayTitle(part: DayPart) = "You shine ${part.phrase()}"
    override fun insightTimeOfDayMessage(sharePercent: Int, part: DayPart) =
        "$sharePercent% of your focus time happens ${part.phrase()}. Plan your hardest missions then."
    override fun insightStreakTitle(days: Int) = "$days day streak"
    override fun insightStreakMessage(days: Int) =
        "You have focused $days days in a row. Consistency beats intensity — keep the chain going."
    override fun insightDailyStepsTitle() = "Small daily steps"
    override fun insightDailyStepsMessage(activeDays: Int) =
        if (activeDays == 1) "You focused on 1 day recently. One short session every day builds a stronger habit."
        else "You focused on $activeDays different days recently. One short session every day builds a stronger habit."
    override fun insightShortSessionsTitle() = "Short sprints work for you"
    override fun insightShortSessionsMessage(averageMinutes: Int) =
        "Your sessions average $averageMinutes minutes. Shorter focus sessions appear to work well for you."
    override fun insightLongInterruptedTitle() = "Long sessions get interrupted"
    override fun insightLongInterruptedMessage() = "About half of your 40+ minute sessions end early. Try 25–30 minute sessions instead."
    override fun insightAverageSessionTitle(averageMinutes: Int) = "Average session: $averageMinutes min"
    override fun insightAverageSessionMessage(averageMinutes: Int) =
        "Your typical focus block is $averageMinutes minutes. Matching your timer to it reduces early stops."
    override fun insightEstimationOverTitle() = "Missions take longer than planned"
    override fun insightEstimationOverMessage(percent: Int) =
        "Your recent missions took about $percent% more pomodoros than estimated. Add a buffer pomodoro when planning."
    override fun insightEstimationUnderTitle() = "You finish ahead of plan"
    override fun insightEstimationUnderMessage(percent: Int) =
        "You complete missions with about $percent% fewer pomodoros than estimated. You can plan more ambitiously."
    override fun insightEarlyStopsTitle() = "Frequent early stops"
    override fun insightEarlyStopsMessage(percent: Int) =
        "$percent% of your sessions end early. Silence notifications and keep your phone out of reach while focusing."
    override fun insightPausesTitle() = "Lots of pauses"
    override fun insightPausesMessage() = "Half of your sessions include a pause. Prepare water and notes before you start."
    override fun insightBestDayTitle(dayName: String) = "$dayName is your power day"
    override fun insightBestDayMessage(dayName: String) =
        "$dayName is the day you focus the most. Schedule important missions for that day."
}
