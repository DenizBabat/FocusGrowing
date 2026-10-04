package com.focusgrowing.app.presentation.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.focusgrowing.app.core.designsystem.theme.FocusTheme
import com.focusgrowing.app.presentation.background.BackgroundAdjustScreen
import com.focusgrowing.app.presentation.background.BackgroundGalleryScreen
import com.focusgrowing.app.presentation.celebration.CelebrationScreen
import com.focusgrowing.app.presentation.focus.FocusScreen
import com.focusgrowing.app.presentation.home.HomeScreen
import com.focusgrowing.app.presentation.mission.MissionDetailScreen
import com.focusgrowing.app.presentation.mission.MissionEditorScreen
import com.focusgrowing.app.presentation.mission.MissionsScreen
import com.focusgrowing.app.presentation.notifications.NotificationsScreen
import com.focusgrowing.app.presentation.onboarding.OnboardingScreen
import com.focusgrowing.app.presentation.premium.PremiumScreen
import com.focusgrowing.app.presentation.profile.ProfileScreen
import com.focusgrowing.app.presentation.settings.SettingsScreen
import com.focusgrowing.app.presentation.statistics.StatisticsScreen

/** Lets screens know whether in-app haptics are enabled (Settings → Haptic feedback). */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

private class TopLevelTab(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val matches: (NavDestination) -> Boolean,
)

private val tabs = listOf(
    TopLevelTab(HomeRoute, "Home", Icons.Rounded.Home) { it.hasRoute<HomeRoute>() },
    TopLevelTab(MissionsRoute, "Missions", Icons.Rounded.Checklist) { it.hasRoute<MissionsRoute>() },
    TopLevelTab(FocusRoute(), "Focus", Icons.Rounded.Timer) { it.hasRoute<FocusRoute>() },
    TopLevelTab(StatisticsRoute, "Stats", Icons.Rounded.BarChart) { it.hasRoute<StatisticsRoute>() },
    TopLevelTab(ProfileRoute, "Profile", Icons.Rounded.Person) { it.hasRoute<ProfileRoute>() },
)

/** Screens that show the bottom navigation bar. */
private fun NavDestination.showsBottomBar(): Boolean =
    hasRoute<HomeRoute>() || hasRoute<MissionsRoute>() || hasRoute<StatisticsRoute>() ||
        hasRoute<ProfileRoute>() || hasRoute<SettingsRoute>() || hasRoute<BackgroundGalleryRoute>() ||
        hasRoute<PremiumRoute>() || hasRoute<NotificationsRoute>()

@Composable
fun FocusGrowingApp(
    showOnboarding: Boolean,
    openFocusSignal: Int,
    hapticsEnabled: Boolean,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    if (showOnboarding) {
        // Onboarding lives outside the NavHost: finishing it flips the stored flag and the app
        // recomposes straight into Home with a clean back stack.
        OnboardingScreen(onFinished = {})
        return
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val celebration by mainViewModel.celebration.collectAsStateWithLifecycle()

    // Show reward screens (focus completed, level up, ...) whenever the queue has one.
    LaunchedEffect(celebration, destination) {
        val dest = destination ?: return@LaunchedEffect
        if (celebration != null && !dest.hasRoute<CelebrationRoute>()) {
            navController.navigate(CelebrationRoute) { launchSingleTop = true }
        }
    }
    LaunchedEffect(openFocusSignal) {
        if (openFocusSignal > 0) navController.navigate(FocusRoute()) { launchSingleTop = true }
    }

    val showBottomBar = destination?.showsBottomBar() == true

    CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
        Scaffold(
            containerColor = FocusTheme.colors.background,
            bottomBar = {
                if (showBottomBar) FocusBottomBar(navController, destination)
            },
        ) { padding ->
            // Screens without the bottom bar draw edge-to-edge and handle insets themselves.
            val contentModifier = if (showBottomBar) {
                Modifier.padding(bottom = padding.calculateBottomPadding()).consumeWindowInsets(padding)
            } else {
                Modifier
            }
            Box(Modifier.fillMaxSize().then(contentModifier)) {
                FocusNavHost(navController)
            }
        }
    }
}

@Composable
private fun FocusBottomBar(navController: NavHostController, destination: NavDestination?) {
    val colors = FocusTheme.colors
    NavigationBar(containerColor = colors.surface, tonalElevation = FocusTheme.dimens.cardElevation) {
        tabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { tab.matches(it) } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, style = FocusTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun FocusNavHost(navController: NavHostController) {
    val reduceMotion = FocusTheme.reduceMotion
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { if (reduceMotion) EnterTransition.None else fadeIn() },
        exitTransition = { if (reduceMotion) ExitTransition.None else fadeOut() },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onStartFocus = { missionId -> navController.navigate(FocusRoute(missionId ?: NO_ID)) },
                onOpenMission = { navController.navigate(MissionDetailRoute(it)) },
                onViewAllMissions = { navController.navigate(MissionsRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                onCreateMission = { navController.navigate(MissionEditorRoute()) },
            )
        }
        composable<MissionsRoute> {
            MissionsScreen(
                onOpenMission = { navController.navigate(MissionDetailRoute(it)) },
                onCreateMission = { navController.navigate(MissionEditorRoute()) },
            )
        }
        composable<MissionDetailRoute> {
            MissionDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(MissionEditorRoute(id)) },
                onStartFocus = { id -> navController.navigate(FocusRoute(id)) },
            )
        }
        composable<MissionEditorRoute> {
            MissionEditorScreen(onDone = { navController.popBackStack() })
        }
        composable<FocusRoute> {
            FocusScreen(
                onBack = { navController.popBackStack() },
                onOpenBackgrounds = { navController.navigate(BackgroundGalleryRoute) },
                onOpenPremium = { navController.navigate(PremiumRoute) },
            )
        }
        composable<StatisticsRoute> {
            StatisticsScreen(onOpenPremium = { navController.navigate(PremiumRoute) })
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onOpenPremium = { navController.navigate(PremiumRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenBackgrounds = { navController.navigate(BackgroundGalleryRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPremium = { navController.navigate(PremiumRoute) },
                onOpenBackgrounds = { navController.navigate(BackgroundGalleryRoute) },
            )
        }
        composable<BackgroundGalleryRoute> {
            BackgroundGalleryScreen(
                onBack = { navController.popBackStack() },
                onAdjust = { id -> navController.navigate(BackgroundAdjustRoute(id)) },
                onOpenPremium = { navController.navigate(PremiumRoute) },
            )
        }
        composable<BackgroundAdjustRoute> {
            BackgroundAdjustScreen(onBack = { navController.popBackStack() }, onOpenPremium = { navController.navigate(PremiumRoute) })
        }
        composable<PremiumRoute> {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
        composable<NotificationsRoute> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenMission = { navController.navigate(MissionDetailRoute(it)) },
            )
        }
        composable<CelebrationRoute> {
            CelebrationScreen(
                onClose = { navController.popBackStack() },
                onOpenMission = { id ->
                    navController.navigate(MissionDetailRoute(id)) { popUpTo<CelebrationRoute> { inclusive = true } }
                },
                onExploreWorld = {
                    navController.navigate(HomeRoute) { popUpTo<CelebrationRoute> { inclusive = true }; launchSingleTop = true }
                },
            )
        }
    }
}

/** Reads the typed route arguments inside a ViewModel's SavedStateHandle. */
internal inline fun <reified T : Any> androidx.lifecycle.SavedStateHandle.route(): T = toRoute<T>()
