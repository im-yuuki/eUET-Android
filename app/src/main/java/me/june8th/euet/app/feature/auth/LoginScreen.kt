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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.app.designsystem.component.ErrorState
import me.june8th.euet.app.di.euetViewModel

private const val LOGIN_URL = "https://studenthub.uet.edu.vn/"

// Some IdPs (Google) reject embedded WebViews; presenting a normal Chrome UA improves success.
private const val DESKTOP_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/126.0.0.0 Mobile Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = euetViewModel { LoginViewModel(it.authRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) onLoggedIn()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sign in to eUET") }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is LoginUiState.Error -> ErrorState(s.message, onRetry = viewModel::retry)
                else -> {
                    LoginWebView(onToken = viewModel::onTokenCaptured)
                    if (s is LoginUiState.Verifying) {
                        Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            LinearProgressIndicator()
                            Text(
                                "Signing you in…",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    }
                }
            }
        }
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
