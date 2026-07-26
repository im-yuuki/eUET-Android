package me.june8th.euet.app.feature.registration

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EmptyState

@Composable
fun RegistrationScreen(onBack: () -> Unit) {
    DetailScaffold(title = "Registration", onBack = onBack) { padding ->
        EmptyState(
            title = "Course registration",
            detail = "Your program, registration window and advising are coming in a future update.",
            modifier = Modifier.padding(padding),
        )
    }
}
