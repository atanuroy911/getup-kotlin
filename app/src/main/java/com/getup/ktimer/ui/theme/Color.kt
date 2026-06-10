package com.getup.ktimer.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme
val LightBackground = Color(0xFFFAFAFA)
val LightCardBackground = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF111827) // Slate 900
val LightTextSecondary = Color(0xFF6B7280) // Gray 500
val LightDivider = Color(0xFFE5E7EB) // Gray 200

// Dark Theme
val DarkBackground = Color(0xFF0F172A) // Deep Slate
val DarkCardBackground = Color(0xFF1E293B) // Lighter Slate for Cards
val DarkTextPrimary = Color(0xFFF9FAFB) // Gray 50
val DarkTextSecondary = Color(0xFF9CA3AF) // Gray 400
val DarkDivider = Color(0xFF334155) // Slate 700

// OLED Theme
val OledBackground = Color(0xFF000000)

// Neon Accents (used in Dark/OLED)
val WorkAccent = Color(0xFF10B981) // Emerald 500
val ExerciseAccent = Color(0xFFF97316) // Orange 500
val WaterAccent = Color(0xFF06B6D4) // Cyan 500

// Light Accents
val LightWorkAccent = Color(0xFF059669) // Emerald 600
val LightExerciseAccent = Color(0xFFEA580C) // Orange 600
val LightWaterAccent = Color(0xFF0891B2) // Cyan 600

// Activity Backgrounds (subtle tint for full screen)
val DarkWorkBackground = Color(0xFF064E3B).copy(alpha = 0.3f)
val DarkExerciseBackground = Color(0xFF7C2D12).copy(alpha = 0.3f)
val DarkWaterBackground = Color(0xFF164E63).copy(alpha = 0.3f)

val LightWorkBackground = Color(0xFFD1FAE5)
val LightExerciseBackground = Color(0xFFFFEDD5)
val LightWaterBackground = Color(0xFFCFFAFE)