package com.arinterior.engine.data.model

/**
 * Furniture catalog item from DynamoDB.
 */
data class CatalogItem(
    val id: String,
    val name: String,
    val category: String,
    val placement: PlacementType = PlacementType.FLOOR,
    val styleTags: List<String> = emptyList(),
    val dimensions: ItemDimensions = ItemDimensions(),
    val modelS3Key: String = "",
    val thumbnailUrl: String = "",
    val priceInr: Double? = null,
    val material: String = "",
    val colorHex: String = "",
    val emissive: Boolean = false,
    val defaultWallHeight: Float? = null
)

enum class PlacementType { FLOOR, WALL }

data class ItemDimensions(
    val width: Float = 0f,
    val height: Float = 0f,
    val depth: Float = 0f
)
