package com.getup.ktimer.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.getup.ktimer.MainActivity
import com.getup.ktimer.data.AppPreferences
import com.getup.ktimer.data.TimerState
import com.getup.ktimer.data.getUpcomingTask
import com.getup.ktimer.data.getCurrentTaskTitle
import com.getup.ktimer.data.toClockString
import com.getup.ktimer.service.TimerService
import com.getup.ktimer.ui.theme.GetUpTheme
import com.getup.ktimer.ui.theme.LocalAppColors

private class FakeLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

object OverlayManager {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var fakeOwner: FakeLifecycleOwner? = null
    private var isShowing = false

    @SuppressLint("ClickableViewAccessibility")
    fun show(activityContext: Context) {
        if (!Settings.canDrawOverlays(activityContext) || isShowing) return

        // Use the application context for the window/view so the overlay never
        // pins the Activity in memory across rotation or process death.
        val context = activityContext.applicationContext
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        composeView = ComposeView(context).apply {
            setContent {
                val status by TimerService.appStatusFlow.collectAsState()
                val prefs = AppPreferences(context)
                val settings = prefs.getSettings()

                GetUpTheme(appTheme = settings.appTheme) {
                    OverlayContent(
                        status = status,
                        settings = settings,
                        onClose = { hide() },
                        onToggle = {
                            val toggleIntent = Intent(context, TimerService::class.java).apply { action = TimerService.ACTION_TOGGLE }
                            context.startService(toggleIntent)
                        },
                        onOpenApp = {
                            hide()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            context.startActivity(intent)
                        },
                        onDrag = { dx, dy ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            windowManager?.updateViewLayout(this@apply, params)
                        }
                    )
                }
            }
        }

        fakeOwner = FakeLifecycleOwner()
        fakeOwner?.start()
        
        composeView?.setViewTreeLifecycleOwner(fakeOwner)
        composeView?.setViewTreeViewModelStoreOwner(fakeOwner)
        composeView?.setViewTreeSavedStateRegistryOwner(fakeOwner)
        
        try {
            windowManager?.addView(composeView, params)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        if (isShowing) {
            try {
                windowManager?.removeView(composeView)
            } catch (e: Exception) {}
            fakeOwner?.stop()
            fakeOwner = null
            composeView = null
            isShowing = false
        }
    }

    val isActive: Boolean get() = isShowing
}

@Composable
fun OverlayContent(
    status: com.getup.ktimer.data.AppStatus,
    settings: com.getup.ktimer.data.AppSettings,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val timeString = status.remainingSeconds.toClockString()

    val colors = LocalAppColors.current
    val bgColor = colors.background.copy(alpha = 0.95f)

    Column(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clickable { onOpenApp() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeString,
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(72.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = status.getCurrentTaskTitle(settings),
                color = colors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.widthIn(max = 100.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.clickable { onToggle() }.padding(4.dp)) {
                Icon(
                    imageVector = if (status.state == TimerState.WORK || status.state == TimerState.EXERCISE || status.state == TimerState.WATER) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Box(modifier = Modifier.clickable { onClose() }.padding(4.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = status.getUpcomingTask(settings),
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}
