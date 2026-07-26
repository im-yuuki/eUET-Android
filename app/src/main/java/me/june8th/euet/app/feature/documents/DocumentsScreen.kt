package me.june8th.euet.app.feature.documents

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EmptyState

@Composable
fun DocumentsScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Documents", onBack = onBack) { padding ->
        EmptyState(
            title = "Syllabus & forms",
            detail = "Browsing course syllabus PDFs and forms is coming in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
