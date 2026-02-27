package com.arinterior.engine

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arinterior.engine.ui.ARInteriorApp
import com.arinterior.engine.unity.UnityARBridge
import com.google.ar.core.ArCoreApk

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ─── ARCore availability check (per docs) ────────────
        ArCoreApk.getInstance().checkAvailabilityAsync(this) { availability ->
            if (!availability.isSupported) {
                Toast.makeText(
                    this,
                    "This device does not support ARCore",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // ─── Thermal monitoring (per ARCore performance docs) ─
        monitorThermalStatus()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00D4FF),
                    secondary = Color(0xFFFF9F43),
                    surface = Color(0xFF1A1A2E),
                    background = Color(0xFF0F0F23),
                    onPrimary = Color.Black,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ARInteriorApp()
                }
            }
        }
    }

    // ─── Unity UAAL Lifecycle Forwarding ─────────────────────
    // UnityPlayer (created inside UnityPlayerView composable) requires
    // the host Activity to forward lifecycle events.

    override fun onResume() {
        super.onResume()
        UnityARBridge.resume()
    }

    override fun onPause() {
        super.onPause()
        UnityARBridge.pause()
    }

    override fun onDestroy() {
        UnityARBridge.quit()
        super.onDestroy()
    }

    // ─── Window focus — required by Unity for input handling ──
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            UnityARBridge.resume()
        } else {
            UnityARBridge.pause()
        }
    }

    /**
     * Monitor device thermal status to detect CPU throttling.
     * Per ARCore performance best practices:
     * "Monitor thermal status using PowerManager to prevent throttling."
     */
    private fun monitorThermalStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29+
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.addThermalStatusListener { status ->
                when (status) {
                    PowerManager.THERMAL_STATUS_MODERATE -> {
                        Log.w(TAG, "⚠ Thermal WARNING: Device moderately hot — AR performance may degrade")
                    }
                    PowerManager.THERMAL_STATUS_SEVERE -> {
                        Log.e(TAG, "🔥 Thermal SEVERE: Device very hot — ARCore may lose tracking")
                        runOnUiThread {
                            Toast.makeText(this, "Device is overheating — AR may be unstable", Toast.LENGTH_SHORT).show()
                        }
                    }
                    PowerManager.THERMAL_STATUS_CRITICAL,
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                        Log.e(TAG, "🔥🔥 Thermal CRITICAL: status=$status")
                        runOnUiThread {
                            Toast.makeText(this, "Device critically hot — please cool down", Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        Log.d(TAG, "Thermal status: $status (OK)")
                    }
                }
            }
        }
    }
}
