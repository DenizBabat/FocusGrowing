package com.focusgrowing.app.core.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat

/**
 * Languages the app is translated into.
 *
 * TO ADD A LANGUAGE
 *  1. Copy res/values/strings.xml to res/values-<tag>/strings.xml and translate it.
 *  2. Add an entry here (the name is written in the language itself).
 *  3. Add the tag to res/xml/locales_config.xml.
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    TURKISH("tr", "Türkçe"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "Français"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    ARABIC("ar", "العربية"),
    HINDI("hi", "हिन्दी"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어");

    companion object {
        /** Used when the phone's language isn't one of ours (same as res/values). */
        val Fallback = ENGLISH

        fun fromLanguageCode(code: String?): AppLanguage? = entries.firstOrNull { it.tag.equals(code, ignoreCase = true) }
    }
}

/**
 * The app's own language setting, independent of the phone's language.
 *
 * Built on AndroidX per-app language support: on Android 13+ the choice also appears in the
 * system's "App languages" settings; on older versions AndroidX stores it and restarts the screen.
 * Call these from the main thread.
 */
object AppLocale {
    /** The language the user picked in the app, or null when the app follows the phone's language. */
    fun selected(): AppLanguage? {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return null
        return AppLanguage.fromLanguageCode(locales[0]?.language)
    }

    /** Pass null to follow the phone's language again. The visible screen is rebuilt in the new language. */
    fun select(language: AppLanguage?) {
        if (language == selected()) return
        val locales = if (language == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** Which of our languages the phone's own language setting resolves to. */
    fun systemLanguage(context: Context): AppLanguage {
        val system = LocaleManagerCompat.getSystemLocales(context)
        for (i in 0 until system.size()) {
            AppLanguage.fromLanguageCode(system[i]?.language)?.let { return it }
        }
        return AppLanguage.Fallback
    }
}
