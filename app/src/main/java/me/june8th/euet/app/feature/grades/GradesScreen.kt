package me.june8th.euet.app.feature.grades

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
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import me.june8th.euet.app.designsystem.component.AnimatedConflictBanner
import me.june8th.euet.app.designsystem.component.ConflictBadge
import me.june8th.euet.app.designsystem.component.ConflictDiffSheet
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.motion.AnimatedValueText
import me.june8th.euet.app.designsystem.motion.HERO_GPA_KEY
import me.june8th.euet.app.designsystem.motion.heroSharedBounds
import me.june8th.euet.app.designsystem.motion.itemMotion
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.data.ConflictDetector
import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.app.di.euetViewModel

@Composable
fun GradesScreen(
    onOpenExams: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: GradesViewModel = euetViewModel { GradesViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GradesScreenContent(
        state = state,
        onOpenExams = onOpenExams,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::load,
        contentPadding = contentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradesScreenContent(
    state: GradesUiState,
    onOpenExams: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showDiff by remember { mutableStateOf(false) }
    var diffFocus by remember { mutableStateOf<String?>(null) }
    // Rows with a field-level disagreement get a badge; onlyIn records have no row to badge.
    val conflictedKeys = remember(state.conflicts) {
        state.conflicts?.conflicts
            ?.filter { it.fields.isNotEmpty() }
            ?.map { it.recordKey }
            ?.toSet()
            .orEmpty()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.title_grades)) },
                actions = {
                    IconButton(onClick = onOpenExams) {
                        Icon(
                            Icons.Rounded.EventNote,
                            contentDescription = stringResource(R.string.title_exams),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        RefreshableBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        ) {
            UiStateContent(
                state.content,
                onRetry = onRetry,
                emptyTitle = stringResource(R.string.grades_empty),
                loading = { SkeletonRows() },
            ) { terms ->
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.conflicts?.takeIf { it.conflicts.isNotEmpty() }?.let { report ->
                        item(key = "conflicts") {
                            AnimatedConflictBanner(
                                count = report.conflicts.size,
                                onClick = {
                                    diffFocus = null
                                    showDiff = true
                                },
                            )
                        }
                    }
                    state.gpa?.let { item(key = "gpa") { GpaCard(it, modifier = itemMotion()) } }
                    terms.forEach { term ->
                        item(key = "h-${term.termCode}") { SectionHeader(term.termCode, modifier = itemMotion()) }
                        items(term.courses, key = { "${term.termCode}-${it.code}" }) { grade ->
                            val recordKey = ConflictDetector.gradeRecordKey(term.termCode, grade.code)
                            GradeRow(
                                grade = grade,
                                conflicted = recordKey in conflictedKeys,
                                onConflictClick = {
                                    diffFocus = recordKey
                                    showDiff = true
                                },
                                modifier = itemMotion(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDiff) {
        state.conflicts?.let { report ->
            ConflictDiffSheet(
                report = report,
                onDismiss = { showDiff = false },
                focusedRecordKey = diffFocus,
            )
        }
    }
}

/**
 * The transcript summary — the destination half of the app's one hero shared element; the
 * matching half is the GPA line on Home's greeting card.
 */
@Composable
private fun GpaCard(gpa: GpaSummary, modifier: Modifier = Modifier) {
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth().heroSharedBounds(HERO_GPA_KEY),
    ) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat(stringResource(R.string.stat_cpa), gpa.cpa, gpa.cpa?.let { "%.2f".format(it) } ?: "–")
            Stat(
                stringResource(R.string.stat_credits),
                gpa.accumulatedCredits,
                gpa.accumulatedCredits?.let { "%.0f".format(it) } ?: "–",
            )
            Stat(
                stringResource(R.string.grades_stat_total),
                gpa.totalCredits,
                gpa.totalCredits?.let { "%.0f".format(it) } ?: "–",
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Double?, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedValueText(
            value = value,
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GradeRow(
    grade: CourseGrade,
    conflicted: Boolean = false,
    onConflictClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(grade.name ?: grade.code, style = MaterialTheme.typography.titleSmall)
                Text(
                    grade.credits
                        ?.let { stringResource(R.string.course_code_credits, grade.code, "%.0f".format(it)) }
                        ?: grade.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (conflicted) {
                ConflictBadge(onClick = onConflictClick, modifier = Modifier.padding(end = 8.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    grade.letter ?: grade.point4?.let { "%.1f".format(it) } ?: "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                grade.point10?.let {
                    Text("%.1f".format(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun GradesPreview() {
    EUetTheme {
        GradesScreenContent(
            state = GradesUiState(gpa = PreviewData.gpa, content = UiState.Data(PreviewData.transcript)),
            onOpenExams = {},
            onRefresh = {},
            onRetry = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun GradesPreviewConflicts() {
    EUetTheme {
        GradesScreenContent(
            state = GradesUiState(
                gpa = PreviewData.gpa,
                content = UiState.Data(PreviewData.transcript),
                conflicts = PreviewData.conflictReport,
            ),
            onOpenExams = {},
            onRefresh = {},
            onRetry = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun GradesPreviewError() {
    EUetTheme {
        GradesScreenContent(
            state = GradesUiState(content = UiState.Error("", ErrorKind.NETWORK)),
            onOpenExams = {},
            onRefresh = {},
            onRetry = {},
            contentPadding = PaddingValues(),
        )
    }
}
