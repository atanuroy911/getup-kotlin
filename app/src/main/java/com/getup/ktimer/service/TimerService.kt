package com.getup.ktimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.getup.ktimer.MainActivity
import com.getup.ktimer.data.AppPreferences
import com.getup.ktimer.data.AppSettings
import com.getup.ktimer.data.AppStatus
import com.getup.ktimer.data.TimerState
import com.getup.ktimer.data.toClockString
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

class TimerService : Service() {

    companion object {
        const val ACTION_PAUSE = "com.getup.ktimer.PAUSE"
        const val ACTION_RESUME = "com.getup.ktimer.RESUME"
        const val ACTION_TOGGLE = "com.getup.ktimer.TOGGLE"
        const val ACTION_RESET = "com.getup.ktimer.RESET"
        const val ACTION_RELOAD_SETTINGS = "com.getup.ktimer.RELOAD_SETTINGS"
        const val ACTION_RELOAD_AND_RESET = "com.getup.ktimer.RELOAD_AND_RESET"
        const val ACTION_SKIP = "com.getup.ktimer.SKIP"
        const val ACTION_DONE = "com.getup.ktimer.DONE"
        const val ACTION_TEST_SOUND = "com.getup.ktimer.TEST_SOUND"
        const val ACTION_EXIT = "com.getup.ktimer.EXIT"

        private const val NOTIFICATION_CHANNEL_ID = "get_up_channel_id"
        private const val NOTIFICATION_ID = 1

        private val _appStatusFlow = MutableStateFlow(AppStatus())
        val appStatusFlow: StateFlow<AppStatus> = _appStatusFlow.asStateFlow()
    }

    private lateinit var prefs: AppPreferences
    private lateinit var settings: AppSettings
    private var status = AppStatus()

    private var targetTimeMs: Long? = null
    private var bellPlayed = false
    private var isTransitioning = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private val activeMediaPlayers = mutableListOf<MediaPlayer>()

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        settings = prefs.getSettings()
        status = prefs.getStatus()

        // Handle initial load
        if (status.state == TimerState.READY) {
            status = status.copy(remainingSeconds = settings.workIntervalMinutes * 60)
            prefs.saveStatus(status)
        } else if (status.state == TimerState.PAUSED && status.pausedState == TimerState.PAUSED) {
            status = status.copy(pausedState = TimerState.WORK, remainingSeconds = settings.workIntervalMinutes * 60)
            prefs.saveStatus(status)
        }

        if (status.state != TimerState.PAUSED && status.state != TimerState.READY) {
            targetTimeMs = System.currentTimeMillis() + (status.remainingSeconds * 1000L)
        }

