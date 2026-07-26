package me.june8th.euet.app.feature.tuition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.core.model.Bill
import me.june8th.euet.app.di.euetViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TuitionScreen(
    onBack: () -> Unit,
    viewModel: TuitionViewModel = euetViewModel { TuitionViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(title = "Tuition", onBack = onBack) { padding ->
        UiStateContent(
            state,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            onRetry = viewModel::load,
            emptyTitle = "No bills",
        ) { bills ->
            val totalRemaining = bills.sumOf { it.remaining ?: 0.0 }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") { RemainingCard(totalRemaining) }
                items(bills, key = { "${it.name}-${it.termCode}" }) { BillCard(it) }
            }
        }
    }
}

@Composable
private fun RemainingCard(totalRemaining: Double) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Outstanding balance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatVnd(totalRemaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (totalRemaining > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BillCard(bill: Bill) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(bill.name ?: "Tuition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            bill.termCode?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatVnd(bill.amount ?: 0.0), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Remaining", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatVnd(bill.remaining ?: 0.0), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            bill.status?.let { AssistChip(onClick = {}, label = { Text(it) }) }
        }
    }
}

private fun formatVnd(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
