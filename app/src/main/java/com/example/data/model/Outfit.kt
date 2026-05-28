package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val topId: Int? = null,
    val bottomId: Int? = null,
    val shoesId: Int? = null,
    val outerwearId: Int? = null,
    val accessoryId: Int? = null,
    val isFavorite: Boolean = false,
    val aiRecommendationReason: String? = null, // Store Qwen recommended reasons
    val scenario: String? = null, // Meeting, Casual, Dating, Sports, Daily
    val timestamp: Long = System.currentTimeMillis()
)
