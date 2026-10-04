package com.focusgrowing.app.presentation.premium

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.billing.BillingConfig
import com.focusgrowing.app.core.designsystem.component.FocusButton
import com.focusgrowing.app.core.designsystem.component.FocusButtonStyle
import com.focusgrowing.app.core.designsystem.component.FocusCard
import com.focusgrowing.app.core.designsystem.component.FocusTextButton
import com.focusgrowing.app.core.designsystem.component.FocusTopBar
import com.focusgrowing.app.core.designsystem.component.Pill
import com.focusgrowing.app.core.designsystem.illustration.LeafFrame
import com.focusgrowing.app.core.designsystem.illustration.MedalBadge
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.core.utility.AppConfig
import com.focusgrowing.app.domain.logic.PremiumPricing
import com.focusgrowing.app.domain.model.BillingPeriod
import com.focusgrowing.app.domain.model.BillingState
import com.focusgrowing.app.domain.model.IsoPeriod
import com.focusgrowing.app.domain.model.PremiumOffer
import java.text.NumberFormat
import java.util.Currency

private val benefits = listOf(
    "Unlimited custom backgrounds",
    "Advanced crop editor, blur & your own motivational text",
    "Advanced statistics: best hours, trends, monthly & yearly",
    "Personalized insights about your focus",
    "More world elements & premium color palettes",
)

@Composable
fun PremiumScreen(onBack: () -> Unit, viewModel: PremiumViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // No browser / Play Store — nothing we can do.
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        FocusTopBar(onBack = onBack) {
            if (state.isTestMode) {
                Pill("Test mode", container = colors.infoContainer, content = colors.info, modifier = Modifier.padding(end = spacing.md))
            }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screen),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Go Premium", style = FocusTheme.typography.headlineMedium, color = colors.onBackground)
                    Text("Unlock the full potential of your journey.", style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Box(contentAlignment = Alignment.Center) {
                    LeafFrame(Modifier.size(130.dp, 110.dp))
                    MedalBadge(Icons.Rounded.WorkspacePremium, colors.premium, size = 96.dp)
                }
            }
            Spacer(Modifier.height(spacing.lg))
            benefits.forEach { benefit ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary)
                    Spacer(Modifier.width(spacing.md))
                    Text(benefit, style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
                }
            }
            Spacer(Modifier.height(spacing.xl))

            if (state.isPremium) {
                ActivePremiumCard(
                    planId = state.activePlanId,
                    justPurchased = state.justPurchased,
                    onManage = { openUrl(viewModel.manageSubscriptionUrl()) },
                )
                if (state.isTestMode) {
                    FocusTextButton("Reset test purchase", onClick = viewModel::resetTestPurchase, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            } else {
                when (val billing = state.billing) {
                    BillingState.Loading -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                    is BillingState.Unavailable -> FocusCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Purchases unavailable", style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(billing.reason, style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(spacing.md))
                        FocusButton("Try again", onClick = viewModel::retry, style = FocusButtonStyle.Soft, modifier = Modifier.fillMaxWidth())
                    }
                    BillingState.Ready -> PurchaseSection(
                        state = state,
                        onSelect = viewModel::selectOffer,
                        onPurchase = { context.findActivity()?.let(viewModel::purchase) },
                        onRestore = viewModel::restore,
                    )
                }
            }

            state.message?.let {
                Spacer(Modifier.height(spacing.sm))
                Text(it, style = FocusTheme.typography.bodyMedium, color = colors.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(spacing.lg))
            Text(
                "Everything you need to focus stays free: timer, missions, your world, basic statistics and your own photos.",
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FocusTextButton("Privacy Policy", onClick = { openUrl(AppConfig.PRIVACY_POLICY_URL) })
            }
            Spacer(Modifier.height(spacing.xl))
        }
    }
}

