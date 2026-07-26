package me.june8th.euet.app.feature.auth

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.common.errorMessage
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.app.di.euetViewModel
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.model.CaptchaChallenge

/**
 * Student-ID sign-in for StudentHub: the portal's own `api/auth/login`, which wants a username, a
 * password and the six digits from a captcha image. Credentials go straight into the form — no web
 * view — and the captcha is only ever shown to the user to read; nothing here tries to solve it.
 */
@Composable
fun StudentHubPasswordLoginScreen(
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentHubPasswordLoginViewModel =
        euetViewModel { StudentHubPasswordLoginViewModel(it.authRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    StudentHubPasswordLoginContent(
        state = state,
        onUserNameChange = viewModel::onUserNameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onCaptchaAnswerChange = viewModel::onCaptchaAnswerChange,
        onRememberPasswordChange = viewModel::onRememberPasswordChange,
        onRefreshCaptcha = viewModel::refreshCaptcha,
        onSubmit = viewModel::submit,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StudentHubPasswordLoginContent(
    state: StudentHubPasswordLoginUiState,
    onUserNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCaptchaAnswerChange: (String) -> Unit,
    onRememberPasswordChange: (Boolean) -> Unit,
    onRefreshCaptcha: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.studenthub_login_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.studenthub_login_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.userName,
            onValueChange = onUserNameChange,
            label = { Text(stringResource(R.string.studenthub_student_id)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(
                // Text, not Number: some StudentHub accounts sign in with an email-style username.
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.studenthub_password)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        CaptchaRow(
            captcha = state.captcha,
            answer = state.captchaAnswer,
            isLoading = state.isLoadingCaptcha,
            enabled = !state.isSubmitting,
            onAnswerChange = onCaptchaAnswerChange,
            onRefresh = onRefreshCaptcha,
            onSubmit = onSubmit,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.studenthub_remember_password),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.rememberPassword,
                onCheckedChange = onRememberPasswordChange,
                enabled = !state.isSubmitting,
            )
        }

        state.error?.let { error ->
            Text(
                errorMessage(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) {
                LoadingIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                )
            }
            Text(
                if (state.isSubmitting) {
                    stringResource(R.string.studenthub_signing_in)
                } else {
                    stringResource(R.string.studenthub_sign_in)
                },
            )
        }

        Text(
            stringResource(R.string.studenthub_password_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The challenge image, a reload button, and the field for what the user reads off it. */
@Composable
private fun CaptchaRow(
    captcha: CaptchaChallenge?,
    answer: String,
    isLoading: Boolean,
    enabled: Boolean,
    onAnswerChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaptchaImage(captcha = captcha, isLoading = isLoading)

        IconButton(onClick = onRefresh, enabled = enabled && !isLoading) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.studenthub_captcha_refresh),
            )
        }

        OutlinedTextField(
            value = answer,
            onValueChange = onAnswerChange,
            label = { Text(stringResource(R.string.studenthub_captcha)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CaptchaImage(captcha: CaptchaChallenge?, isLoading: Boolean) {
    val bitmap = remember(captcha) {
        captcha?.image
            ?.takeIf { it.isNotEmpty() }
            ?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            ?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.studenthub_captcha_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            isLoading -> LoadingIndicator(modifier = Modifier.size(24.dp))
            else -> Text(
                stringResource(R.string.studenthub_captcha_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun StudentHubPasswordLoginPreview() {
    EUetTheme {
        StudentHubPasswordLoginContent(
            state = StudentHubPasswordLoginUiState(
                userName = "22028123",
                password = "•••••••",
                captcha = PreviewData.captchaChallenge,
            ),
            onUserNameChange = {},
            onPasswordChange = {},
            onCaptchaAnswerChange = {},
            onRememberPasswordChange = {},
            onRefreshCaptcha = {},
            onSubmit = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun StudentHubPasswordLoginSubmittingPreview() {
    EUetTheme {
        StudentHubPasswordLoginContent(
            state = StudentHubPasswordLoginUiState(
                userName = "22028123",
                password = "•••••••",
                captchaAnswer = "418293",
                captcha = PreviewData.captchaChallenge,
                isSubmitting = true,
            ),
            onUserNameChange = {},
            onPasswordChange = {},
            onCaptchaAnswerChange = {},
            onRememberPasswordChange = {},
            onRefreshCaptcha = {},
            onSubmit = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun StudentHubPasswordLoginErrorPreview() {
    EUetTheme {
        StudentHubPasswordLoginContent(
            // A rejected attempt spends the captcha, so the replacement is still loading.
            state = StudentHubPasswordLoginUiState(
                userName = "22028123",
                password = "•••••••",
                isLoadingCaptcha = true,
                error = UiState.Error("", ErrorKind.CAPTCHA_REJECTED),
            ),
            onUserNameChange = {},
            onPasswordChange = {},
            onCaptchaAnswerChange = {},
            onRememberPasswordChange = {},
            onRefreshCaptcha = {},
            onSubmit = {},
        )
    }
}
