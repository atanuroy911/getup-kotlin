package com.getup.ktimer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.runtime.SideEffect
import com.getup.ktimer.data.AppTheme
import com.getup.ktimer.data.TimerState

data class AppColors(
    val background: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val workAccent: Color,
    val exerciseAccent: Color,
    val waterAccent: Color,
    val defaultAccent: Color,
    val workBackground: Color,
    val exerciseBackground: Color,
    val waterBackground: Color
) {
    fun getBackgroundForState(state: TimerState): Color {
        return when (state) {
            TimerState.WORK -> workBackground
            TimerState.EXERCISE -> exerciseBackground
            TimerState.WATER -> waterBackground
            else -> background
        }
    }

    fun getAccentForState(state: TimerState): Color {
        return when (state) {
            TimerState.WORK -> workAccent
            TimerState.EXERCISE -> exerciseAccent
            TimerState.WATER -> waterAccent
            else -> defaultAccent
        }
    }
}

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}

@Composable
fun GetUpTheme(
    appTheme: AppTheme = AppTheme.DARK,
    content: @Composable () -> Unit
) {
    val colors = when (appTheme) {
        AppTheme.LIGHT -> AppColors(
            background = LightBackground,
            cardBackground = LightCardBackground,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            divider = LightDivider,
            workAccent = LightWorkAccent,
            exerciseAccent = LightExerciseAccent,
            waterAccent = LightWaterAccent,
            defaultAccent = LightWorkAccent,
            workBackground = LightWorkBackground,
            exerciseBackground = LightExerciseBackground,
            waterBackground = LightWaterBackground
        )
        AppTheme.OLED -> AppColors(
            background = OledBackground,
            cardBackground = DarkCardBackground,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            divider = DarkDivider,
            workAccent = WorkAccent,
            exerciseAccent = ExerciseAccent,
            waterAccent = WaterAccent,
            defaultAccent = WorkAccent,
            workBackground = OledBackground,
            exerciseBackground = OledBackground,
            waterBackground = OledBackground
        )
        AppTheme.DARK -> AppColors(
            background = DarkBackground,
            cardBackground = DarkCardBackground,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            divider = DarkDivider,
            workAccent = WorkAccent,
            exerciseAccent = ExerciseAccent,
            waterAccent = WaterAccent,
            defaultAccent = WorkAccent,
            workBackground = DarkWorkBackground,
            exerciseBackground = DarkExerciseBackground,
            waterBackground = DarkWaterBackground
        )
    }

    val materialColors = if (appTheme == AppTheme.LIGHT) {
        lightColorScheme(
            background = colors.background,
            surface = colors.cardBackground,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            primary = colors.defaultAccent
        )
    } else {
        darkColorScheme(
            background = colors.background,
            surface = colors.cardBackground,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
            primary = colors.defaultAccent
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // The overlay's ComposeView is hosted on the application context (not an
            // Activity), so this cast must be guarded to avoid crashing the whole app
            // when the overlay is shown.
            val activity = view.context as? Activity
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = appTheme == AppTheme.LIGHT
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}