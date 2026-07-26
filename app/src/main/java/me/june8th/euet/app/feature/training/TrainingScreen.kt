package me.june8th.euet.app.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.app.di.euetViewModel
import me.june8th.euet.core.model.TermPerformance

@Composable
fun TrainingScreen(
    onBack: () -> Unit,
    viewModel: TrainingViewModel = euetViewModel { TrainingViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrainingScreenContent(state = state, onBack = onBack, onRetry = viewModel::load)
}

@Composable
private fun TrainingScreenContent(
    state: UiState<List<TermPerformance>>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    DetailScaffold(title = stringResource(R.string.title_training), onBack = onBack) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
            UiStateContent(
                state,
                onRetry = onRetry,
                emptyTitle = stringResource(R.string.training_empty),
                loading = { SkeletonRows() },
            ) { terms ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(terms, key = { it.termCode }) { TermRow(it) }
                    // Conduct scores live on a portal sub-tab that isn't scraped yet, so this
                    // screen shows term GPA instead — same interim behaviour as iOS.
                    if (terms.none { it.conductScore != null }) {
                        item(key = "footer") { FooterNote() }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermRow(term: TermPerformance) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(term.termName ?: term.termCode, style = MaterialTheme.typography.titleSmall)
                val parts = listOfNotNull(
                    term.termGpa?.let { stringResource(R.string.training_term_gpa, "%.2f".format(it)) },
                    term.credits?.let { stringResource(R.string.training_credits, "%.0f".format(it)) },
                )
                Text(
                    parts.joinToString("  ·  ").ifEmpty { stringResource(R.string.training_no_grades) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (term.conductScore != null) {
                    ScoreStat("${term.conductScore}", stringResource(R.string.training_conduct))
                } else {
                    ScoreStat(term.cumulativeGpa?.let { "%.2f".format(it) } ?: "–", stringResource(R.string.stat_cpa))
                }
            }
        }
    }
}

@Composable
private fun ScoreStat(value: String, label: String) {
    Text(
        value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FooterNote() {
    Text(
        stringResource(R.string.training_footer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun TrainingPreview() {
    EUetTheme {
        TrainingScreenContent(state = UiState.Data(PreviewData.termPerformance), onBack = {}, onRetry = {})
    }
}
