package com.getup.ktimer.widget

import android.graphics.Color
import com.getup.ktimer.data.AppSettings
import com.getup.ktimer.data.AppStatus
import com.getup.ktimer.data.AppTheme
import com.getup.ktimer.data.TimerState

object WidgetUtils {
    fun getWidgetColors(settings: AppSettings, status: AppStatus): Pair<Int, Int> {
        if (status.state == TimerState.WATER) {
            return when (settings.appTheme) {
                AppTheme.LIGHT -> Pair(Color.parseColor("#00B0FF"), Color.parseColor("#FFFFFF"))
                AppTheme.DARK -> Pair(Color.parseColor("#01579B"), Color.parseColor("#FFFFFF"))
                AppTheme.OLED -> Pair(Color.parseColor("#000000"), Color.parseColor("#00B0FF"))
            }
        }
        if (status.state == TimerState.EXERCISE) {
            return when (settings.appTheme) {
                AppTheme.LIGHT -> Pair(Color.parseColor("#4CAF50"), Color.parseColor("#FFFFFF"))
                AppTheme.DARK -> Pair(Color.parseColor("#1B5E20"), Color.parseColor("#FFFFFF"))
                AppTheme.OLED -> Pair(Color.parseColor("#000000"), Color.parseColor("#4CAF50"))
            }
        }
        return when (settings.appTheme) {
            AppTheme.LIGHT -> Pair(Color.parseColor("#FAFAFA"), Color.parseColor("#111827"))
            AppTheme.DARK -> Pair(Color.parseColor("#0F172A"), Color.parseColor("#F9FAFB"))
            AppTheme.OLED -> Pair(Color.parseColor("#000000"), Color.parseColor("#FFFFFF"))
        }
    }
    
    fun getWidgetSecondaryColor(settings: AppSettings, status: AppStatus): Int {
        if (settings.appTheme == AppTheme.OLED) {
            if (status.state == TimerState.WATER) return Color.parseColor("#B300B0FF")
            if (status.state == TimerState.EXERCISE) return Color.parseColor("#B34CAF50")
            return Color.parseColor("#9CA3AF")
        }
        if (status.state == TimerState.WATER || status.state == TimerState.EXERCISE) {
            return Color.parseColor("#E0FFFFFF") // slightly transparent white
        }
        return when (settings.appTheme) {
            AppTheme.LIGHT -> Color.parseColor("#6B7280")
            AppTheme.DARK -> Color.parseColor("#9CA3AF")
            AppTheme.OLED -> Color.parseColor("#9CA3AF")
        }
    }
}
