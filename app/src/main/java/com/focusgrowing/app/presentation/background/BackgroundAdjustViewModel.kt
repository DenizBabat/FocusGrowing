package com.focusgrowing.app.presentation.background

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.presentation.app.BackgroundAdjustRoute
import com.focusgrowing.app.presentation.app.route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdjustUiState(
    val loading: Boolean = true,
    val background: BackgroundImage? = null,
    val alignX: Float = 0f,
    val alignY: Float = 0f,
    val zoom: Float = 1f,
    val overlayAlpha: Float = 0.25f,
    val canZoom: Boolean = false,
    val notFound: Boolean = false,
)

@HiltViewModel
class BackgroundAdjustViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val backgrounds: BackgroundRepository,
    private val settings: SettingsRepository,
    premium: PremiumManager,
) : ViewModel() {

    private val id = savedStateHandle.route<BackgroundAdjustRoute>().backgroundId

    private val _state = MutableStateFlow(AdjustUiState(canZoom = premium.hasAccess(PremiumFeature.ADVANCED_CROP)))
    val state: StateFlow<AdjustUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val bg = backgrounds.getBackground(id)
            val overlay = settings.current().focusScreen.overlayAlpha
            _state.update {
                if (bg == null) it.copy(loading = false, notFound = true)
                else it.copy(loading = false, background = bg, alignX = bg.alignX, alignY = bg.alignY, zoom = bg.zoom, overlayAlpha = overlay)
            }
        }
    }

    fun drag(dxFraction: Float, dyFraction: Float) = _state.update {
        it.copy(
            alignX = (it.alignX - dxFraction * 2f).coerceIn(-1f, 1f),
            alignY = (it.alignY - dyFraction * 2f).coerceIn(-1f, 1f),
        )
    }

    fun preset(x: Float, y: Float) = _state.update { it.copy(alignX = x, alignY = y) }

    fun zoomBy(factor: Float) = _state.update {
        if (!it.canZoom) it else it.copy(zoom = (it.zoom * factor).coerceIn(1f, 3f))
    }

    fun setZoom(value: Float) = _state.update { if (!it.canZoom) it else it.copy(zoom = value.coerceIn(1f, 3f)) }

    fun reset() = _state.update { it.copy(alignX = 0f, alignY = 0f, zoom = 1f) }

    fun setOverlay(value: Float) = _state.update { it.copy(overlayAlpha = value.coerceIn(0f, 0.9f)) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val bg = s.background ?: return
        viewModelScope.launch {
            if (!bg.isDefault) backgrounds.updateAdjustment(bg.id, s.alignX, s.alignY, s.zoom)
            settings.updateFocusScreen { it.copy(selectedBackgroundId = bg.id, overlayAlpha = s.overlayAlpha) }
            onDone()
        }
    }
}
