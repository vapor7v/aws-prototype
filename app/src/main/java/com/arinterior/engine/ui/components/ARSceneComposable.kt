package com.arinterior.engine.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.arinterior.engine.data.model.AppState
import com.arinterior.engine.data.model.DesignCandidate
import com.google.ar.core.Config
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode

/**
 * AR camera + SceneView composable.
 * Wraps ArSceneView and handles plane detection, model placement, and scene updates.
 */
@Composable
fun ARSceneComposable(
    modifier: Modifier = Modifier,
    appState: AppState,
    selectedCandidate: DesignCandidate?,
    onFloorDetected: () -> Unit,
    onWallDetected: () -> Unit,
    onObjectTapped: () -> Unit
) {
    var floorNotified by remember { mutableStateOf(false) }
    var wallNotified by remember { mutableStateOf(false) }
    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }

    // Update scene when candidate changes
    LaunchedEffect(selectedCandidate) {
        arSceneView?.let { sceneView ->
            selectedCandidate?.let { candidate ->
                updateScene(sceneView, candidate)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ARSceneView(context).apply {
                arSceneView = this

                // Configure ARCore session
                configureSession { session, config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.depthMode = Config.DepthMode.AUTOMATIC
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }

                // Listen for plane updates
                onSessionUpdated = { session, frame ->
                    val planes = session.getAllTrackables(Plane::class.java)

                    // Check for floor planes
                    if (!floorNotified) {
                        val hasFloor = planes.any {
                            it.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            it.trackingState == com.google.ar.core.TrackingState.TRACKING
                        }
                        if (hasFloor) {
                            floorNotified = true
                            onFloorDetected()
                        }
                    }

                    // Check for wall planes
                    if (!wallNotified) {
                        val hasWall = planes.any {
                            it.type == Plane.Type.VERTICAL &&
                            it.trackingState == com.google.ar.core.TrackingState.TRACKING
                        }
                        if (hasWall) {
                            wallNotified = true
                            onWallDetected()
                        }
                    }
                }

                // Tap to select/edit objects
                onTouchEvent = { hitResult, motionEvent ->
                    if (appState == AppState.DISPLAYING) {
                        onObjectTapped()
                    }
                    true
                }
            }
        },
        update = { view ->
            // PlaneRenderer visibility based on state
            view.planeRenderer.isVisible = appState == AppState.SCANNING || appState == AppState.READY
        }
    )
}

/**
 * Clear existing scene nodes and place new candidate's furniture.
 */
private suspend fun updateScene(sceneView: ARSceneView, candidate: DesignCandidate) {
    // Clear existing model nodes
    sceneView.childNodes
        .filterIsInstance<AnchorNode>()
        .forEach { sceneView.removeChild(it) }

    // Place floor furniture
    for (item in candidate.furniture) {
        try {
            val modelNode = ModelNode(
                modelInstance = sceneView.modelLoader.createModelInstance(
                    // In production, this would load from S3/CloudFront URL
                    // For now, use a placeholder or bundled model
                    "models/${item.itemId}.glb"
                ),
                scaleToUnits = item.scale
            ).apply {
                position = dev.romainguy.kotlin.math.Float3(
                    item.positionX,
                    item.positionY,
                    item.positionZ
                )
                rotation = dev.romainguy.kotlin.math.Float3(
                    0f, item.rotationY, 0f
                )
            }

            // Create anchor on the nearest detected plane
            sceneView.session?.let { session ->
                val planes = session.getAllTrackables(Plane::class.java)
                val floorPlane = planes.firstOrNull {
                    it.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                }
                floorPlane?.let { plane ->
                    val anchor = plane.createAnchor(plane.centerPose)
                    val anchorNode = AnchorNode(sceneView.engine, anchor)
                    anchorNode.addChild(modelNode)
                    sceneView.addChild(anchorNode)
                }
            }
        } catch (e: Exception) {
            // Model loading failed — skip this item
            android.util.Log.w("ARScene", "Failed to load model: ${item.itemId}", e)
        }
    }

    // Place wall elements (art, sconces)
    for (wallItem in candidate.wallElements) {
        try {
            val modelNode = ModelNode(
                modelInstance = sceneView.modelLoader.createModelInstance(
                    "models/${wallItem.itemId}.glb"
                ),
                scaleToUnits = wallItem.scale
            ).apply {
                position = dev.romainguy.kotlin.math.Float3(
                    wallItem.positionX,
                    wallItem.positionY,
                    wallItem.positionZ
                )
            }

            sceneView.session?.let { session ->
                val planes = session.getAllTrackables(Plane::class.java)
                val wallPlane = planes.firstOrNull {
                    it.type == Plane.Type.VERTICAL
                }
                wallPlane?.let { plane ->
                    val anchor = plane.createAnchor(plane.centerPose)
                    val anchorNode = AnchorNode(sceneView.engine, anchor)
                    anchorNode.addChild(modelNode)
                    sceneView.addChild(anchorNode)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ARScene", "Failed to load wall item: ${wallItem.itemId}", e)
        }
    }
}
