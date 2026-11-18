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
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = MountainTealTertiary,
    onTertiary = OnMountainTealTertiary,
    tertiaryContainer = MountainTealContainer,
    onTertiaryContainer = OnMountainTealContainer,
    background = WarmCreamBackground,
    onBackground = OnWarmCreamBackground,
    surface = CrispSurface,
    onSurface = OnWarmCreamBackground,
    surfaceVariant = SoftStoneSurfaceVariant,
    onSurfaceVariant = OnSoftStoneSurfaceVariant,
    outline = OutlineSubtle,
    error = AlertRed,
    errorContainer = AlertRedContainer,
    onErrorContainer = OnAlertRedContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmeraldPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkTerracottaSecondary,
    onSecondary = DarkOnTerracottaSecondary,
    secondaryContainer = DarkTerracottaContainer,
    onSecondaryContainer = DarkOnTerracottaContainer,
    tertiary = DarkMountainTeal,
    onTertiary = DarkOnMountainTeal,
    tertiaryContainer = DarkMountainTealContainer,
    onTertiaryContainer = DarkOnMountainTealContainer,
    background = DarkForestBackground,
    onBackground = DarkOnForestBackground,
    surface = DarkForestSurface,
    onSurface = DarkOnForestBackground,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = DarkOnForestSurfaceVariant,
    outline = DarkOutlineSubtle,
    error = AlertRed,
    errorContainer = AlertRedContainer,
    onErrorContainer = OnAlertRedContainer
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DumingagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
