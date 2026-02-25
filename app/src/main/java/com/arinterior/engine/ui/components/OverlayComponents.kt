package com.arinterior.engine.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinterior.engine.data.model.AppState
import kotlinx.coroutines.delay

/**
 * Top status bar showing scan/ready/processing state.
 */
@Composable
fun StatusBar(
    statusMessage: String,
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val bgColor = when (appState) {
        AppState.SCANNING -> Color(0xCC1A1A2E)
        AppState.READY -> Color(0xCC0A3D2E)
        AppState.PROCESSING -> Color(0xCC2E1A0A)
        AppState.DISPLAYING -> Color(0xCC0A2E3D)
        AppState.EDITING -> Color(0xCC2E0A3D)
        else -> Color(0xCC1A1A2E)
    }

    Box(
        modifier = modifier
            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = statusMessage,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Full-screen loading overlay during AI processing.
 */
@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00D4FF),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Designing your room...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Voice feedback chip shown after voice command is processed.
 */
@Composable
fun VoiceFeedbackChip(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after 2 seconds
    LaunchedEffect(text) {
        delay(2000)
        onDismiss()
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xCC0A3D2E),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * First-time onboarding dialog.
 */
@Composable
fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Welcome to AR Interior Engine",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OnboardingStep("1", "Point camera at the floor and walls")
                OnboardingStep("2", "Type a prompt: \"Modern living room with sage green walls\"")
                OnboardingStep("3", "Browse design candidates and explore in AR")
                OnboardingStep("4", "Use voice commands: \"Move the sofa left\"")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!", color = Color(0xFF00D4FF))
            }
        },
        containerColor = Color(0xFF1A1A2E),
        titleContentColor = Color.White,
        textContentColor = Color(0xCCFFFFFF)
    )
}

@Composable
private fun OnboardingStep(number: String, description: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF00D4FF),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        Text(
            text = description,
            fontSize = 14.sp,
            color = Color(0xCCFFFFFF)
        )
    }
}
