package com.arinterior.engine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arinterior.engine.data.model.*
import com.arinterior.engine.unity.UnityARBridge
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel managing app state, AR data, and design state.
 * 
 * Migration notes:
 * - Now uses UnityARBridge to communicate with Unity AR Foundation
 * - Previous ARCore direct management replaced with Unity commands
 */
class ARViewModel : ViewModel() {

    // ─── App State ───────────────────────────────────────
    private val _appState = MutableStateFlow(AppState.LAUNCHING)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // ─── Room Geometry ───────────────────────────────────
    private val _roomGeometry = MutableStateFlow(RoomGeometry())
    val roomGeometry: StateFlow<RoomGeometry> = _roomGeometry.asStateFlow()

    // ─── Planes Detected ─────────────────────────────────
    private val _floorDetected = MutableStateFlow(false)
    val floorDetected: StateFlow<Boolean> = _floorDetected.asStateFlow()

    private val _wallsDetected = MutableStateFlow(false)
    val wallsDetected: StateFlow<Boolean> = _wallsDetected.asStateFlow()

    // ─── Plane Count (for scanning overlay) ──────────────
    private val _planeCount = MutableStateFlow(0)
    val planeCount: StateFlow<Int> = _planeCount.asStateFlow()

    // ─── Design State ────────────────────────────────────
    private val _candidates = MutableStateFlow<List<DesignCandidate>>(emptyList())
    val candidates: StateFlow<List<DesignCandidate>> = _candidates.asStateFlow()

    private val _selectedCandidate = MutableStateFlow<DesignCandidate?>(null)
    val selectedCandidate: StateFlow<DesignCandidate?> = _selectedCandidate.asStateFlow()

    // ─── Loading / Error ─────────────────────────────────
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ─── Status Message ──────────────────────────────────
    private val _statusMessage = MutableStateFlow("Point camera at a flat surface")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // ─── Scanning Timeout ────────────────────────────────
    private var scanTimeoutJob: Job? = null

    // ─── JSON Serialization ──────────────────────────────
    private val gson = Gson()

    companion object {
        /** Max time to wait for plane detection before auto-advancing to READY.
         *  ARCore often needs 10–15+ seconds to build reliable planes. */
        private const val SCAN_TIMEOUT_MS = 20_000L
    }

    init {
        // Observe Unity AR state
        observeUnityARState()
    }

    private fun observeUnityARState() {
        // Collect Unity AR Bridge state flows
        viewModelScope.launch {
            UnityARBridge.floorDetected.collect { detected ->
                if (detected && !_floorDetected.value) {
                    _floorDetected.value = true
                    onFloorDetected()
                }
            }
        }

        viewModelScope.launch {
            UnityARBridge.wallDetected.collect { detected ->
                if (detected && !_wallsDetected.value) {
                    _wallsDetected.value = true
                    onWallDetected()
                }
            }
        }

        viewModelScope.launch {
            UnityARBridge.planeCount.collect { count ->
                _planeCount.value = count
            }
        }
    }

    fun onPermissionsGranted() {
        _appState.value = AppState.SCANNING
        _statusMessage.value = "Scanning... Move phone slowly side-to-side"
        
        // Initialize Unity AR
        UnityARBridge.startPlaneDetection()
        
        startScanTimeout()
    }

    fun onFloorDetected() {
        scanTimeoutJob?.cancel()          // plane found — cancel timeout
        _floorDetected.value = true
        _statusMessage.value = "Floor found! Keep scanning for walls..."
        
        // Let Unity AR handle detection, we just update UI
        viewModelScope.launch {
            delay(3500L) // slightly longer than stabilization time
            checkReadyState()
        }
    }

    fun onWallDetected() {
        _wallsDetected.value = true
        _statusMessage.value = "Floor & walls detected! Almost ready..."
    }

    fun onPlaneCountChanged(count: Int) {
        _planeCount.value = count
    }

    private fun checkReadyState() {
        if (_floorDetected.value) {
            _appState.value = AppState.READY
            _statusMessage.value = if (_wallsDetected.value) {
                "Ready! Enter a prompt or upload an image"
            } else {
                "Ready! Floor detected — enter a prompt"
            }
        }
    }

    /** Called when scanning timeout expires without detecting a floor plane. */
    fun onScanTimeout() {
        if (_appState.value == AppState.SCANNING) {
            _appState.value = AppState.READY
            _statusMessage.value = "No surface found — you can still enter a prompt"
        }
    }

