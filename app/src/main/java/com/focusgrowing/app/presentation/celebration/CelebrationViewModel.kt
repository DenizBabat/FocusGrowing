package com.focusgrowing.app.presentation.celebration

import androidx.lifecycle.ViewModel
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.repository.CelebrationQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CelebrationViewModel @Inject constructor(
    private val queue: CelebrationQueue,
) : ViewModel() {
    val current: StateFlow<Celebration?> = queue.current

    fun dismiss() = queue.dismissCurrent()
}
