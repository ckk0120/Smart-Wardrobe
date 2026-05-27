package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BeigeGreenPurpleColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = Color.White,
    secondary = AmethystPurple,
    onSecondary = Color.White,
    tertiary = TertiaryRustAccent,
    onTertiary = Color.White,
    background = ElegantBeigeBg,
    onBackground = SoftCharcoalText,
    surface = ElegantBeigeSurface,
    onSurface = SoftCharcoalText,
    surfaceVariant = ElegantBeigeVariant,
    onSurfaceVariant = DarkMutedBrown,
    outline = ElegantBorderGold,
    outlineVariant = ElegantBeigeVariant.copy(alpha = 0.5f)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Ignored to strictly preserve the high-end warm beige, green & purple brand identity
    dynamicColor: Boolean = false, // Enforce our custom palette consistently
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BeigeGreenPurpleColorScheme,
        typography = Typography,
        content = content
    )
}
