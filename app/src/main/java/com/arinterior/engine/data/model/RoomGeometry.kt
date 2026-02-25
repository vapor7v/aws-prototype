package com.arinterior.engine.data.model

/**
 * Room geometry data from ARCore plane detection.
 */
data class RoomGeometry(
    val floorPlanes: List<DetectedPlaneData> = emptyList(),
    val wallPlanes: List<DetectedPlaneData> = emptyList(),
    val estimatedDimensions: RoomDimensions = RoomDimensions(),
    val ambientLight: AmbientLight = AmbientLight()
)

data class RoomDimensions(
    val width: Float = 0f,
    val length: Float = 0f,
    val height: Float = 2.5f
)

data class AmbientLight(
    val intensity: Float = 500f,
    val colorTemperature: Float = 4500f
)

data class DetectedPlaneData(
    val id: String = "",
    val type: PlaneType = PlaneType.HORIZONTAL,
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val centerZ: Float = 0f,
    val extentWidth: Float = 0f,
    val extentHeight: Float = 0f
)

enum class PlaneType { HORIZONTAL, VERTICAL }

/**
 * Application state machine states.
 */
enum class AppState {
    LAUNCHING,
    SCANNING,
    READY,
    PROCESSING,
    DISPLAYING,
    EDITING
}
