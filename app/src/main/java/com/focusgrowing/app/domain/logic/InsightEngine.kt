package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.Insight
import com.focusgrowing.app.domain.model.InsightKind
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.StreakInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Everything a rule may look at. Built from local data only (privacy friendly, offline). */
data class InsightInput(
    val sessions: List<FocusSession>,
    val missions: List<Mission>,
    val streak: StreakInfo,
    val zone: ZoneId,
    val locale: Locale = Locale.getDefault(),
)

/** One independent, unit-testable rule. Return null when the rule has nothing useful to say. */
fun interface InsightRule {
    fun evaluate(input: InsightInput): Insight?
}

/**
 * V1 "Intelligence": a rule engine. Cheap, fast, offline and testable.
 * A future ML/AI implementation can replace this class behind the same use case.
 */
class InsightEngine(private val rules: List<InsightRule> = DefaultRules) {

    fun generate(input: InsightInput): List<Insight> {
        val completedFocus = input.sessions.count { it.type == SessionType.FOCUS && it.completed }
        if (completedFocus < MIN_SESSIONS) {
            return listOf(
                Insight(
                    id = "getting_started",
                    kind = InsightKind.GETTING_STARTED,
                    title = "Your insights are growing",
                    message = "Complete ${MIN_SESSIONS - completedFocus} more focus session(s) to unlock personal insights.",
                    isPremium = false,
                ),
            )
        }
        return rules.mapNotNull { it.evaluate(input) }
            .sortedBy { it.isPremium } // free insights first
    }

    companion object {
        const val MIN_SESSIONS = 3

        val DefaultRules: List<InsightRule> = listOf(
            TimeOfDayRule, ConsistencyRule, SessionLengthRule, EstimationRule, InterruptionRule, BestDayRule,
        )
    }
}

private fun focusSessions(input: InsightInput) = input.sessions.filter { it.type == SessionType.FOCUS }

val TimeOfDayRule = InsightRule { input ->
    val sessions = focusSessions(input).filter { it.completed }
    val byPart = sessions.groupBy { s ->
        when (Instant.ofEpochMilli(s.startedAt).atZone(input.zone).hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..23 -> "evening"
            else -> "night"
        }
    }.mapValues { (_, list) -> list.sumOf { it.actualDurationSeconds } }
    val total = byPart.values.sum()
    val best = byPart.maxByOrNull { it.value } ?: return@InsightRule null
    if (total <= 0 || byPart.size < 2) return@InsightRule null
    val share = (best.value * 100.0 / total).roundToInt()
    if (share < 40) return@InsightRule null
    Insight(
        id = "time_of_day",
        kind = InsightKind.TIME_OF_DAY,
        title = "You shine in the ${best.key}",
        message = "$share% of your focus time happens in the ${best.key}. Plan your hardest missions then.",
        isPremium = false,
    )
}

val ConsistencyRule = InsightRule { input ->
    val days = input.streak.currentDays
    when {
        days >= 3 -> Insight(
            id = "consistency_streak",
            kind = InsightKind.CONSISTENCY,
            title = "$days day streak",
            message = "You have focused $days days in a row. Consistency beats intensity — keep the chain going.",
            isPremium = false,
        )
        else -> {
            val activeDays = focusSessions(input).filter { it.completed }
                .map { Instant.ofEpochMilli(it.startedAt).atZone(input.zone).toLocalDate() }
                .toSet().size
            if (activeDays in 1..4) Insight(
                id = "consistency_tip",
                kind = InsightKind.CONSISTENCY,
                title = "Small daily steps",
                message = "You focused on $activeDays different day(s) recently. One short session every day builds a stronger habit.",
                isPremium = true,
            ) else null
        }
    }
}

val SessionLengthRule = InsightRule { input ->
    val completed = focusSessions(input).filter { it.completed }
    if (completed.size < 3) return@InsightRule null
    val averageMinutes = completed.sumOf { it.actualDurationSeconds } / completed.size / 60
    val longOnes = focusSessions(input).filter { it.plannedDurationSeconds >= 40 * 60 }
    val longInterrupted = longOnes.count { !it.completed }
    when {
        averageMinutes < 15 -> Insight(
            "session_short", InsightKind.SESSION_LENGTH, "Short sprints work for you",
            "Your sessions average $averageMinutes minutes. Shorter focus sessions appear to work well for you.",
            isPremium = true,
        )
        longOnes.size >= 3 && longInterrupted * 2 >= longOnes.size -> Insight(
            "session_long_interrupted", InsightKind.SESSION_LENGTH, "Long sessions get interrupted",
            "About half of your 40+ minute sessions end early. Try 25–30 minute sessions instead.",
            isPremium = true,
        )
        else -> Insight(
            "session_average", InsightKind.SESSION_LENGTH, "Average session: $averageMinutes min",
            "Your typical focus block is $averageMinutes minutes. Matching your timer to it reduces early stops.",
            isPremium = true,
        )
    }
}

val EstimationRule = InsightRule { input ->
    val error = StatisticsCalculator.estimationError(input.missions, 0L) ?: return@InsightRule null
    when {
        error >= 15 -> Insight(
            "estimation_over", InsightKind.ESTIMATION, "Missions take longer than planned",
            "Your recent missions took about $error% more pomodoros than estimated. Add a buffer pomodoro when planning.",
            isPremium = true,
        )
        error <= -15 -> Insight(
            "estimation_under", InsightKind.ESTIMATION, "You finish ahead of plan",
            "You complete missions with about ${-error}% fewer pomodoros than estimated. You can plan more ambitiously.",
            isPremium = true,
        )
        else -> null
    }
}

val InterruptionRule = InsightRule { input ->
    val sessions = focusSessions(input)
    if (sessions.size < 5) return@InsightRule null
    val interrupted = sessions.count { !it.completed }
    val paused = sessions.count { it.interruptionCount > 0 }
    val rate = (interrupted * 100.0 / sessions.size).roundToInt()
    when {
        rate >= 30 -> Insight(
            "interruptions_high", InsightKind.INTERRUPTIONS, "Frequent early stops",
            "$rate% of your sessions end early. Silence notifications and keep your phone out of reach while focusing.",
            isPremium = true,
        )
        paused * 2 >= sessions.size -> Insight(
            "pauses_high", InsightKind.INTERRUPTIONS, "Lots of pauses",
            "Half of your sessions include a pause. Prepare water and notes before you start.",
            isPremium = true,
        )
        else -> null
    }
}

val BestDayRule = InsightRule { input ->
    val sessions = focusSessions(input).filter { it.completed }
    if (sessions.size < 5) return@InsightRule null
    val best = sessions
        .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(input.zone).dayOfWeek }
        .maxByOrNull { (_, list) -> list.sumOf { it.actualDurationSeconds } }
        ?.key ?: return@InsightRule null
    val name = best.getDisplayName(TextStyle.FULL, input.locale)
    Insight(
        "best_day", InsightKind.BEST_DAY, "$name is your power day",
        "You focus the most on ${name}s. Schedule important missions for that day.",
        isPremium = true,
    )
}
