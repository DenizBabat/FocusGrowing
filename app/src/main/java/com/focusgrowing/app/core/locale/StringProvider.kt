package com.focusgrowing.app.core.locale

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translated texts for code that isn't a Composable (ViewModels, notifications, billing messages).
 * Composables use stringResource(...) directly.
 *
 * Always resolves against the app's current language, including the in-app language choice on
 * Android versions before 13 where the application context doesn't follow it by itself.
 */
@Singleton
class StringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** A context whose resources are in the app's current language. */
    val localizedContext: Context get() = ContextCompat.getContextForLanguage(context)

    private val _language = MutableStateFlow(currentLanguageTag())

    /**
     * The app's current language tag ("tr", "en-US", ...). Collect it to rebuild texts that were
     * resolved earlier and are kept in memory (for example the insight list) after a language change.
     */
    val language: StateFlow<String> = _language.asStateFlow()

    /** Called when the screen is (re)created, which is what happens after a language change. */
    fun refreshLanguage() {
        _language.value = currentLanguageTag()
    }

    private fun currentLanguageTag(): String = localizedContext.resources.configuration.locales[0].toLanguageTag()

    fun get(@StringRes id: Int): String = localizedContext.getString(id)

    fun get(@StringRes id: Int, vararg args: Any): String = localizedContext.getString(id, *args)

    fun quantity(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        localizedContext.resources.getQuantityString(id, count, *args)
}
