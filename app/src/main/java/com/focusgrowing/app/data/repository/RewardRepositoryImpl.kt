package com.focusgrowing.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.focusgrowing.app.data.local.datastore.SettingsKeys
import com.focusgrowing.app.di.SettingsStore
import com.focusgrowing.app.domain.model.PaletteTrial
import com.focusgrowing.app.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardRepositoryImpl @Inject constructor(
    @SettingsStore private val store: DataStore<Preferences>,
) : RewardRepository {

    override val paletteTrial: Flow<PaletteTrial?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val id = prefs[SettingsKeys.TrialPaletteId]
            val granted = prefs[SettingsKeys.TrialGrantedAt]
            val expires = prefs[SettingsKeys.TrialExpiresAt]
            if (id != null && granted != null && expires != null) PaletteTrial(id, granted, expires) else null
        }
        .distinctUntilChanged()

    override suspend fun setPaletteTrial(trial: PaletteTrial) {
        store.edit { prefs ->
            prefs[SettingsKeys.TrialPaletteId] = trial.paletteId
            prefs[SettingsKeys.TrialGrantedAt] = trial.grantedAtMillis
            prefs[SettingsKeys.TrialExpiresAt] = trial.expiresAtMillis
        }
    }
}
