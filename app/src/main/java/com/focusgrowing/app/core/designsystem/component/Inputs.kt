package com.focusgrowing.app.core.designsystem.component

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.focusgrowing.app.core.designsystem.theme.FocusTheme

@Composable
fun focusTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = FocusTheme.colors.primary,
    unfocusedBorderColor = FocusTheme.colors.cardBorder,
    focusedLabelColor = FocusTheme.colors.primary,
    cursorColor = FocusTheme.colors.primary,
    focusedContainerColor = FocusTheme.colors.surface,
    unfocusedContainerColor = FocusTheme.colors.surface,
)
