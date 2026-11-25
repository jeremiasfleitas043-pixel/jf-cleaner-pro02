package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val JfDarkColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = PureBlack,
    primaryContainer = EmeraldDarkGlow,
    onPrimaryContainer = CyberMint,
    secondary = EmeraldPrimary,
    onSecondary = PureBlack,
    secondaryContainer = CardElevatedGray,
    onSecondaryContainer = TextPrimary,
    tertiary = TealEnergy,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = CardDarkGray,
    onSurface = TextPrimary,
    surfaceVariant = CardElevatedGray,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderSubtle,
    error = DangerCoral,
    onError = PureBlack
)

val JfCleanerShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JfCleanerTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = JfDarkColorScheme,
        typography = JfCleanerTypography,
        shapes = JfCleanerShapes,
        content = content
    )
}
