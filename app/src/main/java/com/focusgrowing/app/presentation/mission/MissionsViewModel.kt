package com.focusgrowing.app.presentation.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.model.Mission
import com.focusgrowing.app.domain.model.MissionStatus
import com.focusgrowing.app.domain.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class MissionFilter(val label: String) { ALL("All"), TODO("Todo"), IN_PROGRESS("In Progress"), COMPLETED("Completed") }

data class MissionsUiState(
    val loading: Boolean = true,
    val filter: MissionFilter = MissionFilter.ALL,
    val missions: List<Mission> = emptyList(),
    val totalCount: Int = 0,
)

@HiltViewModel
class MissionsViewModel @Inject constructor(
    missions: MissionRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(MissionFilter.ALL)

    val uiState: StateFlow<MissionsUiState> = combine(missions.observeMissions(), filter) { list, f ->
        val filtered = when (f) {
            MissionFilter.ALL -> list
            MissionFilter.TODO -> list.filter { it.status == MissionStatus.TODO }
            MissionFilter.IN_PROGRESS -> list.filter { it.status == MissionStatus.IN_PROGRESS }
            MissionFilter.COMPLETED -> list.filter { it.status == MissionStatus.COMPLETED }
        }
        MissionsUiState(loading = false, filter = f, missions = filtered, totalCount = list.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissionsUiState())

    fun setFilter(value: MissionFilter) {
        filter.value = value
    }
}
