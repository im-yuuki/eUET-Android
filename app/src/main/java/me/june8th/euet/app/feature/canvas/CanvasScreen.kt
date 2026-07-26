package me.june8th.euet.app.feature.canvas

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.common.UiState
import me.june8th.euet.app.common.errorMessage
import me.june8th.euet.app.designsystem.component.DetailScaffold
import me.june8th.euet.app.designsystem.component.EUetCard
import me.june8th.euet.app.designsystem.component.LoadingState
import me.june8th.euet.app.designsystem.component.RefreshableBox
import me.june8th.euet.app.designsystem.component.SectionHeader
import me.june8th.euet.app.designsystem.component.SkeletonRows
import me.june8th.euet.app.designsystem.component.UiStateContent
import me.june8th.euet.app.designsystem.motion.AnimatedValueText
import me.june8th.euet.app.designsystem.motion.itemMotion
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.app.di.euetViewModel
import me.june8th.euet.core.common.ErrorKind
import me.june8th.euet.core.model.CanvasCourse
import me.june8th.euet.core.model.MissingSubmission
import me.june8th.euet.core.model.PlannerItem
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Canvas LMS dashboard. Disconnected: a paste-your-access-token connect form (the UET instance
 * offers no OAuth client, so the official user-generated token is the supported path).
 * Connected: active courses, upcoming planner items, missing submissions and the unread inbox.
 */
@Composable
fun CanvasScreen(
    onBack: () -> Unit,
    viewModel: CanvasViewModel = euetViewModel { CanvasViewModel(it.canvasRepository, it.snapshotCache) },
) {
    val connected by viewModel.isConnected.collectAsStateWithLifecycle()
    val connectState by viewModel.connectState.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    CanvasScreenContent(
        connected = connected,
        connectState = connectState,
        dashboard = dashboard,
        refreshing = refreshing,
        onBack = onBack,
        onDisconnect = viewModel::disconnect,
        onTokenChange = viewModel::onTokenChange,
        onConnect = viewModel::connect,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::load,
    )
}

@Composable
private fun CanvasScreenContent(
    connected: Boolean?,
    connectState: CanvasConnectState,
    dashboard: UiState<CanvasDashboard>,
    refreshing: Boolean,
    onBack: () -> Unit,
    onDisconnect: () -> Unit,
    onTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.title_canvas),
        onBack = onBack,
        actions = {
            if (connected == true) {
                IconButton(onClick = onDisconnect) {
                    Icon(
                        Icons.Rounded.LinkOff,
                        contentDescription = stringResource(R.string.canvas_disconnect),
                    )
                }
            }
        },
    ) { padding ->
        when (connected) {
            null -> LoadingState(Modifier.padding(padding))
            false -> ConnectContent(
                state = connectState,
                onTokenChange = onTokenChange,
                onConnect = onConnect,
                modifier = Modifier.padding(padding),
            )
            true -> RefreshableBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            ) {
                UiStateContent(
                    dashboard,
                    onRetry = onRetry,
                    emptyTitle = stringResource(R.string.canvas_empty),
                    loading = { SkeletonRows(rowHeight = 104.dp) },
                ) { data ->
                    DashboardContent(data)
                }
            }
        }
    }
}

// --- Disconnected: connect form ---

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectContent(
    state: CanvasConnectState,
    onTokenChange: (String) -> Unit,
    onConnect: () -> Unit,
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
            stringResource(R.string.canvas_connect_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.canvas_connect_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.token,
            onValueChange = onTokenChange,
            label = { Text(stringResource(R.string.canvas_access_token)) },
            singleLine = true,
            enabled = !state.isConnecting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onConnect() }),
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { error ->
            Text(
                errorMessage(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onConnect,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isConnecting) {
                LoadingIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(24.dp),
                )
            }
            Text(
                if (state.isConnecting) {
                    stringResource(R.string.canvas_connecting)
                } else {
                    stringResource(R.string.canvas_connect)
                },
            )
        }

        Text(
            stringResource(R.string.canvas_token_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.canvas_token_storage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// --- Connected: dashboard ---

@Composable
private fun DashboardContent(data: CanvasDashboard) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (data.missing.isNotEmpty()) {
            item(key = "missing") { MissingSubmissionsCard(data.missing) }
        }
        if (data.summary.unreadInbox > 0) {
            item(key = "inbox") { UnreadInboxChip(data.summary.unreadInbox) }
        }
        if (data.courses.isNotEmpty()) {
            item(key = "courses-header") { SectionHeader(stringResource(R.string.canvas_active_courses)) }
            items(data.courses, key = { "course-${it.id}" }) { CourseCard(it, modifier = itemMotion()) }
        }
        if (data.upcoming.isNotEmpty()) {
            item(key = "upcoming-header") { SectionHeader(stringResource(R.string.canvas_upcoming)) }
            item(key = "upcoming-card") { UpcomingCard(data.upcoming) }
        }
    }
}

