package com.getup.ktimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.getup.ktimer.MainActivity
import com.getup.ktimer.R
import com.getup.ktimer.data.AppPreferences
import com.getup.ktimer.data.AppTheme
import com.getup.ktimer.data.TimerState
import com.getup.ktimer.data.getUpcomingTask
import com.getup.ktimer.data.getCurrentTaskTitle
import com.getup.ktimer.service.TimerService

class GetUpWidgetProviderLarge : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = AppPreferences(context)
        val status = prefs.getStatus()
        val settings = prefs.getSettings()

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, status, settings)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            status: com.getup.ktimer.data.AppStatus,
            settings: com.getup.ktimer.data.AppSettings
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_large)

            val colors = WidgetUtils.getWidgetColors(settings, status)
            views.setInt(R.id.widget_root, "setBackgroundColor", colors.first)
            views.setTextColor(R.id.widget_title, colors.second)
            views.setTextColor(R.id.widget_time, colors.second)
            views.setTextColor(R.id.widget_upcoming, WidgetUtils.getWidgetSecondaryColor(settings, status))
            views.setInt(R.id.btn_toggle, "setColorFilter", colors.second)
            views.setInt(R.id.btn_reset, "setColorFilter", colors.second)

            val title = status.getCurrentTaskTitle(settings)
            
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_upcoming, status.getUpcomingTask(settings))
            
            val m = status.remainingSeconds / 60
            val s = status.remainingSeconds % 60
            views.setTextViewText(R.id.widget_time, String.format("%02d:%02d", m, s))

            views.setImageViewResource(R.id.btn_toggle, if (status.state == TimerState.WORK || status.state == TimerState.EXERCISE || status.state == TimerState.WATER) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            
            val toggleIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_TOGGLE }
            val resetIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_RESET }

            val togglePending = PendingIntent.getService(context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val resetPending = PendingIntent.getService(context, 1, resetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.btn_toggle, togglePending)
            views.setOnClickPendingIntent(R.id.btn_reset, resetPending)

            val rootIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val rootPending = PendingIntent.getActivity(context, 0, rootIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, rootPending)

            // Progress bar
            val maxTime = if (status.state == TimerState.EXERCISE || status.state == TimerState.WATER) settings.exerciseWindowMinutes * 60 else settings.workIntervalMinutes * 60
            
            val isColorfulBg = (status.state == TimerState.EXERCISE || status.state == TimerState.WATER) && settings.appTheme != AppTheme.OLED
            val activeProgressId = if (isColorfulBg) R.id.widget_progress_white else R.id.widget_progress
            val inactiveProgressId = if (isColorfulBg) R.id.widget_progress else R.id.widget_progress_white
            
            views.setViewVisibility(activeProgressId, android.view.View.VISIBLE)
            views.setViewVisibility(inactiveProgressId, android.view.View.GONE)
            views.setProgressBar(activeProgressId, maxTime, maxTime - status.remainingSeconds, false)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
