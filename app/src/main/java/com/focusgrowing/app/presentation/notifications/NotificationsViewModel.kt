package com.focusgrowing.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.model.AppNotification
import com.focusgrowing.app.domain.model.NotificationType
import com.focusgrowing.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationFilter(val label: String, val types: Set<NotificationType>?) {
    ALL("All", null),
    MISSION("Mission", setOf(NotificationType.MISSION)),
    FOCUS("Focus", setOf(NotificationType.FOCUS, NotificationType.STREAK)),
    WORLD("World", setOf(NotificationType.WORLD)),
    SYSTEM("System", setOf(NotificationType.SYSTEM)),
}

data class NotificationsUiState(
    val filter: NotificationFilter = NotificationFilter.ALL,
    val items: List<AppNotification> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(NotificationFilter.ALL)

    val uiState: StateFlow<NotificationsUiState> = combine(repository.observeAll(), filter) { all, f ->
        val types = f.types
        NotificationsUiState(f, if (types == null) all else all.filter { it.type in types }, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun setFilter(value: NotificationFilter) {
        filter.value = value
    }

    /** Opening the inbox marks everything as read. */
    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead() }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
