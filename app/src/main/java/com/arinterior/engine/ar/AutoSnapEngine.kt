package com.arinterior.engine.ar

import com.arinterior.engine.data.model.*

/**
 * Auto-snap placement engine.
 * Applies smart placement rules based on furniture type and room geometry.
 * All logic runs on-device — no cloud calls.
 */
object AutoSnapEngine {

    /**
     * Snap a placed item to the best position based on its category and room geometry.
     */
    fun snapItem(
        item: PlacedItem,
        category: String,
        roomGeometry: RoomGeometry,
        existingItems: List<PlacedItem>
    ): PlacedItem {
        return when (category) {
            "seating" -> snapToWall(item, roomGeometry)
            "table" -> snapToCenterOfSeating(item, existingItems)
            "rug" -> snapUnderFurniture(item, existingItems)
            else -> item
        }
    }

    /**
     * Snap wall items to appropriate heights.
     */
    fun snapWallItem(
        item: PlacedWallItem,
        category: String,
        existingWallItems: List<PlacedWallItem>
    ): PlacedWallItem {
        return when (category) {
            "wall_art" -> item.copy(
                positionY = EYE_LEVEL_HEIGHT,  // 1.5m — eye level
                positionX = 0f                  // Centered on wall
            )
            "wall_light" -> {
                // Flank existing artwork if present
                val artOnSameWall = existingWallItems.firstOrNull {
                    it.wallId == item.wallId && !it.emissive
                }
                if (artOnSameWall != null) {
                    item.copy(
                        positionY = SCONCE_HEIGHT,
                        positionX = artOnSameWall.positionX + FLANK_OFFSET
                    )
                } else {
                    item.copy(positionY = SCONCE_HEIGHT)
                }
            }
            else -> item
        }
    }

    // ─── Snap Strategies ──────────────────────────────────

    /**
     * Sofas and large furniture snap to nearest wall with ~10cm gap.
     */
    private fun snapToWall(item: PlacedItem, room: RoomGeometry): PlacedItem {
        val wallGap = WALL_GAP
        // Snap to the closest wall plane
        val nearestWall = room.wallPlanes.minByOrNull { wall ->
            kotlin.math.abs(wall.centerZ - item.positionZ)
        }

        return if (nearestWall != null) {
            item.copy(
                positionZ = nearestWall.centerZ + wallGap,
                rotationY = 0f  // Face away from wall
            )
        } else {
            // No wall detected — place against back
            item.copy(positionZ = -(room.estimatedDimensions.length / 2 - wallGap))
        }
    }

    /**
     * Coffee tables snap to center of seating arrangement.
     */
    private fun snapToCenterOfSeating(item: PlacedItem, existingItems: List<PlacedItem>): PlacedItem {
        val seatingItems = existingItems.filter { it.itemId.contains("sofa") || it.itemId.contains("chair") }
        if (seatingItems.isEmpty()) return item

        val avgX = seatingItems.map { it.positionX }.average().toFloat()
        val avgZ = seatingItems.map { it.positionZ }.average().toFloat()

        return item.copy(
            positionX = avgX,
            positionZ = avgZ + TABLE_OFFSET  // Slightly in front of seating
        )
    }

    /**
     * Rugs snap centered under furniture grouping.
     */
    private fun snapUnderFurniture(item: PlacedItem, existingItems: List<PlacedItem>): PlacedItem {
        if (existingItems.isEmpty()) return item

        val avgX = existingItems.map { it.positionX }.average().toFloat()
        val avgZ = existingItems.map { it.positionZ }.average().toFloat()

        return item.copy(
            positionX = avgX,
            positionY = 0.01f,  // Just above floor
            positionZ = avgZ
        )
    }

    // ─── Constants ────────────────────────────────────────
    private const val WALL_GAP = 0.10f          // 10cm gap from wall
    private const val EYE_LEVEL_HEIGHT = 1.5f   // Painting center height
    private const val SCONCE_HEIGHT = 1.8f      // Sconce mounting height
    private const val FLANK_OFFSET = 0.6f       // Sconce offset from artwork center
    private const val MIN_SPACING = 0.5f        // Minimum gap between objects
    private const val TABLE_OFFSET = 0.8f       // Table distance in front of sofa
}
