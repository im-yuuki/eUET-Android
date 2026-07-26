package me.june8th.euet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
object Route {
    @Serializable data object Login

    @Serializable data object Home
    @Serializable data object Timetable
    @Serializable data object Grades
    @Serializable data object More

    @Serializable data object Profile
    @Serializable data object Exams
    @Serializable data object Notifications
    @Serializable data object Tuition
    @Serializable data object Canvas
    @Serializable data object Training
    @Serializable data object Registration
    @Serializable data object Documents
    @Serializable data object Settings
}

/** The four bottom-bar destinations. */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any,
) {
    HOME("Home", Icons.Rounded.Home, Route.Home),
    TIMETABLE("Timetable", Icons.Rounded.CalendarMonth, Route.Timetable),
    GRADES("Grades", Icons.Rounded.Grade, Route.Grades),
    MORE("More", Icons.Rounded.Menu, Route.More),
}
