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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.core.billing.BillingProducts
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
import com.focusgrowing.app.domain.model.PlanChangeMode
import com.focusgrowing.app.domain.model.PremiumOffer
import java.text.NumberFormat
import java.util.Currency

private val benefits = listOf(
    "Unlimited custom backgrounds",
    "Advanced crop editor, blur & your own motivational text",
    "Advanced statistics: best hours, trends, monthly & yearly",
    "Personalized insights about your focus",
    "More world elements & premium color palettes",
    "No ads",
)

@Composable
fun PremiumScreen(onBack: () -> Unit, viewModel: PremiumViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val context = LocalContext.current

    // Coming back from Google Play (cancel / resubscribe / payment change): re-read the subscription.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

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
                    Text(
                        if (state.isPremium) "Your Premium" else "Go Premium",
                        style = FocusTheme.typography.headlineMedium,
                        color = colors.onBackground,
                    )
                    Text(
                        when {
                            state.hasLifetime -> "Yours forever."
                            state.isPremium -> "Manage your subscription."
                            else -> "Unlock the full potential of your journey."
                        },
                        style = FocusTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
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
                if (state.hasLifetime) {
                    LifetimeSection(state = state, onManage = { openUrl(viewModel.manageSubscriptionUrl()) })
                } else {
                    SubscriptionSection(
                        state = state,
                        onSwitchPlan = { context.findActivity()?.let(viewModel::switchPlan) },
                        onBuyLifetime = { context.findActivity()?.let(viewModel::purchaseLifetime) },
                        onManage = { openUrl(viewModel.manageSubscriptionUrl()) },
                    )
                }
                if (state.isTestMode) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        if (state.active != null) {
                            FocusTextButton(
                                if (state.active?.isAutoRenewing == false) "Test: resubscribe" else "Test: cancel",
                                onClick = viewModel::toggleTestAutoRenew,
                            )
                        }
                        FocusTextButton("Test: reset", onClick = viewModel::resetTestPurchase)
                    }
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
                        FocusButton("Try again", onClick = viewModel::refresh, style = FocusButtonStyle.Soft, modifier = Modifier.fillMaxWidth())
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
        when {
            selected.period == BillingPeriod.LIFETIME -> "Buy lifetime Premium"
            trial != null -> "Start ${trialLabel(trial)} free trial"
            else -> "Upgrade to Premium"
        },
        onClick = onPurchase,
        style = FocusButtonStyle.Premium,
        loading = state.processing,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(spacing.sm))
    // Google Play policy: clearly state price, period, trial and how to cancel.
    Text(
        purchaseTerms(selected),
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
                    if (offer.period == BillingPeriod.LIFETIME) {
                        Spacer(Modifier.width(8.dp))
                        Pill("Pay once", container = colors.primary, content = colors.onPrimary)
                    }
                }
                val sub = if (offer.period == BillingPeriod.LIFETIME) {
                    "One payment. Premium forever, no subscription."
                } else {
                    buildString {
                        offer.freeTrial?.let { append("${trialLabel(it)} free, then ") }
                        append("${offer.formattedPrice} ${periodSuffix(offer.period)}")
                    }
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
                if (offer.period == BillingPeriod.LIFETIME) {
                    Text("one-time", style = FocusTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

/** Shown to subscribers: current plan, renewal status, plan switch and a link to Google Play. */
@Composable
private fun SubscriptionSection(
    state: PremiumUiState,
    onSwitchPlan: () -> Unit,
    onBuyLifetime: () -> Unit,
    onManage: () -> Unit,
) {
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    val active = state.active
    val currentOffer = state.currentOffer

    FocusCard(modifier = Modifier.fillMaxWidth(), color = colors.premiumContainer, border = false) {
        Text(
            if (state.justPurchased) "Welcome to Premium! 🎉" else "Premium is active",
            style = FocusTheme.typography.titleMedium,
            color = colors.onSurface,
        )
        val planLine = buildString {
            append(
                when (state.currentPeriod) {
                    BillingPeriod.MONTHLY -> "Monthly plan"
                    BillingPeriod.YEARLY -> "Yearly plan"
                    BillingPeriod.LIFETIME -> "Lifetime"
                    BillingPeriod.OTHER -> "Premium plan"
                },
            )
            currentOffer?.let { append(" · ${it.formattedPrice} ${periodSuffix(it.period)}") }
        }
        Text(planLine, style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        val scheduled = state.scheduledPlanId
        val status = when {
            scheduled != null ->
                "Switches to the ${periodTitle(BillingProducts.periodOf(scheduled)).lowercase()} plan when the current period ends."
            active == null -> "Checking your subscription with Google Play…"
            !active.isAutoRenewing -> "Cancelled. Premium stays active until the end of the period you paid for."
            else -> "Renews automatically. Cancel anytime in Google Play."
        }
        Text(status, style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }

    // Monthly ↔ yearly
    val target = state.switchTarget
    val mode = state.switchMode
    if (target != null && mode != null) {
        Spacer(Modifier.height(spacing.md))
        FocusCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Switch to ${periodTitle(target.period).lowercase()}",
                    style = FocusTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                val monthly = state.monthlyOffer
                if (target.period == BillingPeriod.YEARLY && monthly != null) {
                    PremiumPricing.yearlySavingsPercent(monthly.priceMicros, target.priceMicros)?.let {
                        Pill("Save $it%", container = colors.primary, content = colors.onPrimary)
                    }
                }
            }
            Text("${target.formattedPrice} ${periodSuffix(target.period)}", style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                when (mode) {
                    PlanChangeMode.IMMEDIATE ->
                        "Starts today. You pay ${target.formattedPrice} now, and the unused part of your current plan is added as extra days."
                    PlanChangeMode.AT_NEXT_RENEWAL ->
                        "Starts when your current period ends. Nothing is charged today, and you keep Premium the whole time."
                },
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.md))
            FocusButton(
                "Switch to ${periodTitle(target.period).lowercase()}",
                onClick = onSwitchPlan,
                style = if (mode == PlanChangeMode.IMMEDIATE) FocusButtonStyle.Premium else FocusButtonStyle.Soft,
                loading = state.processing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Pay once instead of subscribing
    val lifetime = state.lifetimeOffer
    if (lifetime != null) {
        Spacer(Modifier.height(spacing.md))
        FocusCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Go lifetime", style = FocusTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                Pill("Pay once", container = colors.primary, content = colors.onPrimary)
            }
            Text("${lifetime.formattedPrice} one-time", style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            val breakEven = state.yearlyOffer?.let { PremiumPricing.lifetimeBreakEvenYears(it.priceMicros, lifetime.priceMicros) }
            Text(
                buildString {
                    append("Keep Premium forever with a single payment. ")
                    if (breakEven != null) append("Costs about as much as $breakEven ${if (breakEven == 1) "year" else "years"} of the yearly plan. ")
                    append("Your subscription isn't cancelled automatically: cancel it in Google Play afterwards.")
                },
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.md))
            FocusButton(
                "Buy lifetime Premium",
                onClick = onBuyLifetime,
                style = FocusButtonStyle.Premium,
                loading = state.processing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Spacer(Modifier.height(spacing.md))
    FocusButton(
        if (active?.isAutoRenewing == false) "Resubscribe in Google Play" else "Manage in Google Play",
        onClick = onManage,
        style = if (active?.isAutoRenewing == false) FocusButtonStyle.Premium else FocusButtonStyle.Soft,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Cancel, resubscribe or change your payment method in Google Play. Changes show up here when you return.",
        style = FocusTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shown to lifetime owners. Reminds them to cancel a subscription that is still renewing. */
@Composable
private fun LifetimeSection(state: PremiumUiState, onManage: () -> Unit) {
    val colors = FocusTheme.colors
    val spacing = FocusTheme.spacing
    FocusCard(modifier = Modifier.fillMaxWidth(), color = colors.premiumContainer, border = false) {
        Text(
            if (state.justPurchased) "Premium is yours forever. 🎉" else "Lifetime Premium",
            style = FocusTheme.typography.titleMedium,
            color = colors.onSurface,
        )
        Text(
            "Paid once. No renewals, nothing more to pay. Thank you for supporting Focus Growing!",
            style = FocusTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
    val sub = state.active
    if (sub != null) {
        Spacer(Modifier.height(spacing.md))
        FocusCard(
            modifier = Modifier.fillMaxWidth(),
            color = if (sub.isAutoRenewing) colors.dangerContainer else colors.surface,
            border = !sub.isAutoRenewing,
        ) {
            if (sub.isAutoRenewing) {
                Text(
                    "Your ${periodTitle(sub.period).lowercase()} subscription is still active",
                    style = FocusTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Lifetime Premium doesn't cancel it automatically. Cancel it in Google Play so you aren't charged again.",
                    style = FocusTheme.typography.bodyMedium,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(spacing.md))
                FocusButton("Cancel subscription in Google Play", onClick = onManage, style = FocusButtonStyle.Danger, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "Your old ${periodTitle(sub.period).lowercase()} subscription is cancelled and won't renew.",
                    style = FocusTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

private fun periodTitle(period: BillingPeriod) = when (period) {
    BillingPeriod.MONTHLY -> "Monthly"
    BillingPeriod.YEARLY -> "Yearly"
    BillingPeriod.LIFETIME -> "Lifetime"
    BillingPeriod.OTHER -> "Premium"
}

private fun periodSuffix(period: BillingPeriod) = when (period) {
    BillingPeriod.MONTHLY -> "per month"
    BillingPeriod.YEARLY -> "per year"
    BillingPeriod.LIFETIME -> "one-time"
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

private fun purchaseTerms(offer: PremiumOffer): String =
    if (offer.period == BillingPeriod.LIFETIME) {
        "One-time payment of ${offer.formattedPrice}. No subscription and no renewals. " +
            "Your purchase is linked to your Google account and can be restored on a new device."
    } else {
        subscriptionTerms(offer)
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
