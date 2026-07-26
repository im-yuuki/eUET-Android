package me.june8th.euet.app.feature.canvas

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EmptyState

@Composable
fun CanvasScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Canvas", onBack = onBack) { padding ->
        EmptyState(
            title = "Courses & assignments",
            detail = "Canvas integration is coming in a future update — it'll show your active courses, upcoming assignments and unread inbox.",
            modifier = Modifier.padding(padding),
        )
    }
}
