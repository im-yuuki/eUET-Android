package org.eu.june8th.euet

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Roboto Flex is a variable font: rather than shipping one file per weight, a
// single file exposes continuous axes. `wght` spans 100..1000, so Expressive's
// heavy display weights are reachable from the same resource. Variation
// settings need API 26+, which minSdk 35 already guarantees.
private fun robotoFlex(weight: FontWeight, opticalSize: TextUnit) = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            // Optical sizing tracks the rendered size so large display text gets
            // tighter, higher-contrast letterforms while small text stays open.
            FontVariation.opticalSizing(opticalSize),
        ),
    ),
)

private fun expressiveStyle(
    weight: FontWeight,
    size: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = robotoFlex(weight = weight, opticalSize = size),
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

val ExpressiveTypography = Typography(
    displayLarge = expressiveStyle(FontWeight.Black, 57.sp, 64.sp, (-0.25).sp),
    displayMedium = expressiveStyle(FontWeight.Black, 45.sp, 52.sp),
    displaySmall = expressiveStyle(FontWeight.ExtraBold, 36.sp, 44.sp),
    headlineLarge = expressiveStyle(FontWeight.ExtraBold, 32.sp, 40.sp),
    headlineMedium = expressiveStyle(FontWeight.ExtraBold, 28.sp, 36.sp),
    headlineSmall = expressiveStyle(FontWeight.Bold, 24.sp, 32.sp),
    titleLarge = expressiveStyle(FontWeight.Bold, 22.sp, 28.sp),
    titleMedium = expressiveStyle(FontWeight.SemiBold, 16.sp, 24.sp, 0.1.sp),
    titleSmall = expressiveStyle(FontWeight.SemiBold, 14.sp, 20.sp, 0.1.sp),
    bodyLarge = expressiveStyle(FontWeight.Normal, 16.sp, 24.sp, 0.5.sp),
    bodyMedium = expressiveStyle(FontWeight.Normal, 14.sp, 20.sp, 0.25.sp),
    bodySmall = expressiveStyle(FontWeight.Normal, 12.sp, 16.sp, 0.4.sp),
    labelLarge = expressiveStyle(FontWeight.Bold, 14.sp, 20.sp, 0.1.sp),
    labelMedium = expressiveStyle(FontWeight.SemiBold, 12.sp, 16.sp, 0.5.sp),
    labelSmall = expressiveStyle(FontWeight.Bold, 11.sp, 16.sp, 0.5.sp),
)
