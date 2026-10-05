package com.focusgrowing.app.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.locale.AppLanguage
import com.focusgrowing.app.core.locale.AppLocale

/** "Türkçe", or "System default (Türkçe)" when the app follows the phone's language. */
@Composable
fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val selected = AppLocale.selected()
    return selected?.nativeName
        ?: stringResource(R.string.language_system_default_with_name, AppLocale.systemLanguage(context).nativeName)
}

/**
 * The language list: "System default" first (selected until the user picks something), then every
 * language in its own name. Picking one applies it immediately; the screen is rebuilt in that language.
 */
@Composable
fun LanguageOptions(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppLocale.selected()) }
    val systemName = remember { AppLocale.systemLanguage(context).nativeName }

    fun choose(language: AppLanguage?) {
        selected = language
        AppLocale.select(language)
    }

    Column(modifier.fillMaxWidth()) {
        LanguageRow(
            title = stringResource(R.string.language_system_default),
            subtitle = systemName,
            selected = selected == null,
            onClick = { choose(null) },
        )
        AppLanguage.entries.forEach { language ->
            LanguageRow(
                title = language.nativeName,
                subtitle = null,
                selected = selected == language,
                onClick = { choose(language) },
            )
        }
    }
}

@Composable
private fun LanguageRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = FocusTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = FocusTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.outline),
        )
        Spacer(Modifier.width(FocusTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

/** Settings → Language. */
@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LanguageOptions()
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) } },
    )
}
