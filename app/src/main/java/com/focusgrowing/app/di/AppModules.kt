package com.focusgrowing.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.focusgrowing.app.BuildConfig
import com.focusgrowing.app.core.billing.FakePurchaseManager
import com.focusgrowing.app.core.billing.PlayBillingManager
import com.focusgrowing.app.core.billing.PurchaseManager
import com.focusgrowing.app.core.premium.DefaultPremiumManager
import com.focusgrowing.app.data.local.dao.BackgroundDao
import com.focusgrowing.app.data.local.dao.FocusSessionDao
import com.focusgrowing.app.data.local.dao.MissionDao
import com.focusgrowing.app.data.local.dao.NotificationDao
import com.focusgrowing.app.data.local.dao.WorldDao
import com.focusgrowing.app.data.local.db.FocusDatabase
import com.focusgrowing.app.data.repository.BackgroundRepositoryImpl
import com.focusgrowing.app.data.repository.FocusSessionRepositoryImpl
import com.focusgrowing.app.data.repository.InMemoryCelebrationQueue
import com.focusgrowing.app.data.repository.MissionRepositoryImpl
import com.focusgrowing.app.data.repository.NotificationRepositoryImpl
import com.focusgrowing.app.data.repository.RewardRepositoryImpl
import com.focusgrowing.app.data.repository.SettingsRepositoryImpl
import com.focusgrowing.app.data.repository.SubscriptionRepositoryImpl
import com.focusgrowing.app.data.repository.SystemTimeProvider
import com.focusgrowing.app.data.repository.TimerStateRepositoryImpl
import com.focusgrowing.app.data.repository.WorldRepositoryImpl
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.CelebrationQueue
import com.focusgrowing.app.domain.repository.FocusSessionRepository
import com.focusgrowing.app.domain.repository.MissionRepository
import com.focusgrowing.app.domain.repository.NotificationRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.RewardRepository
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import com.focusgrowing.app.domain.repository.TimeProvider
import com.focusgrowing.app.domain.repository.TimerStateRepository
import com.focusgrowing.app.domain.repository.WorldRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FocusDatabase =
        Room.databaseBuilder(context, FocusDatabase::class.java, FocusDatabase.NAME)
            // .addMigrations(MIGRATION_1_2) ← add real migrations here when the schema changes.
            .build()

    @Provides fun provideMissionDao(db: FocusDatabase): MissionDao = db.missionDao()
    @Provides fun provideFocusSessionDao(db: FocusDatabase): FocusSessionDao = db.focusSessionDao()
    @Provides fun provideWorldDao(db: FocusDatabase): WorldDao = db.worldDao()
    @Provides fun provideBackgroundDao(db: FocusDatabase): BackgroundDao = db.backgroundDao()
    @Provides fun provideNotificationDao(db: FocusDatabase): NotificationDao = db.notificationDao()

    @Provides
    @Singleton
    @SettingsStore
    fun provideSettingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("settings") })

    @Provides
    @Singleton
    @TimerStore
    fun provideTimerStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { context.preferencesDataStoreFile("timer_state") })

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindMissionRepository(impl: MissionRepositoryImpl): MissionRepository
    @Binds abstract fun bindFocusSessionRepository(impl: FocusSessionRepositoryImpl): FocusSessionRepository
    @Binds abstract fun bindWorldRepository(impl: WorldRepositoryImpl): WorldRepository
    @Binds abstract fun bindBackgroundRepository(impl: BackgroundRepositoryImpl): BackgroundRepository
    @Binds abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun bindTimerStateRepository(impl: TimerStateRepositoryImpl): TimerStateRepository
    @Binds abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
    @Binds abstract fun bindRewardRepository(impl: RewardRepositoryImpl): RewardRepository
    @Binds abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
    @Binds abstract fun bindCelebrationQueue(impl: InMemoryCelebrationQueue): CelebrationQueue
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PremiumModule {
    @Binds abstract fun bindPremiumManager(impl: DefaultPremiumManager): PremiumManager

    companion object {
        /** Release: real Google Play Billing. Debug: simulated store (see BuildConfig.FAKE_BILLING). */
        @Provides
        @Singleton
        fun providePurchaseManager(
            play: Provider<PlayBillingManager>,
            fake: Provider<FakePurchaseManager>,
        ): PurchaseManager = if (BuildConfig.FAKE_BILLING) fake.get() else play.get()
    }
}
