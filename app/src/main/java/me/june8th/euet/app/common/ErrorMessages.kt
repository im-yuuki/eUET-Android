package me.june8th.euet.app.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.june8th.euet.R
import me.june8th.euet.core.common.ErrorKind

/**
 * Localized copy for the common error kinds emitted by the data layer. [ErrorKind.UNKNOWN] (and
 * anything core didn't classify) falls back to the raw message it produced.
 */
@Composable
fun errorMessage(kind: ErrorKind, fallback: String): String = when (kind) {
    ErrorKind.NETWORK -> stringResource(R.string.error_network)
    ErrorKind.SESSION_EXPIRED -> stringResource(R.string.error_session_expired)
    ErrorKind.FORBIDDEN -> stringResource(R.string.error_forbidden)
    ErrorKind.NOT_FOUND -> stringResource(R.string.error_not_found)
    ErrorKind.SERVER -> stringResource(R.string.error_server)
    ErrorKind.BAD_CREDENTIALS -> stringResource(R.string.error_bad_credentials)
    ErrorKind.CAPTCHA_REJECTED -> stringResource(R.string.error_captcha_rejected)
    ErrorKind.CANVAS_TOKEN_REJECTED -> stringResource(R.string.error_canvas_token_rejected)
    ErrorKind.SIGN_IN_DAOTAO -> stringResource(R.string.error_sign_in_daotao)
    ErrorKind.SIGN_IN_STUDENTHUB -> stringResource(R.string.error_sign_in_studenthub)
    ErrorKind.CONNECT_CANVAS -> stringResource(R.string.error_connect_canvas)
    ErrorKind.UNKNOWN -> fallback
}

/** Localized message for a [UiState.Error]. */
@Composable
fun errorMessage(error: UiState.Error): String = errorMessage(error.kind, error.message)
