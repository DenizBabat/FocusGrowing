package com.focusgrowing.app.presentation.background

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusChipRow
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.image.BackgroundImageView
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.PremiumLimits

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackgroundGalleryScreen(
    onBack: () -> Unit,
    onAdjust: (String) -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: BackgroundGalleryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = FocusTheme.spacing
    val colors = FocusTheme.colors
    var showLimitDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BackgroundImage?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onImagePicked(uri, BackgroundSource.GALLERY)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onImagePicked(uri, BackgroundSource.FILE)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        cameraUri?.let { viewModel.onCameraResult(Uri.parse(it), success) }
        cameraUri = null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GalleryEvent.Message -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                is GalleryEvent.OpenAdjust -> onAdjust(event.backgroundId)
                GalleryEvent.LimitReached -> showLimitDialog = true
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        FocusTopBar(title = "Background Gallery", onBack = onBack) {
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp, color = colors.primary)
        }
        FocusChipRow(
            options = GalleryTab.entries.map { it.label },
            selectedIndex = GalleryTab.entries.indexOf(state.tab),
            onSelect = { viewModel.setTab(GalleryTab.entries[it]) },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(horizontal = spacing.screen, vertical = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AddBackgroundCard(
                    customCount = state.customCount,
                    isPremium = state.isPremium,
                    onGallery = {
                        if (viewModel.canAddMore()) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    onCamera = {
                        if (viewModel.canAddMore()) {
                            try {
                                val uri = viewModel.newCameraUri()
                                cameraUri = uri.toString()
                                camera.launch(uri)
                            } catch (_: Exception) {
                                cameraUri = null
                                Toast.makeText(context, "No camera app is available.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onFiles = {
                        if (viewModel.canAddMore()) {
                            try {
                                filePicker.launch(arrayOf("image/*"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No file manager is available.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }
            if (state.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        if (state.tab == GalleryTab.FAVORITES) "Tap the heart on a background to add it here." else "No backgrounds here yet.",
                        style = FocusTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(spacing.lg),
                    )
                }
            }
            items(state.items, key = { it.id }) { item ->
                BackgroundTile(
                    item = item,
                    selected = item.id == state.selectedId,
                    onSelect = { if (item.id == state.selectedId) onAdjust(item.id) else viewModel.select(item.id) },
                    onAdjust = { onAdjust(item.id) },
                    onFavorite = { viewModel.toggleFavorite(item) },
                    onDelete = { pendingDelete = item },
                )
            }
        }
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Free limit reached") },
            text = { Text("Free users can keep ${PremiumLimits.FREE_CUSTOM_BACKGROUNDS} personal backgrounds. Remove one, or unlock unlimited backgrounds with Premium.") },
            confirmButton = { TextButton(onClick = { showLimitDialog = false; onOpenPremium() }) { Text("See Premium") } },
            dismissButton = { TextButton(onClick = { showLimitDialog = false }) { Text("Not now") } },
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove background?") },
            text = {
                Text(
                    if (item.source == BackgroundSource.CAMERA) {
                        "\"${item.name}\" was taken for this app and is stored only here, so it will be deleted."
                    } else {
                        "\"${item.name}\" will be removed from the app. The original photo on your device is not deleted."
                    },
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.delete(item); pendingDelete = null }) { Text("Remove", color = colors.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddBackgroundCard(customCount: Int, isPremium: Boolean, onGallery: () -> Unit, onCamera: () -> Unit, onFiles: () -> Unit) {
    val colors = FocusTheme.colors
    FocusCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.AddPhotoAlternate, colors.primary, colors.primaryContainer)
            Spacer(Modifier.width(FocusTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Add Background", style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    (if (isPremium) "Your photos stay on this device" else "$customCount / ${PremiumLimits.FREE_CUSTOM_BACKGROUNDS} personal backgrounds") +
                        "\nTap the selected background again to preview & adjust.",
                    style = FocusTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(FocusTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm)) {
            SourceButton(Icons.Rounded.PhotoLibrary, "Gallery", onGallery, Modifier.weight(1f))
            SourceButton(Icons.Rounded.PhotoCamera, "Camera", onCamera, Modifier.weight(1f))
            SourceButton(Icons.Rounded.Folder, "Files", onFiles, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SourceButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    FocusCard(modifier = modifier, onClick = onClick, color = FocusTheme.colors.surfaceMuted, border = false, contentPadding = PaddingValues(vertical = 10.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = FocusTheme.colors.primary)
            Text(label, style = FocusTheme.typography.labelMedium, color = FocusTheme.colors.onSurface)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BackgroundTile(
    item: BackgroundImage,
    selected: Boolean,
    onSelect: () -> Unit,
    onAdjust: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = FocusTheme.colors
    var menu by remember { mutableStateOf(false) }
    val shape = FocusTheme.shapes.medium
    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(shape)
                .border(if (selected) 3.dp else 0.dp, if (selected) colors.primary else colors.cardBorder, shape)
                .combinedClickable(role = Role.RadioButton, onClick = onSelect, onLongClick = { menu = true })
                .semantics { contentDescription = item.name + if (selected) ", selected" else "" },
        ) {
            BackgroundImageView(item, Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(colors.scrim.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(item.name, style = FocusTheme.typography.labelSmall, color = colors.onScrim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(CircleShape).background(colors.surface),
                )
            }
            Icon(
                if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (item.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (item.isFavorite) colors.danger else colors.onScrim,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .combinedClickable(onClick = onFavorite)
                    .padding(6.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (item.isDefault) "Preview" else "Preview & adjust") },
                leadingIcon = { Icon(Icons.Rounded.Crop, contentDescription = null) },
                onClick = { menu = false; onAdjust() },
            )
            DropdownMenuItem(
                text = { Text(if (item.isFavorite) "Unfavorite" else "Favorite") },
                leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                onClick = { menu = false; onFavorite() },
            )
            if (!item.isDefault) {
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}
