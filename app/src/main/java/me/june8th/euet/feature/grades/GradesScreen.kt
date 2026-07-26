package me.june8th.euet.feature.grades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.core.designsystem.component.SectionHeader
import me.june8th.euet.core.designsystem.component.UiStateContent
import me.june8th.euet.core.model.CourseGrade
import me.june8th.euet.core.model.GpaSummary
import me.june8th.euet.core.model.TermGrades
import me.june8th.euet.di.euetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    onOpenExams: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: GradesViewModel = euetViewModel { GradesViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Grades") },
                actions = {
                    IconButton(onClick = onOpenExams) {
                        Icon(Icons.Rounded.EventNote, contentDescription = "Exams")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        UiStateContent(
            state.content,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            onRetry = viewModel::load,
            emptyTitle = "No grades yet",
        ) { terms ->
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.gpa?.let { item(key = "gpa") { GpaCard(it) } }
                terms.forEach { term ->
                    item(key = "h-${term.termCode}") { SectionHeader(term.termCode) }
                    items(term.courses, key = { "${term.termCode}-${it.code}" }) { GradeRow(it) }
                }
            }
        }
    }
}

@Composable
private fun GpaCard(gpa: GpaSummary) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Stat("CPA", gpa.cpa?.let { "%.2f".format(it) } ?: "–")
            Stat("Credits", gpa.accumulatedCredits?.let { "%.0f".format(it) } ?: "–")
            Stat("Total", gpa.totalCredits?.let { "%.0f".format(it) } ?: "–")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GradeRow(grade: CourseGrade) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(grade.name ?: grade.code, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        append(grade.code)
                        grade.credits?.let { append("  ·  ${"%.0f".format(it)} cr") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
