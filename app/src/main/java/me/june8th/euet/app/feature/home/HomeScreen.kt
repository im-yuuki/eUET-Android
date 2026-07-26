package me.june8th.euet.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.app.designsystem.component.EUetCard
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.app.di.euetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTimetable: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenProfile: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = euetViewModel { HomeViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("eUET") }, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EUetCard {
                Text(
                    greeting(state.profileName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Here's your day at a glance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.gpa?.let { GpaInline(it) }
            }

            SectionHeader("Today's classes")
            if (state.todayClasses.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (state.loading) "Loading…" else "No classes today 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            } else {
                state.todayClasses.forEach { TodayClassRow(it) }
            }

            SectionHeader("Quick access")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Timetable", Icons.Rounded.CalendarMonth, Modifier.weight(1f), onOpenTimetable)
                QuickAction("Grades", Icons.Rounded.Grade, Modifier.weight(1f), onOpenGrades)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Exams", Icons.Rounded.EventNote, Modifier.weight(1f), onOpenExams)
                QuickAction("Profile", Icons.Rounded.Person, Modifier.weight(1f), onOpenProfile)
            }
        }
    }
}

@Composable
private fun GpaInline(gpa: GpaSummary) {
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        gpa.cpa?.let {
            Column {
                Text("%.2f".format(it), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("CPA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        gpa.accumulatedCredits?.let {
            Column {
                Text("%.0f".format(it), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Credits", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TodayClassRow(entry: TimetableEntry) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.courseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    entry.sessionStart?.let {
                        Label(Icons.Rounded.Schedule, "Period $it${entry.sessionEnd?.let { e -> "–$e" } ?: ""}")
                    }
                    if (!entry.room.isNullOrBlank()) Label(Icons.Rounded.Place, entry.room)
                }
            }
        }
    }
}

@Composable
private fun Label(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.height(96.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterStart) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null)
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun greeting(name: String?): String {
    val who = name?.substringAfterLast(' ')?.takeIf { it.isNotBlank() }
    return if (who != null) "Hi, $who 👋" else "Welcome back 👋"
}
