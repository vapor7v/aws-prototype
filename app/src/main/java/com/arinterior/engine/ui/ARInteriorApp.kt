package com.arinterior.engine.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arinterior.engine.data.model.AppState
import com.arinterior.engine.ui.components.*
import com.arinterior.engine.viewmodel.ARViewModel
import com.arinterior.engine.viewmodel.DesignViewModel

/**
 * Root composable — AR camera + overlay UI.
 */
@Composable
fun ARInteriorApp(
    arViewModel: ARViewModel = viewModel(),
    designViewModel: DesignViewModel = viewModel()
) {
    val appState by arViewModel.appState.collectAsState()
    val statusMessage by arViewModel.statusMessage.collectAsState()
    val candidates by arViewModel.candidates.collectAsState()
    val selectedCandidate by arViewModel.selectedCandidate.collectAsState()
    val isProcessing by arViewModel.isProcessing.collectAsState()
    val errorMessage by arViewModel.errorMessage.collectAsState()
    val roomGeometry by arViewModel.roomGeometry.collectAsState()
    val floorDetected by arViewModel.floorDetected.collectAsState()
    val planeCount by arViewModel.planeCount.collectAsState()

    // Voice state
    var voiceFeedback by remember { mutableStateOf<String?>(null) }
    var showOnboarding by remember { mutableStateOf(true) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (cameraGranted) {
            arViewModel.onPermissionsGranted()
        }
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ─── AR Camera View ──────────────────────────────
        ARSceneComposable(
            modifier = Modifier.fillMaxSize(),
            appState = appState,
            selectedCandidate = selectedCandidate,
            onFloorDetected = { arViewModel.onFloorDetected() },
            onWallDetected = { arViewModel.onWallDetected() },
            onObjectTapped = { arViewModel.onEditingStarted() },
            onPlaneCountChanged = { arViewModel.onPlaneCountChanged(it) }
        )

        // ─── Scanning Overlay (animated dots + crosshair) ─
        if (appState == AppState.SCANNING) {
            ScanningOverlay(
                planeCount = planeCount,
                floorDetected = floorDetected
            )
        }

        // ─── Status Bar (Top) ────────────────────────────
        StatusBar(
            status = statusMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // ─── Retry Scan Button ───────────────────────────
        // Shown when scanning timed out (READY but no floor detected)
        if (appState == AppState.READY && !floorDetected) {
            TextButton(
                onClick = { arViewModel.onRetryScan() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Text("\uD83D\uDD04 Retry Scan", color = MaterialTheme.colorScheme.primary)
            }
        }

        // ─── Loading Overlay ─────────────────────────────
        if (isProcessing) {
            LoadingOverlay()
        }

        // ─── Candidate Carousel ──────────────────────────
        if (candidates.isNotEmpty() && !isProcessing) {
            CandidateCarousel(
                candidates = candidates,
                selectedCandidate = selectedCandidate,
                onCandidateSelected = { arViewModel.onCandidateSelected(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    .offset(y = (-80).dp)  // Above prompt bar
            )
        }

        // ─── Voice Feedback Chip ─────────────────────────
        voiceFeedback?.let { feedback ->
            VoiceFeedbackChip(
                text = feedback,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )
        }

        // ─── Prompt Bar (Bottom) ─────────────────────────
        if (appState == AppState.READY || appState == AppState.DISPLAYING || appState == AppState.EDITING) {
            PromptBar(
                onPromptSubmit = { prompt ->
                    arViewModel.onProcessingStarted()
                    designViewModel.extractIntent(
                        prompt = prompt,
                        onSuccess = { intent ->
                            val newCandidates = designViewModel.generateCandidates(intent, roomGeometry)
                            arViewModel.onCandidatesReady(newCandidates)
                        },
                        onError = { arViewModel.onError(it) }
                    )
                },
                onImageSelected = { /* Pipeline B — handled separately */ },
                onVoiceInput = { transcribedText ->
                    designViewModel.parseVoiceCommand(
                        text = transcribedText,
                        onSuccess = { command ->
                            voiceFeedback = "${command.action} ${command.target} ✓"
                            // Forward parsed command to Unity for scene manipulation
                            arViewModel.applyVoiceCommand(command)
                        },
                        onError = { voiceFeedback = "Didn't catch that" }
                    )
                },
                isEnabled = !isProcessing,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }

        // ─── Error Snackbar ──────────────────────────────
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { arViewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }

        // ─── Onboarding Dialog ───────────────────────────
        if (showOnboarding && appState == AppState.READY) {
            OnboardingDialog(onDismiss = { showOnboarding = false })
        }
    }
}
