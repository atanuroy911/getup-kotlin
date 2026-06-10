package com.getup.ktimer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("getup_prefs", Context.MODE_PRIVATE)

    // Onboarding
    var hasLaunched: Boolean
        get() = prefs.getBoolean("hasLaunched", false)
        set(value) = prefs.edit().putBoolean("hasLaunched", value).apply()

    var hasSeenNotificationOnboarding: Boolean
        get() = prefs.getBoolean("hasSeenNotificationOnboarding", false)
        set(value) = prefs.edit().putBoolean("hasSeenNotificationOnboarding", value).apply()

    var hasSeenOverlayOnboarding: Boolean
        get() = prefs.getBoolean("hasSeenOverlayOnboarding", false)
        set(value) = prefs.edit().putBoolean("hasSeenOverlayOnboarding", value).apply()

    // Settings
    fun getSettings(): AppSettings {
        val exercisesStr = prefs.getString("exercises", null)
        val exercises = if (exercisesStr != null) {
            try {
                Json.decodeFromString<List<Exercise>>(exercisesStr)
            } catch (e: Exception) {
                AppSettings().exercises
            }
        } else {
            AppSettings().exercises
        }

        return AppSettings(
            workIntervalMinutes = prefs.getInt("workIntervalMinutes", 10),
            exerciseWindowMinutes = prefs.getInt("exerciseWindowMinutes", 5),
            notificationSound = prefs.getString("notificationSound", "beep") ?: "beep",
            appTheme = AppTheme.valueOf(prefs.getString("appTheme", AppTheme.DARK.name) ?: AppTheme.DARK.name),
            soundMode = com.getup.ktimer.data.SoundMode.valueOf(prefs.getString("soundMode", com.getup.ktimer.data.SoundMode.RINGER.name) ?: com.getup.ktimer.data.SoundMode.RINGER.name),
            enableNotifications = prefs.getBoolean("enableNotifications", true),
            enableOverlay = prefs.getBoolean("enableOverlay", true),
            enableWaterBreak = prefs.getBoolean("enableWaterBreak", true),
            exercises = exercises
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putInt("workIntervalMinutes", settings.workIntervalMinutes)
            .putInt("exerciseWindowMinutes", settings.exerciseWindowMinutes)
            .putString("notificationSound", settings.notificationSound)
            .putString("appTheme", settings.appTheme.name)
            .putString("soundMode", settings.soundMode.name)
            .putBoolean("enableNotifications", settings.enableNotifications)
            .putBoolean("enableOverlay", settings.enableOverlay)
            .putBoolean("enableWaterBreak", settings.enableWaterBreak)
            .putString("exercises", Json.encodeToString(settings.exercises))
            .apply()
    }

    // State
    fun getStatus(): AppStatus {
        return AppStatus(
            state = TimerState.valueOf(prefs.getString("currentState", TimerState.READY.name) ?: TimerState.READY.name),
            pausedState = TimerState.valueOf(prefs.getString("pausedState", TimerState.WORK.name) ?: TimerState.WORK.name),
            completedTasks = prefs.getInt("completedTasks", 0),
            skippedTasks = prefs.getInt("skippedTasks", 0),
            remainingSeconds = prefs.getInt("remainingSeconds", 600),
            cycleIndex = prefs.getInt("cycleIndex", 0),
            breakCount = prefs.getInt("breakCount", 0)
        )
    }

    fun saveStatus(status: AppStatus) {
        prefs.edit()
            .putString("currentState", status.state.name)
            .putString("pausedState", status.pausedState.name)
            .putInt("completedTasks", status.completedTasks)
            .putInt("skippedTasks", status.skippedTasks)
            .putInt("remainingSeconds", status.remainingSeconds)
            .putInt("cycleIndex", status.cycleIndex)
            .putInt("breakCount", status.breakCount)
            .apply()
    }
}
