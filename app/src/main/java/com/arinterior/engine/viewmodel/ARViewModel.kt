package com.arinterior.engine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arinterior.engine.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel managing app state, AR data, and design state.
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


    fun onPermissionsGranted() {
        _appState.value = AppState.SCANNING
        _statusMessage.value = "Scanning for surfaces..."
    }

    fun onFloorDetected() {
        _floorDetected.value = true
        checkReadyState()
    }

    fun onWallDetected() {
        _wallsDetected.value = true
        checkReadyState()
    }

    private fun checkReadyState() {
        if (_floorDetected.value) {
            _appState.value = AppState.READY
            _statusMessage.value = "Ready! Enter a prompt or upload an image"
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
    }

    fun onCandidateSelected(candidate: DesignCandidate) {
        _selectedCandidate.value = candidate
    }

    fun onEditingStarted() {
        _appState.value = AppState.EDITING
        _statusMessage.value = "Editing — move, resize, or voice command"
    }

    fun onClearScene() {
        _candidates.value = emptyList()
        _selectedCandidate.value = null
        _appState.value = AppState.READY
        _statusMessage.value = "Ready! Enter a prompt or upload an image"
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
}
