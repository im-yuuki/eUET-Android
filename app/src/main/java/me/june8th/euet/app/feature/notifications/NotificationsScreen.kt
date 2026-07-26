package me.june8th.euet.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.motion.itemMotion
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.app.di.euetViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = euetViewModel { NotificationsViewModel(it.aggregateRepository, it.snapshotCache) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationsScreenContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::load,
        onLoadMore = viewModel::loadMore,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationsScreenContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    DetailScaffold(title = stringResource(R.string.title_notifications), onBack = onBack) { padding ->
        RefreshableBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) {
            UiStateContent(
                state.content,
                onRetry = onRetry,
                emptyTitle = stringResource(R.string.notifications_empty),
                loading = { SkeletonRows() },
            ) { notifications ->
                val listState = rememberLazyListState()
                val nearEnd by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= info.totalItemsCount - 3
                    }
                }
                LaunchedEffect(nearEnd, notifications.size) {
                    if (nearEnd) onLoadMore()
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (state.news.isNotEmpty()) {
                        item(key = "news-header") {
                            SectionHeader(
                                stringResource(R.string.notifications_news),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        item(key = "news-row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.news, key = { it.id }) { NewsCard(it, modifier = itemMotion()) }
                            }
                        }
                    }
                    item(key = "noti-header") {
                        SectionHeader(
                            stringResource(R.string.notifications_announcements),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    // Each appended page fades in where it lands rather than jumping into place.
                    items(notifications, key = { it.id }) { NotificationRow(it, modifier = itemMotion()) }
                    if (state.loadingMore) {
                        item(key = "loading-more") {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(news: NewsItem, modifier: Modifier = Modifier) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = modifier.width(260.dp)) {
        news.imageUrl?.let { NewsImage(it) }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(news.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
            news.summary?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
            news.createdAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
        }
    }
}

@Composable
private fun NewsImage(url: String) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(MaterialTheme.shapes.large),
        loading = { ImageFallback(showIcon = false) },
        error = { ImageFallback(showIcon = true) },
    )
}

/** Neutral block shown while a news image loads, or in its place when loading fails. */
@Composable
private fun ImageFallback(showIcon: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxSize()) {
        if (showIcon) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(noti: AppNotification, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!noti.read) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)) {
                    Box(Modifier.size(8.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    noti.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (noti.read) FontWeight.Normal else FontWeight.SemiBold,
                )
                noti.content?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                }
                noti.createdAt?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) }
            }
        }
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun NotificationsPreview() {
    EUetTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(
                news = PreviewData.news,
                content = UiState.Data(PreviewData.notifications),
                endReached = true,
            ),
            onBack = {},
            onRefresh = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun NotificationsPreviewEmpty() {
    EUetTheme {
        NotificationsScreenContent(
            state = NotificationsUiState(content = UiState.Empty, endReached = true),
            onBack = {},
            onRefresh = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}
