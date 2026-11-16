package com.example.a3dmausecad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.a3dmausecad.network.Network3DMouseManager
import com.example.a3dmausecad.ui.screen.TouchpadScreen
import com.example.a3dmausecad.ui.theme._3DMAUSECADTheme

class MainActivity : ComponentActivity() {

    private lateinit var networkManager: Network3DMouseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system bars for immersive fullscreen touchpad experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        networkManager = Network3DMouseManager(applicationContext)

        enableEdgeToEdge()
        setContent {
            _3DMAUSECADTheme {
                val prefs = remember { applicationContext.getSharedPreferences("3d_mouse_prefs", MODE_PRIVATE) }
                var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("keep_screen_on", true)) }

                LaunchedEffect(keepScreenOn) {
                    if (keepScreenOn) {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    TouchpadScreen(
                        networkManager = networkManager,
                        hasPermissions = true,
                        keepScreenOn = keepScreenOn,
                        onKeepScreenOnChanged = { newStatus: Boolean ->
                            keepScreenOn = newStatus
                            prefs.edit().putBoolean("keep_screen_on", newStatus).apply()
                        },
                        onRequestPermissions = {}
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::networkManager.isInitialized) {
            networkManager.disconnect()
        }
    }
}