@Composable
private fun ColumnScope.PurchaseSection(
    state: PremiumUiState,
    onSelect: (PremiumOffer) -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val selected = state.selectedOffer
    if (selected == null) {
        Text("No plans available right now.", style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        return
    }
    val monthly = state.monthlyOffer

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        state.offers.forEach { offer ->
            val savings = if (offer.period == BillingPeriod.YEARLY && monthly != null) {
                PremiumPricing.yearlySavingsPercent(monthly.priceMicros, offer.priceMicros)
            } else null
            PlanCard(offer = offer, selected = offer.id == selected.id, savingsPercent = savings, onClick = { onSelect(offer) })
        }
    }

    Spacer(Modifier.height(spacing.lg))
    val trial = selected.freeTrial
    FocusButton(
        if (trial != null) "Start ${trialLabel(trial)} free trial" else "Upgrade to Premium",
        onClick = onPurchase,
        style = FocusButtonStyle.Premium,
        loading = state.processing,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(spacing.sm))
    // Google Play subscription policy: clearly state price, period, trial and how to cancel.
    Text(
        subscriptionTerms(selected),
        style = FocusTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    FocusTextButton("Restore purchase", onClick = onRestore, modifier = Modifier.align(Alignment.CenterHorizontally))
}

@Composable
private fun PlanCard(offer: PremiumOffer, selected: Boolean, savingsPercent: Int?, onClick: () -> Unit) {
    val colors = FocusTheme.colors
    val shape = FocusTheme.shapes.large
    FocusCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .border(if (selected) 2.dp else 0.dp, if (selected) colors.premium else colors.cardBorder, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected },
        color = if (selected) colors.premiumContainer else colors.surface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(periodTitle(offer.period), style = FocusTheme.typography.titleMedium, color = colors.onSurface)
                    if (savingsPercent != null) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Save $savingsPercent%", container = colors.primary, content = colors.onPrimary)
                    }
                }
                val sub = buildString {
                    offer.freeTrial?.let { append("${trialLabel(it)} free, then ") }
                    append("${offer.formattedPrice} ${periodSuffix(offer.period)}")
                }
                Text(sub, style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(offer.formattedPrice, style = FocusTheme.typography.titleMedium, color = colors.onSurface)
                if (offer.period == BillingPeriod.YEARLY) {
                    formatMicros(PremiumPricing.monthlyEquivalentMicros(offer.priceMicros), offer.currencyCode)?.let {
                        Text("≈ $it / month", style = FocusTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePremiumCard(planId: String?, justPurchased: Boolean, onManage: () -> Unit) {
    val colors = FocusTheme.colors
    FocusCard(modifier = Modifier.fillMaxWidth(), color = colors.premiumContainer, border = false) {
        Text(
            if (justPurchased) "Welcome to Premium! 🎉" else "Premium is active",
            style = FocusTheme.typography.titleMedium,
            color = colors.onSurface,
        )
        val plan = when (planId) {
            BillingConfig.BASE_PLAN_YEARLY -> "Yearly plan · "
            BillingConfig.BASE_PLAN_MONTHLY -> "Monthly plan · "
            else -> ""
        }
        Text("${plan}Thank you for supporting Focus Growing!", style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(FocusTheme.spacing.md))
        FocusButton("Manage subscription", onClick = onManage, style = FocusButtonStyle.Soft, modifier = Modifier.fillMaxWidth())
    }
}

private fun periodTitle(period: BillingPeriod) = when (period) {
    BillingPeriod.MONTHLY -> "Monthly"
    BillingPeriod.YEARLY -> "Yearly"
    BillingPeriod.OTHER -> "Premium"
}

private fun periodSuffix(period: BillingPeriod) = when (period) {
    BillingPeriod.MONTHLY -> "per month"
    BillingPeriod.YEARLY -> "per year"
    BillingPeriod.OTHER -> "per period"
}

private fun trialLabel(trial: IsoPeriod): String = when {
    trial.years > 0 -> "${trial.years}-year"
    trial.months > 0 -> "${trial.months}-month"
    trial.weeks > 0 -> "${trial.weeks * 7 + trial.days}-day"
    else -> "${trial.days}-day"
}

/** "7 days", "1 month", "2 weeks" */
private fun trialDuration(trial: IsoPeriod): String {
    fun unit(n: Int, one: String) = if (n == 1) "1 $one" else "$n ${one}s"
    return when {
        trial.years > 0 -> unit(trial.years, "year")
        trial.months > 0 -> unit(trial.months, "month")
        trial.weeks > 0 -> unit(trial.weeks * 7 + trial.days, "day")
        else -> unit(trial.days, "day")
    }
}

private fun subscriptionTerms(offer: PremiumOffer): String = buildString {
    offer.freeTrial?.let { append("Free for ${trialDuration(it)}, then ") }
    append("${offer.formattedPrice} ${periodSuffix(offer.period)}. ")
    append("Renews automatically until you cancel. ")
    if (offer.freeTrial != null) append("Cancel before the trial ends and you won't be charged. ")
    append("Cancel anytime in Google Play → Payments & subscriptions.")
}

private fun formatMicros(micros: Long, currencyCode: String): String? = try {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(currencyCode)
    }.format(micros / 1_000_000.0)
} catch (_: IllegalArgumentException) {
    null
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
