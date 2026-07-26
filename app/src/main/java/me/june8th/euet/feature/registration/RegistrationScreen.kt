package me.june8th.euet.feature.registration

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.june8th.euet.core.designsystem.component.DetailScaffold
import me.june8th.euet.core.designsystem.component.EmptyState

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
