package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Tops, Bottoms, Shoes, Outerwear, Accessories
    val color: String, // White, Black, Navy, etc.
    val season: String, // Spring, Summer, Autumn, Winter, All
    val tags: String = "", // Comma-separated tags
    val imageUri: String? = null, // Path to local photograph or identifier of styled svg template (e.g., "tpl_white_tshirt")
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val CATEGORY_TOPS = "Tops"
        const val CATEGORY_BOTTOMS = "Bottoms"
        const val CATEGORY_SHOES = "Shoes"
        const val CATEGORY_OUTERBODY = "Outerwear"
        const val CATEGORY_ACCESSORIES = "Accessories"

        val ALL_CATEGORIES = listOf(
            CATEGORY_TOPS,
            CATEGORY_BOTTOMS,
            CATEGORY_SHOES,
            CATEGORY_OUTERBODY,
            CATEGORY_ACCESSORIES
        )

        val ALL_SEASONS = listOf("All", "Spring", "Summer", "Autumn", "Winter")
        
        val ALL_COLORS = listOf(
            "White", "Black", "Gray", "Navy", "Beige", "Brown", "Khaki", 
            "Red", "Blue", "Green", "Yellow", "Pink", "Purple", "Orange"
        )
    }
}
