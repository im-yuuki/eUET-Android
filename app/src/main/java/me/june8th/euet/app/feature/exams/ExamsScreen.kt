package me.june8th.euet.app.feature.exams

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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.TermSelector
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.motion.itemMotion
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.data.ConflictDetector
import me.june8th.euet.core.model.Exam
import me.june8th.euet.app.di.euetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    onBack: () -> Unit,
    viewModel: ExamsViewModel = euetViewModel { ExamsViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExamsScreenContent(
        state = state,
        onBack = onBack,
        onSelectTerm = viewModel::selectTerm,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamsScreenContent(
    state: ExamsUiState,
    onBack: () -> Unit,
    onSelectTerm: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
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

    DetailScaffold(title = stringResource(R.string.title_exams), onBack = onBack) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
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
                    emptyTitle = stringResource(R.string.exams_empty),
                    loading = { SkeletonRows() },
                ) { exams ->
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
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
                        items(exams, key = { "${it.courseCode}-${it.date}-${it.startTime}" }) { exam ->
                            val recordKey =
                                ConflictDetector.examRecordKey(state.selectedTerm.orEmpty(), exam)
                            ExamCard(
                                exam = exam,
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

@Composable
private fun ExamCard(
    exam: Exam,
    conflicted: Boolean = false,
    onConflictClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exam.courseName ?: exam.courseCode ?: stringResource(R.string.exam_fallback_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (conflicted) ConflictBadge(onClick = onConflictClick)
            }
            exam.courseCode?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                exam.date?.let { IconText(Icons.Rounded.CalendarToday, it) }
                exam.startTime?.let { IconText(Icons.Rounded.Schedule, it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!exam.room.isNullOrBlank()) IconText(Icons.Rounded.Place, exam.room)
                if (!exam.seat.isNullOrBlank()) {
                    IconText(Icons.Rounded.EventSeat, stringResource(R.string.exam_seat, exam.seat))
                }
            }
            exam.method?.let {
                AssistChip(
                    onClick = {},
                    label = { Text(it) },
                    colors = AssistChipDefaults.assistChipColors(),
                )
            }
        }
    }
}

@Composable
private fun IconText(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun ExamsPreview() {
    EUetTheme {
        ExamsScreenContent(
            state = ExamsUiState(
                terms = PreviewData.terms,
                selectedTerm = PreviewData.activeTermCode,
                content = UiState.Data(PreviewData.exams),
            ),
            onBack = {},
            onSelectTerm = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun ExamsPreviewConflicts() {
    EUetTheme {
        ExamsScreenContent(
            state = ExamsUiState(
                terms = PreviewData.terms,
                selectedTerm = PreviewData.activeTermCode,
                content = UiState.Data(PreviewData.exams),
                conflicts = PreviewData.examConflictReport,
            ),
            onBack = {},
            onSelectTerm = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}
