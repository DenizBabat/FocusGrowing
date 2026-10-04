package com.focusgrowing.app.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TimerStore

/** Coroutine scope that lives as long as the process (timer, alarms). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
