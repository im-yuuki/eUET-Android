package me.june8th.euet.app.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App theme built on Material 3 with dynamic color (Material You). minSdk 34 guarantees dynamic
 * color is available, so the app tints itself from the user's wallpaper — the Pixel-native look.
 * The UET-blue [LightColors]/[DarkColors] are the fallback when dynamic color is disabled.
 *
 * Note: the Material 3 *Expressive* theme/components (`MaterialExpressiveTheme`, `LoadingIndicator`)
 * are marked `internal` in the resolved Compose BOM, so this uses the standard [MaterialTheme]. The
 * expressive look is approximated via generous rounded shapes, tonal surfaces and springy defaults.
 */
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

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
