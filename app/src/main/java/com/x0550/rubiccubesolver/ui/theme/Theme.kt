package com.x0550.rubiccubesolver.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Single unified color scheme used for ALL system modes
private val AppColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ObsidianBackground,
    primaryContainer = FrostedSlateSurface,
    onPrimaryContainer = CyberCyan,

    secondary = ElectricLime,
    onSecondary = ObsidianBackground,
    secondaryContainer = DarkStroke,
    onSecondaryContainer = ElectricLime,

    tertiary = VividAmber,
    onTertiary = ObsidianBackground,

    background = ObsidianBackground,
    onBackground = TextPrimary,

    surface = FrostedSlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkStroke,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = ObsidianBackground
)

@Composable
fun RubicCubeSolverTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Keep status bar and navigation bar consistently styled with the app background
                window.statusBarColor = ObsidianBackground.toArgb()
                window.navigationBarColor = ObsidianBackground.toArgb()

                // Ensure icons on the status/nav bar remain bright/white against the dark background
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = false
                windowInsetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = appTypography(),
        content = content
    )
}