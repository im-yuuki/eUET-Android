package me.june8th.euet.app.feature.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.core.model.AppNotification
import me.june8th.euet.core.model.NewsItem
import me.june8th.euet.app.di.euetViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = euetViewModel { NotificationsViewModel(it.studentRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DetailScaffold(title = "Notifications", onBack = onBack) { padding ->
        UiStateContent(
            state.content,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            onRetry = viewModel::load,
            emptyTitle = "No announcements",
        ) { notifications ->
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.news.isNotEmpty()) {
                    item(key = "news-header") {
                        SectionHeader("News", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    item(key = "news-row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.news, key = { it.id }) { NewsCard(it) }
                        }
                    }
                }
                item(key = "noti-header") {
                    SectionHeader("Announcements", modifier = Modifier.padding(horizontal = 16.dp))
                }
                items(notifications, key = { it.id }) { NotificationRow(it) }
            }
        }
    }
}

@Composable
private fun NewsCard(news: NewsItem) {
    ElevatedCard(shape = MaterialTheme.shapes.large, modifier = Modifier.width(260.dp)) {
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
private fun NotificationRow(noti: AppNotification) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
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
