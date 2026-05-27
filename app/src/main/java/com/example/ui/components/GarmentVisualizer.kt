package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClothingItem

@Composable
fun GarmentVisualizer(
    category: String,
    colorName: String,
    modifier: Modifier = Modifier
) {
    val garmentColor = getColorFromName(colorName)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        garmentColor.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic gradient circle behind
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(garmentColor.copy(alpha = 0.7f))
                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        )

        // Draw illustrative shape or render specialized icon with high contrast
        val iconModifier = Modifier.size(36.dp)
        val iconColor = if (isColorDark(garmentColor)) Color.White else Color.Black

        when (category) {
            ClothingItem.CATEGORY_TOPS -> {
                // Render Top T-shirt outline or custom Checkroom icon
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = "上衣",
                    modifier = iconModifier,
                    tint = iconColor
                )
            }
            ClothingItem.CATEGORY_BOTTOMS -> {
                // Bottoms icon (using layered categories or specialized shape)
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "下装",
                    modifier = iconModifier,
                    tint = iconColor
                )
            }
            ClothingItem.CATEGORY_SHOES -> {
                Icon(
                    imageVector = Icons.Default.ElectricBolt, // Represents sporty active sneakers energy!
                    contentDescription = "鞋子",
                    modifier = iconModifier,
                    tint = iconColor
                )
            }
            ClothingItem.CATEGORY_OUTERBODY -> {
                Icon(
                    imageVector = Icons.Default.DryCleaning, // Represent coat / dry cleaning
                    contentDescription = "外套",
                    modifier = iconModifier,
                    tint = iconColor
                )
            }
            else -> {
                // Accessories
                Icon(
                    imageVector = Icons.Default.Info, // Hanger badge/hat / sunglasses accessory
                    contentDescription = "配件",
                    modifier = iconModifier,
                    tint = iconColor
                )
            }
        }

        // Mini Badge showing category initial
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = when (category) {
                    ClothingItem.CATEGORY_TOPS -> "衣"
                    ClothingItem.CATEGORY_BOTTOMS -> "裤"
                    ClothingItem.CATEGORY_SHOES -> "鞋"
                    ClothingItem.CATEGORY_OUTERBODY -> "外"
                    else -> "配"
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

/**
 * Returns Compose Color object matching English design color constants
 */
fun getColorFromName(colorName: String): Color {
    return when (colorName.lowercase().trim()) {
        "white" -> Color.White
        "black" -> Color(0xFF1F1F1F)
        "gray" -> Color(0xFF9E9E9E)
        "navy" -> Color(0xFF1B365D)
        "blue" -> Color(0xFF4A90E2)
        "red" -> Color(0xFFD0021B)
        "green" -> Color(0xFF417505)
        "yellow" -> Color(0xFFF8E71C)
        "orange" -> Color(0xFFF5A623)
        "pink" -> Color(0xFFF5A623).copy(green = 0.5f) // Pinkish
        "purple" -> Color(0xFF9013FE)
        "beige" -> Color(0xFFF4EBE1)
        "khaki" -> Color(0xFFC3B091)
        "brown" -> Color(0xFF8B572A)
        else -> Color(0xFFDCD1BF) // Gold Border default
    }
}

/**
 * Returns true if the background is dark to assure black/white icon contrast readability
 */
fun isColorDark(color: Color): Boolean {
    // Standard luminance formula
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}
