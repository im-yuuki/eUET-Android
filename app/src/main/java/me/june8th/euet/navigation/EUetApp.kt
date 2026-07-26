package me.june8th.euet.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.di.euetViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.june8th.euet.feature.auth.LoginScreen
import me.june8th.euet.feature.canvas.CanvasScreen
import me.june8th.euet.feature.documents.DocumentsScreen
import me.june8th.euet.feature.exams.ExamsScreen
import me.june8th.euet.feature.grades.GradesScreen
import me.june8th.euet.feature.home.HomeScreen
import me.june8th.euet.feature.more.MoreScreen
import me.june8th.euet.feature.notifications.NotificationsScreen
import me.june8th.euet.feature.profile.ProfileScreen
import me.june8th.euet.feature.registration.RegistrationScreen
import me.june8th.euet.feature.settings.SettingsScreen
import me.june8th.euet.feature.timetable.TimetableScreen
import me.june8th.euet.feature.training.TrainingScreen
import me.june8th.euet.feature.tuition.TuitionScreen

@Composable
fun EUetApp(rootViewModel: RootViewModel = euetViewModel { RootViewModel(it.session) }) {
    val authState by rootViewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        AuthState.Loading -> Box(Modifier.fillMaxSize())
        AuthState.LoggedOut -> LoginScreen(onLoggedIn = {})
        AuthState.LoggedIn -> MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isTopLevel = TopLevelDestination.entries.any { dest ->
        currentDestination?.hierarchyHasRoute(dest.route) == true
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevel,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchyHasRoute(dest.route) == true
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
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier,
        ) {
            composable<Route.Home> {
                HomeScreen(
                    onOpenTimetable = { navController.navigate(Route.Timetable) },
                    onOpenGrades = { navController.navigate(Route.Grades) },
                    onOpenExams = { navController.navigate(Route.Exams) },
                    onOpenProfile = { navController.navigate(Route.Profile) },
                    contentPadding = padding,
                )
            }
            composable<Route.Timetable> { TimetableScreen(contentPadding = padding) }
            composable<Route.Grades> {
                GradesScreen(
                    onOpenExams = { navController.navigate(Route.Exams) },
                    contentPadding = padding,
                )
            }
            composable<Route.More> {
                MoreScreen(
                    onNavigate = { navController.navigate(it) },
                    contentPadding = padding,
                )
            }

            composable<Route.Profile> { ProfileScreen(onBack = navController::navigateUp) }
            composable<Route.Exams> { ExamsScreen(onBack = navController::navigateUp) }
            composable<Route.Notifications> { NotificationsScreen(onBack = navController::navigateUp) }
            composable<Route.Tuition> { TuitionScreen(onBack = navController::navigateUp) }
            composable<Route.Canvas> { CanvasScreen(onBack = navController::navigateUp) }
            composable<Route.Training> { TrainingScreen(onBack = navController::navigateUp) }
            composable<Route.Registration> { RegistrationScreen(onBack = navController::navigateUp) }
            composable<Route.Documents> { DocumentsScreen(onBack = navController::navigateUp) }
            composable<Route.Settings> { SettingsScreen(onBack = navController::navigateUp) }
        }
    }
}

private fun androidx.navigation.NavDestination.hierarchyHasRoute(route: Any): Boolean =
    hierarchy.any { it.hasRoute(route::class) }
