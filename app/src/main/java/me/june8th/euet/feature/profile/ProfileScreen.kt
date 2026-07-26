package me.june8th.euet.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.core.designsystem.component.DetailScaffold
import me.june8th.euet.core.designsystem.component.EUetCard
import me.june8th.euet.core.designsystem.component.InfoRow
import me.june8th.euet.core.designsystem.component.UiStateContent
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.di.euetViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = euetViewModel { ProfileViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(title = "Profile", onBack = onBack) { padding ->
        UiStateContent(state, Modifier.padding(padding), onRetry = viewModel::load) { profile ->
            ProfileContent(profile, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProfileContent(profile: StudentProfile, modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                    Text(
                        profile.name.take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                profile.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                profile.code,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        EUetCard {
            InfoRow("Student code", profile.code)
            HorizontalDivider()
            profile.email?.let { InfoRow("School email", it); HorizontalDivider() }
            profile.className?.let { InfoRow("Class", it); HorizontalDivider() }
            profile.major?.let { InfoRow("Major", it); HorizontalDivider() }
            profile.program?.let { InfoRow("Program", it) }
        }
    }
}
