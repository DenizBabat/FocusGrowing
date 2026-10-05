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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusChipRow
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.IconBadge
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.image.BackgroundImageView
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundSource
import com.focusgrowing.app.domain.model.PremiumLimits
import com.focusgrowing.app.presentation.common.displayLabel

/** Translated name of a gallery filter tab. */
@Composable
private fun GalleryTab.label(): String = stringResource(
    when (this) {
        GalleryTab.ALL -> R.string.bg_tab_all
        GalleryTab.MY_PHOTOS -> R.string.bg_category_my_photos
        GalleryTab.FAVORITES -> R.string.bg_tab_favorites
        GalleryTab.NATURE -> R.string.bg_category_nature
        GalleryTab.CITY -> R.string.bg_category_city
        GalleryTab.ABSTRACT -> R.string.bg_category_abstract
        GalleryTab.SPACE -> R.string.bg_category_space
        GalleryTab.MINIMAL -> R.string.bg_category_minimal
        GalleryTab.DARK -> R.string.bg_category_dark
    },
)

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
    val noCameraMessage = stringResource(R.string.bg_error_no_camera_app)
    val noFileManagerMessage = stringResource(R.string.bg_error_no_file_manager)

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
        FocusTopBar(title = stringResource(R.string.bg_gallery_title), onBack = onBack) {
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp).padding(end = 4.dp), strokeWidth = 2.dp, color = colors.primary)
        }
        FocusChipRow(
            options = GalleryTab.entries.map { it.label() },
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
                                Toast.makeText(context, noCameraMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onFiles = {
                        if (viewModel.canAddMore()) {
                            try {
                                filePicker.launch(arrayOf("image/*"))
                            } catch (_: Exception) {
                                Toast.makeText(context, noFileManagerMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }
            if (state.items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(if (state.tab == GalleryTab.FAVORITES) R.string.bg_empty_favorites else R.string.bg_empty_tab),
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
            title = { Text(stringResource(R.string.bg_limit_title)) },
            text = { Text(stringResource(R.string.bg_limit_message, PremiumLimits.FREE_CUSTOM_BACKGROUNDS)) },
            confirmButton = { TextButton(onClick = { showLimitDialog = false; onOpenPremium() }) { Text(stringResource(R.string.bg_limit_see_premium)) } },
            dismissButton = { TextButton(onClick = { showLimitDialog = false }) { Text(stringResource(R.string.bg_limit_not_now)) } },
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.bg_delete_title)) },
            text = {
                Text(
                    stringResource(
                        if (item.source == BackgroundSource.CAMERA) R.string.bg_delete_message_camera else R.string.bg_delete_message_photo,
                        item.displayLabel(),
                    ),
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.delete(item); pendingDelete = null }) { Text(stringResource(R.string.bg_remove), color = colors.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
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
                Text(stringResource(R.string.bg_add_title), style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    (if (isPremium) stringResource(R.string.bg_add_photos_stay_on_device) else stringResource(R.string.bg_add_free_count, customCount, PremiumLimits.FREE_CUSTOM_BACKGROUNDS)) +
                        "\n" + stringResource(R.string.bg_add_adjust_hint),
                    style = FocusTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(FocusTheme.spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(FocusTheme.spacing.sm)) {
            SourceButton(Icons.Rounded.PhotoLibrary, stringResource(R.string.bg_source_gallery), onGallery, Modifier.weight(1f))
            SourceButton(Icons.Rounded.PhotoCamera, stringResource(R.string.bg_source_camera), onCamera, Modifier.weight(1f))
            SourceButton(Icons.Rounded.Folder, stringResource(R.string.bg_source_files), onFiles, Modifier.weight(1f))
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
    val label = item.displayLabel()
    val tileDescription = if (selected) stringResource(R.string.bg_a11y_tile_selected, label) else label
    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(shape)
                .border(if (selected) 3.dp else 0.dp, if (selected) colors.primary else colors.cardBorder, shape)
                .combinedClickable(role = Role.RadioButton, onClick = onSelect, onLongClick = { menu = true })
                .semantics { contentDescription = tileDescription },
        ) {
            BackgroundImageView(item, Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(colors.scrim.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(label, style = FocusTheme.typography.labelSmall, color = colors.onScrim, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                contentDescription = stringResource(if (item.isFavorite) R.string.bg_a11y_remove_favorite else R.string.bg_a11y_add_favorite),
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
                text = { Text(stringResource(if (item.isDefault) R.string.bg_menu_preview else R.string.bg_menu_preview_adjust)) },
                leadingIcon = { Icon(Icons.Rounded.Crop, contentDescription = null) },
                onClick = { menu = false; onAdjust() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(if (item.isFavorite) R.string.bg_menu_unfavorite else R.string.bg_menu_favorite)) },
                leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                onClick = { menu = false; onFavorite() },
            )
            if (!item.isDefault) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bg_remove)) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = { menu = false; onDelete() },
                )
            }
        }
    }
}
