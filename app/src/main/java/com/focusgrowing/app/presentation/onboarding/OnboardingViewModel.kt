package com.focusgrowing.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.usecase.MissionDraft
import com.focusgrowing.app.domain.usecase.SaveMissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val page: Int = 0,
    val focusMinutes: Int = 25,
    val customMinutesText: String = "",
    val useCustom: Boolean = false,
    val name: String = "",
    val firstGoal: String = "",
    val saving: Boolean = false,
) {
    val isLastPage: Boolean get() = page == OnboardingViewModel.PAGE_COUNT - 1
    val resolvedMinutes: Int
        get() = if (useCustom) customMinutesText.toIntOrNull()?.coerceIn(1, 180) ?: 25 else focusMinutes
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val saveMission: SaveMissionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun next() = _state.update { it.copy(page = (it.page + 1).coerceAtMost(PAGE_COUNT - 1)) }
    fun back() = _state.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }
    fun skipIntro() = _state.update { it.copy(page = PAGE_FOCUS_LENGTH) }
    fun selectMinutes(minutes: Int) = _state.update { it.copy(focusMinutes = minutes, useCustom = false) }
    fun selectCustom() = _state.update { it.copy(useCustom = true) }
    fun setCustomMinutes(text: String) = _state.update { it.copy(customMinutesText = text.filter(Char::isDigit).take(3), useCustom = true) }
    fun setName(name: String) = _state.update { it.copy(name = name.take(40)) }
    fun setGoal(goal: String) = _state.update { it.copy(firstGoal = goal.take(80)) }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            settings.updatePomodoro { it.copy(focusMinutes = s.resolvedMinutes) }
            if (s.name.isNotBlank()) settings.setUserName(s.name)
            if (s.firstGoal.isNotBlank()) saveMission(MissionDraft(title = s.firstGoal, estimatedPomodoros = 4))
            settings.setOnboardingCompleted(true)
            onDone()
        }
    }

    companion object {
        const val PAGE_COUNT = 6
        const val PAGE_FOCUS_LENGTH = 4
    }
}
