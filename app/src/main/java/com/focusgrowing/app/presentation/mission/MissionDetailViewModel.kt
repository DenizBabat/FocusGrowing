package com.focusgrowing.app.presentation.mission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.usecase.ArchiveMissionUseCase
import com.focusgrowing.app.domain.usecase.CompleteMissionUseCase
import com.focusgrowing.app.domain.usecase.ReopenMissionUseCase
import com.focusgrowing.app.presentation.app.MissionDetailRoute
import com.focusgrowing.app.presentation.app.route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MissionDetailUiState {
    data object Loading : MissionDetailUiState
    data object NotFound : MissionDetailUiState
    data class Loaded(val mission: Mission) : MissionDetailUiState
}

@HiltViewModel
class MissionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val missions: MissionRepository,
    private val completeMission: CompleteMissionUseCase,
    private val reopenMission: ReopenMissionUseCase,
    private val archiveMission: ArchiveMissionUseCase,
) : ViewModel() {

    private val missionId = savedStateHandle.route<MissionDetailRoute>().missionId

    val uiState: StateFlow<MissionDetailUiState> = missions.observeMission(missionId)
        .map { if (it == null) MissionDetailUiState.NotFound else MissionDetailUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissionDetailUiState.Loading)

    private val _newSubTask = MutableStateFlow("")
    val newSubTask: StateFlow<String> = _newSubTask.asStateFlow()

    fun onNewSubTaskChange(text: String) {
        _newSubTask.value = text.take(80)
    }

    fun addSubTask() {
        val title = _newSubTask.value.trim()
        if (title.isEmpty()) return
        _newSubTask.value = ""
        viewModelScope.launch { missions.addSubTask(missionId, title) }
    }

    fun toggleSubTask(id: Long, done: Boolean) {
        viewModelScope.launch { missions.setSubTaskDone(id, done) }
    }

    fun deleteSubTask(id: Long) {
        viewModelScope.launch { missions.deleteSubTask(id) }
    }

    fun complete() {
        viewModelScope.launch { completeMission(missionId) }
    }

    fun reopen() {
        viewModelScope.launch { reopenMission(missionId) }
    }

    fun archive(onDone: () -> Unit) {
        viewModelScope.launch {
            archiveMission(missionId)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            missions.deleteMission(missionId)
            onDone()
        }
    }
}
