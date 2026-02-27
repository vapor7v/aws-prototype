package com.arinterior.engine.ui.components

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.arinterior.engine.data.model.AppState
import com.arinterior.engine.data.model.DesignCandidate

private const val TAG = "ARSceneComposable"

/**
 * AR Scene composable that uses Unity AR Foundation instead of SceneView.
 * This replaces the previous ARSceneView implementation.
 * 
 * Migration notes:
 * - Previous: ARSceneView from sceneview library
 * - New: UnityPlayerView with AR Foundation
 */
@Composable
fun ARSceneComposable(
    modifier: Modifier = Modifier,
    appState: AppState,
    selectedCandidate: DesignCandidate?,
    onFloorDetected: () -> Unit,
    onWallDetected: () -> Unit,
    onObjectTapped: () -> Unit,
    onPlaneCountChanged: (Int) -> Unit = {}
) {
    // Track floor/wall detection
    var floorNotified by remember { mutableStateOf(false) }
    var wallNotified by remember { mutableStateOf(false) }

    // Track previous app state for reset detection
    var previousAppState by remember { mutableStateOf(appState) }

    // Handle state transitions
    LaunchedEffect(appState) {
        // Reset detection flags when entering SCANNING state
        if (appState == AppState.SCANNING && previousAppState != AppState.SCANNING) {
            floorNotified = false
            wallNotified = false
        }
        previousAppState = appState
    }

    // Unity AR View - replaces previous ARSceneView
    UnityPlayerView(
        modifier = modifier,
        onFloorDetected = {
            if (!floorNotified) {
                floorNotified = true
                Log.d(TAG, "✓ FLOOR DETECTED")
                onFloorDetected()
            }
        },
        onWallDetected = {
            if (!wallNotified) {
                wallNotified = true
                Log.d(TAG, "✓ WALL DETECTED")
                onWallDetected()
            }
        },
        onObjectTapped = { _ ->
            if (appState == AppState.DISPLAYING) {
                Log.d(TAG, "Object tapped in AR scene")
                onObjectTapped()
            }
        },
        onPlaneCountChanged = { count ->
            Log.d(TAG, "Plane count changed: $count")
            onPlaneCountChanged(count)
        },
        onTrackingStateChanged = { state ->
            Log.d(TAG, "Tracking state: $state")
        }
    )
}

/**
 * Legacy function for backward compatibility during migration.
 * @deprecated Use UnityPlayerView directly
 */
@Deprecated(
    message = "Use UnityPlayerView instead",
    replaceWith = ReplaceWith("UnityPlayerView(modifier = modifier)")
)
@Composable
fun ARSceneComposableLegacy(
    modifier: Modifier = Modifier,
    appState: AppState,
    selectedCandidate: DesignCandidate?,
    onFloorDetected: () -> Unit,
    onWallDetected: () -> Unit,
    onObjectTapped: () -> Unit,
    onPlaneCountChanged: (Int) -> Unit
) {
    ARSceneComposable(
        modifier = modifier,
        appState = appState,
        selectedCandidate = selectedCandidate,
        onFloorDetected = onFloorDetected,
        onWallDetected = onWallDetected,
        onObjectTapped = onObjectTapped,
        onPlaneCountChanged = onPlaneCountChanged
    )
}
