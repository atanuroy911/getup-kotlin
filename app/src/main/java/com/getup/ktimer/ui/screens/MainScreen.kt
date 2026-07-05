package com.getup.ktimer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.getup.ktimer.data.AppPreferences
import com.getup.ktimer.data.AppSettings
import com.getup.ktimer.data.AppStatus
import com.getup.ktimer.data.DailyLog
import com.getup.ktimer.data.TimerState
import com.getup.ktimer.data.getUpcomingTask
import com.getup.ktimer.data.toClockString
import com.getup.ktimer.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.Locale
import com.getup.ktimer.ui.theme.Typography

private val DangerColor = Color(0xFFE5484D)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    status: AppStatus,
    settings: AppSettings,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    onReset: () -> Unit,
    onSettingsClick: () -> Unit,
    onToggleSoundMode: () -> Unit,
    onExitApp: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val isActivity = status.state == TimerState.EXERCISE || status.state == TimerState.WATER
    
    val bgColor = colors.getBackgroundForState(status.state)
    val accentColor = colors.getAccentForState(status.state)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        var overlayMode by remember { mutableStateOf<com.getup.ktimer.data.SoundMode?>(null) }
        var showInfoDialog by remember { mutableStateOf(false) }
        var exitConfirmStep by remember { mutableStateOf(0) }
        val context = LocalContext.current
        var weeklyActivity by remember { mutableStateOf<List<DailyLog>>(emptyList()) }

        LaunchedEffect(showInfoDialog) {
            if (showInfoDialog) {
                weeklyActivity = AppPreferences(context).getRecentActivity(7)
            }
        }

        LaunchedEffect(overlayMode) {
            if (overlayMode != null) {
                kotlinx.coroutines.delay(1500)
                overlayMode = null
            }
        }

        // Settings Icon
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.textPrimary)
        }

        // Sound/Vibrate Toggle
        IconButton(
            onClick = {
                onToggleSoundMode()
                overlayMode = if (settings.soundMode == com.getup.ktimer.data.SoundMode.VIBRATE) {
                    com.getup.ktimer.data.SoundMode.RINGER
                } else {
                    com.getup.ktimer.data.SoundMode.VIBRATE
                }
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            val icon = if (settings.soundMode == com.getup.ktimer.data.SoundMode.VIBRATE) {
                Icons.Default.Vibration
            } else {
                Icons.Default.Notifications
            }
            Icon(icon, contentDescription = "Toggle Sound Mode", tint = colors.textPrimary)
        }

        AnimatedContent(
            targetState = isActivity,
            transitionSpec = {
                fadeIn(tween(500)) togetherWith fadeOut(tween(500))
            }, label = "ScreenTransition"
        ) { activityMode ->
            if (activityMode) {
                ActivityView(status, settings, accentColor, onSkip, onDone)
            } else {
                TimerView(status, settings, accentColor, onToggle, onReset)
            }
        }

        AnimatedVisibility(
            visible = overlayMode != null,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
            exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            overlayMode?.let { mode ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.cardBackground.copy(alpha = 0.95f))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (mode == com.getup.ktimer.data.SoundMode.VIBRATE) Icons.Default.Vibration else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (mode == com.getup.ktimer.data.SoundMode.VIBRATE) "Vibrate Mode On" else "Ring Mode On",
                        style = Typography.titleLarge,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // Info Icon
        IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = "Information", tint = colors.textPrimary)
        }

        // Exit App Icon
        IconButton(
            onClick = { exitConfirmStep = 1 },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.MeetingRoom, contentDescription = "Exit App", tint = DangerColor)
        }

        if (exitConfirmStep == 1) {
            AlertDialog(
                onDismissRequest = { exitConfirmStep = 0 },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerColor) },
                title = { Text("Exit GetUp?", color = colors.textPrimary) },
                text = {
                    Text(
                        "This stops the timer completely and closes the app. It will not send you any more reminders until you reopen it.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { exitConfirmStep = 2 }) {
                        Text("Continue", color = DangerColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exitConfirmStep = 0 }) {
                        Text("Cancel", color = colors.defaultAccent)
                    }
                },
                containerColor = colors.cardBackground,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary
            )
        }

        if (exitConfirmStep == 2) {
            AlertDialog(
                onDismissRequest = { exitConfirmStep = 0 },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerColor) },
                title = { Text("Are you absolutely sure?", color = colors.textPrimary) },
                text = {
                    Text(
                        "This is your last chance to back out. Confirm to exit and stop all reminders.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        exitConfirmStep = 0
                        onExitApp()
                    }) {
                        Text("Exit", color = DangerColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exitConfirmStep = 0 }) {
                        Text("Cancel", color = colors.defaultAccent)
                    }
                },
                containerColor = colors.cardBackground,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary
            )
        }

        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                title = { Text("Your Stats & The Science", color = colors.textPrimary) },
                text = {
                    Column {
                        Text("Task Performance", style = Typography.titleMedium, color = accentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Completed: ${status.completedTasks} tasks\nSkipped: ${status.skippedTasks} tasks", color = colors.textPrimary)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Last 7 Days", style = Typography.titleMedium, color = accentColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        WeeklyActivityChart(
                            logs = weeklyActivity,
                            completedColor = colors.workAccent,
                            skippedColor = colors.exerciseAccent,
                            labelColor = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Why it matters", style = Typography.titleMedium, color = accentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Frequent micro-breaks prevent musculoskeletal disorders and relieve eye strain.\n\n" +
                            "• Routine movement increases blood flow, boosting cognitive function and sustaining focus.\n\n" +
                            "• Staying hydrated maintains energy levels, flushes toxins, and prevents headaches.",
                            color = colors.textSecondary,
                            style = Typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Got it", color = accentColor)
                    }
                },
                containerColor = colors.cardBackground,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary
            )
        }
    }
}

