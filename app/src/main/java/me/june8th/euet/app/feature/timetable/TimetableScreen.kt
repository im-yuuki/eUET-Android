package me.june8th.euet.app.feature.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.TermSelector
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.motion.itemMotion
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.app.di.euetViewModel

@Composable
fun TimetableScreen(
    contentPadding: PaddingValues,
    viewModel: TimetableViewModel = euetViewModel { TimetableViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TimetableScreenContent(
        state = state,
        onSelectTerm = viewModel::selectTerm,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableScreenContent(
    state: TimetableUiState,
    onSelectTerm: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.title_timetable)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(top = innerPadding.calculateTopPadding())) {
            if (state.terms.isNotEmpty()) {
                TermSelector(
                    terms = state.terms,
                    selected = state.selectedTerm,
                    onSelect = onSelectTerm,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            RefreshableBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                UiStateContent(
                    state.content,
                    onRetry = onRetry,
                    emptyTitle = stringResource(R.string.timetable_empty),
                    loading = { SkeletonRows() },
                ) { entries ->
                    TimetableList(entries, bottomPadding = contentPadding.calculateBottomPadding())
                }
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
            item(key = "header-$day") { SectionHeader(weekdayLabel(day), modifier = itemMotion()) }
            items(dayEntries.sortedBy { it.sessionStart ?: 0 }, key = { "$day-${it.courseCode}-${it.sessionStart}" }) { entry ->
                // A term change regroups the whole list; the rows slide to their new day instead
                // of the list blinking.
                ClassCard(entry, modifier = itemMotion())
            }
        }
    }
}

@Composable
private fun ClassCard(entry: TimetableEntry, modifier: Modifier = Modifier) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
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

@Composable
internal fun weekdayLabel(day: Int): String = when (day) {
    2 -> stringResource(R.string.weekday_monday)
    3 -> stringResource(R.string.weekday_tuesday)
    4 -> stringResource(R.string.weekday_wednesday)
    5 -> stringResource(R.string.weekday_thursday)
    6 -> stringResource(R.string.weekday_friday)
    7 -> stringResource(R.string.weekday_saturday)
    8, 1 -> stringResource(R.string.weekday_sunday)
    else -> stringResource(R.string.weekday_unknown, day)
}

@Composable
private fun periodLabel(start: Int, end: Int?): String =
    if (end != null && end != start) {
        stringResource(R.string.period_range, start, end)
    } else {
        stringResource(R.string.period_single, start)
    }

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun TimetablePreview() {
    EUetTheme {
        TimetableScreenContent(
            state = TimetableUiState(
                terms = PreviewData.terms,
                selectedTerm = PreviewData.activeTermCode,
                content = UiState.Data(PreviewData.timetable),
            ),
            onSelectTerm = {},
            onRefresh = {},
            onRetry = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun TimetablePreviewEmpty() {
    EUetTheme {
        TimetableScreenContent(
            state = TimetableUiState(
                terms = PreviewData.terms,
                selectedTerm = PreviewData.activeTermCode,
                content = UiState.Empty,
            ),
            onSelectTerm = {},
            onRefresh = {},
            onRetry = {},
            contentPadding = PaddingValues(),
        )
    }
}