        _appStatusFlow.value = status

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        startTimerLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_PAUSE -> handlePause()
                ACTION_RESUME -> handleResume()
                ACTION_TOGGLE -> handleToggle()
                ACTION_RESET -> handleReset()
                ACTION_RELOAD_SETTINGS -> handleReloadSettings()
                ACTION_RELOAD_AND_RESET -> handleReloadAndReset()
                ACTION_SKIP -> handleSkipOrDone(isSkip = true)
                ACTION_DONE -> handleSkipOrDone(isSkip = false)
                ACTION_TEST_SOUND -> handleTestSound()
                ACTION_EXIT -> handleExit()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        prefs.saveStatus(status)
        serviceScope.cancel()
        synchronized(activeMediaPlayers) {
            activeMediaPlayers.forEach { runCatching { it.stop(); it.release() } }
            activeMediaPlayers.clear()
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                tick()
                delay(1000)
            }
        }
    }

    private suspend fun tick() {
        if (status.state == TimerState.PAUSED || status.state == TimerState.READY || targetTimeMs == null) {
            _appStatusFlow.value = status
            updateNotification()
            updateWidgets()
            return
        }

        val now = System.currentTimeMillis()
        val diffSeconds = ((targetTimeMs!! - now) / 1000.0).toInt()
        
        status = status.copy(remainingSeconds = maxOf(0, diffSeconds))

        // Play bell 3 seconds before work ends (<= guards against tick drift skipping exactly 3)
        if (status.state == TimerState.WORK && status.remainingSeconds in 1..3 && !bellPlayed) {
            bellPlayed = true
            playBell()
        }

        if (status.remainingSeconds <= 0) {
            status = status.copy(remainingSeconds = 0)
            if (!isTransitioning) {
                isTransitioning = true
                transitionState()
                isTransitioning = false
            }
        } else {
            if (status.remainingSeconds % 5 == 0) {
                prefs.saveStatus(status)
            }
        }

        _appStatusFlow.value = status
        updateNotification()
        updateWidgets()
    }

    private fun transitionState() {
        var nextState = status.state
        var nextSeconds = status.remainingSeconds
        var nextCycle = status.cycleIndex
        var nextBreakCount = status.breakCount

        if (status.state == TimerState.WORK) {
            nextBreakCount = status.breakCount + 1
            if (nextBreakCount % 4 == 0 && settings.enableWaterBreak) {
                nextState = TimerState.WATER
            } else {
                nextState = TimerState.EXERCISE
            }
            nextSeconds = settings.exerciseWindowMinutes * 60
            targetTimeMs = System.currentTimeMillis() + (nextSeconds * 1000L)
            
            if (!com.getup.ktimer.overlay.OverlayManager.isActive && !MainActivity.isAppInForeground) {
                showHighPriorityAlert(nextState)
            }
        } else if (status.state == TimerState.EXERCISE || status.state == TimerState.WATER) {
            nextState = TimerState.WORK
            nextSeconds = settings.workIntervalMinutes * 60
            targetTimeMs = System.currentTimeMillis() + (nextSeconds * 1000L)
            bellPlayed = false
            if (status.state == TimerState.EXERCISE) {
                nextCycle = status.cycleIndex + 1
            }
        }

        status = status.copy(state = nextState, remainingSeconds = nextSeconds, cycleIndex = nextCycle, breakCount = nextBreakCount)
        prefs.saveStatus(status)
    }

    private fun playBell() {
        if (!settings.enableNotifications) return
        
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val shouldVibrate = audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE || 
                            audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_SILENT ||
                            settings.soundMode == com.getup.ktimer.data.SoundMode.VIBRATE
                            
        if (shouldVibrate) {
            playTripleVibration()
            return
        }

        try {
            serviceScope.launch(Dispatchers.IO) {
                for (i in 0 until 3) {
                    val player = MediaPlayer.create(this@TimerService, com.getup.ktimer.R.raw.bell)
                    synchronized(activeMediaPlayers) { activeMediaPlayers.add(player) }
                    player.setOnCompletionListener {
                        synchronized(activeMediaPlayers) { activeMediaPlayers.remove(it) }
                        it.release()
                    }
                    player.start()
                    delay(1000.milliseconds)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun forceVibrate(durationMillis: Long = 500L) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }

        if (!vibrator.hasVibrator()) return

        val effect = android.os.VibrationEffect.createOneShot(durationMillis, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    private fun playTripleVibration() {
        serviceScope.launch(Dispatchers.IO) {
            for (i in 0 until 3) {
                forceVibrate(500L)
                delay(1000.milliseconds)
            }
        }
    }

    private fun showHighPriorityAlert(newState: TimerState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                "get_up_alert_channel_v2",
                "High Priority Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows full screen alerts when it's time to move"
                setBypassDnd(true)
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(alertChannel)
        }

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (newState == TimerState.WATER) "Drink Water!" else "Time to Move!"
        
        val taskText = if (newState == TimerState.WATER) {
            "Hydrate"
        } else {
            val exIndex = if (settings.exercises.isNotEmpty()) status.cycleIndex % settings.exercises.size else 0
            val ex = settings.exercises.getOrNull(exIndex)
            "${ex?.reps ?: ""} ${ex?.name ?: ""}".trim()
        }
        
        val builder = NotificationCompat.Builder(this, "get_up_alert_channel_v2")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(taskText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        manager.notify(2, builder.build())
    }

    private fun handlePause() {
        if (status.state != TimerState.PAUSED) {
            status = status.copy(pausedState = status.state)
        }
        status = status.copy(state = TimerState.PAUSED)
        targetTimeMs = null
        prefs.saveStatus(status)
        updateAll()
    }

    private fun handleResume() {
        if (status.state == TimerState.READY) {
            status = status.copy(
                state = TimerState.WORK,
                remainingSeconds = settings.workIntervalMinutes * 60
            )
            bellPlayed = false
        } else {
            status = status.copy(state = status.pausedState)
        }
        targetTimeMs = System.currentTimeMillis() + (status.remainingSeconds * 1000L)
        prefs.saveStatus(status)
        updateAll()
    }

    private fun handleToggle() {
        if (status.state == TimerState.PAUSED) {
            status = status.copy(state = status.pausedState)
            targetTimeMs = System.currentTimeMillis() + (status.remainingSeconds * 1000L)
        } else if (status.state == TimerState.READY) {
            status = status.copy(
                state = TimerState.WORK,
                remainingSeconds = settings.workIntervalMinutes * 60
            )
            targetTimeMs = System.currentTimeMillis() + (status.remainingSeconds * 1000L)
            bellPlayed = false
        } else {
            status = status.copy(pausedState = status.state, state = TimerState.PAUSED)
            targetTimeMs = null
        }
        prefs.saveStatus(status)
        updateAll()
    }

    private fun handleReset() {
        status = status.copy(
            state = TimerState.READY,
            pausedState = TimerState.WORK,
            remainingSeconds = settings.workIntervalMinutes * 60
        )
        targetTimeMs = null
        bellPlayed = false
        prefs.saveStatus(status)
        updateAll()
    }

    private fun handleReloadSettings() {
        settings = prefs.getSettings()
        // Do not reset remainingSeconds here so the timer keeps running seamlessly.
        updateAll()
    }

    private fun handleTestSound() {
        if (settings.soundMode == com.getup.ktimer.data.SoundMode.VIBRATE) {
            forceVibrate(300L)
        } else {
            try {
                val player = MediaPlayer.create(this, com.getup.ktimer.R.raw.bell)
                synchronized(activeMediaPlayers) { activeMediaPlayers.add(player) }
                player.setOnCompletionListener {
                    synchronized(activeMediaPlayers) { activeMediaPlayers.remove(it) }
                    it.release()
                }
                player.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleReloadAndReset() {
        settings = prefs.getSettings()
        status = status.copy(
            state = TimerState.READY,
            pausedState = TimerState.WORK,
            remainingSeconds = settings.workIntervalMinutes * 60
        )
        targetTimeMs = null
        bellPlayed = false
        prefs.saveStatus(status)
        updateAll()
    }

    private fun handleSkipOrDone(isSkip: Boolean) {
        if (status.state == TimerState.EXERCISE || status.state == TimerState.WATER) {
            status = status.copy(
                remainingSeconds = 0,
                skippedTasks = if (isSkip) status.skippedTasks + 1 else status.skippedTasks,
                completedTasks = if (!isSkip) status.completedTasks + 1 else status.completedTasks
            )
            prefs.recordTaskEvent(completed = !isSkip)
            if (!isTransitioning) {
                isTransitioning = true
                transitionState()
                isTransitioning = false
                updateAll()
            }
        } else {
            // Stale/duplicate action (e.g. a delayed PendingIntent) arrived after the
            // state already moved on. Resync the UI instead of silently dropping it.
            updateAll()
        }
    }

    private fun handleExit() {
        timerJob?.cancel()
        status = status.copy(state = TimerState.READY, remainingSeconds = settings.workIntervalMinutes * 60)
        targetTimeMs = null
        prefs.saveStatus(status)
        _appStatusFlow.value = status

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(2)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateAll() {
        _appStatusFlow.value = status
        updateNotification()
        updateWidgets()
    }

    private fun updateWidgets() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
        
        val standardIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, com.getup.ktimer.widget.GetUpWidgetProvider::class.java))
        for (id in standardIds) {
            com.getup.ktimer.widget.GetUpWidgetProvider.updateAppWidget(this, appWidgetManager, id, status, settings)
        }

        val smallIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, com.getup.ktimer.widget.GetUpWidgetProviderSmall::class.java))
        for (id in smallIds) {
            com.getup.ktimer.widget.GetUpWidgetProviderSmall.updateAppWidget(this, appWidgetManager, id, status, settings)
        }

        val largeIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, com.getup.ktimer.widget.GetUpWidgetProviderLarge::class.java))
        for (id in largeIds) {
            com.getup.ktimer.widget.GetUpWidgetProviderLarge.updateAppWidget(this, appWidgetManager, id, status, settings)
        }

        val tinyIds = appWidgetManager.getAppWidgetIds(android.content.ComponentName(this, com.getup.ktimer.widget.GetUpWidgetProviderTiny::class.java))
        for (id in tinyIds) {
            com.getup.ktimer.widget.GetUpWidgetProviderTiny.updateAppWidget(this, appWidgetManager, id, status, settings)
        }
    }

    private fun formatTime(secs: Int): String = secs.toClockString()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Get Up Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the timer running in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val title = "GET UP ACTIVE"
        var text = ""

        if (status.state == TimerState.READY) {
            text = "Ready to start!"
        } else if (status.state == TimerState.WORK) {
            val nextTask = if ((status.breakCount + 1) % 4 == 0 && settings.enableWaterBreak) "Drink Water" else {
                val exIndex = if (settings.exercises.isNotEmpty()) status.cycleIndex % settings.exercises.size else 0
                val ex = settings.exercises.getOrNull(exIndex)
                "${ex?.reps ?: ""} ${ex?.name ?: ""}".trim()
            }
            text = "Next: $nextTask | ${formatTime(status.remainingSeconds)}"
        } else if (status.state == TimerState.EXERCISE) {
            val exIndex = if (settings.exercises.isNotEmpty()) status.cycleIndex % settings.exercises.size else 0
            val ex = settings.exercises.getOrNull(exIndex)
            val task = "${ex?.reps ?: ""} ${ex?.name ?: ""}".trim()
            text = "$task! Window: ${formatTime(status.remainingSeconds)}"
        } else if (status.state == TimerState.WATER) {
            text = "Drink Water! Window: ${formatTime(status.remainingSeconds)}"
        } else {
            text = "Paused"
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        val resumeIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
        val exitIntent = Intent(this, TimerService::class.java).apply { action = ACTION_EXIT }

        val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val resumePending = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val exitPending = PendingIntent.getService(this, 3, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Placeholder icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (status.state != TimerState.PAUSED && status.state != TimerState.READY) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else if (status.state == TimerState.PAUSED) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", exitPending)

        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
