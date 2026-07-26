package me.june8th.euet.app.feature.registration

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.june8th.euet.R
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EmptyState
import me.june8th.euet.app.designsystem.theme.EUetTheme

@Composable
fun RegistrationScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.title_registration), onBack = onBack) { padding ->
        EmptyState(
            title = stringResource(R.string.registration_empty_title),
            detail = stringResource(R.string.registration_empty_detail),
            modifier = Modifier.padding(padding),
        )
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun RegistrationPreview() {
    EUetTheme {
        RegistrationScreen(onBack = {})
    }
}
