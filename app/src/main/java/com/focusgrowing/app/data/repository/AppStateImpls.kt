package com.focusgrowing.app.data.repository

import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.focusgrowing.app.domain.repository.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

@Singleton
class InMemoryCelebrationQueue @Inject constructor() : CelebrationQueue {
    private val lock = Any()
    private val pending = ArrayDeque<Celebration>()
    private val _current = MutableStateFlow<Celebration?>(null)
    override val current: StateFlow<Celebration?> = _current.asStateFlow()

    override fun enqueue(celebrations: List<Celebration>) {
        synchronized(lock) {
            pending.addAll(celebrations)
            if (_current.value == null) {
                _current.value = pending.removeFirstOrNull()
            }
        }
    }

    override fun dismissCurrent() {
        synchronized(lock) {
            _current.value = pending.removeFirstOrNull()
        }
    }
}
