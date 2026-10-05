package com.focusgrowing.app.domain.repository

import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.AppearanceSettings
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.model.FocusScreenSettings
import com.focusgrowing.app.domain.model.FocusSession
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.model.NotificationSettings
import com.focusgrowing.app.domain.model.PomodoroSettings
import com.focusgrowing.app.domain.model.TimerState
import com.focusgrowing.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

/** Abstraction over the clock so time-based logic is testable. */
interface TimeProvider {
    fun now(): Long
    fun zone(): ZoneId
}

interface MissionRepository {
    fun observeMissions(): Flow<List<Mission>>
    fun observeMission(id: Long): Flow<Mission?>
    suspend fun getMission(id: Long): Mission?
    suspend fun createMission(mission: Mission, subTaskTitles: List<String>): Long
    suspend fun updateMission(mission: Mission)
    suspend fun updateStatus(id: Long, status: MissionStatus, completedAt: Long?)
    suspend fun deleteMission(id: Long)
    suspend fun addSubTask(missionId: Long, title: String)
    suspend fun setSubTaskDone(subTaskId: Long, done: Boolean)
    suspend fun deleteSubTask(subTaskId: Long)
}

interface FocusSessionRepository {
    suspend fun insert(session: FocusSession): Long
    fun observeSessionsSince(fromMillis: Long): Flow<List<FocusSession>>
    fun observeCompletedFocusStartTimes(): Flow<List<Long>>
    suspend fun getCompletedFocusStartTimes(): List<Long>
    fun observeCompletedFocusCount(): Flow<Int>
    fun observeTotalFocusSeconds(): Flow<Long>
}

interface WorldRepository {
    fun observeTotalXp(): Flow<Int>
    suspend fun getTotalXp(): Int
    /** Adds XP and returns the new total. */
    suspend fun addXp(amount: Int): Int
}

interface BackgroundRepository {
    /** Built-in scenes followed by the user's own images. */
    fun observeBackgrounds(): Flow<List<BackgroundImage>>
    fun observeBackground(id: String): Flow<BackgroundImage?>
    suspend fun getBackground(id: String): BackgroundImage?
    suspend fun customCount(): Int
    suspend fun addCustom(uri: String, source: BackgroundSource, name: String): BackgroundImage
    suspend fun delete(id: String)
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun updateAdjustment(id: String, alignX: Float, alignY: Float, zoom: Float)
}

interface SettingsRepository {
    val preferences: Flow<UserPreferences>
    suspend fun current(): UserPreferences
    suspend fun setUserName(name: String)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updatePomodoro(transform: (PomodoroSettings) -> PomodoroSettings)
    suspend fun updateFocusScreen(transform: (FocusScreenSettings) -> FocusScreenSettings)
    suspend fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings)
    suspend fun updateNotifications(transform: (NotificationSettings) -> NotificationSettings)
}

interface TimerStateRepository {
    val state: Flow<TimerState>
    suspend fun current(): TimerState
    suspend fun save(state: TimerState)
}

interface NotificationRepository {
    fun observeAll(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun add(notification: AppNotification)
    suspend fun markAllRead()
    suspend fun clearAll()
}

/** Cached entitlement, so Premium works offline and right after app start. Google Play is the source of truth. */
interface SubscriptionRepository {
    val isPremium: Flow<Boolean>
    /** Product id of the active subscription ("premium_monthly", "premium_yearly"), null when not premium or unknown. */
    val activePlanId: Flow<String?>
    /** Product the user will switch to when the current period ends (deferred plan change), if any. */
    val scheduledPlanId: Flow<String?>
    /** [planId] null keeps the cached plan. Turning premium off also clears a scheduled change. */
    suspend fun setPremium(active: Boolean, planId: String? = null)
    suspend fun setScheduledPlan(planId: String?)
}

/** In-memory queue of reward screens waiting to be shown. */
interface CelebrationQueue {
    val current: StateFlow<Celebration?>
    fun enqueue(celebrations: List<Celebration>)
    fun dismissCurrent()
}
