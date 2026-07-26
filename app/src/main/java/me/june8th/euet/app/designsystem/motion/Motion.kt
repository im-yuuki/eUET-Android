@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package me.june8th.euet.app.designsystem.motion

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset

/**
 * The app's shared motion vocabulary, built on the Material 3 Expressive [MotionScheme] the theme
 * installs: spatial specs drive movement and size, effects specs drive alpha.
 *
 * Everything here funnels through [rememberReducedMotion] so a single switch — the system
 * animation scale — collapses every animation the app adds to an instant swap.
 */

/**
 * True when the user has turned system animations off (Developer options, battery saver, or an
 * accessibility preference). Read once per composition, like the platform itself does.
 */
@Composable
fun rememberReducedMotion(): Boolean = remember { !ValueAnimator.areAnimatorsEnabled() }

// --- Navigation ---

/** The shared-axis transitions [rememberNavMotion] hands to the `NavHost`. */
@Immutable
data class NavMotion(
    val topLevelEnter: EnterTransition,
    val topLevelExit: ExitTransition,
    val detailEnter: EnterTransition,
    val detailExit: ExitTransition,
    val detailPopEnter: EnterTransition,
    val detailPopExit: ExitTransition,
)

/**
 * Navigation transitions derived from the theme's [MotionScheme]. Bottom-bar destinations are
 * siblings with no direction between them, so they fade through each other; pushed detail routes
 * travel along the horizontal shared axis, and their pop transitions are the exact reversal so a
 * predictive-back gesture reads correctly while it seeks them.
 */
@Composable
fun rememberNavMotion(): NavMotion {
    val reduced = rememberReducedMotion()
    val motionScheme = MaterialTheme.motionScheme
    return remember(reduced, motionScheme) {
        if (reduced) {
            NavMotion(
                topLevelEnter = EnterTransition.None,
                topLevelExit = ExitTransition.None,
                detailEnter = EnterTransition.None,
                detailExit = ExitTransition.None,
                detailPopEnter = EnterTransition.None,
                detailPopExit = ExitTransition.None,
            )
        } else {
            val slideSpec = motionScheme.defaultSpatialSpec<IntOffset>()
            val scaleSpec = motionScheme.defaultSpatialSpec<Float>()
            val fadeSpec = motionScheme.defaultEffectsSpec<Float>()
            // A fifth of the screen is enough to imply the axis without the empty-canvas look of
            // a full-width slide.
            NavMotion(
                topLevelEnter = fadeIn(fadeSpec) + scaleIn(scaleSpec, initialScale = 0.94f),
                topLevelExit = fadeOut(fadeSpec),
                detailEnter = slideInHorizontally(slideSpec) { it / 5 } + fadeIn(fadeSpec),
                detailExit = slideOutHorizontally(slideSpec) { -it / 5 } + fadeOut(fadeSpec),
                detailPopEnter = slideInHorizontally(slideSpec) { -it / 5 } + fadeIn(fadeSpec),
                detailPopExit = slideOutHorizontally(slideSpec) { it / 5 } + fadeOut(fadeSpec),
            )
        }
    }
}

// --- Hero shared element ---

/**
 * Scopes for the app's single hero shared-element pair (Home's GPA figures ↔ the Grades GPA card).
 * Only the two destinations that take part provide them; [heroSharedBounds] is a no-op elsewhere,
 * which keeps previews and the rest of the app free of shared-transition plumbing.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The per-destination [AnimatedVisibilityScope] the `NavHost` composes each route in. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Stable key for the one hero pair the app animates. */
const val HERO_GPA_KEY = "hero-gpa-summary"

/**
 * Marks this element as one half of the hero pair identified by [key], morphing its bounds into
 * the matching element on the destination screen. Falls back to the plain modifier outside the
 * participating destinations and under reduced motion.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.heroSharedBounds(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedScope.current
    val reduced = rememberReducedMotion()
    val motionScheme = MaterialTheme.motionScheme
    if (sharedScope == null || animatedScope == null || reduced) return this

    val boundsTransform = remember(motionScheme) {
        BoundsTransform { _, _ -> motionScheme.defaultSpatialSpec() }
    }
    // The two halves hold the same figures in different layouts, so scale rather than remeasure:
    // the text never reflows mid-flight.
    val resizeMode = remember {
        SharedTransitionScope.ResizeMode.scaleToBounds(
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.Center,
        )
    }
    return with(sharedScope) {
        this@heroSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animatedScope,
            enter = fadeIn(motionScheme.defaultEffectsSpec()),
            exit = fadeOut(motionScheme.defaultEffectsSpec()),
            boundsTransform = boundsTransform,
            resizeMode = resizeMode,
        )
    }
}

// --- Lists ---

/**
 * Appearance and placement animation for a lazy-list item, so appended pages fade in and
 * regrouped rows slide to their new slot. Empty under reduced motion.
 */
@Composable
fun LazyItemScope.itemMotion(): Modifier {
    if (rememberReducedMotion()) return Modifier
    val motionScheme = MaterialTheme.motionScheme
    return Modifier.animateItem(
        fadeInSpec = motionScheme.defaultEffectsSpec(),
        placementSpec = motionScheme.defaultSpatialSpec(),
        fadeOutSpec = motionScheme.defaultEffectsSpec(),
    )
}

// --- Values ---

/**
 * Text that swaps with a vertical slide whenever it changes, in the direction implied by [value]:
 * a figure that went up scrolls in from below, one that went down scrolls in from above. [text] is
 * the formatted label to show — [value] only picks the direction. Renders a plain [Text] under
 * reduced motion.
 */
@Composable
fun AnimatedValueText(
    value: Double?,
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
) {
    if (rememberReducedMotion()) {
        Text(text, style = style, fontWeight = fontWeight, color = color, modifier = modifier)
        return
    }
    val motionScheme = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = value to text,
        transitionSpec = {
            val direction = if ((targetState.first ?: 0.0) >= (initialState.first ?: 0.0)) 1 else -1
            val slideSpec = motionScheme.defaultSpatialSpec<IntOffset>()
            val fadeSpec = motionScheme.defaultEffectsSpec<Float>()
            (slideInVertically(slideSpec) { it * direction } + fadeIn(fadeSpec)) togetherWith
                (slideOutVertically(slideSpec) { -it * direction } + fadeOut(fadeSpec))
        },
        modifier = modifier,
        label = "animated-value",
    ) { (_, label) ->
        Text(label, style = style, fontWeight = fontWeight, color = color)
    }
}
