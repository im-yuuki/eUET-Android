package me.june8th.euet.app.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App theme built on Material 3 Expressive with dynamic color (Material You). minSdk 34 guarantees
 * dynamic color is available, so the app tints itself from the user's wallpaper — the Pixel-native
 * look. The UET-blue [LightColors]/[DarkColors] are the fallback when dynamic color is disabled.
 *
 * [MaterialExpressiveTheme] supplies the expressive baseline (springy motion scheme, updated
 * component defaults); expressive motion still respects the system animation scale, so
 * reduced-motion users get instant transitions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EUetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