/** Alert-style card: the student has submittable work past its due date. */
@Composable
private fun MissingSubmissionsCard(missing: List<MissingSubmission>) {
    val onColor = MaterialTheme.colorScheme.onErrorContainer
    ElevatedCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = onColor)
                // A refresh that clears (or adds) work rolls the count rather than swapping it.
                AnimatedValueText(
                    value = missing.size.toDouble(),
                    text = pluralStringResource(R.plurals.canvas_missing_submissions, missing.size, missing.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onColor,
                )
            }
            missing.take(5).forEach { item ->
                val due = formatCanvasDate(item.dueAt)
                Text(
                    if (due != null) {
                        stringResource(R.string.canvas_missing_due, item.name, due)
                    } else {
                        item.name
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onColor,
                    maxLines = 2,
                )
            }
            if (missing.size > 5) {
                Text(
                    stringResource(R.string.canvas_more_items, missing.size - 5),
                    style = MaterialTheme.typography.bodySmall,
                    color = onColor,
                )
            }
        }
    }
}

@Composable
private fun UnreadInboxChip(count: Int) {
    AssistChip(
        onClick = {},
        leadingIcon = { Icon(Icons.Rounded.Inbox, contentDescription = null) },
        label = {
            AnimatedValueText(
                value = count.toDouble(),
                text = pluralStringResource(R.plurals.canvas_unread_inbox, count, count),
                style = MaterialTheme.typography.labelLarge,
            )
        },
    )
}

@Composable
private fun CourseCard(course: CanvasCourse, modifier: Modifier = Modifier) {
    ElevatedCard(shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) {
        course.imageUrl?.let { CourseImage(it) }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            course.courseCode?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            course.term?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun CourseImage(url: String) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(MaterialTheme.shapes.extraLarge),
        loading = { ImageFallback(showIcon = false) },
        error = { ImageFallback(showIcon = true) },
    )
}

/** Neutral block shown while a course image loads, or in its place when loading fails. */
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
private fun UpcomingCard(items: List<PlannerItem>) {
    EUetCard {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            UpcomingRow(item)
        }
    }
}

@Composable
private fun UpcomingRow(item: PlannerItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            item.kind.icon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
            item.courseName?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            formatCanvasDate(item.dueDate)?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (item.isSubmitted) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.canvas_submitted),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun PlannerItem.Kind.icon(): ImageVector = when (this) {
    PlannerItem.Kind.ANNOUNCEMENT -> Icons.Rounded.Campaign
    PlannerItem.Kind.QUIZ -> Icons.Rounded.Quiz
    PlannerItem.Kind.ASSIGNMENT -> Icons.AutoMirrored.Rounded.Assignment
    PlannerItem.Kind.DISCUSSION -> Icons.Rounded.Forum
    PlannerItem.Kind.CALENDAR_EVENT -> Icons.Rounded.Event
    PlannerItem.Kind.OTHER -> Icons.Rounded.Checklist
}

/**
 * Formats a Canvas ISO-8601 timestamp in the device time zone using the locale's preferred
 * weekday/day/month/time arrangement; falls back to the raw string.
 */
private fun formatCanvasDate(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    return try {
        val locale = Locale.getDefault()
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMMHHmm")
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern, locale))
    } catch (_: Exception) {
        iso
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun CanvasDashboardPreview() {
    EUetTheme {
        CanvasScreenContent(
            connected = true,
            connectState = CanvasConnectState(),
            dashboard = UiState.Data(
                CanvasDashboard(
                    courses = PreviewData.canvasCourses,
                    upcoming = PreviewData.plannerItems,
                    missing = PreviewData.missingSubmissions,
                    summary = PreviewData.canvasSummary,
                ),
            ),
            refreshing = false,
            onBack = {},
            onDisconnect = {},
            onTokenChange = {},
            onConnect = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun CanvasConnectPreview() {
    EUetTheme {
        CanvasScreenContent(
            connected = false,
            connectState = CanvasConnectState(),
            dashboard = UiState.Loading,
            refreshing = false,
            onBack = {},
            onDisconnect = {},
            onTokenChange = {},
            onConnect = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun CanvasConnectErrorPreview() {
    EUetTheme {
        CanvasScreenContent(
            connected = false,
            connectState = CanvasConnectState(
                token = "1050~rejected-sample-token",
                error = UiState.Error("", ErrorKind.CANVAS_TOKEN_REJECTED),
            ),
            dashboard = UiState.Loading,
            refreshing = false,
            onBack = {},
            onDisconnect = {},
            onTokenChange = {},
            onConnect = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}
