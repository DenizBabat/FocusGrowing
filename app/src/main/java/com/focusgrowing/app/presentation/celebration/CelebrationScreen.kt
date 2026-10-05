package com.focusgrowing.app.presentation.celebration

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.ads.SessionEndAdCard
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusProgressBar
import com.focusgrowing.app.core.designsystem.component.FocusTextButton
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.component.RewardStat
import com.focusgrowing.app.core.designsystem.illustration.ChecklistIllustration
import com.focusgrowing.app.core.designsystem.illustration.Confetti
import com.focusgrowing.app.core.designsystem.illustration.ForestFooter
import com.focusgrowing.app.core.designsystem.illustration.LeafFrame
import com.focusgrowing.app.core.designsystem.illustration.MedalBadge
import com.focusgrowing.app.core.designsystem.illustration.WorldIsland
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.logic.WorldProgression
import com.focusgrowing.app.domain.model.Celebration
import com.focusgrowing.app.domain.model.WorldItemType
import com.focusgrowing.app.presentation.common.icon
import com.focusgrowing.app.presentation.common.label
import com.google.android.gms.ads.nativead.NativeAd
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CelebrationScreen(
    onClose: () -> Unit,
    onOpenMission: (Long) -> Unit,
    onExploreWorld: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: CelebrationViewModel = hiltViewModel(),
) {
    val current by viewModel.current.collectAsStateWithLifecycle()
    val liveAd by viewModel.sessionEndAd.collectAsStateWithLifecycle()
    // Only show an ad that was ready when the screen opened. An ad popping in later would move
    // the "Continue" button under the user's finger and cause accidental taps.
    val adReadyAtEntry = remember { viewModel.sessionEndAd.value != null }
    val sessionEndAd = if (adReadyAtEntry) liveAd else null
    LaunchedEffect(current) { if (current == null) onClose() }
    BackHandler { viewModel.dismiss() }

    val reduceMotion = FocusTheme.reduceMotion
    val celebration = current ?: return
    AnimatedContent(
        targetState = celebration,
        transitionSpec = {
            if (reduceMotion) fadeIn() togetherWith fadeOut()
            else (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith fadeOut()
        },
        label = "celebration",
    ) { item ->
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FocusTheme.spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = viewModel::dismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = FocusTheme.colors.onSurfaceVariant)
                    }
                }
                when (item) {
                    is Celebration.FocusCompleted -> FocusCompletedContent(
                        item,
                        ad = sessionEndAd,
                        onAdShown = viewModel::onAdShown,
                        onRemoveAds = onOpenPremium,
                        onContinue = viewModel::dismiss,
                    )
                    is Celebration.MissionCompleted -> MissionCompletedContent(
                        item,
                        onViewMission = { viewModel.dismiss(); onOpenMission(item.missionId) },
                        onContinue = viewModel::dismiss,
                    )
                    is Celebration.StreakContinued -> StreakContent(item, onContinue = viewModel::dismiss)
                    is Celebration.WorldLevelUp -> LevelUpContent(
                        item,
                        onExplore = { viewModel.dismiss(); onExploreWorld() },
                    )
                    is Celebration.MissionCreated -> MissionCreatedContent(
                        item,
                        onViewMission = { viewModel.dismiss(); onOpenMission(item.missionId) },
                        onContinue = viewModel::dismiss,
                    )
                }
                Spacer(Modifier.height(FocusTheme.spacing.lg))
            }
        }
    }
}

@Composable
private fun Headline(title: String, subtitle: String? = null, body: String? = null) {
    Text(title, style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground, textAlign = TextAlign.Center)
    subtitle?.let {
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(it, style = FocusTheme.typography.titleMedium, color = FocusTheme.colors.onBackground, textAlign = TextAlign.Center)
    }
    body?.let {
        Spacer(Modifier.height(FocusTheme.spacing.md))
        Text(it, style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MedalHero(icon: ImageVector, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
        Confetti(Modifier.fillMaxSize())
        LeafFrame(Modifier.size(260.dp, 200.dp))
        MedalBadge(icon, color, size = 150.dp)
    }
}

@Composable
private fun StatsCard(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    FocusCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(FocusTheme.spacing.sm)) {
        Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceEvenly, content = content)
    }
}

@Composable
private fun StatDivider() {
    VerticalDivider(color = FocusTheme.colors.divider, modifier = Modifier.padding(vertical = FocusTheme.spacing.md))
}

