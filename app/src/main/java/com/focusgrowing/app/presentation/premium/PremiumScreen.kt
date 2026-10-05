package com.focusgrowing.app.presentation.premium

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.annotation.PluralsRes
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focusgrowing.app.R
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
    R.string.premium_benefit_backgrounds,
    R.string.premium_benefit_editor,
    R.string.premium_benefit_statistics,
    R.string.premium_benefit_insights,
    R.string.premium_benefit_world,
    R.string.premium_benefit_no_ads,
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
                Pill(stringResource(R.string.premium_test_mode), container = colors.infoContainer, content = colors.info, modifier = Modifier.padding(end = spacing.md))
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
                        stringResource(if (state.isPremium) R.string.premium_title_member else R.string.premium_title_upsell),
                        style = FocusTheme.typography.headlineMedium,
                        color = colors.onBackground,
                    )
                    Text(
                        stringResource(
                            when {
                                state.hasLifetime -> R.string.premium_subtitle_lifetime
                                state.isPremium -> R.string.premium_subtitle_subscribed
                                else -> R.string.premium_subtitle_upsell
                            },
                        ),
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
                    Text(stringResource(benefit), style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
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
                                stringResource(if (state.active?.isAutoRenewing == false) R.string.premium_test_resubscribe else R.string.premium_test_cancel),
                                onClick = viewModel::toggleTestAutoRenew,
                            )
                        }
                        FocusTextButton(stringResource(R.string.premium_test_reset), onClick = viewModel::resetTestPurchase)
                    }
                }
            } else {
                when (val billing = state.billing) {
                    BillingState.Loading -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                    is BillingState.Unavailable -> FocusCard(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.premium_unavailable_title), style = FocusTheme.typography.titleSmall, color = colors.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(billing.reason, style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Spacer(Modifier.height(spacing.md))
                        FocusButton(stringResource(R.string.premium_try_again), onClick = viewModel::refresh, style = FocusButtonStyle.Soft, modifier = Modifier.fillMaxWidth())
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
                stringResource(R.string.premium_free_note),
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FocusTextButton(stringResource(R.string.premium_privacy_policy), onClick = { openUrl(AppConfig.PRIVACY_POLICY_URL) })
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
        Text(stringResource(R.string.premium_no_plans), style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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
            selected.period == BillingPeriod.LIFETIME -> stringResource(R.string.premium_buy_lifetime)
            trial != null -> {
                val (plural, count) = trialPlural(
                    trial,
                    years = R.plurals.premium_start_trial_years,
                    months = R.plurals.premium_start_trial_months,
                    days = R.plurals.premium_start_trial_days,
                )
                pluralStringResource(plural, count, count)
            }
            else -> stringResource(R.string.premium_upgrade)
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
    FocusTextButton(stringResource(R.string.premium_restore_purchase), onClick = onRestore, modifier = Modifier.align(Alignment.CenterHorizontally))
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
                        Pill(stringResource(R.string.premium_save_percent, savingsPercent), container = colors.primary, content = colors.onPrimary)
                    }
                    if (offer.period == BillingPeriod.LIFETIME) {
                        Spacer(Modifier.width(8.dp))
                        Pill(stringResource(R.string.premium_pay_once), container = colors.primary, content = colors.onPrimary)
                    }
                }
                val planTrial = offer.freeTrial
                val sub = when {
                    offer.period == BillingPeriod.LIFETIME -> stringResource(R.string.premium_plan_lifetime_desc)
                    planTrial != null -> {
                        val (plural, count) = trialPlural(
                            planTrial,
                            years = R.plurals.premium_plan_trial_then_price_years,
                            months = R.plurals.premium_plan_trial_then_price_months,
                            days = R.plurals.premium_plan_trial_then_price_days,
                        )
                        pluralStringResource(plural, count, count, priceWithPeriod(offer))
                    }
                    else -> priceWithPeriod(offer)
                }
                Text(sub, style = FocusTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(offer.formattedPrice, style = FocusTheme.typography.titleMedium, color = colors.onSurface)
                if (offer.period == BillingPeriod.YEARLY) {
                    formatMicros(PremiumPricing.monthlyEquivalentMicros(offer.priceMicros), offer.currencyCode)?.let {
                        Text(stringResource(R.string.premium_approx_per_month, it), style = FocusTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
                if (offer.period == BillingPeriod.LIFETIME) {
                    Text(stringResource(R.string.premium_one_time), style = FocusTheme.typography.labelSmall, color = colors.onSurfaceVariant)
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
            stringResource(if (state.justPurchased) R.string.premium_welcome else R.string.premium_active),
            style = FocusTheme.typography.titleMedium,
            color = colors.onSurface,
        )
        val planName = stringResource(
            when (state.currentPeriod) {
                BillingPeriod.MONTHLY -> R.string.premium_plan_monthly
                BillingPeriod.YEARLY -> R.string.premium_plan_yearly
                BillingPeriod.LIFETIME -> R.string.premium_period_lifetime
                BillingPeriod.OTHER -> R.string.premium_plan_other
            },
        )
        val planLine = if (currentOffer != null) {
            stringResource(R.string.premium_plan_with_price, planName, priceWithPeriod(currentOffer))
        } else {
            planName
        }
        Text(planLine, style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        val scheduled = state.scheduledPlanId
        val status = stringResource(
            when {
                scheduled != null -> when (BillingProducts.periodOf(scheduled)) {
                    BillingPeriod.MONTHLY -> R.string.premium_status_switches_monthly
                    BillingPeriod.YEARLY -> R.string.premium_status_switches_yearly
                    BillingPeriod.LIFETIME, BillingPeriod.OTHER -> R.string.premium_status_switches_other
                }
                active == null -> R.string.premium_status_checking
                !active.isAutoRenewing -> R.string.premium_status_cancelled
                else -> R.string.premium_status_renews
            },
        )
        Text(status, style = FocusTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }

    // Monthly ↔ yearly
    val target = state.switchTarget
    val mode = state.switchMode
    if (target != null && mode != null) {
        val switchLabel = stringResource(
            when (target.period) {
                BillingPeriod.MONTHLY -> R.string.premium_switch_to_monthly
                BillingPeriod.YEARLY -> R.string.premium_switch_to_yearly
                BillingPeriod.LIFETIME, BillingPeriod.OTHER -> R.string.premium_switch_to_other
            },
        )
        Spacer(Modifier.height(spacing.md))
        FocusCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    switchLabel,
                    style = FocusTheme.typography.titleMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                val monthly = state.monthlyOffer
                if (target.period == BillingPeriod.YEARLY && monthly != null) {
                    PremiumPricing.yearlySavingsPercent(monthly.priceMicros, target.priceMicros)?.let {
                        Pill(stringResource(R.string.premium_save_percent, it), container = colors.primary, content = colors.onPrimary)
                    }
                }
            }
            Text(priceWithPeriod(target), style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                when (mode) {
                    PlanChangeMode.IMMEDIATE ->
                        stringResource(R.string.premium_switch_immediate_desc, target.formattedPrice)
                    PlanChangeMode.AT_NEXT_RENEWAL ->
                        stringResource(R.string.premium_switch_deferred_desc)
                },
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.md))
            FocusButton(
                switchLabel,
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
                Text(stringResource(R.string.premium_go_lifetime), style = FocusTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                Pill(stringResource(R.string.premium_pay_once), container = colors.primary, content = colors.onPrimary)
            }
            Text(stringResource(R.string.premium_price_one_time, lifetime.formattedPrice), style = FocusTheme.typography.bodyLarge, color = colors.onSurface)
            Spacer(Modifier.height(4.dp))
            val breakEven = state.yearlyOffer?.let { PremiumPricing.lifetimeBreakEvenYears(it.priceMicros, lifetime.priceMicros) }
            Text(
                if (breakEven != null) {
                    pluralStringResource(R.plurals.premium_go_lifetime_desc_break_even, breakEven, breakEven)
                } else {
                    stringResource(R.string.premium_go_lifetime_desc)
                },
                style = FocusTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(spacing.md))
            FocusButton(
                stringResource(R.string.premium_buy_lifetime),
                onClick = onBuyLifetime,
                style = FocusButtonStyle.Premium,
                loading = state.processing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Spacer(Modifier.height(spacing.md))
    FocusButton(
        stringResource(if (active?.isAutoRenewing == false) R.string.premium_resubscribe_google_play else R.string.premium_manage_google_play),
        onClick = onManage,
        style = if (active?.isAutoRenewing == false) FocusButtonStyle.Premium else FocusButtonStyle.Soft,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.premium_manage_hint),
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
            stringResource(if (state.justPurchased) R.string.premium_forever_celebration else R.string.premium_lifetime_title),
            style = FocusTheme.typography.titleMedium,
            color = colors.onSurface,
        )
        Text(
            stringResource(R.string.premium_lifetime_thanks),
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
                    stringResource(
                        when (sub.period) {
                            BillingPeriod.MONTHLY -> R.string.premium_lifetime_sub_active_monthly
                            BillingPeriod.YEARLY -> R.string.premium_lifetime_sub_active_yearly
                            BillingPeriod.LIFETIME, BillingPeriod.OTHER -> R.string.premium_lifetime_sub_active_other
                        },
                    ),
                    style = FocusTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.premium_lifetime_sub_cancel_hint),
                    style = FocusTheme.typography.bodyMedium,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(spacing.md))
                FocusButton(stringResource(R.string.premium_cancel_subscription_google_play), onClick = onManage, style = FocusButtonStyle.Danger, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    stringResource(
                        when (sub.period) {
                            BillingPeriod.MONTHLY -> R.string.premium_lifetime_sub_cancelled_monthly
                            BillingPeriod.YEARLY -> R.string.premium_lifetime_sub_cancelled_yearly
                            BillingPeriod.LIFETIME, BillingPeriod.OTHER -> R.string.premium_lifetime_sub_cancelled_other
                        },
                    ),
                    style = FocusTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun periodTitle(period: BillingPeriod): String = stringResource(
    when (period) {
        BillingPeriod.MONTHLY -> R.string.premium_period_monthly
        BillingPeriod.YEARLY -> R.string.premium_period_yearly
        BillingPeriod.LIFETIME -> R.string.premium_period_lifetime
        BillingPeriod.OTHER -> R.string.common_premium
    },
)

/** The offer's price with its billing period: "₺49,99 per month". */
@Composable
private fun priceWithPeriod(offer: PremiumOffer): String = stringResource(
    when (offer.period) {
        BillingPeriod.MONTHLY -> R.string.premium_price_per_month
        BillingPeriod.YEARLY -> R.string.premium_price_per_year
        BillingPeriod.LIFETIME -> R.string.premium_price_one_time
        BillingPeriod.OTHER -> R.string.premium_price_per_period
    },
    offer.formattedPrice,
)

/**
 * Picks the plural resource for a trial length and the number to show with it:
 * years, months, or days (weeks are counted as days).
 */
private fun trialPlural(
    trial: IsoPeriod,
    @PluralsRes years: Int,
    @PluralsRes months: Int,
    @PluralsRes days: Int,
): Pair<Int, Int> = when {
    trial.years > 0 -> years to trial.years
    trial.months > 0 -> months to trial.months
    trial.weeks > 0 -> days to (trial.weeks * 7 + trial.days)
    else -> days to trial.days
}

@Composable
private fun purchaseTerms(offer: PremiumOffer): String =
    if (offer.period == BillingPeriod.LIFETIME) {
        stringResource(R.string.premium_terms_lifetime, offer.formattedPrice)
    } else {
        subscriptionTerms(offer)
    }

@Composable
private fun subscriptionTerms(offer: PremiumOffer): String {
    val trial = offer.freeTrial
    return if (trial == null) {
        stringResource(R.string.premium_terms_subscription, priceWithPeriod(offer))
    } else {
        val (plural, count) = trialPlural(
            trial,
            years = R.plurals.premium_terms_trial_years,
            months = R.plurals.premium_terms_trial_months,
            days = R.plurals.premium_terms_trial_days,
        )
        pluralStringResource(plural, count, count, priceWithPeriod(offer))
    }
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
