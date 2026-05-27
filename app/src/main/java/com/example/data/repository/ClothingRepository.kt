package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.ClothingDao
import com.example.data.model.ClothingItem
import com.example.data.model.Outfit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClothingRepository(private val clothingDao: ClothingDao) {

    val allClothingItems: Flow<List<ClothingItem>> = clothingDao.getAllClothingItems()
    val allOutfits: Flow<List<Outfit>> = clothingDao.getAllOutfits()

    init {
        // Pre-populate sample wardrobe items if empty on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentItems = clothingDao.getAllClothingItems().first()
                if (currentItems.isEmpty()) {
                    Log.d("ClothingRepository", "Wardrobe is empty, pre-populating starter closet items...")
                    getStarterItems().forEach { item ->
                        clothingDao.insertClothingItem(item)
                    }
                }
            } catch (e: Exception) {
                Log.e("ClothingRepository", "Error pre-populating starter items: ${e.message}")
            }
        }
    }

    suspend fun getClothingItemById(id: Int): ClothingItem? = withContext(Dispatchers.IO) {
        clothingDao.getClothingItemById(id)
    }

    suspend fun getClothingItemsByIds(ids: List<Int>): List<ClothingItem> = withContext(Dispatchers.IO) {
        clothingDao.getClothingItemsByIds(ids)
    }

    suspend fun insertClothingItem(item: ClothingItem): Long = withContext(Dispatchers.IO) {
        clothingDao.insertClothingItem(item)
    }

    suspend fun deleteClothingItem(item: ClothingItem) = withContext(Dispatchers.IO) {
        clothingDao.deleteClothingItem(item)
    }

    suspend fun insertOutfit(outfit: Outfit): Long = withContext(Dispatchers.IO) {
        clothingDao.insertOutfit(outfit)
    }

    suspend fun deleteOutfit(outfit: Outfit) = withContext(Dispatchers.IO) {
        clothingDao.deleteOutfit(outfit)
    }

    suspend fun updateOutfit(outfit: Outfit) = withContext(Dispatchers.IO) {
        clothingDao.updateOutfit(outfit)
    }

    private fun getStarterItems(): List<ClothingItem> {
        return listOf(
            ClothingItem(
                name = "白色棉质经典短袖 T恤",
                category = ClothingItem.CATEGORY_TOPS,
                color = "White",
                season = "Summer",
                tags = "舒适, 简约, 经典纯色, 百搭",
                imageUri = "tpl_white_tshirt",
                notes = "万能内搭，舒适透气。"
            ),
            ClothingItem(
                name = "黑色复古立领皮夹克",
                category = ClothingItem.CATEGORY_OUTERBODY,
                color = "Black",
                season = "Autumn",
                tags = "皮质, 保暖, 机车风, 帅气",
                imageUri = "tpl_black_jacket",
                notes = "帅气有型，防风效果极佳。"
            ),
            ClothingItem(
                name = "深蓝色复古修身牛仔裤",
                category = ClothingItem.CATEGORY_BOTTOMS,
                color = "Navy",
                season = "All",
                tags = "丹宁, 经典, 休闲, 耐磨",
                imageUri = "tpl_navy_jeans",
                notes = "修身剪裁，经典百搭，四季可穿。"
            ),
            ClothingItem(
                name = "浅灰色连帽温暖卫衣",
                category = ClothingItem.CATEGORY_TOPS,
                color = "Gray",
                season = "Spring",
                tags = "连帽, 宽松, 潮流, 慵懒",
                imageUri = "tpl_gray_hoodie",
                notes = "春秋单穿或叠穿外套都很棒。"
            ),
            ClothingItem(
                name = "经典拼色复古休闲小白鞋",
                category = ClothingItem.CATEGORY_SHOES,
                color = "White",
                season = "All",
                tags = "平底, 舒适, 运动, 百搭风",
                imageUri = "tpl_white_sneakers",
                notes = "出行极度舒适，搭配任何裤裝都很配。"
            ),
            ClothingItem(
                name = "卡其色多口袋直筒工装裤",
                category = ClothingItem.CATEGORY_BOTTOMS,
                color = "Khaki",
                season = "All",
                tags = "多袋, 街头, 纯棉, 硬朗",
                imageUri = "tpl_khaki_pants",
                notes = "日系复古，功能袋多，美式街头风。"
            ),
            ClothingItem(
                name = "黑色刺绣棒球帽",
                category = ClothingItem.CATEGORY_ACCESSORIES,
                color = "Black",
                season = "All",
                tags = "遮阳, 户外, 嘻哈, 懒人单品",
                imageUri = "tpl_black_cap",
                notes = "不想整理头发时的懒人救星。"
            )
        )
    }
}
