package com.focusgrowing.app.core.ads

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.focusgrowing.app.R
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/** Theme colors handed to the (non-Compose) ad view. */
private data class AdCardColors(
    val headline: Int,
    val body: Int,
    val accent: Int,
    val badgeBackground: Int,
    val badgeText: Int,
    val mediaBackground: Int,
)

/**
 * Native ad shown under the stats on the "focus completed" screen.
 *
 * Google requires native ads to be rendered inside a [NativeAdView] with each asset registered,
 * and to carry a visible "Ad" label. The AdChoices icon is added by the SDK itself.
 */
@Composable
fun SessionEndAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    val colors = FocusTheme.colors
    val cardColors = AdCardColors(
        headline = colors.onSurface.toArgb(),
        body = colors.onSurfaceVariant.toArgb(),
        accent = colors.primary.toArgb(),
        badgeBackground = colors.secondaryContainer.toArgb(),
        badgeText = colors.onSecondaryContainer.toArgb(),
        mediaBackground = colors.surfaceMuted.toArgb(),
    )
    FocusCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(FocusTheme.spacing.md)) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                LayoutInflater.from(context).inflate(R.layout.ad_native_session_end, null, false) as NativeAdView
            },
            update = { view -> bindNativeAd(view, ad, cardColors) },
        )
    }
}

private fun bindNativeAd(view: NativeAdView, ad: NativeAd, colors: AdCardColors) {
    val density = view.resources.displayMetrics.density
    val media = view.findViewById<MediaView>(R.id.ad_media)
    val icon = view.findViewById<ImageView>(R.id.ad_icon)
    val badge = view.findViewById<TextView>(R.id.ad_badge)
    val headline = view.findViewById<TextView>(R.id.ad_headline)
    val body = view.findViewById<TextView>(R.id.ad_body)
    val cta = view.findViewById<TextView>(R.id.ad_cta)

    media.background = GradientDrawable().apply {
        setColor(colors.mediaBackground)
        cornerRadius = 12 * density
    }
    media.clipToOutline = true
    media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)

    badge.setTextColor(colors.badgeText)
    badge.background = GradientDrawable().apply {
        setColor(colors.badgeBackground)
        cornerRadius = 4 * density
    }

    headline.setTextColor(colors.headline)
    headline.text = ad.headline.orEmpty()

    val bodyText = ad.body
    body.setTextColor(colors.body)
    body.text = bodyText.orEmpty()
    body.visibility = if (bodyText.isNullOrBlank()) View.GONE else View.VISIBLE

    val ctaText = ad.callToAction
    cta.setTextColor(colors.accent)
    cta.text = ctaText.orEmpty()
    cta.visibility = if (ctaText.isNullOrBlank()) View.GONE else View.VISIBLE
    cta.background = GradientDrawable().apply {
        setColor(android.graphics.Color.TRANSPARENT)
        setStroke((1 * density).toInt().coerceAtLeast(1), colors.accent)
        cornerRadius = 22 * density
    }

    val iconDrawable = ad.icon?.drawable
    icon.setImageDrawable(iconDrawable)
    icon.visibility = if (iconDrawable == null) View.GONE else View.VISIBLE

    view.mediaView = media
    view.headlineView = headline
    view.bodyView = body
    view.callToActionView = cta
    view.iconView = icon
    // Must be the last call: registers the views above for clicks and impressions.
    view.setNativeAd(ad)
}
