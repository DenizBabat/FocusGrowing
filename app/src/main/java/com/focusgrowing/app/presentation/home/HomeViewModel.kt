package com.focusgrowing.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.StreakInfo
import com.focusgrowing.app.domain.model.TimerState
import com.focusgrowing.app.domain.model.WorldItemType
import com.focusgrowing.app.domain.model.WorldState
import com.focusgrowing.app.domain.repository.FocusSessionRepository
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.TimerStateRepository
import com.focusgrowing.app.domain.usecase.ObserveStreakUseCase
import com.focusgrowing.app.domain.usecase.ObserveWorldUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val userName: String = "",
    val hour: Int = LocalTime.now().hour,
    val world: WorldState = WorldState.Empty,
    val worldItems: Set<WorldItemType> = emptySet(),
    val totalFocusSeconds: Long = 0,
    val completedMissions: Int = 0,
    val activeMissions: Int = 0,
    val streak: StreakInfo = StreakInfo.Empty,
    val currentMission: Mission? = null,
    val unreadNotifications: Int = 0,
    val timer: TimerState = TimerState(),
)

private data class MissionSummary(val current: Mission?, val completed: Int, val active: Int)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeWorld: ObserveWorldUseCase,
    observeStreak: ObserveStreakUseCase,
    missions: MissionRepository,
    sessions: FocusSessionRepository,
    settings: SettingsRepository,
    notifications: NotificationRepository,
    premium: PremiumManager,
    timerState: TimerStateRepository,
) : ViewModel() {

    private val missionSummary = combine(missions.observeMissions(), timerState.state) { list, timer ->
        // The mission of a running session wins, then the most recent in-progress one, then the next todo.
        val active = list.filter { it.isActive }
        val current = active.firstOrNull { timer.isActive && it.id == timer.missionId }
            ?: active.firstOrNull { it.status == MissionStatus.IN_PROGRESS }
            ?: active.firstOrNull()
        MissionSummary(current, list.count { it.isCompleted }, active.size)
    }

    private val worldAndItems = combine(observeWorld(), premium.premiumState) { world, isPremium ->
        world to WorldProgression.unlockedItems(world.level, isPremium)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        worldAndItems,
        observeStreak(),
        missionSummary,
        sessions.observeTotalFocusSeconds(),
        combine(settings.preferences, notifications.observeUnreadCount(), timerState.state) { p, unread, timer ->
            Triple(p.userName, unread, timer)
        },
    ) { (world, items), streak, summary, focusSeconds, (name, unread, timer) ->
        HomeUiState(
            loading = false,
            userName = name,
            hour = LocalTime.now().hour,
            world = world,
            worldItems = items,
            totalFocusSeconds = focusSeconds,
            completedMissions = summary.completed,
            activeMissions = summary.active,
            streak = streak,
            currentMission = summary.current,
            unreadNotifications = unread,
            timer = timer,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
