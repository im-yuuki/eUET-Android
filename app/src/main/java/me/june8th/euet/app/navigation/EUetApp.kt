package me.june8th.euet.app.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.app.designsystem.motion.LocalNavAnimatedScope
import me.june8th.euet.app.designsystem.motion.LocalSharedTransitionScope
import me.june8th.euet.app.designsystem.motion.NavMotion
import me.june8th.euet.app.designsystem.motion.rememberNavMotion
import me.june8th.euet.app.designsystem.motion.rememberReducedMotion
import me.june8th.euet.app.di.euetViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.june8th.euet.app.feature.auth.SignInScreen
import me.june8th.euet.app.feature.canvas.CanvasScreen
import me.june8th.euet.app.feature.documents.DocumentsScreen
import me.june8th.euet.app.feature.exams.ExamsScreen
import me.june8th.euet.app.feature.grades.GradesScreen
import me.june8th.euet.app.feature.home.HomeScreen
import me.june8th.euet.app.feature.more.MoreScreen
import me.june8th.euet.app.feature.notifications.NotificationsScreen
import me.june8th.euet.app.feature.profile.ProfileScreen
import me.june8th.euet.app.feature.registration.RegistrationScreen
import me.june8th.euet.app.feature.settings.SettingsScreen
import me.june8th.euet.app.feature.timetable.TimetableScreen
import me.june8th.euet.app.feature.training.TrainingScreen
import me.june8th.euet.app.feature.tuition.TuitionScreen

@Composable
fun EUetApp(rootViewModel: RootViewModel = euetViewModel { RootViewModel(it.session) }) {
    val authState by rootViewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        AuthState.Loading -> Box(Modifier.fillMaxSize())
        AuthState.LoggedOut -> SignInScreen(onSignedIn = {})
        AuthState.LoggedIn -> MainScaffold()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val navMotion = rememberNavMotion()
    val reducedMotion = rememberReducedMotion()
    val barSlideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()

    val isTopLevel = TopLevelDestination.entries.any { dest ->
        currentDestination?.hierarchyHasRoute(dest.route) == true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevel,
                enter = if (reducedMotion) EnterTransition.None else slideInVertically(barSlideSpec) { it },
                exit = if (reducedMotion) ExitTransition.None else slideOutVertically(barSlideSpec) { it },
            ) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchyHasRoute(dest.route) == true
                        val label = stringResource(dest.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { NavIcon(dest, selected, label, reducedMotion) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        // Hosts the app's one hero shared element (Home's GPA figures ↔ the Grades GPA card).
        SharedTransitionLayout {
            val sharedScope = this
            NavHost(
                navController = navController,
                startDestination = Route.Home,
                modifier = Modifier,
                // Bottom-bar siblings fade through; detail routes override this below.
                enterTransition = { navMotion.topLevelEnter },
                exitTransition = { navMotion.topLevelExit },
                popEnterTransition = { navMotion.topLevelEnter },
                popExitTransition = { navMotion.topLevelExit },
            ) {
                composable<Route.Home> {
                    HeroScope(sharedScope) {
                        HomeScreen(
                            onOpenTimetable = { navController.navigate(Route.Timetable) },
                            onOpenGrades = { navController.navigate(Route.Grades) },
                            onOpenExams = { navController.navigate(Route.Exams) },
                            onOpenProfile = { navController.navigate(Route.Profile) },
                            contentPadding = padding,
                        )
                    }
                }
                composable<Route.Timetable> { TimetableScreen(contentPadding = padding) }
                composable<Route.Grades> {
                    HeroScope(sharedScope) {
                        GradesScreen(
                            onOpenExams = { navController.navigate(Route.Exams) },
                            contentPadding = padding,
                        )
                    }
                }
                composable<Route.More> {
                    MoreScreen(
                        onNavigate = { navController.navigate(it) },
                        contentPadding = padding,
                    )
                }

                detailRoute<Route.Profile>(navMotion) { ProfileScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Exams>(navMotion) { ExamsScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Notifications>(navMotion) { NotificationsScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Tuition>(navMotion) { TuitionScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Canvas>(navMotion) { CanvasScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Training>(navMotion) { TrainingScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Registration>(navMotion) { RegistrationScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Documents>(navMotion) { DocumentsScreen(onBack = navController::navigateUp) }
                detailRoute<Route.Settings>(navMotion) { SettingsScreen(onBack = navController::navigateUp) }
            }
        }
    }
}

/**
 * Registers a pushed detail route on the horizontal shared axis. The pop transitions mirror the
 * push ones, which is also what the predictive-back gesture seeks as the user drags.
 */
private inline fun <reified T : Any> NavGraphBuilder.detailRoute(
    motion: NavMotion,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
    enterTransition = { motion.detailEnter },
    exitTransition = { motion.detailExit },
    popEnterTransition = { motion.detailPopEnter },
    popExitTransition = { motion.detailPopExit },
    content = content,
)

/** Publishes the scopes the hero shared element needs to the destination underneath. */
@Composable
private fun AnimatedContentScope.HeroScope(
    sharedScope: SharedTransitionScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSharedTransitionScope provides sharedScope,
        LocalNavAnimatedScope provides this,
        content = content,
    )
}

/**
 * Bottom-bar icon with a springy selection response. [NavigationBarItem] animates the indicator
 * and colours already, but not the icon itself, so a small scale bump gives the tap a body.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavIcon(
    dest: TopLevelDestination,
    selected: Boolean,
    label: String,
    reducedMotion: Boolean,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.88f,
        animationSpec = if (reducedMotion) snap() else MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "nav-icon-scale",
    )
    Icon(
        dest.icon,
        contentDescription = label,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

private fun androidx.navigation.NavDestination.hierarchyHasRoute(route: Any): Boolean =
    hierarchy.any { it.hasRoute(route::class) }
