package com.focusgrowing.app.presentation.background

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.R
import com.focusgrowing.app.core.image.ImageStorage
import com.focusgrowing.app.core.locale.StringProvider
import com.focusgrowing.app.domain.model.BackgroundCategory
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.PremiumLimits
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.usecase.AddBackgroundResult
import com.focusgrowing.app.domain.usecase.AddCustomBackgroundUseCase
import com.focusgrowing.app.domain.usecase.DeleteBackgroundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GalleryTab {
    ALL, MY_PHOTOS, FAVORITES, NATURE,
    CITY, ABSTRACT, SPACE, MINIMAL, DARK,
}

data class GalleryUiState(
    val tab: GalleryTab = GalleryTab.ALL,
    val items: List<BackgroundImage> = emptyList(),
    val selectedId: String = BackgroundImage.FallbackId,
    val customCount: Int = 0,
    val isPremium: Boolean = false,
    val busy: Boolean = false,
) {
    val freeLimitReached: Boolean get() = !isPremium && customCount >= PremiumLimits.FREE_CUSTOM_BACKGROUNDS
}

sealed interface GalleryEvent {
    data class Message(val text: String) : GalleryEvent
    data class OpenAdjust(val backgroundId: String) : GalleryEvent
    data object LimitReached : GalleryEvent
}

@HiltViewModel
class BackgroundGalleryViewModel @Inject constructor(
    private val backgrounds: BackgroundRepository,
    private val settings: SettingsRepository,
    private val premium: PremiumManager,
    private val addBackground: AddCustomBackgroundUseCase,
    private val deleteBackground: DeleteBackgroundUseCase,
    private val imageStorage: ImageStorage,
    private val strings: StringProvider,
) : ViewModel() {

    private val tab = MutableStateFlow(GalleryTab.ALL)
    private val busy = MutableStateFlow(false)
    private val _events = Channel<GalleryEvent>(Channel.BUFFERED)
    val events: Flow<GalleryEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<GalleryUiState> = combine(
        backgrounds.observeBackgrounds(), settings.preferences, premium.premiumState, tab, busy,
    ) { all, prefs, isPremium, t, isBusy ->
        val items = when (t) {
            GalleryTab.ALL -> all.sortedByDescending { it.isFavorite }
            GalleryTab.MY_PHOTOS -> all.filter { !it.isDefault }
            GalleryTab.FAVORITES -> all.filter { it.isFavorite }
            GalleryTab.NATURE -> all.filter { it.category == BackgroundCategory.NATURE }
            GalleryTab.CITY -> all.filter { it.category == BackgroundCategory.CITY }
            GalleryTab.ABSTRACT -> all.filter { it.category == BackgroundCategory.ABSTRACT }
            GalleryTab.SPACE -> all.filter { it.category == BackgroundCategory.SPACE }
            GalleryTab.MINIMAL -> all.filter { it.category == BackgroundCategory.MINIMAL }
            GalleryTab.DARK -> all.filter { it.category == BackgroundCategory.DARK }
        }
        GalleryUiState(
            tab = t,
            items = items,
            selectedId = prefs.focusScreen.selectedBackgroundId,
            customCount = all.count { !it.isDefault },
            isPremium = isPremium,
            busy = isBusy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    fun setTab(value: GalleryTab) {
        tab.value = value
    }

    fun select(id: String) {
        viewModelScope.launch { settings.updateFocusScreen { it.copy(selectedBackgroundId = id) } }
    }

    fun toggleFavorite(item: BackgroundImage) {
        viewModelScope.launch { backgrounds.setFavorite(item.id, !item.isFavorite) }
    }

    fun delete(item: BackgroundImage) {
        viewModelScope.launch {
            deleteBackground(item.id)
            item.uri?.let { imageStorage.release(it) }
            _events.send(GalleryEvent.Message(strings.get(R.string.bg_message_removed)))
        }
    }

    /** Checked before opening a picker so users are not asked to pick a photo we can't save. */
    fun canAddMore(): Boolean {
        if (uiState.value.freeLimitReached) {
            _events.trySend(GalleryEvent.LimitReached)
            return false
        }
        return true
    }

    fun newCameraUri(): Uri = imageStorage.newCameraUri()

    fun onCameraResult(uri: Uri, success: Boolean) {
        if (!success) {
            imageStorage.discardCameraUri(uri)
            return
        }
        add(uri, BackgroundSource.CAMERA)
    }

    fun onImagePicked(uri: Uri?, source: BackgroundSource) {
        if (uri == null) return // picker cancelled — nothing to do
        add(uri, source)
    }

    private fun add(uri: Uri, source: BackgroundSource) {
        viewModelScope.launch {
            busy.value = true
            val persisted = imageStorage.persist(uri)
            if (persisted == null) {
                busy.value = false
                _events.send(GalleryEvent.Message(strings.get(R.string.bg_error_image_unusable)))
                return@launch
            }
            val name = if (source == BackgroundSource.CAMERA) strings.get(R.string.bg_default_name_camera) else imageStorage.displayName(uri)
            when (val result = addBackground(persisted, source, name)) {
                is AddBackgroundResult.Added -> _events.send(GalleryEvent.OpenAdjust(result.background.id))
                AddBackgroundResult.LimitReached -> {
                    imageStorage.release(persisted)
                    _events.send(GalleryEvent.LimitReached)
                }
                is AddBackgroundResult.Failed -> _events.send(GalleryEvent.Message(result.message))
            }
            busy.value = false
        }
    }
}