    /** Reset detection flags and re-enter SCANNING with a fresh timeout. */
    fun onRetryScan() {
        _floorDetected.value = false
        _wallsDetected.value = false
        _planeCount.value = 0
        _appState.value = AppState.SCANNING
        _statusMessage.value = "Scanning... Move phone slowly side-to-side"
        
        // Reset Unity AR detection state
        UnityARBridge.resetDetectionState()
        
        startScanTimeout()
    }

    private fun startScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = viewModelScope.launch {
            delay(SCAN_TIMEOUT_MS)
            onScanTimeout()
        }
    }

    fun onProcessingStarted() {
        _appState.value = AppState.PROCESSING
        _isProcessing.value = true
        _statusMessage.value = "Designing your room..."
    }

    fun onCandidatesReady(newCandidates: List<DesignCandidate>) {
        _candidates.value = newCandidates
        _selectedCandidate.value = newCandidates.firstOrNull { it.isBestTake } ?: newCandidates.firstOrNull()
        _isProcessing.value = false
        _appState.value = AppState.DISPLAYING
        _statusMessage.value = "Design ready! Tap to explore"
        
        // Load candidates into Unity
        loadCandidatesToUnity(newCandidates)
    }

    fun onCandidateSelected(candidate: DesignCandidate) {
        _selectedCandidate.value = candidate
        
        // Load selected candidate into Unity
        candidateToUnity(candidate)
    }

    fun onEditingStarted() {
        _appState.value = AppState.EDITING
        _statusMessage.value = "Editing — move, resize, or voice command"
        UnityARBridge.setEditingMode("EDITING")
    }

    fun onClearScene() {
        _candidates.value = emptyList()
        _selectedCandidate.value = null
        _appState.value = AppState.READY
        _statusMessage.value = "Ready! Enter a prompt or upload an image"
        
        // Clear Unity scene
        UnityARBridge.clearScene()
    }

    fun onError(message: String) {
        _errorMessage.value = message
        _isProcessing.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateRoomGeometry(geometry: RoomGeometry) {
        _roomGeometry.value = geometry
    }

    fun onObjectTapped(itemId: String) {
        // Handle object selection from Unity
        onEditingStarted()
    }

    // ─────────────────────────────────────────────────────────────
    // Unity Communication
    // ─────────────────────────────────────────────────────────────

    /**
     * Load design candidates into Unity AR scene.
     */
    private fun loadCandidatesToUnity(candidates: List<DesignCandidate>) {
        val json = gson.toJson(candidates)
        UnityARBridge.loadCandidates(json)
    }

    /**
     * Load a single candidate to Unity.
     */
    private fun candidateToUnity(candidate: DesignCandidate) {
        UnityARBridge.selectCandidate(candidate.id)
        
        // Place furniture items
        candidate.furniture.forEach { item ->
            UnityARBridge.placeFurniture(
                itemId = item.itemId,
                positionX = item.positionX,
                positionY = item.positionY,
                positionZ = item.positionZ,
                rotationY = item.rotationY,
                scale = item.scale
            )
        }
        
        // Place wall elements
        candidate.wallElements.forEach { item ->
            UnityARBridge.placeFurniture(
                itemId = item.itemId,
                positionX = item.positionX,
                positionY = item.positionY,
                positionZ = item.positionZ,
                rotationY = 0f,
                scale = item.scale
            )
        }
        
        // Apply wall color
        candidate.wallDesign.let { wallDesign ->
            UnityARBridge.recolorWall(wallDesign.primaryColor)
        }
    }

    /**
     * Apply a voice command to the Unity scene.
     */
    fun applyVoiceCommand(command: VoiceCommand) {
        val json = gson.toJson(command)
        UnityARBridge.processVoiceCommand(json)
    }

    /**
     * Move selected furniture item.
     */
    fun moveItem(itemId: String, positionX: Float, positionY: Float, positionZ: Float) {
        UnityARBridge.moveFurniture(itemId, positionX, positionY, positionZ)
    }

    /**
     * Rotate selected furniture item.
     */
    fun rotateItem(itemId: String, degrees: Float) {
        UnityARBridge.rotateFurniture(itemId, degrees)
    }

    /**
     * Scale selected furniture item.
     */
    fun scaleItem(itemId: String, scaleFactor: Float) {
        UnityARBridge.scaleFurniture(itemId, scaleFactor)
    }

    /**
     * Remove selected furniture item.
     */
    fun removeItem(itemId: String) {
        UnityARBridge.removeFurniture(itemId)
    }

    /**
     * Change wall color.
     */
    fun changeWallColor(colorHex: String) {
        UnityARBridge.recolorWall(colorHex)
    }

    override fun onCleared() {
        super.onCleared()
        UnityARBridge.quit()
    }
}
