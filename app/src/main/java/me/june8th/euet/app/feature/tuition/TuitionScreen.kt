package me.june8th.euet.app.feature.tuition

import android.content.Context
import android.content.res.Configuration
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.model.Bill
import me.june8th.euet.app.di.euetViewModel
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun TuitionScreen(
    onBack: () -> Unit,
    viewModel: TuitionViewModel = euetViewModel { TuitionViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    TuitionScreenContent(
        state = state,
        refreshing = refreshing,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuitionScreenContent(
    state: UiState<List<Bill>>,
    refreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val toolbarColor = MaterialTheme.colorScheme.surfaceContainer

    DetailScaffold(title = stringResource(R.string.title_tuition), onBack = onBack) { padding ->
        RefreshableBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) {
            UiStateContent(
                state,
                onRetry = onRetry,
                emptyTitle = stringResource(R.string.tuition_empty),
                loading = { SkeletonRows(rowHeight = 120.dp) },
            ) { bills ->
                val totalRemaining = bills.sumOf { it.remaining ?: 0.0 }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "summary") { RemainingCard(totalRemaining) }
                    items(bills, key = { "${it.name}-${it.termCode}" }) { bill ->
                        BillCard(bill) { url -> openInvoice(context, url, toolbarColor) }
                    }
                }
            }
        }
    }
}

/** Opens the bill's invoice in a Chrome Custom Tab themed to match the app. */
private fun openInvoice(context: Context, url: String, toolbarColor: Color) {
    CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor.toArgb())
                .build(),
        )
        .setShowTitle(true)
        .build()
        .launchUrl(context, url.toUri())
}

@Composable
private fun RemainingCard(totalRemaining: Double) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.tuition_outstanding),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun BillCard(bill: Bill, onOpenInvoice: (String) -> Unit) {
    val invoiceUrl = bill.invoiceUrl
    val cardModifier = Modifier.fillMaxWidth()
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                bill.name ?: stringResource(R.string.title_tuition),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            bill.termCode?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.tuition_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatVnd(bill.amount ?: 0.0), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.tuition_remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatVnd(bill.remaining ?: 0.0), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                bill.status?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                if (invoiceUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.tuition_view_invoice),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
    if (invoiceUrl != null) {
        ElevatedCard(
            onClick = { onOpenInvoice(invoiceUrl) },
            shape = MaterialTheme.shapes.large,
            modifier = cardModifier,
        ) { content() }
    } else {
        ElevatedCard(shape = MaterialTheme.shapes.large, modifier = cardModifier) { content() }
    }
}

/** VND amount formatted with the user's locale conventions (grouping, symbol placement). */
private fun formatVnd(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = Currency.getInstance("VND")
        maximumFractionDigits = 0
    }.format(amount)

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun TuitionPreviewOutstanding() {
    EUetTheme {
        TuitionScreenContent(
            state = UiState.Data(PreviewData.bills),
            refreshing = false,
            onBack = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TuitionPreviewDark() {
    EUetTheme {
        TuitionScreenContent(
            state = UiState.Data(PreviewData.bills),
            refreshing = false,
            onBack = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun TuitionPreviewAllPaid() {
    EUetTheme {
        TuitionScreenContent(
            state = UiState.Data(PreviewData.bills.filter { (it.remaining ?: 0.0) == 0.0 }),
            refreshing = false,
            onBack = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}
