package com.arinterior.engine.data.model

/**
 * Parsed voice command from Bedrock.
 */
data class VoiceCommand(
    val target: String,
    val action: VoiceAction,
    val direction: Direction? = null,
    val amount: Float? = null,
    val color: String? = null,
    val scaleFactor: Float? = null,
    val rotationDegrees: Float? = null,
    val confidence: Float = 0f
)

enum class VoiceAction {
    MOVE, RECOLOR, REMOVE, RESIZE, ROTATE, UNDO, RESET
}

enum class Direction {
    LEFT, RIGHT, FORWARD, BACKWARD, UP, DOWN
}
