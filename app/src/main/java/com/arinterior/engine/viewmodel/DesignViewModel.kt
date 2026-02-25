package com.arinterior.engine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arinterior.engine.data.api.ApiClient
import com.arinterior.engine.data.api.IntentRequest
import com.arinterior.engine.data.api.VoiceCommandRequest
import com.arinterior.engine.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AI pipeline operations (intent extraction, candidate generation, voice parsing).
 */
class DesignViewModel : ViewModel() {

    private val gson = Gson()

    private val _designIntent = MutableStateFlow<DesignIntent?>(null)
    val designIntent: StateFlow<DesignIntent?> = _designIntent.asStateFlow()

    /**
     * Pipeline A: Extract design intent from user prompt via Bedrock.
     */
    fun extractIntent(
        prompt: String,
        onSuccess: (DesignIntent) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.service.extractIntent(IntentRequest(prompt))
                val intentJson = gson.toJson(response.designIntent)
                val intent = gson.fromJson(intentJson, DesignIntent::class.java)
                _designIntent.value = intent
                onSuccess(intent)
            } catch (e: Exception) {
                onError("Failed to extract intent: ${e.message}")
            }
        }
    }

    /**
     * Generate 3 design candidates from intent + room geometry.
     */
    fun generateCandidates(
        intent: DesignIntent,
        roomGeometry: RoomGeometry
    ): List<DesignCandidate> {
        val candidates = mutableListOf<DesignCandidate>()
        val variations = listOf(
            "Bold & Centered" to 0f,
            "Asymmetric Flow" to 30f,
            "Cozy Corner" to -15f
        )

        for ((index, variation) in variations.withIndex()) {
            val (name, rotationOffset) = variation
            val furniture = intent.objects.mapIndexed { i, obj ->
                PlacedItem(
                    itemId = "${obj.type}_${obj.style ?: "default"}_0$i",
                    positionX = (i - 1) * 1.5f,
                    positionY = 0f,
                    positionZ = -2f + (index * 0.3f),
                    rotationY = rotationOffset,
                    scale = 1f
                )
            }

            val wallElements = intent.wallDecor.mapIndexed { i, decor ->
                PlacedWallItem(
                    itemId = "${decor.type}_${decor.style ?: "default"}_0$i",
                    wallId = "wall_${i % 2}",
                    positionX = 0f,
                    positionY = if (decor.type.contains("light") || decor.type.contains("sconce")) 1.8f else 1.5f,
                    positionZ = 0f,
                    emissive = decor.type.contains("light") || decor.type.contains("sconce"),
                    lightColor = if (decor.type.contains("light")) "#FFE4B5" else null
                )
            }

            candidates.add(
                DesignCandidate(
                    id = "candidate_${('A' + index)}",
                    name = name,
                    furniture = furniture,
                    wallDesign = WallDesign(
                        primaryColor = intent.wallTreatment.color,
                        accentWall = intent.wallTreatment.accentWall?.let {
                            AccentWallDesign("wall_${it.wallIndex}", it.color)
                        },
                        texture = intent.wallTreatment.texture
                    ),
                    wallElements = wallElements,
                    scores = CandidateScores(),
                    isBestTake = index == 0
                )
            )
        }

        // Score candidates
        return scoreCandidates(candidates, intent, roomGeometry)
    }

    /**
     * Score candidates on 6 dimensions.
     */
    private fun scoreCandidates(
        candidates: List<DesignCandidate>,
        intent: DesignIntent,
        roomGeometry: RoomGeometry
    ): List<DesignCandidate> {
        return candidates.map { candidate ->
            val colorHarmony = scoreColorHarmony(candidate, intent)
            val spatialFit = scoreSpatialFit(candidate, roomGeometry)
            val wallCohesion = 70f + (Math.random() * 20).toFloat()
            val lightingMatch = 65f + (Math.random() * 25).toFloat()
            val styleConsistency = 75f + (Math.random() * 20).toFloat()
            val constraintSat = if (intent.constraints.budgetMax != null) 80f else 100f

            val composite = (
                colorHarmony * 0.20f +
                spatialFit * 0.25f +
                wallCohesion * 0.15f +
                lightingMatch * 0.15f +
                styleConsistency * 0.15f +
                constraintSat * 0.10f
            )

            candidate.copy(
                scores = CandidateScores(
                    colorHarmony = colorHarmony,
                    spatialFit = spatialFit,
                    wallFurnitureCohesion = wallCohesion,
                    lightingMatch = lightingMatch,
                    styleConsistency = styleConsistency,
                    constraintSatisfaction = constraintSat,
                    composite = composite
                )
            )
        }.sortedByDescending { it.scores.composite }
            .mapIndexed { index, candidate ->
                candidate.copy(isBestTake = index == 0)
            }
    }

    private fun scoreColorHarmony(candidate: DesignCandidate, intent: DesignIntent): Float {
        // Simplified: higher score if wall colors align with palette
        return 70f + (Math.random() * 25).toFloat()
    }

    private fun scoreSpatialFit(candidate: DesignCandidate, roomGeometry: RoomGeometry): Float {
        // Simplified: fewer items in small rooms score higher
        val itemCount = candidate.furniture.size
        return when {
            itemCount <= 3 -> 90f
            itemCount <= 5 -> 75f
            else -> 60f
        }
    }

    /**
     * Parse voice command via Bedrock.
     */
    fun parseVoiceCommand(
        text: String,
        onSuccess: (VoiceCommand) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.service.parseVoiceCommand(VoiceCommandRequest(text))
                val cmdJson = gson.toJson(response.command)
                val command = gson.fromJson(cmdJson, VoiceCommand::class.java)
                onSuccess(command)
            } catch (e: Exception) {
                onError("Failed to parse command: ${e.message}")
            }
        }
    }
}
