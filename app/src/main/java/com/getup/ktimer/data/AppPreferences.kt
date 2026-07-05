package com.getup.ktimer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private inline fun <reified T : Enum<T>> safeValueOf(name: String?, default: T): T {
    if (name == null) return default
    return try {
        java.lang.Enum.valueOf(T::class.java, name)
    } catch (e: IllegalArgumentException) {
        default
    }
}

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
            appTheme = safeValueOf(prefs.getString("appTheme", AppTheme.DARK.name), AppTheme.DARK),
            soundMode = safeValueOf(prefs.getString("soundMode", com.getup.ktimer.data.SoundMode.RINGER.name), com.getup.ktimer.data.SoundMode.RINGER),
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
            state = safeValueOf(prefs.getString("currentState", TimerState.READY.name), TimerState.READY),
            pausedState = safeValueOf(prefs.getString("pausedState", TimerState.WORK.name), TimerState.WORK),
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

    // Daily activity log, used for the streak/history chart. Bounded to the
    // last 30 days so this preference entry can't grow unbounded.
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun getDailyLogs(): List<DailyLog> {
        val raw = prefs.getString("dailyLogs", null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<DailyLog>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDailyLogs(logs: List<DailyLog>) {
        prefs.edit().putString("dailyLogs", Json.encodeToString(logs)).apply()
    }

    fun recordTaskEvent(completed: Boolean) {
        val today = dayFormat.format(Calendar.getInstance().time)
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time
        val logs = getDailyLogs().toMutableList()
        val existingIndex = logs.indexOfFirst { it.date == today }

        val updatedEntry = if (existingIndex >= 0) {
            val entry = logs[existingIndex]
            if (completed) entry.copy(completed = entry.completed + 1) else entry.copy(skipped = entry.skipped + 1)
        } else {
            if (completed) DailyLog(today, completed = 1) else DailyLog(today, skipped = 1)
        }

        if (existingIndex >= 0) logs[existingIndex] = updatedEntry else logs.add(updatedEntry)

        val pruned = logs.filter { entry ->
            try {
                dayFormat.parse(entry.date)?.after(cutoff) == true
            } catch (e: Exception) {
                false
            }
        }
        saveDailyLogs(pruned)
    }

    /** Last [days] days including today, oldest first, with zero-filled gaps for days with no activity. */
    fun getRecentActivity(days: Int = 7): List<DailyLog> {
        val logs = getDailyLogs().associateBy { it.date }
        val calendar = Calendar.getInstance()
        val result = mutableListOf<DailyLog>()
        repeat(days) { offset ->
            val date = dayFormat.format(calendar.time)
            result.add(logs[date] ?: DailyLog(date))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return result.reversed()
    }
}
