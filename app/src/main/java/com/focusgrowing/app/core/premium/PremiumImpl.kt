package com.focusgrowing.app.core.premium

import com.focusgrowing.app.di.ApplicationScope
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the cached entitlement. The billing layer keeps that cache in sync with Google Play. */
@Singleton
class DefaultPremiumManager @Inject constructor(
    subscriptions: SubscriptionRepository,
    @ApplicationScope scope: CoroutineScope,
) : PremiumManager {
    override val premiumState: StateFlow<Boolean> =
        subscriptions.isPremium.stateIn(scope, SharingStarted.Eagerly, false)
}
