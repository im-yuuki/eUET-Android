package me.june8th.euet.app.feature.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.R
import me.june8th.euet.app.common.errorMessage
import me.june8th.euet.app.designsystem.component.ErrorState
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.app.di.euetViewModel
import me.june8th.euet.core.common.ErrorKind

private const val LOGIN_URL = "https://studenthub.uet.edu.vn/"

// Some IdPs (Google) reject embedded WebViews; presenting a normal Chrome UA improves success.
private const val DESKTOP_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/126.0.0.0 Mobile Safari/537.36"

/**
 * StudentHub sign-in: loads the portal in a web view and captures the bearer token its own API
 * calls carry. Chrome (title bar / back) is supplied by [SignInScreen].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = euetViewModel { LoginViewModel(it.authRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) onLoggedIn()
    }

    Box(modifier.fillMaxSize()) {
        when (val s = state) {
            is LoginUiState.Error -> ErrorState(errorMessage(s.kind, s.message), onRetry = viewModel::retry)
            else -> {
                LoginWebView(onToken = viewModel::onTokenCaptured)
                if (s is LoginUiState.Verifying) {
                    VerifyingOverlay()
                }
            }
        }
    }
}

/** Centered progress overlay shown while the captured token is being verified. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VerifyingOverlay() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingIndicator()
        Text(
            stringResource(R.string.signin_verifying),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginWebView(onToken: (String) -> Unit) {
    val context = LocalContext.current
    // Guard so we only forward the first captured token.
    val captured = remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(context).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = DESKTOP_UA
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url?.toString().orEmpty()
                        val auth = request?.requestHeaders?.get("Authorization")
                        if (!captured.value &&
                            auth != null &&
                            auth.startsWith("Bearer ", ignoreCase = true) &&
                            url.contains("studenthub.uet.edu.vn/api")
                        ) {
                            captured.value = true
                            val token = auth.substring("Bearer ".length).trim()
                            view?.post { onToken(token) }
                        }
                        return null
                    }
                }
                loadUrl(LOGIN_URL)
            }
        },
    )
}

// --- Previews ---
// The SignIn state hosts a live WebView, which cannot render in a preview; only the
// verifying overlay and error states are previewed.

@Preview(locale = "vi", showBackground = true)
@Composable
private fun LoginVerifyingPreview() {
    EUetTheme {
        VerifyingOverlay()
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun LoginErrorPreview() {
    EUetTheme {
        ErrorState(errorMessage(ErrorKind.SIGN_IN_STUDENTHUB, ""), onRetry = {})
    }
}
