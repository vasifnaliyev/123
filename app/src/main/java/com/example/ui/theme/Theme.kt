package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VoxDarkColorScheme = darkColorScheme(
    primary = SignalCoral,
    onPrimary = Color(0xFF1F0702),
    primaryContainer = SignalCoralContainerDark,
    onPrimaryContainer = SignalCoralOnContainerDark,
    secondary = LiveEmerald,
    onSecondary = Color(0xFF032119),
    secondaryContainer = LiveEmeraldContainerDark,
    onSecondaryContainer = LiveEmeraldOnContainerDark,
    tertiary = WarmAmber,
    onTertiary = Color(0xFF241602),
    tertiaryContainer = WarmAmberContainerDark,
    onTertiaryContainer = WarmAmberOnContainerDark,
    background = ObsidianBg,
    onBackground = SoftWhite,
    surface = ObsidianSurface,
    onSurface = SoftWhite,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1F2738),
    error = ErrorCrimson,
    onError = Color(0xFF28040A),
    errorContainer = Color(0xFF4C1119),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val VoxLightColorScheme = lightColorScheme(
    primary = SignalCoralDark,
    onPrimary = Color.White,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = CoralOnContainerLight,
    secondary = LiveEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldOnContainerLight,
    tertiary = Color(0xFFC26E0B),
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = AmberOnContainerLight,
    background = IvoryBg,
    onBackground = IvoryInk,
    surface = IvorySurface,
    onSurface = IvoryInk,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = IvoryMutedInk,
    surfaceContainerLowest = IvorySurface,
    surfaceContainerLow = Color(0xFFF6F3EE),
    surfaceContainer = IvorySurfaceVariant,
    surfaceContainerHigh = IvorySurfaceHigh,
    outline = IvoryOutline,
    outlineVariant = Color(0xFFE5DFD7),
    error = ErrorCrimsonLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF5F0A14)
)

val VoxRelayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun VoxRelayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VoxDarkColorScheme else VoxLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoxRelayTypography,
        shapes = VoxRelayShapes,
        content = content
    )
}