@Composable
private fun FocusCompletedContent(
    item: Celebration.FocusCompleted,
    ad: NativeAd?,
    onAdShown: () -> Unit,
    onRemoveAds: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = FocusTheme.colors
    MedalHero(Icons.Rounded.Star, colors.xp)
    Headline("Great Job!", body = "You completed your focus session.")
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    StatsCard {
        RewardStat(Icons.Rounded.AccessTime, "%d:00".format(item.focusMinutes), "Focus Time", colors.info, colors.infoContainer, Modifier.weight(1f))
        StatDivider()
        RewardStat(Icons.Rounded.Eco, "+${item.xpEarned} XP", "World XP", colors.success, colors.primaryContainer, Modifier.weight(1f))
        if (item.missionProgressDelta > 0) {
            StatDivider()
            RewardStat(Icons.AutoMirrored.Rounded.TrendingUp, "+${item.missionProgressDelta}", "Progress", colors.accentPurple, colors.accentPurpleContainer, Modifier.weight(1f))
        }
    }
    // The only ad placement in the session flow: after the session, under the rewards. Never for Premium.
    if (ad != null) {
        LaunchedEffect(ad) { onAdShown() }
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        SessionEndAdCard(ad)
    }
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    FocusButton("Continue", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    if (ad != null) {
        FocusTextButton("Remove ads with Premium", onClick = onRemoveAds)
    }
}

@Composable
private fun MissionCompletedContent(item: Celebration.MissionCompleted, onViewMission: () -> Unit, onContinue: () -> Unit) {
    val colors = FocusTheme.colors
    MedalHero(Icons.Rounded.Star, colors.xp)
    Headline("Mission Completed!", subtitle = "“${item.title}”", body = "Great job! You completed your mission and earned rewards.")
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    StatsCard {
        RewardStat(Icons.Rounded.Eco, "+${item.xpEarned} XP", "World XP", colors.success, colors.primaryContainer, Modifier.weight(1f))
        StatDivider()
        RewardStat(Icons.AutoMirrored.Rounded.TrendingUp, "${item.pomodoros}", "Pomodoros", colors.info, colors.infoContainer, Modifier.weight(1f))
        if (item.focusMinutes > 0) {
            StatDivider()
            RewardStat(Icons.Rounded.Schedule, "+${item.focusMinutes} min", "Focus Time", colors.accentPurple, colors.accentPurpleContainer, Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    FocusButton("View Mission", onClick = onViewMission, modifier = Modifier.fillMaxWidth())
    FocusTextButton("Continue", onClick = onContinue)
    ForestFooter(Modifier.fillMaxWidth().height(110.dp))
}

@Composable
private fun StreakContent(item: Celebration.StreakContinued, onContinue: () -> Unit) {
    val colors = FocusTheme.colors
    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
        ForestFooter(Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter))
        MedalBadge(Icons.Rounded.LocalFireDepartment, colors.streak, size = 150.dp, modifier = Modifier.align(Alignment.TopCenter))
    }
    Headline(
        "Streak Continues!",
        subtitle = "You’re on a ${item.days} day streak!",
        body = "Your focus and consistency are building a better you.",
    )
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    FocusCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.values().forEachIndexed { index, day ->
                val active = item.weekActivity.getOrElse(index) { false }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (active) {
                        IconBadge(Icons.Rounded.Check, colors.onPrimary, colors.primary, size = 34.dp)
                    } else {
                        IconBadge(Icons.Rounded.Check, colors.progressTrack, colors.surfaceMuted, size = 34.dp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = FocusTheme.typography.labelSmall,
                        color = if (active) colors.onSurface else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    FocusButton("Keep Going", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(FocusTheme.spacing.lg))
    Text(
        "“Discipline is the bridge between goals and achievement.”",
        style = FocusTheme.extraTypography.quote,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LevelUpContent(item: Celebration.WorldLevelUp, onExplore: () -> Unit) {
    val colors = FocusTheme.colors
    WorldIsland(
        items = WorldProgression.unlockedItems(item.level, isPremium = true).filterNot { it.isPremium && it !in item.unlocked }.toSet(),
        modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).padding(horizontal = FocusTheme.spacing.xl),
    )
    Headline("World Level Up!", subtitle = "Your world has reached Level ${item.level}!")
    Spacer(Modifier.height(FocusTheme.spacing.lg))
    FocusCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MedalBadge(Icons.Rounded.Star, colors.xp, size = 56.dp)
            Spacer(Modifier.width(FocusTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Level ${item.level}", style = FocusTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(6.dp))
                FocusProgressBar(item.xpIntoLevel.toFloat() / item.xpForNextLevel.coerceAtLeast(1))
                Spacer(Modifier.height(4.dp))
                Text(
                    "${item.xpIntoLevel} / ${item.xpForNextLevel} XP",
                    style = FocusTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
    if (item.unlocked.isNotEmpty()) {
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        Text(
            "New elements unlocked:",
            style = FocusTheme.typography.titleSmall,
            color = colors.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm)) {
            item.unlocked.take(4).forEach { unlocked -> UnlockTile(unlocked, Modifier.weight(1f)) }
        }
    }
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    FocusButton("Explore Your World", onClick = onExplore, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(FocusTheme.spacing.md))
    Text("Your focus builds a more beautiful world.", style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
}

@Composable
private fun UnlockTile(item: WorldItemType, modifier: Modifier) {
    FocusCard(modifier = modifier, contentPadding = PaddingValues(FocusTheme.spacing.sm)) {
        WorldIsland(setOf(item), Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            item.displayName,
            style = FocusTheme.typography.labelSmall,
            color = FocusTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MissionCreatedContent(item: Celebration.MissionCreated, onViewMission: () -> Unit, onContinue: () -> Unit) {
    val colors = FocusTheme.colors
    ChecklistIllustration(Modifier.fillMaxWidth().height(220.dp))
    Headline("New Mission Assigned", subtitle = "“${item.title}”", body = "It's time to add a new mission to your list. Keep your momentum going!")
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    StatsCard {
        RewardStat(Icons.Rounded.Schedule, "${item.estimatedPomodoros} Pomodoros", "Estimated", colors.info, colors.infoContainer, Modifier.weight(1f))
        StatDivider()
        RewardStat(Icons.Rounded.Flag, item.priority.label(), "Priority", colors.streak, colors.streakContainer, Modifier.weight(1f))
        StatDivider()
        RewardStat(item.category.icon(), item.category.label(), "Category", colors.accentPurple, colors.accentPurpleContainer, Modifier.weight(1f))
    }
    Spacer(Modifier.height(FocusTheme.spacing.xl))
    FocusButton("View Mission", onClick = onViewMission, modifier = Modifier.fillMaxWidth())
    FocusTextButton("Continue", onClick = onContinue)
    FocusCard(modifier = Modifier.fillMaxWidth(), color = colors.primaryContainer.copy(alpha = 0.5f), border = false) {
        Text("Small steps every day lead to big results. 🌱", style = FocusTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
    }
}