@Composable
fun TimerView(
    status: AppStatus,
    settings: AppSettings,
    accentColor: Color,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val colors = LocalAppColors.current
    val haptics = LocalHapticFeedback.current

    val timeString = status.remainingSeconds.toClockString()

    val label = when (status.state) {
        TimerState.READY -> "Ready to Focus"
        TimerState.WORK -> "Deep Work"
        TimerState.PAUSED -> "Paused"
        else -> ""
    }

    val maxTime = settings.workIntervalMinutes * 60
    val progress = if (maxTime > 0) status.remainingSeconds.toFloat() / maxTime else 0f

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status.state == TimerState.WORK) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (status.state == TimerState.WORK) 0.15f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = Typography.titleLarge,
            color = colors.textPrimary
        )

        if (status.cycleIndex > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cycle ${status.cycleIndex + 1}",
                style = Typography.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp)
        ) {
            // Background pulsing glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .background(accentColor.copy(alpha = pulseAlpha), shape = CircleShape)
            )

            // Background Track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                color = colors.divider.copy(alpha = 0.5f),
                strokeWidth = 16.dp,
                strokeCap = StrokeCap.Round
            )

            // Active Track
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                color = accentColor,
                strokeWidth = 16.dp,
                strokeCap = StrokeCap.Round
            )
            
            Text(
                text = timeString,
                style = Typography.displayLarge,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = status.getUpcomingTask(settings),
            style = Typography.bodyLarge,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        val quotes = listOf(
            "Every minute of focus counts.",
            "Stay hydrated, stay sharp.",
            "Movement is medicine.",
            "You're doing great!",
            "Keep the momentum going.",
            "Health and productivity go hand in hand."
        )
        var currentQuoteIndex by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while(true) {
                kotlinx.coroutines.delay(10000)
                currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
            }
        }

        AnimatedContent(
            targetState = currentQuoteIndex,
            transitionSpec = {
                fadeIn(tween(1000)) togetherWith fadeOut(tween(1000))
            }, label = "QuoteTransition"
        ) { targetIndex ->
            Text(
                text = "\"${quotes[targetIndex]}\"",
                style = Typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = colors.textSecondary.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                containerColor = accentColor,
                modifier = Modifier
                    .size(80.dp)
                    .shadow(12.dp, CircleShape),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (status.state == TimerState.WORK) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            if (status.state == TimerState.PAUSED) {
                Spacer(modifier = Modifier.width(24.dp))
                FloatingActionButton(
                    onClick = onReset,
                    containerColor = colors.cardBackground,
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(8.dp, CircleShape),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityView(
    status: AppStatus,
    settings: AppSettings,
    accentColor: Color,
    onSkip: () -> Unit,
    onDone: () -> Unit
) {
    val colors = LocalAppColors.current
    val haptics = LocalHapticFeedback.current

    val timeString = status.remainingSeconds.toClockString()

    val title = if (status.state == TimerState.WATER) "Drink Water!" else "Time to Move!"
    val task = if (status.state == TimerState.WATER) "Hydrate" else {
        val exIndex = if (settings.exercises.isNotEmpty()) status.cycleIndex % settings.exercises.size else 0
        val ex = settings.exercises.getOrNull(exIndex)
        "${ex?.reps ?: ""} ${ex?.name ?: ""}".trim()
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = Typography.headlineLarge,
            color = colors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(tween(800)) + scaleIn(initialScale = 0.8f, animationSpec = tween(800))
        ) {
            Text(
                text = task,
                style = Typography.displayMedium,
                color = accentColor,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = timeString,
            style = Typography.displayLarge,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(80.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkip()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.divider.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Skip", style = Typography.titleLarge, color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDone()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Done", style = Typography.titleLarge, color = Color.White)
            }
        }
    }
}

@Composable
fun WeeklyActivityChart(
    logs: List<DailyLog>,
    completedColor: Color,
    skippedColor: Color,
    labelColor: Color
) {
    val maxCount = (logs.maxOfOrNull { it.completed + it.skipped } ?: 0).coerceAtLeast(1)
    val weekdayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        logs.forEach { log ->
            val dayLabel = try {
                weekdayFormat.format(dateFormat.parse(log.date)!!).take(1)
            } catch (e: Exception) {
                "?"
            }
            val total = log.completed + log.skipped
            val emptyWeight = (maxCount - total).coerceAtLeast(0).toFloat()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .weight(1f)
                        .width(14.dp)
                ) {
                    if (total == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(labelColor.copy(alpha = 0.25f))
                        )
                    } else {
                        if (emptyWeight > 0f) {
                            Spacer(modifier = Modifier.weight(emptyWeight))
                        }
                        if (log.skipped > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(log.skipped.toFloat())
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(skippedColor.copy(alpha = 0.7f))
                            )
                        }
                        if (log.completed > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(log.completed.toFloat())
                                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(completedColor)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = dayLabel, style = Typography.bodySmall, color = labelColor, fontSize = 10.sp)
            }
        }
    }
}
