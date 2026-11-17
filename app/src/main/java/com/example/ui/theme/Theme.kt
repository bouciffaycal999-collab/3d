package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaOnContainer,
    secondary = OliveStoneSecondary,
    onSecondary = OliveStoneOnSecondary,
    secondaryContainer = OliveStoneContainer,
    onSecondaryContainer = OliveStoneOnContainer,
    tertiary = WarmBrassTertiary,
    tertiaryContainer = WarmBrassContainer,
    background = AlabasterBackground,
    onBackground = CharcoalInk,
    surface = StudioSurface,
    onSurface = CharcoalInk,
    surfaceVariant = TravertineSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = StoneOutline,
    outlineVariant = StoneOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkTerracottaPrimary,
    onPrimary = DarkTerracottaOnPrimary,
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = DarkTerracottaOnContainer,
    secondary = DarkOliveSecondary,
    secondaryContainer = DarkOliveContainer,
    background = DarkStudioBackground,
    onBackground = DarkInk,
    surface = DarkStudioSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkStudioSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
)

val TileCraftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun TileCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TileCraftTypography,
        shapes = TileCraftShapes,
        content = content,
    )
}
