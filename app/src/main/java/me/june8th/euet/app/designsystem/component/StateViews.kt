package me.june8th.euet.app.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.june8th.euet.R
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.common.errorMessage
import me.june8th.euet.app.designsystem.motion.rememberReducedMotion

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Expressive replacement for the indeterminate circular spinner (short waits, <~5 s).
        LoadingIndicator()
    }
}

@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    MessageState(
        icon = Icons.Rounded.CloudOff,
        title = stringResource(R.string.error_title),
        detail = message,
        actionLabel = if (onRetry != null) stringResource(R.string.action_retry) else null,
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(
    title: String = stringResource(R.string.empty_title_default),
    detail: String? = null,
    modifier: Modifier = Modifier,
) {
    MessageState(icon = Icons.Rounded.Inbox, title = title, detail = detail, modifier = modifier)
}

/**
 * Renders the right state view for a [UiState], delegating to [content] when data is present.
 * List screens can swap the centered spinner for skeleton rows via [loading].
 *
 * States fade through each other so skeleton → data and data → error read as one surface
 * changing rather than a pop. Data → data (a refresh landing) keeps the same slot, so a live
 * list is never crossfaded out from under the reader.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    emptyTitle: String = stringResource(R.string.empty_title_default),
    loading: @Composable () -> Unit = { LoadingState(modifier) },
    content: @Composable (T) -> Unit,
) {
    val reduced = rememberReducedMotion()
    val motionScheme = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = state,
        modifier = Modifier.fillMaxSize(),
        contentKey = { it.slot() },
        transitionSpec = {
            // Every state view fills the parent, so there is no size to transform between.
            if (reduced) {
                (EnterTransition.None togetherWith ExitTransition.None) using null
            } else {
                val fadeSpec = motionScheme.defaultEffectsSpec<Float>()
                (fadeIn(fadeSpec) + scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.96f))
                    .togetherWith(fadeOut(fadeSpec)) using null
            }
        },
        label = "ui-state",
    ) { current ->
        when (current) {
            is UiState.Loading -> loading()
            is UiState.Empty -> EmptyState(title = emptyTitle, modifier = modifier)
            is UiState.Error -> ErrorState(errorMessage(current), onRetry, modifier)
            is UiState.Data -> content(current.value)
        }
    }
}

/** Which of the four state views a [UiState] maps to; drives the crossfade, the payload does not. */
private fun UiState<*>.slot(): Int = when (this) {
    is UiState.Loading -> 0
    is UiState.Empty -> 1
    is UiState.Error -> 2
    is UiState.Data -> 3
}
