package com.example.data.db

import androidx.room.*
import com.example.data.model.ClothingItem
import com.example.data.model.Outfit
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingDao {
    // --- Clothing Items Queries ---
    @Query("SELECT * FROM clothing_items ORDER BY timestamp DESC")
    fun getAllClothingItems(): Flow<List<ClothingItem>>

    @Query("SELECT * FROM clothing_items WHERE category = :category ORDER BY timestamp DESC")
    fun getClothingItemsByCategory(category: String): Flow<List<ClothingItem>>

    @Query("SELECT * FROM clothing_items WHERE id = :id LIMIT 1")
    suspend fun getClothingItemById(id: Int): ClothingItem?

    @Query("SELECT * FROM clothing_items WHERE id IN (:ids)")
    suspend fun getClothingItemsByIds(ids: List<Int>): List<ClothingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClothingItem(item: ClothingItem): Long

    @Delete
    suspend fun deleteClothingItem(item: ClothingItem)

    // --- Outfits Queries ---
    @Query("SELECT * FROM outfits ORDER BY timestamp DESC")
    fun getAllOutfits(): Flow<List<Outfit>>

    @Query("SELECT * FROM outfits WHERE id = :id LIMIT 1")
    suspend fun getOutfitById(id: Int): Outfit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: Outfit): Long

    @Delete
    suspend fun deleteOutfit(outfit: Outfit)
    
    @Update
    suspend fun updateOutfit(outfit: Outfit)
}
