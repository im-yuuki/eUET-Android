package me.june8th.euet.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.BuildConfig
import me.june8th.euet.R
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EUetCard
import me.june8th.euet.app.designsystem.component.sourceName
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.app.di.euetViewModel
import me.june8th.euet.core.model.SourceId

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = euetViewModel { SettingsViewModel(it.authRepository, it.session) },
) {
    val preferredSource by viewModel.preferredSource.collectAsStateWithLifecycle()
    SettingsScreenContent(
        preferredSource = preferredSource,
        onSelectSource = viewModel::setPreferredSource,
        onBack = onBack,
        onSignOut = viewModel::logout,
    )
}

@Composable
private fun SettingsScreenContent(
    preferredSource: SourceId,
    onSelectSource: (SourceId) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    DetailScaffold(title = stringResource(R.string.title_settings), onBack = onBack) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EUetCard {
                Text(stringResource(R.string.settings_data_sources), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceId.entries.forEach { source ->
                        SourceChip(
                            source = source,
                            selected = preferredSource == source,
                            onClick = { onSelectSource(source) },
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_data_sources_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            EUetCard {
                Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.settings_app_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(onClick = { showConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Text(stringResource(R.string.settings_sign_out), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.settings_sign_out_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.settings_sign_out_confirm_text),
                    textAlign = TextAlign.Start,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onSignOut()
                }) { Text(stringResource(R.string.settings_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SourceChip(source: SourceId, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(sourceName(source)) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun SettingsPreview() {
    EUetTheme {
        SettingsScreenContent(
            preferredSource = SourceId.STUDENT_HUB,
            onSelectSource = {},
            onBack = {},
            onSignOut = {},
        )
    }
}

@Preview(locale = "en", showBackground = true)
@Composable
private fun SettingsPreviewVnuPreferred() {
    EUetTheme {
        SettingsScreenContent(
            preferredSource = SourceId.VNU_PORTAL,
            onSelectSource = {},
            onBack = {},
            onSignOut = {},
        )
    }
}
