package com.focusgrowing.app.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusChip
import com.focusgrowing.app.core.designsystem.component.FocusTextButton
import com.focusgrowing.app.core.designsystem.component.focusTextFieldColors
import com.focusgrowing.app.core.designsystem.illustration.ChecklistIllustration
import com.focusgrowing.app.core.designsystem.illustration.InsightIllustration
import com.focusgrowing.app.core.designsystem.illustration.LandscapeBackdrop
import com.focusgrowing.app.core.designsystem.illustration.WorldIsland
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.domain.logic.WorldProgression

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.page > 0) { viewModel.back() }

    AnimatedContent(
        targetState = state.page,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboarding",
        modifier = Modifier.fillMaxSize().background(FocusTheme.colors.background),
    ) { page ->
        when (page) {
            0 -> SplashPage(onStart = viewModel::next)
            1 -> IntroPage(
                number = 1, title = "Mission",
                text = "Create your tasks, set your goals and take action.",
                page = page, onNext = viewModel::next, onSkip = viewModel::skipIntro,
            ) { ChecklistIllustration(Modifier.fillMaxWidth().aspectRatio(1.1f)) }
            2 -> IntroPage(
                number = 2, title = "World",
                text = "Complete focus sessions, grow your personal world.",
                page = page, onNext = viewModel::next, onSkip = viewModel::skipIntro,
            ) { WorldIsland(WorldProgression.unlockedItems(5, isPremium = false), Modifier.fillMaxWidth().aspectRatio(1.1f)) }
            3 -> IntroPage(
                number = 3, title = "Intelligence",
                text = "Discover your habits, get insights, become a better you.",
                page = page, onNext = viewModel::next, onSkip = null, nextLabel = "Get Started",
            ) { InsightIllustration(Modifier.fillMaxWidth().aspectRatio(1.1f)) }
            4 -> FocusLengthPage(state, viewModel)
            else -> FirstGoalPage(state, viewModel, onFinish = { viewModel.finish(onFinished) })
        }
    }
}

@Composable
private fun SplashPage(onStart: () -> Unit) {
    val colors = FocusTheme.colors
    Box(Modifier.fillMaxSize()) {
        LandscapeBackdrop(Modifier.fillMaxSize(), showLake = true)
        Box(
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Brush.verticalGradient(listOf(colors.background, colors.background.copy(alpha = 0f)))),
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = FocusTheme.spacing.xl, vertical = FocusTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(FocusTheme.spacing.xl))
            Icon(Icons.Rounded.Eco, contentDescription = null, tint = colors.primary, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(FocusTheme.spacing.md))
            Text(
                "Mission\nWorld\nIntelligence",
                style = FocusTheme.typography.displaySmall,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FocusTheme.spacing.md))
            Text(
                "Focus on your missions.\nGrow your world.",
                style = FocusTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            FocusButton(
                "Get Started",
                onClick = onStart,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IntroPage(
    number: Int,
    title: String,
    text: String,
    page: Int,
    onNext: () -> Unit,
    onSkip: (() -> Unit)?,
    nextLabel: String = "Next",
    art: @Composable () -> Unit,
) {
    OnboardingScaffold(
        page = page,
        primaryLabel = nextLabel,
        onPrimary = onNext,
        secondaryLabel = if (onSkip != null) "Skip" else null,
        onSecondary = onSkip ?: {},
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$number.", style = FocusTheme.typography.headlineLarge, color = FocusTheme.colors.primary)
            Spacer(Modifier.width(FocusTheme.spacing.sm))
            Text(title, style = FocusTheme.typography.headlineLarge, color = FocusTheme.colors.onBackground)
        }
        Spacer(Modifier.height(FocusTheme.spacing.md))
        Text(text, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.xl))
        art()
    }
}

@Composable
private fun FocusLengthPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    OnboardingScaffold(
        page = state.page,
        primaryLabel = "Continue",
        onPrimary = viewModel::next,
    ) {
        Text("How long do you usually focus?", style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text("You can change this any time in Settings.", style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.xl))
        listOf(25, 30, 45).forEach { minutes ->
            OptionRow(
                label = "$minutes minutes",
                selected = !state.useCustom && state.focusMinutes == minutes,
                onClick = { viewModel.selectMinutes(minutes) },
            )
            Spacer(Modifier.height(FocusTheme.spacing.sm))
        }
        OptionRow(label = "Custom", selected = state.useCustom, onClick = viewModel::selectCustom)
        if (state.useCustom) {
            Spacer(Modifier.height(FocusTheme.spacing.sm))
            OutlinedTextField(
                value = state.customMinutesText,
                onValueChange = viewModel::setCustomMinutes,
                label = { Text("Minutes (1–180)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = focusTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FirstGoalPage(state: OnboardingUiState, viewModel: OnboardingViewModel, onFinish: () -> Unit) {
    OnboardingScaffold(
        page = state.page,
        primaryLabel = "Start growing",
        onPrimary = onFinish,
        loading = state.saving,
        secondaryLabel = "Skip for now",
        onSecondary = onFinish,
    ) {
        Text("Choose your first goal.", style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(
            "It becomes your first mission. Every focus session moves it forward.",
            style = FocusTheme.typography.bodyMedium,
            color = FocusTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(FocusTheme.spacing.xl))
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text("Your name (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = focusTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FocusTheme.spacing.md))
        OutlinedTextField(
            value = state.firstGoal,
            onValueChange = viewModel::setGoal,
            label = { Text("e.g. Finish authentication") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = focusTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm)) {
            listOf("Read a book", "Learn Kotlin", "Exercise").forEach { idea ->
                FocusChip(idea, selected = state.firstGoal == idea, onClick = { viewModel.setGoal(idea) })
            }
        }
    }
}

@Composable
private fun OnboardingScaffold(
    page: Int,
    primaryLabel: String,
    onPrimary: () -> Unit,
    loading: Boolean = false,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = FocusTheme.spacing.xl),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = FocusTheme.spacing.xxl),
            content = content,
        )
        PageDots(current = page - 1, count = OnboardingViewModel.PAGE_COUNT - 1)
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        FocusButton(primaryLabel, onClick = onPrimary, loading = loading, modifier = Modifier.fillMaxWidth())
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            if (secondaryLabel != null) FocusTextButton(secondaryLabel, onClick = onSecondary)
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val selected = i == current
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (selected) 18.dp else 7.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(if (selected) FocusTheme.colors.primary else FocusTheme.colors.progressTrack),
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    FocusChip(label = label, selected = selected, onClick = onClick, modifier = Modifier.fillMaxWidth())
}
