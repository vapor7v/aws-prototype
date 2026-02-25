package com.arinterior.engine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinterior.engine.data.model.DesignCandidate

/**
 * Horizontal scrollable candidate cards — tap to swap AR scene.
 */
@Composable
fun CandidateCarousel(
    candidates: List<DesignCandidate>,
    selectedCandidate: DesignCandidate?,
    onCandidateSelected: (DesignCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(candidates) { candidate ->
            CandidateCard(
                candidate = candidate,
                isSelected = candidate.id == selectedCandidate?.id,
                onClick = { onCandidateSelected(candidate) }
            )
        }
    }
}

@Composable
fun CandidateCard(
    candidate: DesignCandidate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            candidate.isBestTake -> Color(0xFFD4AF37) // Gold for Best Take
            isSelected -> Color(0xFF00D4FF)
            else -> Color(0x40FFFFFF)
        },
        label = "border"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC1A1A2E)
        ),
        border = BorderStroke(
            width = if (isSelected || candidate.isBestTake) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top — Best Take badge or name
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (candidate.isBestTake) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Best Take",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Best Take",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                } else {
                    Text(
                        text = candidate.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Middle — candidate name (for Best Take) or preview colors
            if (candidate.isBestTake) {
                Text(
                    text = candidate.name,
                    fontSize = 11.sp,
                    color = Color(0xB0FFFFFF),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom — score
            Text(
                text = "Score: ${candidate.scores.composite.toInt()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (candidate.isBestTake) Color(0xFFD4AF37) else Color(0xFF00D4FF)
            )
        }
    }
}
