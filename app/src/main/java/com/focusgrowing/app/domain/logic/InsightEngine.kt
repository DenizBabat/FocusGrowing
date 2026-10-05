package com.focusgrowing.app.domain.logic

import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.Insight
import com.focusgrowing.app.domain.model.InsightKind
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.SessionType
import com.focusgrowing.app.domain.model.StreakInfo
import com.focusgrowing.app.domain.repository.DayPart
import com.focusgrowing.app.domain.repository.DomainStrings
import com.focusgrowing.app.domain.repository.EnglishDomainStrings
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
    /** Translated texts. The app passes the resource-backed implementation. */
    val strings: DomainStrings = EnglishDomainStrings,
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
                    title = input.strings.insightGettingStartedTitle(),
                    message = input.strings.insightGettingStartedMessage(MIN_SESSIONS - completedFocus),
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
            in 5..11 -> DayPart.MORNING
            in 12..17 -> DayPart.AFTERNOON
            in 18..23 -> DayPart.EVENING
            else -> DayPart.NIGHT
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
        title = input.strings.insightTimeOfDayTitle(best.key),
        message = input.strings.insightTimeOfDayMessage(share, best.key),
        isPremium = false,
    )
}

val ConsistencyRule = InsightRule { input ->
    val days = input.streak.currentDays
    when {
        days >= 3 -> Insight(
            id = "consistency_streak",
            kind = InsightKind.CONSISTENCY,
            title = input.strings.insightStreakTitle(days),
            message = input.strings.insightStreakMessage(days),
            isPremium = false,
        )
        else -> {
            val activeDays = focusSessions(input).filter { it.completed }
                .map { Instant.ofEpochMilli(it.startedAt).atZone(input.zone).toLocalDate() }
                .toSet().size
            if (activeDays in 1..4) Insight(
                id = "consistency_tip",
                kind = InsightKind.CONSISTENCY,
                title = input.strings.insightDailyStepsTitle(),
                message = input.strings.insightDailyStepsMessage(activeDays),
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
            "session_short", InsightKind.SESSION_LENGTH, input.strings.insightShortSessionsTitle(),
            input.strings.insightShortSessionsMessage(averageMinutes.toInt()),
            isPremium = true,
        )
        longOnes.size >= 3 && longInterrupted * 2 >= longOnes.size -> Insight(
            "session_long_interrupted", InsightKind.SESSION_LENGTH, input.strings.insightLongInterruptedTitle(),
            input.strings.insightLongInterruptedMessage(),
            isPremium = true,
        )
        else -> Insight(
            "session_average", InsightKind.SESSION_LENGTH, input.strings.insightAverageSessionTitle(averageMinutes.toInt()),
            input.strings.insightAverageSessionMessage(averageMinutes.toInt()),
            isPremium = true,
        )
    }
}

val EstimationRule = InsightRule { input ->
    val error = StatisticsCalculator.estimationError(input.missions, 0L) ?: return@InsightRule null
    when {
        error >= 15 -> Insight(
            "estimation_over", InsightKind.ESTIMATION, input.strings.insightEstimationOverTitle(),
            input.strings.insightEstimationOverMessage(error),
            isPremium = true,
        )
        error <= -15 -> Insight(
            "estimation_under", InsightKind.ESTIMATION, input.strings.insightEstimationUnderTitle(),
            input.strings.insightEstimationUnderMessage(-error),
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
            "interruptions_high", InsightKind.INTERRUPTIONS, input.strings.insightEarlyStopsTitle(),
            input.strings.insightEarlyStopsMessage(rate),
            isPremium = true,
        )
        paused * 2 >= sessions.size -> Insight(
            "pauses_high", InsightKind.INTERRUPTIONS, input.strings.insightPausesTitle(),
            input.strings.insightPausesMessage(),
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
    val name = best.getDisplayName(TextStyle.FULL_STANDALONE, input.locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(input.locale) else it.toString() }
    Insight(
        "best_day", InsightKind.BEST_DAY, input.strings.insightBestDayTitle(name),
        input.strings.insightBestDayMessage(name),
        isPremium = true,
    )
}
