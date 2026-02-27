package com.arinterior.engine.ui.components

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arinterior.engine.unity.UnityARBridge
import com.unity3d.player.UnityPlayer

/**
 * Composable that embeds Unity as a Library (UAAL) view.
 * 
 * This replaces the ARSceneView from SceneView library with Unity AR Foundation.
 * The UnityPlayer is embedded as an Android View inside Jetpack Compose via AndroidView.
 * 
 * Usage:
 * ```
 * UnityPlayerView(
 *     modifier = Modifier.fillMaxSize(),
 *     onFloorDetected = { viewModel.onFloorDetected() },
 *     onWallDetected = { viewModel.onWallDetected() },
 *     onObjectTapped = { itemId -> viewModel.onObjectTapped(itemId) },
 *     onPlaneCountChanged = { count -> viewModel.onPlaneCountChanged(count) }
 * )
 * ```
 */
@Composable
fun UnityPlayerView(
    modifier: Modifier = Modifier,
    onFloorDetected: () -> Unit = {},
    onWallDetected: () -> Unit = {},
    onObjectTapped: (String) -> Unit = {},
    onPlaneCountChanged: (Int) -> Unit = {},
    onTrackingStateChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Hold a reference to the UnityPlayer — created once
    val unityPlayer = remember {
        val activity = context as? Activity
        if (activity != null) {
            UnityPlayer(activity, UnityARBridge).also { player ->
                UnityARBridge.initialize(activity, player)
            }
        } else {
            null
        }
    }

    // Track Unity AR state flows
    val floorDetected by UnityARBridge.floorDetected.collectAsState()
    val wallDetected by UnityARBridge.wallDetected.collectAsState()
    val planeCount by UnityARBridge.planeCount.collectAsState()
    val selectedItemId by UnityARBridge.selectedItemId.collectAsState()

    // Notify parent callbacks when Unity state changes
    LaunchedEffect(floorDetected) {
        if (floorDetected) onFloorDetected()
    }

    LaunchedEffect(wallDetected) {
        if (wallDetected) onWallDetected()
    }

    LaunchedEffect(planeCount) {
        onPlaneCountChanged(planeCount)
    }

    LaunchedEffect(selectedItemId) {
        selectedItemId?.let { onObjectTapped(it) }
    }

    // Lifecycle handling — forward lifecycle events to UnityPlayer
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    unityPlayer?.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    unityPlayer?.resume()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initialize AR after UnityPlayer is ready
    LaunchedEffect(unityPlayer) {
        if (unityPlayer != null) {
            // Small delay to let Unity engine initialize
            kotlinx.coroutines.delay(1000)
            UnityARBridge.initializeAR()
        }
    }

    // Embed UnityPlayer's view into Compose via AndroidView
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // UnityPlayer IS a FrameLayout — wrap it in another FrameLayout
            // to let Compose manage layout sizing
            FrameLayout(ctx).apply {
                if (unityPlayer != null) {
                    // Remove from previous parent if any (UnityPlayer can only
                    // be attached to one parent at a time)
                    (unityPlayer.parent as? FrameLayout)?.removeView(unityPlayer)

                    addView(
                        unityPlayer,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            }
        },
        update = { _ ->
            // No dynamic updates needed — UnityPlayer handles its own rendering
        }
    )
}

/**
 * Simplified UnityPlayerView for basic use cases.
 * Use this when you don't need individual callbacks.
 */
@Composable
fun UnityARView(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    if (!isEnabled) {
        // Show placeholder when AR is disabled
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                android.widget.FrameLayout(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            }
        )
        return
    }

    UnityPlayerView(modifier = modifier)
}
