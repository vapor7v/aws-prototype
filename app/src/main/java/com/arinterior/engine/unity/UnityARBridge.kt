package com.arinterior.engine.unity

import android.content.Context
import android.util.Log
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * JNI Bridge for Unity as a Library (UAAL).
 * Handles bidirectional communication between Android (Kotlin) and Unity (C#).
 * 
 * Communication flow:
 * - Android → Unity: Methods on UnityARBridge call UnityPlayer methods
 * - Unity → Android: Unity sends messages via UnityPlayer.UnitySendMessage()
 */
object UnityARBridge : IUnityPlayerLifecycleEvents {

    private const val TAG = "UnityARBridge"
    
    // Unity GameObject name that receives messages
    const val UNITY_BRIDGE_GO_NAME = "AndroidBridge"
    
    // Unity method names
    const val UNITY_METHOD_ON_FLOOR_DETECTED = "OnFloorDetected"
    const val UNITY_METHOD_ON_WALL_DETECTED = "OnWallDetected"
    const val UNITY_METHOD_ON_PLANE_COUNT_CHANGED = "OnPlaneCountChanged"
    const val UNITY_METHOD_ON_OBJECT_TAPPED = "OnObjectTapped"
    const val UNITY_METHOD_ON_TRACKING_STATE_CHANGED = "OnTrackingStateChanged"
    const val UNITY_METHOD_ON_AR_SESSION_ERROR = "OnARSessionError"

    private var unityPlayer: UnityPlayer? = null
    private var isInitialized = false

    // AR State flows for UI observation
    private val _floorDetected = MutableStateFlow(false)
    val floorDetected: StateFlow<Boolean> = _floorDetected.asStateFlow()

    private val _wallDetected = MutableStateFlow(false)
    val wallDetected: StateFlow<Boolean> = _wallDetected.asStateFlow()

    private val _planeCount = MutableStateFlow(0)
    val planeCount: StateFlow<Int> = _planeCount.asStateFlow()

    private val _trackingState = MutableStateFlow(TrackingState.NOT_TRACKING)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    /**
     * Initialize Unity as a Library.
     * Call this from MainActivity.onCreate() after permissions are granted.
     */
    fun initialize(context: Context, unityPlayer: UnityPlayer) {
        if (isInitialized) {
            Log.w(TAG, "UnityARBridge already initialized")
            return
        }
        
        this.unityPlayer = unityPlayer
        isInitialized = true
        Log.d(TAG, "UnityARBridge initialized")
    }

    /**
     * Pause Unity AR session.
     * Call from Activity.onPause()
     */
    fun pause() {
        unityPlayer?.pause()
        Log.d(TAG, "Unity AR paused")
    }

    /**
     * Resume Unity AR session.
     * Call from Activity.onResume()
     */
    fun resume() {
        unityPlayer?.resume()
        Log.d(TAG, "Unity AR resumed")
    }

    /**
     * Quit Unity (when app is destroyed).
     */
    fun quit() {
        unityPlayer?.quit()
        unityPlayer = null
        isInitialized = false
        Log.d(TAG, "Unity AR quit")
    }

    // ─────────────────────────────────────────────────────────────
    // Android → Unity Commands
    // ─────────────────────────────────────────────────────────────

    /**
     * Initialize Unity AR session with configuration.
     */
    fun initializeAR() {
        sendUnityMessage("InitializeAR", "")
    }

    /**
     * Start plane detection.
     */
    fun startPlaneDetection() {
        sendUnityMessage("StartPlaneDetection", "")
    }

    /**
     * Stop plane detection.
     */
    fun stopPlaneDetection() {
        sendUnityMessage("StopPlaneDetection", "")
    }

    /**
     * Place a furniture item at the specified position.
     * @param itemId The furniture item ID from the catalog
     * @param positionX X coordinate in world space
     * @param positionY Y coordinate (height)
     * @param positionZ Z coordinate in world space
     * @param rotationY Rotation around Y axis in degrees
     * @param scale Scale factor
     */
    fun placeFurniture(
        itemId: String,
        positionX: Float,
        positionY: Float,
        positionZ: Float,
        rotationY: Float = 0f,
        scale: Float = 1f
    ) {
        val data = "$itemId|$positionX|$positionY|$positionZ|$rotationY|$scale"
        sendUnityMessage("PlaceFurniture", data)
    }

    /**
     * Move an existing furniture item to a new position.
     */
    fun moveFurniture(itemId: String, positionX: Float, positionY: Float, positionZ: Float) {
        val data = "$itemId|$positionX|$positionY|$positionZ"
        sendUnityMessage("MoveFurniture", data)
    }

    /**
     * Rotate a furniture item.
     */
    fun rotateFurniture(itemId: String, degrees: Float) {
        val data = "$itemId|$degrees"
        sendUnityMessage("RotateFurniture", data)
    }

    /**
     * Scale a furniture item.
     */
    fun scaleFurniture(itemId: String, scaleFactor: Float) {
        val data = "$itemId|$scaleFactor"
        sendUnityMessage("ScaleFurniture", data)
    }

    /**
     * Remove a furniture item from the scene.
     */
    fun removeFurniture(itemId: String) {
        sendUnityMessage("RemoveFurniture", itemId)
    }

    /**
     * Apply wall color.
     * @param colorHex Color in hex format (e.g., "#F5F5DC")
     */
    fun recolorWall(colorHex: String) {
        sendUnityMessage("RecolorWall", colorHex)
    }

    /**
     * Clear all furniture and reset the scene.
     */
    fun clearScene() {
        sendUnityMessage("ClearScene", "")
    }

    /**
     * Load design candidates from JSON.
     * @param candidateJson JSON array of DesignCandidate objects
     */
    fun loadCandidates(candidateJson: String) {
        sendUnityMessage("LoadCandidates", candidateJson)
    }

    /**
     * Select a candidate to display.
     * @param candidateId The candidate ID to display
     */
    fun selectCandidate(candidateId: String) {
        sendUnityMessage("SelectCandidate", candidateId)
    }

    /**
     * Enable/disable object selection by tap.
     */
    fun setObjectSelectionEnabled(enabled: Boolean) {
        sendUnityMessage("SetObjectSelectionEnabled", if (enabled) "1" else "0")
    }

    /**
     * Set the current editing mode.
     */
    fun setEditingMode(mode: String) {
        sendUnityMessage("SetEditingMode", mode)
    }

    /**
     * Apply auto-snap placement to an item.
     */
    fun applyAutoSnap(itemId: String, category: String) {
        val data = "$itemId|$category"
        sendUnityMessage("ApplyAutoSnap", data)
    }

    /**
     * Send a voice command to Unity for processing.
     */
    fun processVoiceCommand(commandJson: String) {
        sendUnityMessage("ProcessVoiceCommand", commandJson)
    }

    // ─────────────────────────────────────────────────────────────
    // Unity → Android Callbacks (called from Unity via JNI)
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when Unity detects a floor plane.
     */
    @JvmStatic
    fun onFloorDetected() {
        Log.d(TAG, "onFloorDetected")
        _floorDetected.value = true
    }

    /**
     * Called when Unity detects a wall plane.
     */
    @JvmStatic
    fun onWallDetected() {
        Log.d(TAG, "onWallDetected")
        _wallDetected.value = true
    }

    /**
     * Called when plane count changes.
     */
    @JvmStatic
    fun onPlaneCountChanged(count: Int) {
        Log.d(TAG, "onPlaneCountChanged: $count")
        _planeCount.value = count
    }

    /**
     * Called when an object is tapped in the AR scene.
     */
    @JvmStatic
    fun onObjectTapped(itemId: String) {
        Log.d(TAG, "onObjectTapped: $itemId")
        _selectedItemId.value = itemId
    }

    /**
     * Called when AR tracking state changes.
     */
    @JvmStatic
    fun onTrackingStateChanged(state: String) {
        Log.d(TAG, "onTrackingStateChanged: $state")
        _trackingState.value = TrackingState.fromString(state)
    }

    /**
     * Called when AR session encounters an error.
     */
    @JvmStatic
    fun onARSessionError(error: String) {
        Log.e(TAG, "onARSessionError: $error")
    }

    /**
     * Reset detection state (for retry scan).
     */
    fun resetDetectionState() {
        _floorDetected.value = false
        _wallDetected.value = false
        _planeCount.value = 0
    }

    // ─────────────────────────────────────────────────────────────
    // IUnityPlayerLifecycleEvents Implementation
    // ─────────────────────────────────────────────────────────────

    override fun onUnityPlayerUnloaded() {
        Log.d(TAG, "onUnityPlayerUnloaded")
        isInitialized = false
    }

    override fun onUnityPlayerQuitted() {
        Log.d(TAG, "onUnityPlayerQuitted")
        isInitialized = false
    }

    // ─────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────

    private fun sendUnityMessage(method: String, data: String) {
        if (!isInitialized) {
            Log.w(TAG, "Cannot send message - Unity not initialized: $method")
            return
        }
        try {
            UnityPlayer.UnitySendMessage(UNITY_BRIDGE_GO_NAME, method, data)
            Log.d(TAG, "Sent to Unity: $method with data: $data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to Unity: $method", e)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tracking State Enum
    // ─────────────────────────────────────────────────────────────

    enum class TrackingState {
        TRACKING,
        PAUSED,
        NOT_TRACKING;

        companion object {
            fun fromString(value: String): TrackingState {
                return when (value.uppercase()) {
                    "TRACKING" -> TRACKING
                    "PAUSED" -> PAUSED
                    else -> NOT_TRACKING
                }
            }
        }
    }
}
