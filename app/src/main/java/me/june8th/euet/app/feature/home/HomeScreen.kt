package me.june8th.euet.app.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.designsystem.component.EUetCard
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.motion.AnimatedValueText
import me.june8th.euet.app.designsystem.motion.HERO_GPA_KEY
import me.june8th.euet.app.designsystem.motion.heroSharedBounds
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TimetableEntry
import me.june8th.euet.app.di.euetViewModel

@Composable
fun HomeScreen(
    onOpenTimetable: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenProfile: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = euetViewModel { HomeViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreenContent(
        state = state,
        onRefresh = viewModel::refresh,
        onOpenTimetable = onOpenTimetable,
        onOpenGrades = onOpenGrades,
        onOpenExams = onOpenExams,
        onOpenProfile = onOpenProfile,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onOpenTimetable: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenProfile: () -> Unit,
    contentPadding: PaddingValues,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        RefreshableBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EUetCard {
                    val who = state.profileName?.substringAfterLast(' ')?.takeIf { it.isNotBlank() }
                    Text(
                        if (who != null) {
                            stringResource(R.string.home_greeting_name, who)
                        } else {
                            stringResource(R.string.home_greeting)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.gpa?.let { GpaInline(it) }
                }

                SectionHeader(stringResource(R.string.home_today_classes))
                if (state.loading && state.todayClasses.isEmpty()) {
                    // The parent column already insets 16.dp on each side.
                    SkeletonRows(rows = 2, rowHeight = 72.dp, horizontalPadding = 0.dp)
                } else if (state.todayClasses.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.home_no_classes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                } else {
                    state.todayClasses.forEach { TodayClassRow(it) }
                }

                SectionHeader(stringResource(R.string.home_quick_access))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        stringResource(R.string.title_timetable),
                        Icons.Rounded.CalendarMonth,
                        Modifier.weight(1f),
                        onOpenTimetable,
                    )
                    QuickAction(
                        stringResource(R.string.nav_grades),
                        Icons.Rounded.Grade,
                        Modifier.weight(1f),
                        onOpenGrades,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        stringResource(R.string.title_exams),
                        Icons.Rounded.EventNote,
                        Modifier.weight(1f),
                        onOpenExams,
                    )
                    QuickAction(
                        stringResource(R.string.title_profile),
                        Icons.Rounded.Person,
                        Modifier.weight(1f),
                        onOpenProfile,
                    )
                }
            }
        }
    }
}

/**
 * The greeting card's GPA figures — the origin half of the app's one hero shared element; the
 * matching half is the Grades screen's GPA card.
 */
@Composable
private fun GpaInline(gpa: GpaSummary) {
    Row(
        Modifier.padding(top = 8.dp).heroSharedBounds(HERO_GPA_KEY),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        gpa.cpa?.let {
            Column {
                AnimatedValueText(
                    value = it,
                    text = "%.2f".format(it),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.stat_cpa),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        gpa.accumulatedCredits?.let {
            Column {
                AnimatedValueText(
                    value = it,
                    text = "%.0f".format(it),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.stat_credits),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    entry.sessionStart?.let { start ->
                        val end = entry.sessionEnd
                        val periodText = if (end != null && end != start) {
                            stringResource(R.string.period_range, start, end)
                        } else {
                            stringResource(R.string.period_single, start)
                        }
                        Label(Icons.Rounded.Schedule, periodText)
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

// --- Previews ---

private val homePreviewState = HomeUiState(
    loading = false,
    profileName = PreviewData.profile.name,
    gpa = PreviewData.gpa,
    todayClasses = PreviewData.todayClasses,
)

@Preview(locale = "vi", showBackground = true)
@Composable
private fun HomePreview() {
    EUetTheme {
        HomeScreenContent(homePreviewState, {}, {}, {}, {}, {}, PaddingValues())
    }
}

@Preview(locale = "en", showBackground = true)
@Composable
private fun HomePreviewEnglish() {
    EUetTheme {
        HomeScreenContent(homePreviewState, {}, {}, {}, {}, {}, PaddingValues())
    }
}

@Preview(locale = "vi", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreviewDark() {
    EUetTheme {
        HomeScreenContent(homePreviewState, {}, {}, {}, {}, {}, PaddingValues())
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun HomePreviewLoading() {
    EUetTheme {
        HomeScreenContent(HomeUiState(loading = true), {}, {}, {}, {}, {}, PaddingValues())
    }
}
