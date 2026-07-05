package com.getup.ktimer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.getup.ktimer.overlay.OverlayManager
import com.getup.ktimer.service.TimerService
import com.getup.ktimer.ui.MainViewModel
import com.getup.ktimer.ui.screens.GetStartedScreen
import com.getup.ktimer.ui.screens.MainScreen
import com.getup.ktimer.ui.screens.PermissionScreen
import com.getup.ktimer.ui.screens.SettingsScreen
import com.getup.ktimer.ui.theme.GetUpTheme
import android.provider.Settings

class MainActivity : ComponentActivity() {

    companion object {
        var isAppInForeground = false
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var playServicesManager: com.getup.ktimer.service.PlayServicesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        playServicesManager = com.getup.ktimer.service.PlayServicesManager(this)
        playServicesManager.initBilling()

        // Ensure service is running
        if (viewModel.hasLaunched) {
            val intent = Intent(this, TimerService::class.java)
            startService(intent)
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            val status by viewModel.status.collectAsState()

            GetUpTheme(appTheme = settings.appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    val startDest = if (!viewModel.hasLaunched) {
                        "get_started"
                    } else if (!viewModel.hasSeenNotificationOnboarding) {
                        "perm_notification"
                    } else if (!viewModel.hasSeenOverlayOnboarding) {
                        "perm_overlay"
                    } else {
                        "main"
                    }

                    NavHost(
                        navController = navController, 
                        startDestination = startDest,
                        enterTransition = {
                            androidx.compose.animation.slideInHorizontally(
                                initialOffsetX = { 1000 },
                                animationSpec = androidx.compose.animation.core.tween(500)
                            )
                        },
                        exitTransition = {
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { -1000 },
                                animationSpec = androidx.compose.animation.core.tween(500)
                            )
                        },
                        popEnterTransition = {
                            androidx.compose.animation.slideInHorizontally(
                                initialOffsetX = { -1000 },
                                animationSpec = androidx.compose.animation.core.tween(500)
                            )
                        },
                        popExitTransition = {
                            androidx.compose.animation.slideOutHorizontally(
                                targetOffsetX = { 1000 },
                                animationSpec = androidx.compose.animation.core.tween(500)
                            )
                        }
                    ) {
                        composable("get_started") {
                            GetStartedScreen(
                                onComplete = {
                                    viewModel.setLaunched()
                                    // Start service after launch
                                    val sIntent = Intent(this@MainActivity, TimerService::class.java)
                                    startService(sIntent)
                                    navController.navigate("perm_notification") {
                                        popUpTo("get_started") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("perm_notification") {
                            PermissionScreen(
                                type = "notification",
                                onGrantedOrSkipped = {
                                    viewModel.setSeenNotificationOnboarding()
                                    navController.navigate("perm_overlay") {
                                        popUpTo("perm_notification") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("perm_overlay") {
                            PermissionScreen(
                                type = "overlay",
                                onGrantedOrSkipped = {
                                    viewModel.setSeenOverlayOnboarding()
                                    navController.navigate("main") {
                                        popUpTo("perm_overlay") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("main") {
                            MainScreen(
                                status = status,
                                settings = settings,
                                onToggle = { viewModel.toggleTimer() },
                                onSkip = { viewModel.skipTask() },
                                onDone = { viewModel.doneTask() },
                                onReset = { viewModel.resetTimer() },
                                onSettingsClick = { navController.navigate("settings") },
                                onToggleSoundMode = { viewModel.toggleSoundMode() },
                                onExitApp = {
                                    OverlayManager.hide()
                                    viewModel.exitApp()
                                    finishAndRemoveTask()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                settings = settings,
                                onSave = { newSettings ->
                                    viewModel.updateSettings(newSettings)
                                },
                                onBack = { navController.popBackStack() },
                                onTestOverlay = {
                                    if (Settings.canDrawOverlays(this@MainActivity)) {
                                        OverlayManager.show(this@MainActivity)
                                    }
                                },
                                onDonateClick = {
                                    playServicesManager.launchDonateFlow(this@MainActivity, "donate_small")
                                },
                                onRequestReview = {
                                    playServicesManager.requestReview(this@MainActivity)
                                },
                                onExitApp = {
                                    OverlayManager.hide()
                                    viewModel.exitApp()
                                    finishAndRemoveTask()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isAppInForeground = true
        // Close overlay when app is foregrounded
        OverlayManager.hide()
        playServicesManager.checkAndStartUpdate(this, 1001)
    }

    override fun onPause() {
        super.onPause()
        isAppInForeground = false
        // Show overlay if running
        if (viewModel.settings.value.enableOverlay &&
            viewModel.status.value.state != com.getup.ktimer.data.TimerState.READY && 
            viewModel.status.value.state != com.getup.ktimer.data.TimerState.PAUSED) {
            OverlayManager.show(this)
        }
    }
}