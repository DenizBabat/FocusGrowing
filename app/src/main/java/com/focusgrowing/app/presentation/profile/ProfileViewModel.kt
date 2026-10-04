package com.focusgrowing.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.WorldItemType
import com.focusgrowing.app.domain.model.WorldState
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.usecase.ObserveWorldUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val world: WorldState = WorldState.Empty,
    val worldItems: Set<WorldItemType> = emptySet(),
    val isPremium: Boolean = false,
    val unread: Int = 0,
) {
    /** Small identity line under the name, grows with the world. */
    val title: String
        get() = when {
            world.level >= 8 -> "Master • Focused • Thriving"
            world.level >= 5 -> "Builder • Focused • Growing"
            world.level >= 3 -> "Explorer • Focused • Growing"
            else -> "Seedling • Getting started"
        }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settings: SettingsRepository,
    observeWorld: ObserveWorldUseCase,
    premium: PremiumManager,
    notifications: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        settings.preferences, observeWorld(), premium.premiumState, notifications.observeUnreadCount(),
    ) { prefs, world, isPremium, unread ->
        ProfileUiState(
            name = prefs.userName,
            world = world,
            worldItems = WorldProgression.unlockedItems(world.level, isPremium),
            isPremium = isPremium,
            unread = unread,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setName(name: String) {
        viewModelScope.launch { settings.setUserName(name) }
    }
}
