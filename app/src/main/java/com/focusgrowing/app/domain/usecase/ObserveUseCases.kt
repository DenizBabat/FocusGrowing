package com.focusgrowing.app.domain.usecase

import com.focusgrowing.app.domain.logic.InsightEngine
import com.focusgrowing.app.domain.logic.InsightInput
import com.focusgrowing.app.domain.logic.StatisticsCalculator
import com.focusgrowing.app.domain.logic.StreakCalculator
import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.FocusStatistics
import com.focusgrowing.app.domain.model.Insight
import com.focusgrowing.app.domain.model.StatsRange
import com.focusgrowing.app.domain.model.StreakInfo
import com.focusgrowing.app.domain.model.WorldState
import com.focusgrowing.app.domain.repository.FocusSessionRepository
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import com.focusgrowing.app.domain.repository.WorldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

private fun TimeProvider.today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone()).toLocalDate()

class ObserveWorldUseCase @Inject constructor(
    private val world: WorldRepository,
    private val sessions: FocusSessionRepository,
) {
    operator fun invoke(): Flow<WorldState> =
        combine(world.observeTotalXp(), sessions.observeCompletedFocusCount()) { xp, count ->
            WorldProgression.stateFor(xp, count)
        }
}

class ObserveStreakUseCase @Inject constructor(
    private val sessions: FocusSessionRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(): Flow<StreakInfo> =
        sessions.observeCompletedFocusStartTimes().map { times ->
            StreakCalculator.calculate(times, time.zone(), time.today())
        }
}

class ObserveStatisticsUseCase @Inject constructor(
    private val sessions: FocusSessionRepository,
    private val missions: MissionRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(range: StatsRange): Flow<FocusStatistics> {
        val today = time.today()
        // Load the current period + the previous one (for % change) + this week.
        val from = today.minusDays((range.days * 2).toLong().coerceAtLeast(14))
            .atStartOfDay(time.zone()).toInstant().toEpochMilli()
        return combine(sessions.observeSessionsSince(from), missions.observeMissions()) { list, allMissions ->
            StatisticsCalculator.calculate(range, list, allMissions, time.zone(), time.today())
        }
    }
}

class ObserveInsightsUseCase @Inject constructor(
    private val sessions: FocusSessionRepository,
    private val missions: MissionRepository,
    private val observeStreak: ObserveStreakUseCase,
    private val time: TimeProvider,
) {
    private val engine = InsightEngine()

    operator fun invoke(lookbackDays: Int = 30): Flow<List<Insight>> {
        val from = time.today().minusDays(lookbackDays.toLong()).atStartOfDay(time.zone()).toInstant().toEpochMilli()
        return combine(
            sessions.observeSessionsSince(from),
            missions.observeMissions(),
            observeStreak(),
        ) { list, allMissions, streak ->
            engine.generate(InsightInput(list, allMissions, streak, time.zone()))
        }
    }
}
