package com.arinterior.engine.data.model

/**
 * A design candidate with placed items and scoring.
 */
data class DesignCandidate(
    val id: String,
    val name: String,
    val furniture: List<PlacedItem>,
    val wallDesign: WallDesign,
    val wallElements: List<PlacedWallItem>,
    val scores: CandidateScores,
    val isBestTake: Boolean = false
)

data class WallDesign(
    val primaryColor: String,
    val accentWall: AccentWallDesign? = null,
    val texture: String = "matte"
)

data class AccentWallDesign(
    val wallId: String,
    val color: String
)

data class CandidateScores(
    val colorHarmony: Float = 0f,
    val spatialFit: Float = 0f,
    val wallFurnitureCohesion: Float = 0f,
    val lightingMatch: Float = 0f,
    val styleConsistency: Float = 0f,
    val constraintSatisfaction: Float = 0f,
    val composite: Float = 0f
)

data class PlacedItem(
    val itemId: String,
    val catalogItem: CatalogItem? = null,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val positionZ: Float = 0f,
    val rotationY: Float = 0f,
    val scale: Float = 1f
)

data class PlacedWallItem(
    val itemId: String,
    val catalogItem: CatalogItem? = null,
    val wallId: String = "wall_0",
    val positionX: Float = 0f,
    val positionY: Float = 1.5f,
    val positionZ: Float = 0f,
    val scale: Float = 1f,
    val emissive: Boolean = false,
    val lightColor: String? = null
)
