package com.arinterior.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wall color picker dialog with preset colors and custom input.
 */
@Composable
fun WallColorPicker(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presetColors = listOf(
        "#F5F0EB" to "Warm White",
        "#FFFFFF" to "Pure White",
        "#E8E0D8" to "Cream",
        "#D4C5B0" to "Beige",
        "#C5D5C5" to "Sage Green",
        "#B0C4DE" to "Light Blue",
        "#D4B8A0" to "Warm Sand",
        "#A0A0A0" to "Light Gray",
        "#3D3D3D" to "Charcoal",
        "#1A1A2E" to "Navy",
        "#8B7355" to "Warm Brown",
        "#A0785A" to "Terracotta",
        "#FFE4B5" to "Peach",
        "#E6D5E6" to "Lavender",
        "#D5E6D5" to "Mint",
        "#FFD700" to "Gold Accent"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Wall Color", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(presetColors) { (hex, name) ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    val isSelected = hex.equals(currentColor, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected)
                                    Modifier.border(3.dp, Color(0xFF00D4FF), CircleShape)
                                else
                                    Modifier.border(1.dp, Color(0x40FFFFFF), CircleShape)
                            )
                            .clickable { onColorSelected(hex) },
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFF00D4FF))
            }
        },
        containerColor = Color(0xFF1A1A2E)
    )
}
