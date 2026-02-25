package com.arinterior.engine.data.model

/**
 * Structured design intent extracted by Bedrock from a user prompt.
 */
data class DesignIntent(
    val style: String,
    val palette: Palette,
    val objects: List<ObjectRequest>,
    val wallTreatment: WallTreatment,
    val wallDecor: List<WallDecorRequest>,
    val accentLighting: List<LightingRequest>,
    val constraints: DesignConstraints
)

data class Palette(
    val primary: String,
    val secondary: String,
    val accent: String
)

data class ObjectRequest(
    val type: String,
    val style: String? = null,
    val material: String? = null,
    val color: String? = null
)

data class WallTreatment(
    val color: String,
    val texture: String,
    val accentWall: AccentWall? = null
)

data class AccentWall(
    val wallIndex: Int,
    val color: String
)

data class WallDecorRequest(
    val type: String,
    val style: String? = null,
    val size: String? = null
)

data class LightingRequest(
    val type: String,
    val position: String? = null,
    val warmth: String? = null,
    val color: String? = null
)

data class DesignConstraints(
    val budgetMax: Double? = null,
    val currency: String? = null
)
