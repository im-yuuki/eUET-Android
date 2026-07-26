package me.june8th.euet.feature.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.core.designsystem.component.DetailScaffold
import me.june8th.euet.core.designsystem.component.TermSelector
import me.june8th.euet.core.designsystem.component.UiStateContent
import me.june8th.euet.core.model.Exam
import me.june8th.euet.di.euetViewModel

@Composable
fun ExamsScreen(
    onBack: () -> Unit,
    viewModel: ExamsViewModel = euetViewModel { ExamsViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(title = "Exams", onBack = onBack) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
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
                emptyTitle = "No exams scheduled",
            ) { exams ->
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(exams, key = { "${it.courseCode}-${it.date}-${it.startTime}" }) { ExamCard(it) }
                }
            }
        }
    }
}

@Composable
private fun ExamCard(exam: Exam) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                exam.courseName ?: exam.courseCode ?: "Exam",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            exam.courseCode?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                exam.date?.let { IconText(Icons.Rounded.CalendarToday, it) }
                exam.startTime?.let { IconText(Icons.Rounded.Schedule, it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!exam.room.isNullOrBlank()) IconText(Icons.Rounded.Place, exam.room)
                if (!exam.seat.isNullOrBlank()) IconText(Icons.Rounded.EventSeat, "Seat ${exam.seat}")
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
