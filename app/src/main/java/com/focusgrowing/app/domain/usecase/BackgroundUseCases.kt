package com.focusgrowing.app.domain.usecase

import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.PremiumLimits
import com.focusgrowing.app.domain.repository.BackgroundRepository
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.repository.SettingsRepository
import com.focusgrowing.app.domain.repository.DomainStrings
import javax.inject.Inject

sealed interface AddBackgroundResult {
    data class Added(val background: BackgroundImage) : AddBackgroundResult
    data object LimitReached : AddBackgroundResult
    data class Failed(val message: String) : AddBackgroundResult
}

class AddCustomBackgroundUseCase @Inject constructor(
    private val backgrounds: BackgroundRepository,
    private val settings: SettingsRepository,
    private val premium: PremiumManager,
    private val strings: DomainStrings,
) {
    suspend operator fun invoke(uri: String, source: BackgroundSource, name: String, select: Boolean = true): AddBackgroundResult {
        if (!premium.hasAccess(PremiumFeature.UNLIMITED_BACKGROUNDS) &&
            backgrounds.customCount() >= PremiumLimits.FREE_CUSTOM_BACKGROUNDS
        ) {
            return AddBackgroundResult.LimitReached
        }
        return try {
            val added = backgrounds.addCustom(uri, source, name)
            if (select) settings.updateFocusScreen { it.copy(selectedBackgroundId = added.id) }
            AddBackgroundResult.Added(added)
        } catch (e: Exception) {
            AddBackgroundResult.Failed(strings.imageAddFailed())
        }
    }
}

class DeleteBackgroundUseCase @Inject constructor(
    private val backgrounds: BackgroundRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(id: String) {
        if (id.startsWith(BackgroundImage.DEFAULT_PREFIX)) return
        backgrounds.delete(id)
        if (settings.current().focusScreen.selectedBackgroundId == id) {
            settings.updateFocusScreen { it.copy(selectedBackgroundId = BackgroundImage.FallbackId) }
        }
    }
}
