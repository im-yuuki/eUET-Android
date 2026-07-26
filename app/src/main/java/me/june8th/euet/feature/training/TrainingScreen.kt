package me.june8th.euet.feature.training

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.june8th.euet.core.designsystem.component.DetailScaffold
import me.june8th.euet.core.designsystem.component.EmptyState

@Composable
fun TrainingScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Training points", onBack = onBack) { padding ->
        EmptyState(
            title = "Training & conduct points",
            detail = "Term-by-term conduct scores are coming in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
