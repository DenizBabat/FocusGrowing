package com.focusgrowing.app.domain.repository

import com.focusgrowing.app.domain.model.PaletteTrial
import kotlinx.coroutines.flow.Flow

/** Rewards earned by watching a rewarded ad. Stored on the device only. */
interface RewardRepository {
    val paletteTrial: Flow<PaletteTrial?>
    suspend fun setPaletteTrial(trial: PaletteTrial)
}
