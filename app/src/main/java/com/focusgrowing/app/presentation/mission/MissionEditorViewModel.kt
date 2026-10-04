package com.focusgrowing.app.presentation.mission

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.model.MissionCategory
import com.focusgrowing.app.domain.model.MissionPriority
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.usecase.MissionDraft
import com.focusgrowing.app.domain.usecase.MissionValidationError
import com.focusgrowing.app.domain.usecase.SaveMissionResult
import com.focusgrowing.app.domain.usecase.SaveMissionUseCase
import com.focusgrowing.app.presentation.app.MissionEditorRoute
import com.focusgrowing.app.presentation.app.NO_ID
import com.focusgrowing.app.presentation.app.route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MissionEditorUiState(
    val isNew: Boolean = true,
    val loading: Boolean = false,
    val draft: MissionDraft = MissionDraft(),
    val newSubTask: String = "",
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class MissionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val missions: MissionRepository,
    private val saveMission: SaveMissionUseCase,
) : ViewModel() {

    private val missionId: Long? = savedStateHandle.route<MissionEditorRoute>().missionId.takeIf { it != NO_ID }

    private val _state = MutableStateFlow(MissionEditorUiState(isNew = missionId == null, loading = missionId != null))
    val state: StateFlow<MissionEditorUiState> = _state.asStateFlow()

    init {
        if (missionId != null) {
            viewModelScope.launch {
                val m = missions.getMission(missionId)
                _state.update {
                    if (m == null) it.copy(loading = false, error = "Mission not found")
                    else it.copy(
                        loading = false,
                        draft = MissionDraft(
                            id = m.id,
                            title = m.title,
                            description = m.description.orEmpty(),
                            estimatedPomodoros = m.estimatedPomodoros,
                            priority = m.priority,
                            category = m.category,
                        ),
                    )
                }
            }
        }
    }

    private fun updateDraft(transform: (MissionDraft) -> MissionDraft) =
        _state.update { it.copy(draft = transform(it.draft), error = null) }

    fun setTitle(v: String) = updateDraft { it.copy(title = v.take(SaveMissionUseCase.MAX_TITLE)) }
    fun setDescription(v: String) = updateDraft { it.copy(description = v.take(500)) }
    fun setPriority(v: MissionPriority) = updateDraft { it.copy(priority = v) }
    fun setCategory(v: MissionCategory) = updateDraft { it.copy(category = v) }
    fun changeEstimate(delta: Int) = updateDraft {
        it.copy(estimatedPomodoros = (it.estimatedPomodoros + delta).coerceIn(1, SaveMissionUseCase.MAX_ESTIMATE))
    }

    fun setNewSubTask(v: String) = _state.update { it.copy(newSubTask = v.take(80)) }
    fun addSubTask() = _state.update {
        val title = it.newSubTask.trim()
        if (title.isEmpty()) it else it.copy(draft = it.draft.copy(subTasks = it.draft.subTasks + title), newSubTask = "")
    }
    fun removeSubTask(index: Int) = updateDraft { d -> d.copy(subTasks = d.subTasks.filterIndexed { i, _ -> i != index }) }

    fun save(onSaved: () -> Unit) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            // A step typed but not added yet is still included.
            val pending = _state.value.newSubTask.trim()
            val draft = _state.value.draft.let { if (pending.isNotEmpty()) it.copy(subTasks = it.subTasks + pending) else it }
            when (val result = saveMission(draft)) {
                is SaveMissionResult.Saved -> onSaved()
                is SaveMissionResult.Invalid -> _state.update { it.copy(saving = false, error = result.error.message()) }
            }
        }
    }

    private fun MissionValidationError.message() = when (this) {
        MissionValidationError.EMPTY_TITLE -> "Please give your mission a title."
        MissionValidationError.TITLE_TOO_LONG -> "The title is too long."
        MissionValidationError.INVALID_ESTIMATE -> "Estimate must be between 1 and ${SaveMissionUseCase.MAX_ESTIMATE} pomodoros."
    }
}
