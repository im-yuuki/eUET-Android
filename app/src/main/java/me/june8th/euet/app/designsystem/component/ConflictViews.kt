package me.june8th.euet.app.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.june8th.euet.R
import me.june8th.euet.app.common.PreviewData
import me.june8th.euet.app.designsystem.motion.rememberReducedMotion
import me.june8th.euet.app.designsystem.theme.EUetTheme
import me.june8th.euet.core.model.ConflictFields
import me.june8th.euet.core.model.ConflictReport
import me.june8th.euet.core.model.FieldDiff
import me.june8th.euet.core.model.SourceId
import me.june8th.euet.core.model.other

/** Display name of a data source ("StudentHub" / "Cổng VNU"). */
@Composable
fun sourceName(source: SourceId): String = when (source) {
    SourceId.STUDENT_HUB -> stringResource(R.string.source_studenthub)
    SourceId.VNU_PORTAL -> stringResource(R.string.source_vnu_portal)
}

/** Localized label for a [ConflictFields] key; unknown keys render as-is. */
@Composable
fun conflictFieldLabel(key: String): String = when (key) {
    ConflictFields.COURSE_NAME -> stringResource(R.string.field_course_name)
    ConflictFields.CREDITS -> stringResource(R.string.field_credits)
    ConflictFields.SCORE_10 -> stringResource(R.string.field_score10)
    ConflictFields.SCORE_4 -> stringResource(R.string.field_score4)
    ConflictFields.LETTER -> stringResource(R.string.field_letter)
    ConflictFields.EXAM_DATE -> stringResource(R.string.field_exam_date)
    ConflictFields.EXAM_TIME -> stringResource(R.string.field_exam_time)
    ConflictFields.EXAM_ROOM -> stringResource(R.string.field_exam_room)
    ConflictFields.EXAM_SEAT -> stringResource(R.string.field_exam_seat)
    ConflictFields.EXAM_METHOD -> stringResource(R.string.field_exam_method)
    ConflictFields.FULL_NAME -> stringResource(R.string.field_full_name)
    ConflictFields.STUDENT_CODE -> stringResource(R.string.profile_student_code)
    ConflictFields.EMAIL -> stringResource(R.string.profile_email)
    ConflictFields.CLASS_NAME -> stringResource(R.string.profile_class)
    ConflictFields.MAJOR -> stringResource(R.string.profile_major)
    ConflictFields.PROGRAM -> stringResource(R.string.profile_program)
    else -> key
}

/**
 * Tappable warning banner shown at the top of a list when the connected sources disagree.
 * Tapping opens the diff sheet.
 */
@Composable
fun ConflictBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column {
                Text(
                    stringResource(R.string.conflict_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    pluralStringResource(R.plurals.conflict_count, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

/**
 * [ConflictBanner] that expands into place. The source comparison lands after the list is already
 * on screen, so the banner would otherwise shove the rows down without warning.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedConflictBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val motionScheme = MaterialTheme.motionScheme
    // Starts hidden so the very first composition animates rather than popping in.
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = if (reducedMotion) {
            EnterTransition.None
        } else {
            expandVertically(motionScheme.defaultSpatialSpec()) + fadeIn(motionScheme.defaultEffectsSpec())
        },
        exit = if (reducedMotion) {
            ExitTransition.None
        } else {
            shrinkVertically(motionScheme.defaultSpatialSpec()) + fadeOut(motionScheme.defaultEffectsSpec())
        },
    ) {
        ConflictBanner(count = count, onClick = onClick, modifier = modifier)
    }
}

/** Small warning badge on a row whose record differs between sources; opens the diff sheet. */
@Composable
fun ConflictBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(32.dp)) {
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = stringResource(R.string.conflict_badge_description),
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Bottom sheet listing every conflict of a screen. With [focusedRecordKey] set (row badge tap)
 * only that record's conflict is shown — falling back to the full list if the key is unknown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictDiffSheet(
    report: ConflictReport,
    onDismiss: () -> Unit,
    focusedRecordKey: String? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ConflictDiffContent(report, focusedRecordKey)
    }
}

@Composable
fun ConflictDiffContent(
    report: ConflictReport,
    focusedRecordKey: String? = null,
    modifier: Modifier = Modifier,
) {
    val conflicts = focusedRecordKey
        ?.let { key -> report.conflicts.filter { it.recordKey == key }.ifEmpty { report.conflicts } }
        ?: report.conflicts

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "title") {
            Text(
                stringResource(R.string.conflict_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        conflicts.forEachIndexed { index, conflict ->
            item(key = "h-$index-${conflict.recordKey}") {
                Column(Modifier.padding(top = if (index == 0) 0.dp else 8.dp)) {
                    Text(
                        conflict.recordLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    conflict.onlyIn?.let { source ->
                        Text(
                            stringResource(R.string.conflict_only_in, sourceName(source)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(conflict.fields, key = { "$index-${conflict.recordKey}-${it.fieldLabel}" }) { field ->
                FieldDiffRow(field, preferredSource = report.source)
            }
        }
    }
}

/** One differing field: both values side by side, each labeled with its source. */
@Composable
private fun FieldDiffRow(field: FieldDiff, preferredSource: SourceId) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                conflictFieldLabel(field.fieldLabel),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SourcedValue(
                    source = sourceName(preferredSource),
                    value = field.preferredValue,
                    emphasized = true,
                    modifier = Modifier.weight(1f),
                )
                SourcedValue(
                    source = sourceName(preferredSource.other()),
                    value = field.otherValue,
                    emphasized = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SourcedValue(
    source: String,
    value: String?,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            source,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value ?: "–",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// --- Previews ---

@Preview(locale = "vi", showBackground = true)
@Composable
private fun ConflictBannerPreview() {
    EUetTheme {
        ConflictBanner(count = PreviewData.conflictReport.conflicts.size, onClick = {})
    }
}

@Preview(locale = "vi", showBackground = true)
@Composable
private fun ConflictDiffContentPreview() {
    EUetTheme {
        ConflictDiffContent(PreviewData.conflictReport)
    }
}

@Preview(locale = "en", showBackground = true)
@Composable
private fun ConflictDiffContentPreviewEnglish() {
    EUetTheme {
        ConflictDiffContent(PreviewData.conflictReport)
    }
}
