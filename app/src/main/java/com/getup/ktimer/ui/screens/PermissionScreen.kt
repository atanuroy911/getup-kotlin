package com.getup.ktimer.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.getup.ktimer.ui.theme.LocalAppColors
import android.content.pm.PackageManager

@Composable
fun PermissionScreen(
    type: String, // "notification" or "overlay"
    onGrantedOrSkipped: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    
    var isGranted by remember { mutableStateOf(false) }

    // Check initial status
    LaunchedEffect(type) {
        if (type == "notification") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                isGranted = true
            }
        } else if (type == "overlay") {
            isGranted = Settings.canDrawOverlays(context)
        }
        if (isGranted) {
            onGrantedOrSkipped()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, type) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (type == "notification") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else {
                        isGranted = true
                    }
                } else if (type == "overlay") {
                    isGranted = Settings.canDrawOverlays(context)
                }
                if (isGranted) {
                    onGrantedOrSkipped()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onGrantedOrSkipped()
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = if (type == "notification") "Notification Permission" else "Overlay Permission",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (type == "notification") 
                "We need to send you notifications so you know when to exercise or drink water."
            else 
                "We use a floating bubble to show your timer over other apps while you work.",
            fontSize = 16.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (type == "notification") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onGrantedOrSkipped()
                    }
                } else if (type == "overlay") {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                    // The user will return here, we can rely on onResume in MainActivity or just let them click Skip if they get stuck.
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.defaultAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Allow", fontSize = 18.sp, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onGrantedOrSkipped,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Skip for now", fontSize = 16.sp, color = colors.textSecondary)
        }
    }
}
