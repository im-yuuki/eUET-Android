package me.june8th.euet.app.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.june8th.euet.app.designsystem.motion.itemMotion
import me.june8th.euet.core.model.Term

/** Horizontal chip row for picking the active term. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TermSelector(
    terms: List<Term>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(terms, key = { it.code }) { term ->
            FilterChip(
                selected = term.code == selected,
                onClick = { onSelect(term.code) },
                label = { Text(term.name) },
                // Expressive shape morph: the chip rounds out as it becomes the active term and
                // squares up under the finger. Material animates between them with the theme's
                // motion scheme, which is already reduced-motion aware.
                shapes = FilterChipDefaults.shapes(
                    shape = MaterialTheme.shapes.small,
                    selectedShape = MaterialTheme.shapes.large,
                    pressedShape = MaterialTheme.shapes.extraSmall,
                ),
                modifier = itemMotion(),
            )
        }
    }
}
