package com.arinterior.engine.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Scanning overlay — LIGHTWEIGHT version.
 *
 * The original version drew 144 animated dots with sin/cos per frame,
 * which caused ARCore CPU starvation (VIO dropped to 5 Hz, never initialized).
 *
 * This version uses only simple Compose elements — no Canvas, no trigonometry,
 * no heavy per-frame calculations. CPU cost is near zero.
 */
@Composable
fun ScanningOverlay(
    planeCount: Int,
    floorDetected: Boolean,
    modifier: Modifier = Modifier
) {
    // Single lightweight animation: pulsing alpha for the instruction text
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // ─── Center: status indicator ────────────────────────
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scanning progress indicator (lightweight)
            if (!floorDetected) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF00D4FF),
                    strokeWidth = 3.dp
                )
            }

            Text(
                text = if (floorDetected) "✓ Floor detected" else "Scanning…",
                color = if (floorDetected) Color(0xFF4CAF50) else Color(0xFF00D4FF).copy(alpha = pulseAlpha),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // ─── Bottom: instructions + plane count ──────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Plane count chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    text = if (planeCount > 0) "Surfaces found: $planeCount" else "Searching for surfaces…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (planeCount > 0) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            // Motion hint
            Text(
                text = "Point at the floor and move slowly",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Existing overlay components below
// ──────────────────────────────────────────────────────────────

@Composable
fun StatusBar(
    status: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(12.dp),
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoadingOverlay(
    message: String = "Processing…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF00D4FF)
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun VoiceFeedbackChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF00D4FF).copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(0xFF00D4FF),
            fontSize = 13.sp
        )
    }
}

@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        },
        title = { Text("How to use") },
        text = {
            Text(
                "1. Point your camera at the floor\n" +
                "2. Move slowly to scan the room\n" +
                "3. Describe what you want or tap to select\n" +
                "4. AI will generate design options"
            )
        }
    )
}
