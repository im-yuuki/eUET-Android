package me.june8th.euet.feature.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.core.designsystem.component.SectionHeader
import me.june8th.euet.core.designsystem.component.TermSelector
import me.june8th.euet.core.designsystem.component.UiStateContent
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.di.euetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    contentPadding: PaddingValues,
    viewModel: TimetableViewModel = euetViewModel { TimetableViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Timetable") }, scrollBehavior = scrollBehavior) },
    ) { innerPadding ->
        Column(Modifier.padding(top = innerPadding.calculateTopPadding())) {
            if (state.terms.isNotEmpty()) {
                TermSelector(
                    terms = state.terms,
                    selected = state.selectedTerm,
                    onSelect = viewModel::selectTerm,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            UiStateContent(
                state.content,
                onRetry = viewModel::retry,
                emptyTitle = "No classes this term",
            ) { entries ->
                TimetableList(entries, bottomPadding = contentPadding.calculateBottomPadding())
            }
        }
    }
}

@Composable
private fun TimetableList(entries: List<TimetableEntry>, bottomPadding: androidx.compose.ui.unit.Dp) {
    val byDay = entries.groupBy { it.weekday }.toSortedMap()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        byDay.forEach { (day, dayEntries) ->
            item(key = "header-$day") { SectionHeader(weekdayLabel(day)) }
            items(dayEntries.sortedBy { it.sessionStart ?: 0 }, key = { "$day-${it.courseCode}-${it.sessionStart}" }) { entry ->
                ClassCard(entry)
            }
        }
    }
}

@Composable
private fun ClassCard(entry: TimetableEntry) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                entry.courseCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (entry.sessionStart != null) {
                    IconText(Icons.Rounded.Schedule, periodLabel(entry.sessionStart, entry.sessionEnd))
                }
                if (!entry.room.isNullOrBlank()) {
                    IconText(Icons.Rounded.Place, entry.room)
                }
            }
        }
    }
}

@Composable
private fun IconText(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

internal fun weekdayLabel(day: Int): String = when (day) {
    2 -> "Monday"
    3 -> "Tuesday"
    4 -> "Wednesday"
    5 -> "Thursday"
    6 -> "Friday"
    7 -> "Saturday"
    8, 1 -> "Sunday"
    else -> "Day $day"
}

private fun periodLabel(start: Int, end: Int?): String =
    if (end != null && end != start) "Periods $start–$end" else "Period $start"
