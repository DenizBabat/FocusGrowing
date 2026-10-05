package com.focusgrowing.app

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.ads.AdsManager
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.model.ThemeMode
import com.focusgrowing.app.presentation.app.FocusGrowingApp
import com.focusgrowing.app.presentation.app.MainUiState
import com.focusgrowing.app.presentation.app.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var adsManager: AdsManager

    /** Incremented when a notification asks to open the Focus screen. */
    private val openFocusSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.uiState.value is MainUiState.Loading }
        enableEdgeToEdge()
        handleIntent(intent)
        // Ads: asks for consent where required, then loads ads. Does nothing for Premium users.
        adsManager.start(this)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainUiState.Ready
            if (ready != null) {
                val appearance = ready.appearance
                val dark = when (appearance.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                // Status/navigation bar icons follow the app theme, not only the system theme.
                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                    )
                    onDispose { }
                }
                FocusTheme(
                    palette = ready.palette,
                    darkTheme = dark,
                    dynamicColor = appearance.dynamicColor,
                    textScale = appearance.textScale.factor,
                    reduceMotion = appearance.reduceMotion,
                ) {
                    FocusGrowingApp(
                        showOnboarding = !ready.onboardingCompleted,
                        openFocusSignal = openFocusSignal.intValue,
                        hapticsEnabled = ready.hapticsEnabled,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_FOCUS, false) == true) {
            intent.removeExtra(EXTRA_OPEN_FOCUS)
            openFocusSignal.intValue += 1
        }
    }

    companion object {
        const val EXTRA_OPEN_FOCUS = "open_focus"
        private val LIGHT_SCRIM = Color.argb(0x00, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x00, 0x1B, 0x1B, 0x1B)
    }
}
