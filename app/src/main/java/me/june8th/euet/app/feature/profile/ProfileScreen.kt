package me.june8th.euet.app.feature.profile

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.designsystem.component.ConflictBanner
import me.june8th.euet.app.designsystem.component.ConflictDiffSheet
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EUetCard
import me.june8th.euet.app.designsystem.component.InfoRow
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.StudentProfile
import me.june8th.euet.app.di.euetViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = euetViewModel { ProfileViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreenContent(state = state, onBack = onBack, onRetry = viewModel::load)
}

@Composable
private fun ProfileScreenContent(
    state: ProfileUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    var showDiff by remember { mutableStateOf(false) }

    DetailScaffold(title = stringResource(R.string.title_profile), onBack = onBack) { padding ->
        UiStateContent(state.content, Modifier.padding(padding), onRetry = onRetry) { profile ->
            ProfileContent(
                profile = profile,
                conflicts = state.conflicts,
                onShowDiff = { showDiff = true },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showDiff) {
        state.conflicts?.let { report ->
            ConflictDiffSheet(report = report, onDismiss = { showDiff = false })
        }
    }
}

@Composable
private fun ProfileContent(
    profile: StudentProfile,
    conflicts: ConflictReport?,
    onShowDiff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        conflicts?.takeIf { it.conflicts.isNotEmpty() }?.let { report ->
            ConflictBanner(count = report.conflicts.size, onClick = onShowDiff)
        }
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
            InfoRow(stringResource(R.string.profile_student_code), profile.code)
            HorizontalDivider()
            profile.email?.let { InfoRow(stringResource(R.string.profile_email), it); HorizontalDivider() }
            profile.className?.let { InfoRow(stringResource(R.string.profile_class), it); HorizontalDivider() }
            profile.major?.let { InfoRow(stringResource(R.string.profile_major), it); HorizontalDivider() }
            profile.program?.let { InfoRow(stringResource(R.string.profile_program), it) }
        }
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun ProfilePreview() {
    EUetTheme {
        ProfileScreenContent(
            state = ProfileUiState(content = UiState.Data(PreviewData.profile)),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun ProfilePreviewConflicts() {
    EUetTheme {
        ProfileScreenContent(
            state = ProfileUiState(
                content = UiState.Data(PreviewData.profile),
                conflicts = PreviewData.profileConflictReport,
            ),
            onBack = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "en", showBackground = true)
@Composable
private fun ProfilePreviewEnglish() {
    EUetTheme {
        ProfileScreenContent(
            state = ProfileUiState(content = UiState.Data(PreviewData.profile)),
            onBack = {},
            onRetry = {},
        )
    }
}
