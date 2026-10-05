package com.focusgrowing.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusgrowing.app.core.locale.StringProvider
import com.focusgrowing.app.domain.model.FocusStatistics
import com.focusgrowing.app.domain.model.Insight
import com.focusgrowing.app.domain.model.PremiumFeature
import com.focusgrowing.app.domain.model.StatsRange
import com.focusgrowing.app.domain.model.StreakInfo
import com.focusgrowing.app.domain.repository.PremiumManager
import com.focusgrowing.app.domain.usecase.ObserveInsightsUseCase
import com.focusgrowing.app.domain.usecase.ObserveStatisticsUseCase
import com.focusgrowing.app.domain.usecase.ObserveStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatisticsUiState(
    val loading: Boolean = true,
    val range: StatsRange = StatsRange.WEEK,
    val stats: FocusStatistics = FocusStatistics.empty(StatsRange.WEEK),
    val insights: List<Insight> = emptyList(),
    val streak: StreakInfo = StreakInfo.Empty,
    val isPremium: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    observeStatistics: ObserveStatisticsUseCase,
    observeInsights: ObserveInsightsUseCase,
    observeStreak: ObserveStreakUseCase,
    private val premium: PremiumManager,
    strings: StringProvider,
) : ViewModel() {

    private val range = MutableStateFlow(StatsRange.WEEK)

    val uiState: StateFlow<StatisticsUiState> = combine(
        range.flatMapLatest { observeStatistics(it) },
        // Insight texts are produced in the current language: rebuild them when it changes.
        strings.language.flatMapLatest { observeInsights() },
        observeStreak(),
        premium.premiumState,
    ) { stats, insights, streak, isPremium ->
        StatisticsUiState(
            loading = false,
            range = stats.range,
            stats = stats,
            insights = insights,
            streak = streak,
            isPremium = isPremium,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    /** Returns false when the range needs premium (the UI then opens the Premium screen). */
    fun selectRange(value: StatsRange): Boolean {
        if (value.isPremium && !premium.hasAccess(PremiumFeature.ADVANCED_STATISTICS)) return false
        range.value = value
        return true
    }
}
