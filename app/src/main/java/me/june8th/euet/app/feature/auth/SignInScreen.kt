package me.june8th.euet.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.june8th.euet.R
import me.june8th.euet.app.designsystem.theme.EUetTheme

/**
 * Which sign-in flow the user picked. StudentHub accepts two, so it gets a chooser of its own:
 * the Google OAuth its SPA uses, and its `api/auth/login` student-ID form.
 */
private enum class SignInStep {
    Choose,
    StudentHub,
    StudentHubGoogle,
    StudentHubPassword,
    Daotao,
}

/** Where the back arrow goes from each step. */
private val SignInStep.parent: SignInStep?
    get() = when (this) {
        SignInStep.Choose -> null
        SignInStep.StudentHub, SignInStep.Daotao -> SignInStep.Choose
        SignInStep.StudentHubGoogle, SignInStep.StudentHubPassword -> SignInStep.StudentHub
    }

/**
 * Entry point when no provider is connected. Lets the user pick a backend, then routes to the
 * matching flow: for StudentHub a web view (Google OAuth) or a native student-ID form, and for the
 * VNU portal a native form.
 *
 * One connected provider is enough to enter the app; the other can be added later from Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(onSignedIn: () -> Unit) {
    var step by remember { mutableStateOf(SignInStep.Choose) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (step) {
                            SignInStep.Choose -> stringResource(R.string.signin_title)
                            SignInStep.StudentHub,
                            SignInStep.StudentHubGoogle,
                            SignInStep.StudentHubPassword,
                            -> stringResource(R.string.signin_studenthub)
                            SignInStep.Daotao -> stringResource(R.string.signin_daotao)
                        },
                    )
                },
                navigationIcon = {
                    step.parent?.let { parent ->
                        IconButton(onClick = { step = parent }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (step) {
                SignInStep.Choose -> ProviderChooser(
                    onStudentHub = { step = SignInStep.StudentHub },
                    onDaotao = { step = SignInStep.Daotao },
                )
                SignInStep.StudentHub -> StudentHubMethodChooser(
                    onGoogle = { step = SignInStep.StudentHubGoogle },
                    onPassword = { step = SignInStep.StudentHubPassword },
                )
                SignInStep.StudentHubGoogle -> LoginScreen(onLoggedIn = onSignedIn)
                SignInStep.StudentHubPassword -> StudentHubPasswordLoginScreen(onSignedIn = onSignedIn)
                SignInStep.Daotao -> DaotaoLoginScreen(onSignedIn = onSignedIn)
            }
        }
    }
}

/** The two ways into StudentHub. Both end in the same session; neither is a fallback. */
@Composable
private fun StudentHubMethodChooser(
    onGoogle: () -> Unit,
    onPassword: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.signin_studenthub_method_intro),
            style = MaterialTheme.typography.bodyLarge,
        )

        ProviderCard(
            title = stringResource(R.string.signin_studenthub_google),
            subtitle = stringResource(R.string.signin_studenthub_google_subtitle),
            icon = Icons.Rounded.Language,
            onClick = onGoogle,
        )
        ProviderCard(
            title = stringResource(R.string.signin_studenthub_password),
            subtitle = stringResource(R.string.signin_studenthub_password_subtitle),
            icon = Icons.Rounded.Badge,
            onClick = onPassword,
        )
    }
}

@Composable
private fun ProviderChooser(
    onStudentHub: () -> Unit,
    onDaotao: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.signin_intro),
            style = MaterialTheme.typography.bodyLarge,
        )

        ProviderCard(
            title = stringResource(R.string.signin_studenthub),
            subtitle = stringResource(R.string.signin_studenthub_subtitle),
            icon = Icons.Rounded.AccountBalance,
            onClick = onStudentHub,
        )
        ProviderCard(
            title = stringResource(R.string.signin_daotao),
            subtitle = stringResource(R.string.signin_daotao_subtitle),
            icon = Icons.Rounded.School,
            onClick = onDaotao,
        )

        Text(
            stringResource(R.string.signin_add_later),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, contentDescription = null) },
        )
    }
}

// --- Previews ---

// SignInScreen starts on the provider chooser, which composes no ViewModel — safe to preview
// as-is. The StudentHub/Daotao steps are previewed in their own files.
@Preview(locale = "vi", showBackground = true)
@Composable
private fun SignInPreview() {
    EUetTheme {
        SignInScreen(onSignedIn = {})
    }
}
