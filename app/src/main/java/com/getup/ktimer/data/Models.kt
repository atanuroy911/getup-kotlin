package com.getup.ktimer.data

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val name: String,
    val reps: Int
)

enum class AppTheme {
    LIGHT, DARK, OLED
}

enum class SoundMode {
    RINGER, VIBRATE
}

data class AppSettings(
    val workIntervalMinutes: Int = 10,
    val exerciseWindowMinutes: Int = 5,
    val notificationSound: String = "beep",
    val appTheme: AppTheme = AppTheme.DARK,
    val enableNotifications: Boolean = true,
    val soundMode: SoundMode = SoundMode.RINGER,
    val enableOverlay: Boolean = true,
    val enableWaterBreak: Boolean = true,
    val exercises: List<Exercise> = listOf(
        Exercise("Pushups", 10),
        Exercise("Squats", 10),
        Exercise("Burpees", 5)
    )
)

enum class TimerState {
    READY, PAUSED, WORK, EXERCISE, WATER
}

data class AppStatus(
    val state: TimerState = TimerState.READY,
    val pausedState: TimerState = TimerState.WORK,
    val completedTasks: Int = 0,
    val skippedTasks: Int = 0,
    val remainingSeconds: Int = 600,
    val cycleIndex: Int = 0,
    val breakCount: Int = 0
)

fun AppStatus.getUpcomingTask(settings: AppSettings): String {
    val effectiveState = if (state == TimerState.PAUSED) pausedState else state
    return when (effectiveState) {
        TimerState.READY, TimerState.WORK, TimerState.PAUSED -> {
            if ((breakCount + 1) % 4 == 0 && settings.enableWaterBreak) {
                "Upcoming: Hydrate"
            } else {
                val exIndex = if (settings.exercises.isNotEmpty()) cycleIndex % settings.exercises.size else 0
                val ex = settings.exercises.getOrNull(exIndex)
                "Upcoming: ${ex?.reps ?: ""} ${ex?.name ?: ""}".trim()
            }
        }
        TimerState.EXERCISE, TimerState.WATER -> "Upcoming: Deep Work"
    }
}

fun AppStatus.getCurrentTaskTitle(settings: AppSettings): String {
    val effectiveState = if (state == TimerState.PAUSED) pausedState else state
    return when (effectiveState) {
        TimerState.READY -> "Ready to Focus"
        TimerState.WORK -> "Deep Work"
        TimerState.EXERCISE -> {
            val exIndex = if (settings.exercises.isNotEmpty()) cycleIndex % settings.exercises.size else 0
            val ex = settings.exercises.getOrNull(exIndex)
            "${ex?.reps ?: ""} ${ex?.name ?: ""}".trim()
        }
        TimerState.WATER -> "Hydrate"
        TimerState.PAUSED -> "Paused"
    }
}
