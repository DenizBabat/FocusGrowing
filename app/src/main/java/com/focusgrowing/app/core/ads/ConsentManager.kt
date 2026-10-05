package com.focusgrowing.app.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google's User Messaging Platform: shows the ad consent form where the law requires one
 * (EEA, UK, Switzerland, some US states). Elsewhere nothing is shown.
 *
 * The form itself is created in AdMob → Privacy & messaging. Without a published message there,
 * no form appears and ads are simply requested.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    /** True once consent was collected (or isn't needed). Remembered from the previous launch. */
    val canRequestAds: Boolean get() = info.canRequestAds()

    private val _privacyOptionsRequired = MutableStateFlow(readPrivacyOptionsRequired())
    /** When true, Settings must offer a way to change the consent choice. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Refreshes the consent status and shows the form if one is due. [onComplete] runs on the main thread. */
    fun gather(activity: Activity, onComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    _privacyOptionsRequired.value = readPrivacyOptionsRequired()
                    onComplete()
                }
            },
            { _ ->
                // Offline or misconfigured: keep whatever was decided last time.
                _privacyOptionsRequired.value = readPrivacyOptionsRequired()
                onComplete()
            },
        )
    }

    /** Lets the user change their ad consent choice (Settings → Privacy). */
    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ ->
            _privacyOptionsRequired.value = readPrivacyOptionsRequired()
            onDismissed()
        }
    }

    private fun readPrivacyOptionsRequired(): Boolean =
        info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
}
