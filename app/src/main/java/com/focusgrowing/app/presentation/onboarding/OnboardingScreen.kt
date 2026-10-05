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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
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
import com.focusgrowing.app.presentation.common.LanguageOptions

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
            OnboardingViewModel.PAGE_LANGUAGE -> LanguagePage(page = page, onNext = viewModel::next)
            2 -> IntroPage(
                number = 1, title = stringResource(R.string.onboarding_mission_title),
                text = stringResource(R.string.onboarding_mission_text),
                page = page, onNext = viewModel::next, onSkip = viewModel::skipIntro,
            ) { ChecklistIllustration(Modifier.fillMaxWidth().aspectRatio(1.1f)) }
            3 -> IntroPage(
                number = 2, title = stringResource(R.string.onboarding_world_title),
                text = stringResource(R.string.onboarding_world_text),
                page = page, onNext = viewModel::next, onSkip = viewModel::skipIntro,
            ) { WorldIsland(WorldProgression.unlockedItems(5, isPremium = false), Modifier.fillMaxWidth().aspectRatio(1.1f)) }
            4 -> IntroPage(
                number = 3, title = stringResource(R.string.onboarding_intelligence_title),
                text = stringResource(R.string.onboarding_intelligence_text),
                page = page, onNext = viewModel::next, onSkip = null, nextLabel = stringResource(R.string.onboarding_get_started),
            ) { InsightIllustration(Modifier.fillMaxWidth().aspectRatio(1.1f)) }
            OnboardingViewModel.PAGE_FOCUS_LENGTH -> FocusLengthPage(state, viewModel)
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
                stringResource(R.string.onboarding_tagline),
                style = FocusTheme.typography.displaySmall,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FocusTheme.spacing.md))
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = FocusTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            FocusButton(
                stringResource(R.string.onboarding_get_started),
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
    nextLabel: String = stringResource(R.string.onboarding_next),
    art: @Composable () -> Unit,
) {
    OnboardingScaffold(
        page = page,
        primaryLabel = nextLabel,
        onPrimary = onNext,
        secondaryLabel = if (onSkip != null) stringResource(R.string.onboarding_skip) else null,
        onSecondary = onSkip ?: {},
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.onboarding_step_number, number), style = FocusTheme.typography.headlineLarge, color = FocusTheme.colors.primary)
            Spacer(Modifier.width(FocusTheme.spacing.sm))
            Text(title, style = FocusTheme.typography.headlineLarge, color = FocusTheme.colors.onBackground)
        }
        Spacer(Modifier.height(FocusTheme.spacing.md))
        Text(text, style = FocusTheme.typography.bodyLarge, color = FocusTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.xl))
        art()
    }
}

/** Language choice. "System default" is preselected; picking a language switches the app immediately. */
@Composable
private fun LanguagePage(page: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        page = page,
        primaryLabel = stringResource(R.string.common_continue),
        onPrimary = onNext,
    ) {
        Text(stringResource(R.string.language_title), style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(stringResource(R.string.onboarding_change_later), style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        LanguageOptions()
    }
}

@Composable
private fun FocusLengthPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    OnboardingScaffold(
        page = state.page,
        primaryLabel = stringResource(R.string.common_continue),
        onPrimary = viewModel::next,
    ) {
        Text(stringResource(R.string.onboarding_focus_length_title), style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(stringResource(R.string.onboarding_change_later), style = FocusTheme.typography.bodyMedium, color = FocusTheme.colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.xl))
        listOf(25, 30, 45).forEach { minutes ->
            OptionRow(
                label = stringResource(R.string.onboarding_minutes_option, minutes),
                selected = !state.useCustom && state.focusMinutes == minutes,
                onClick = { viewModel.selectMinutes(minutes) },
            )
            Spacer(Modifier.height(FocusTheme.spacing.sm))
        }
        OptionRow(label = stringResource(R.string.onboarding_custom), selected = state.useCustom, onClick = viewModel::selectCustom)
        if (state.useCustom) {
            Spacer(Modifier.height(FocusTheme.spacing.sm))
            OutlinedTextField(
                value = state.customMinutesText,
                onValueChange = viewModel::setCustomMinutes,
                label = { Text(stringResource(R.string.onboarding_custom_minutes_label)) },
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
        primaryLabel = stringResource(R.string.onboarding_start_growing),
        onPrimary = onFinish,
        loading = state.saving,
        secondaryLabel = stringResource(R.string.onboarding_skip_for_now),
        onSecondary = onFinish,
    ) {
        Text(stringResource(R.string.onboarding_first_goal_title), style = FocusTheme.typography.headlineMedium, color = FocusTheme.colors.onBackground)
        Spacer(Modifier.height(FocusTheme.spacing.sm))
        Text(
            stringResource(R.string.onboarding_first_goal_text),
            style = FocusTheme.typography.bodyMedium,
            color = FocusTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(FocusTheme.spacing.xl))
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = focusTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FocusTheme.spacing.md))
        OutlinedTextField(
            value = state.firstGoal,
            onValueChange = viewModel::setGoal,
            label = { Text(stringResource(R.string.onboarding_goal_example)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = focusTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FocusTheme.spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm)) {
            listOf(
                stringResource(R.string.onboarding_idea_read),
                stringResource(R.string.onboarding_idea_learn),
                stringResource(R.string.onboarding_idea_exercise),
            ).forEach { idea ->
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
