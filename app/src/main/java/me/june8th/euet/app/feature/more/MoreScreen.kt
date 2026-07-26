package me.june8th.euet.app.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import me.june8th.euet.app.navigation.Route

private data class MoreItem(val label: String, val subtitle: String, val icon: ImageVector, val route: Any)

private val moreItems = listOf(
    MoreItem("Profile", "Your student information", Icons.Rounded.AccountCircle, Route.Profile),
    MoreItem("Exams", "Schedule, rooms & seats", Icons.Rounded.EventNote, Route.Exams),
    MoreItem("Notifications", "Announcements & news", Icons.Rounded.Notifications, Route.Notifications),
    MoreItem("Tuition", "Bills & payment status", Icons.Rounded.CreditCard, Route.Tuition),
    MoreItem("Canvas", "Courses & assignments", Icons.Rounded.School, Route.Canvas),
    MoreItem("Training points", "Conduct & activity score", Icons.Rounded.WorkspacePremium, Route.Training),
    MoreItem("Registration", "Course registration", Icons.Rounded.EditCalendar, Route.Registration),
    MoreItem("Documents", "Syllabus & forms", Icons.Rounded.Description, Route.Documents),
    MoreItem("Settings", "App preferences", Icons.Rounded.Settings, Route.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onNavigate: (Any) -> Unit, contentPadding: PaddingValues) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("More") }, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(moreItems) { item ->
                MoreRow(item, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Column(Modifier.padding(start = 16.dp).width(0.dp).weight(1f)) {
                Text(item.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
