package com.getup.ktimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.getup.ktimer.data.AppSettings
import com.getup.ktimer.data.AppTheme
import com.getup.ktimer.data.Exercise
import com.getup.ktimer.ui.theme.LocalAppColors
import com.getup.ktimer.ui.theme.Typography

private val DangerColor = Color(0xFFE5484D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onTestOverlay: () -> Unit,
    onDonateClick: () -> Unit = {},
    onRequestReview: () -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    var showExitConfirm by remember { mutableStateOf(false) }

    var workInterval by remember { mutableStateOf(settings.workIntervalMinutes.toString()) }
    var exerciseWindow by remember { mutableStateOf(settings.exerciseWindowMinutes.toString()) }
    var theme by remember { mutableStateOf(settings.appTheme) }
    var enableNotifs by remember { mutableStateOf(settings.enableNotifications) }
    var enableOverlay by remember { mutableStateOf(settings.enableOverlay) }
    var enableWaterBreak by remember { mutableStateOf(settings.enableWaterBreak) }
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var showUpdatedDialog by remember { mutableStateOf(false) }
    LaunchedEffect(showUpdatedDialog) {
        if (showUpdatedDialog) {
            delay(1500)
            showUpdatedDialog = false
        }
    }
    
    // Dynamic Exercises List
    val exerciseList = remember { mutableStateListOf(*settings.exercises.toTypedArray()) }

    fun buildSettings(newTheme: AppTheme = theme): AppSettings {
        return settings.copy(
            workIntervalMinutes = workInterval.toIntOrNull() ?: 10,
            exerciseWindowMinutes = exerciseWindow.toIntOrNull() ?: 5,
            appTheme = newTheme,
            enableNotifications = enableNotifs,
            enableOverlay = enableOverlay,
            enableWaterBreak = enableWaterBreak,
            exercises = exerciseList.toList()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = Typography.titleLarge, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intervals Card
            SettingsCard(title = "Intervals", icon = Icons.Default.Timer, colors = colors) {
                Text("Configure how long you focus and how long you take breaks.", style = Typography.bodySmall, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = workInterval,
                    onValueChange = { workInterval = it.filter { c -> c.isDigit() } },
                    label = { Text("Work Interval (Minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.defaultAccent,
                        unfocusedBorderColor = colors.divider
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = exerciseWindow,
                    onValueChange = { exerciseWindow = it.filter { c -> c.isDigit() } },
                    label = { Text("Exercise Window (Minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.defaultAccent,
                        unfocusedBorderColor = colors.divider
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        keyboardController?.hide()
                        onSave(buildSettings())
                        showUpdatedDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.defaultAccent)
                ) { Text("Update Intervals", style = Typography.titleMedium, color = Color.White) }
            }

            // Exercises Card
            SettingsCard(title = "Exercises", icon = Icons.Default.FitnessCenter, colors = colors) {
                Text("Customize your exercise routine when taking a break.", style = Typography.bodySmall, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                
                exerciseList.forEachIndexed { index, exercise ->
                    ExerciseInput(
                        label = "Exercise ${index + 1}",
                        name = exercise.name,
                        reps = exercise.reps.toString(),
                        onNameChange = { exerciseList[index] = exerciseList[index].copy(name = it) },
                        onRepsChange = { exerciseList[index] = exerciseList[index].copy(reps = it.toIntOrNull() ?: 10) },
                        onRemove = { exerciseList.removeAt(index) },
                        colors = colors
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { exerciseList.add(Exercise("", 10)) },
                        modifier = Modifier
                            .background(colors.defaultAccent.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise", tint = colors.defaultAccent)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        keyboardController?.hide()
                        onSave(buildSettings())
                        showUpdatedDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.defaultAccent)
                ) { Text("Update Exercises", style = Typography.titleMedium, color = Color.White) }
            }

            // Appearance Card
            SettingsCard(title = "Appearance", icon = Icons.Default.ColorLens, colors = colors) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTheme.entries.forEach { t ->
                        val isSelected = theme == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) colors.defaultAccent.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) colors.defaultAccent else colors.divider,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { 
                                    theme = t
                                    onSave(buildSettings(newTheme = t))
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null, // handled by Box clickable
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.defaultAccent),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = t.name, style = Typography.bodyMedium, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }

            // Features Card
            SettingsCard(title = "Features", icon = Icons.Default.Timer, colors = colors) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Sound Notifications", color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = enableNotifs, 
                        onCheckedChange = { 
                            enableNotifs = it
                            onSave(buildSettings())
                        }, 
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.defaultAccent, checkedTrackColor = colors.defaultAccent.copy(alpha = 0.5f))
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Picture-in-Picture Overlay", color = colors.textPrimary)
                        Text("Show a floating timer when outside the app.", style = Typography.bodySmall, color = colors.textSecondary)
                    }
                    Switch(
                        checked = enableOverlay, 
                        onCheckedChange = { 
                            enableOverlay = it
                            onSave(buildSettings())
                        }, 
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.defaultAccent, checkedTrackColor = colors.defaultAccent.copy(alpha = 0.5f))
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Water Break", color = colors.textPrimary)
                        Text("Reminds you to drink water every 4th break.", style = Typography.bodySmall, color = colors.textSecondary)
                    }
                    Switch(
                        checked = enableWaterBreak, 
                        onCheckedChange = { 
                            enableWaterBreak = it
                            onSave(buildSettings())
                        }, 
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.defaultAccent, checkedTrackColor = colors.defaultAccent.copy(alpha = 0.5f))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onTestOverlay,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.defaultAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.defaultAccent)
                ) { Text("Test Overlay", style = Typography.titleMedium) }
            }

            // Support Card
            SettingsCard(title = "Support Development", icon = Icons.Default.Favorite, colors = colors) {
                Text("If you find this app helpful, consider supporting its development!", style = Typography.bodyMedium, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDonateClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.defaultAccent)
                ) { Text("Donate", style = Typography.titleMedium, color = Color.White) }
            }

            // About Card
            SettingsCard(title = "About", icon = Icons.Default.Info, colors = colors) {
                Text("Developer", style = Typography.bodyMedium, color = colors.textSecondary)
                Text("Atanu Shuvam Roy", style = Typography.bodyLarge, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "atanusroy.com",
                    style = Typography.bodyLarge,
                    color = colors.defaultAccent,
                    modifier = Modifier.clickable { uriHandler.openUri("https://atanusroy.com") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestReview,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.defaultAccent)
                ) { Text("Rate on Play Store", style = Typography.titleMedium, color = Color.White) }
            }

            // Exit Card
            SettingsCard(title = "Quit", icon = Icons.Default.ExitToApp, colors = colors) {
                Text(
                    "Stops the timer, removes the background service and notification, and closes the app completely.",
                    style = Typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showExitConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerColor)
                ) { Text("Exit App", style = Typography.titleMedium) }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showExitConfirm) {
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = DangerColor) },
                title = { Text("Exit GetUp?", color = colors.textPrimary) },
                text = {
                    Text(
                        "This stops the timer completely and closes the app. It will not send you any more reminders until you reopen it.",
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExitConfirm = false
                        onExitApp()
                    }) {
                        Text("Exit", color = DangerColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirm = false }) {
                        Text("Cancel", color = colors.defaultAccent)
                    }
                },
                containerColor = colors.cardBackground,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary
            )
        }

        if (showUpdatedDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // Intercept clicks
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(colors.cardBackground, RoundedCornerShape(16.dp))
                        .padding(32.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = colors.defaultAccent, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Updated", style = Typography.titleLarge, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: com.getup.ktimer.ui.theme.AppColors,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = colors.defaultAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = Typography.titleLarge, color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ExerciseInput(
    label: String,
    name: String,
    reps: String,
    onNameChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRemove: () -> Unit,
    colors: com.getup.ktimer.ui.theme.AppColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(label) },
            modifier = Modifier.weight(2f),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.defaultAccent,
                unfocusedBorderColor = colors.divider
            )
        )
        OutlinedTextField(
            value = reps,
            onValueChange = { onRepsChange(it.filter { c -> c.isDigit() }) },
            label = { Text("Reps") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.defaultAccent,
                unfocusedBorderColor = colors.divider
            )
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = colors.textSecondary)
        }
    }
}
