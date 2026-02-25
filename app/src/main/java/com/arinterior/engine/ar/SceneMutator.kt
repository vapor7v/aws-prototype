package com.arinterior.engine.ar

import com.arinterior.engine.data.model.*

/**
 * Applies voice commands to the AR scene.
 * Mutates placed items based on parsed VoiceCommand.
 */
object SceneMutator {

    /**
     * Apply a voice command to the current design candidate.
     * Returns the modified candidate.
     */
    fun applyCommand(
        command: VoiceCommand,
        candidate: DesignCandidate
    ): DesignCandidate {
        return when (command.action) {
            VoiceAction.MOVE -> moveTarget(command, candidate)
            VoiceAction.RECOLOR -> recolorWall(command, candidate)
            VoiceAction.REMOVE -> removeTarget(command, candidate)
            VoiceAction.RESIZE -> resizeTarget(command, candidate)
            VoiceAction.ROTATE -> rotateTarget(command, candidate)
            VoiceAction.RESET -> resetScene(candidate)
            VoiceAction.UNDO -> candidate // Undo handled by ViewModel history
        }
    }

    private fun moveTarget(command: VoiceCommand, candidate: DesignCandidate): DesignCandidate {
        val amount = command.amount ?: 0.3f
        val dx = when (command.direction) {
            Direction.LEFT -> -amount
            Direction.RIGHT -> amount
            else -> 0f
        }
        val dz = when (command.direction) {
            Direction.FORWARD -> -amount
            Direction.BACKWARD -> amount
            else -> 0f
        }

        val updatedFurniture = candidate.furniture.map { item ->
            if (matchesTarget(item.itemId, command.target)) {
                item.copy(
                    positionX = item.positionX + dx,
                    positionZ = item.positionZ + dz
                )
            } else item
        }

        return candidate.copy(furniture = updatedFurniture)
    }

    private fun recolorWall(command: VoiceCommand, candidate: DesignCandidate): DesignCandidate {
        val newColor = command.color ?: return candidate
        return candidate.copy(
            wallDesign = candidate.wallDesign.copy(primaryColor = newColor)
        )
    }

    private fun removeTarget(command: VoiceCommand, candidate: DesignCandidate): DesignCandidate {
        return candidate.copy(
            furniture = candidate.furniture.filterNot { matchesTarget(it.itemId, command.target) },
            wallElements = candidate.wallElements.filterNot { matchesTarget(it.itemId, command.target) }
        )
    }

    private fun resizeTarget(command: VoiceCommand, candidate: DesignCandidate): DesignCandidate {
        val factor = command.scaleFactor ?: 1.2f
        val updatedFurniture = candidate.furniture.map { item ->
            if (matchesTarget(item.itemId, command.target)) {
                item.copy(scale = item.scale * factor)
            } else item
        }
        return candidate.copy(furniture = updatedFurniture)
    }

    private fun rotateTarget(command: VoiceCommand, candidate: DesignCandidate): DesignCandidate {
        val degrees = command.rotationDegrees ?: 45f
        val updatedFurniture = candidate.furniture.map { item ->
            if (matchesTarget(item.itemId, command.target)) {
                item.copy(rotationY = item.rotationY + degrees)
            } else item
        }
        return candidate.copy(furniture = updatedFurniture)
    }

    private fun resetScene(candidate: DesignCandidate): DesignCandidate {
        return candidate.copy(
            furniture = emptyList(),
            wallElements = emptyList(),
            wallDesign = candidate.wallDesign.copy(primaryColor = "#FFFFFF")
        )
    }

    /**
     * Fuzzy match item ID against voice target.
     * e.g., "sofa" matches "sofa_minimal_01"
     */
    private fun matchesTarget(itemId: String, target: String): Boolean {
        val normalizedTarget = target.lowercase().replace(" ", "_")
        val normalizedId = itemId.lowercase()
        return normalizedId.contains(normalizedTarget) ||
               normalizedTarget.contains(normalizedId.substringBefore("_"))
    }
}
