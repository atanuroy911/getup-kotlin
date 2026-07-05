package com.getup.ktimer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getup.ktimer.data.AppPreferences
import com.getup.ktimer.data.AppSettings
import com.getup.ktimer.data.AppStatus
import com.getup.ktimer.service.TimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    private val _settings = MutableStateFlow(prefs.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val status: StateFlow<AppStatus> = TimerService.appStatusFlow

    val hasLaunched: Boolean get() = prefs.hasLaunched
    val hasSeenNotificationOnboarding: Boolean get() = prefs.hasSeenNotificationOnboarding
    val hasSeenOverlayOnboarding: Boolean get() = prefs.hasSeenOverlayOnboarding

    fun setLaunched() {
        prefs.hasLaunched = true
    }

    fun setSeenNotificationOnboarding() {
        prefs.hasSeenNotificationOnboarding = true
    }

    fun setSeenOverlayOnboarding() {
        prefs.hasSeenOverlayOnboarding = true
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.saveSettings(newSettings)
        _settings.value = newSettings
        sendCommand(TimerService.ACTION_RELOAD_SETTINGS)
    }

    fun toggleSoundMode() {
        val current = _settings.value.soundMode
        val newMode = if (current == com.getup.ktimer.data.SoundMode.RINGER) {
            com.getup.ktimer.data.SoundMode.VIBRATE
        } else {
            com.getup.ktimer.data.SoundMode.RINGER
        }
        updateSettings(_settings.value.copy(soundMode = newMode))
        
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            this.action = TimerService.ACTION_TEST_SOUND
        }
        getApplication<Application>().startService(intent)
    }

    fun toggleTimer() {
        sendCommand(TimerService.ACTION_TOGGLE)
    }

    fun pauseTimer() {
        sendCommand(TimerService.ACTION_PAUSE)
    }

    fun resumeTimer() {
        sendCommand(TimerService.ACTION_RESUME)
    }

    fun skipTask() {
        sendCommand(TimerService.ACTION_SKIP)
    }

    fun doneTask() {
        sendCommand(TimerService.ACTION_DONE)
    }

    fun resetTimer() {
        sendCommand(TimerService.ACTION_RESET)
    }

    fun exitApp() {
        sendCommand(TimerService.ACTION_EXIT)
    }

    private fun sendCommand(action: String) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            this.action = action
        }
        getApplication<Application>().startService(intent)
    }
}
