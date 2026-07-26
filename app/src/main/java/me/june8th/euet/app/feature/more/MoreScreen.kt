package me.june8th.euet.app.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import me.june8th.euet.R
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.app.navigation.Route

private data class MoreItem(
    @StringRes val labelRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val route: Any,
)

private val moreItems = listOf(
    MoreItem(R.string.title_profile, R.string.more_profile_subtitle, Icons.Rounded.AccountCircle, Route.Profile),
    MoreItem(R.string.title_exams, R.string.more_exams_subtitle, Icons.Rounded.EventNote, Route.Exams),
    MoreItem(R.string.title_notifications, R.string.more_notifications_subtitle, Icons.Rounded.Notifications, Route.Notifications),
    MoreItem(R.string.title_tuition, R.string.more_tuition_subtitle, Icons.Rounded.CreditCard, Route.Tuition),
    MoreItem(R.string.title_canvas, R.string.more_canvas_subtitle, Icons.Rounded.School, Route.Canvas),
    MoreItem(R.string.title_training, R.string.more_training_subtitle, Icons.Rounded.WorkspacePremium, Route.Training),
    MoreItem(R.string.title_registration, R.string.more_registration_subtitle, Icons.Rounded.EditCalendar, Route.Registration),
    MoreItem(R.string.title_documents, R.string.more_documents_subtitle, Icons.Rounded.Description, Route.Documents),
    MoreItem(R.string.title_settings, R.string.more_settings_subtitle, Icons.Rounded.Settings, Route.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onNavigate: (Any) -> Unit, contentPadding: PaddingValues) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.title_more)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 12.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(moreItems) { item ->
                MoreRow(item, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun MoreRow(item: MoreItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            androidx.compose.foundation.layout.Column(Modifier.padding(start = 16.dp).width(0.dp).weight(1f)) {
                Text(stringResource(item.labelRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(item.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun MorePreview() {
    EUetTheme {
        MoreScreen(onNavigate = {}, contentPadding = PaddingValues())
    }
}
