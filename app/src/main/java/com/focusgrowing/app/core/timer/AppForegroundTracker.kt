package com.focusgrowing.app.core.timer

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/** Knows whether any app screen is visible, so we don't post a notification the user is already looking at. */
@Singleton
class AppForegroundTracker @Inject constructor() : DefaultLifecycleObserver {
    @Volatile
    var isInForeground: Boolean = false
        private set

    private var onForeground: (() -> Unit)? = null

    /** Call once from Application.onCreate (main thread). */
    fun start(onForeground: () -> Unit) {
        this.onForeground = onForeground
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isInForeground = true
        onForeground?.invoke()
    }

    override fun onStop(owner: LifecycleOwner) {
        isInForeground = false
    }
}
