package me.june8th.euet.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import me.june8th.euet.R

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
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: Any,
) {
    HOME(R.string.nav_home, Icons.Rounded.Home, Route.Home),
    TIMETABLE(R.string.nav_timetable, Icons.Rounded.CalendarMonth, Route.Timetable),
    GRADES(R.string.nav_grades, Icons.Rounded.Grade, Route.Grades),
    MORE(R.string.nav_more, Icons.Rounded.Menu, Route.More),
}
