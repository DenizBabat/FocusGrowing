package com.focusgrowing.app.core.image

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.focusgrowing.app.core.designsystem.illustration.SceneBackground
import com.focusgrowing.app.domain.model.BackgroundImage
import com.focusgrowing.app.domain.model.BackgroundScene

/**
 * Renders any background (built-in scene or user photo) so that it fills its bounds
 * WITHOUT distortion:
 * - ContentScale.Crop keeps the aspect ratio and crops the overflow,
 * - [BackgroundImage.alignX]/[BackgroundImage.alignY] choose which part stays visible,
 * - [BackgroundImage.zoom] adds optional extra zoom.
 * Coil decodes the bitmap at the size of this composable (downsampling large photos) and
 * applies EXIF rotation, so a 48 MP photo never lands in memory at full resolution.
 * A missing/deleted/revoked image falls back to a built-in scene and reports [onLoadError].
 */
@Composable
fun BackgroundImageView(
    background: BackgroundImage?,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 0.dp,
    alignXOverride: Float? = null,
    alignYOverride: Float? = null,
    zoomOverride: Float? = null,
    onLoadError: () -> Unit = {},
) {
    val blurModifier = if (blurRadius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(blurRadius) else Modifier
    Box(modifier.clipToBounds()) {
        val uri = background?.uri
        val scene = background?.scene
        when {
            scene != null -> SceneBackground(scene, Modifier.fillMaxSize().then(blurModifier))
            uri != null -> {
                var failed by remember(uri) { mutableStateOf(false) }
                if (failed) {
                    SceneBackground(BackgroundScene.MOUNTAIN_LAKE, Modifier.fillMaxSize().then(blurModifier))
                } else {
                    val context = LocalContext.current
                    val alignX = alignXOverride ?: background?.alignX ?: 0f
                    val alignY = alignYOverride ?: background?.alignY ?: 0f
                    val zoom = (zoomOverride ?: background?.zoom ?: 1f).coerceIn(1f, 3f)
                    val request = remember(uri) {
                        ImageRequest.Builder(context)
                            .data(uri)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(alignX, alignY),
                        onError = {
                            failed = true
                            onLoadError()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                transformOrigin = TransformOrigin((alignX + 1f) / 2f, (alignY + 1f) / 2f)
                            }
                            .then(blurModifier),
                    )
                }
            }
            else -> SceneBackground(BackgroundScene.MOUNTAIN_LAKE, Modifier.fillMaxSize().then(blurModifier))
        }
    }
}
