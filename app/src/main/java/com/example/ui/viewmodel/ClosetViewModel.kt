package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AttributeResult
import com.example.data.api.GeminiService
import com.example.data.api.RecommendationResult
import com.example.data.db.AppDatabase
import com.example.data.model.ClothingItem
import com.example.data.model.Outfit
import com.example.data.repository.ClothingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClosetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ClothingRepository(db.clothingDao())

    // Expose all clothes and outfits
    val allItems = repository.allClothingItems
    val outfits = repository.allOutfits

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedSeason = MutableStateFlow("All")

    // Reactive filtered clothing items list
    val filteredItems: StateFlow<List<ClothingItem>> = combine(
        allItems,
        searchQuery,
        selectedCategory,
        selectedSeason
    ) { items, query, cat, season ->
        items.filter { item ->
            val matchesQuery = item.name.contains(query, ignoreCase = true) || 
                               item.tags.contains(query, ignoreCase = true) ||
                               item.notes.contains(query, ignoreCase = true)
            val matchesCategory = cat == "All" || item.category == cat
            val matchesSeason = season == "All" || item.season == season || item.season == "All"
            matchesQuery && matchesCategory && matchesSeason
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected items for manual outfit draft
    val draftTop = MutableStateFlow<ClothingItem?>(null)
    val draftBottom = MutableStateFlow<ClothingItem?>(null)
    val draftShoes = MutableStateFlow<ClothingItem?>(null)
    val draftOuterwear = MutableStateFlow<ClothingItem?>(null)
    val draftAccessory = MutableStateFlow<ClothingItem?>(null)

    // UI States
    val isApiKeyAvailable = MutableStateFlow(GeminiService.hasValidKey())

    private val _aiRecommendationState = MutableStateFlow<AiRecommendState>(AiRecommendState.Idle)
    val aiRecommendationState: StateFlow<AiRecommendState> = _aiRecommendationState.asStateFlow()

    private val _addClothingState = MutableStateFlow<AddClothingState>(AddClothingState.Idle)
    val addClothingState: StateFlow<AddClothingState> = _addClothingState.asStateFlow()

    fun updateApiKeyStatus() {
        isApiKeyAvailable.value = GeminiService.hasValidKey()
    }

    // --- Actions ---

    fun insertClothing(
        name: String,
        category: String,
        color: String,
        season: String,
        tags: String,
        notes: String,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            _addClothingState.value = AddClothingState.Saving
            try {
                val item = ClothingItem(
                    name = name,
                    category = category,
                    color = color,
                    season = season,
                    tags = tags,
                    notes = notes,
                    imageUri = imageUri
                )
                repository.insertClothingItem(item)
                _addClothingState.value = AddClothingState.Success
            } catch (e: Exception) {
                _addClothingState.value = AddClothingState.Error(e.localizedMessage ?: "保存物品失败")
            }
        }
    }

    fun deleteClothing(item: ClothingItem) {
        viewModelScope.launch {
            repository.deleteClothingItem(item)
            
            // Clean drafts if deleted
            if (draftTop.value?.id == item.id) draftTop.value = null
            if (draftBottom.value?.id == item.id) draftBottom.value = null
            if (draftShoes.value?.id == item.id) draftShoes.value = null
            if (draftOuterwear.value?.id == item.id) draftOuterwear.value = null
            if (draftAccessory.value?.id == item.id) draftAccessory.value = null
        }
    }

    fun runSmartClothingAnalysis(
        name: String,
        notes: String,
        bitmap: Bitmap?,
        onCompleted: (AttributeResult) -> Unit
    ) {
        viewModelScope.launch {
            _addClothingState.value = AddClothingState.AiAnalyzing
            try {
                val result = GeminiService.analyzeClothingItem(name, notes, bitmap)
                _addClothingState.value = AddClothingState.Idle
                onCompleted(result)
            } catch (e: Exception) {
                _addClothingState.value = AddClothingState.Error("AI 建议获取失败: ${e.message}")
            }
        }
    }

    fun resetAddClothingState() {
        _addClothingState.value = AddClothingState.Idle
    }

    // --- Outfits ---

    fun loadDraftFromRecommendation(result: RecommendationResult, items: List<ClothingItem>) {
        draftTop.value = items.find { it.id == result.topId }
        draftBottom.value = items.find { it.id == result.bottomId }
        draftShoes.value = items.find { it.id == result.shoesId }
        draftOuterwear.value = items.find { it.id == result.outerwearId }
        draftAccessory.value = items.find { it.id == result.accessoryId }
    }

    fun generateAiOutfitRecommendation(weather: String, scenario: String) {
        viewModelScope.launch {
            _aiRecommendationState.value = AiRecommendState.Loading
            try {
                val items = allItems.first()
                if (items.isEmpty()) {
                    _aiRecommendationState.value = AiRecommendState.Error("您的衣橱还是空的，请先去添加衣服吧")
                    return@launch
                }

                val result = GeminiService.getSmartRecommendation(items, weather, scenario)
                if (result.error != null) {
                    _aiRecommendationState.value = AiRecommendState.Error(result.error)
                } else {
                    _aiRecommendationState.value = AiRecommendState.Success(result)
                    loadDraftFromRecommendation(result, items)
                }
            } catch (e: Exception) {
                _aiRecommendationState.value = AiRecommendState.Error("智能穿搭生成失败: ${e.localizedMessage}")
            }
        }
    }

    fun resetRecommendationState() {
        _aiRecommendationState.value = AiRecommendState.Idle
    }

    fun saveDraftAsOutfit(name: String, explanation: String? = null, scenario: String? = null) {
        viewModelScope.launch {
            try {
                val topId = draftTop.value?.id
                val bottomId = draftBottom.value?.id
                val shoesId = draftShoes.value?.id
                val outerId = draftOuterwear.value?.id
                val accId = draftAccessory.value?.id

                if (topId == null && bottomId == null && shoesId == null && outerId == null && accId == null) {
                    return@launch
                }

                val outfit = Outfit(
                    name = name,
                    topId = topId,
                    bottomId = bottomId,
                    shoesId = shoesId,
                    outerwearId = outerId,
                    accessoryId = accId,
                    aiRecommendationReason = explanation,
                    scenario = scenario ?: "日常",
                    isFavorite = true
                )
                repository.insertOutfit(outfit)
                
                // Clear draft after saving
                clearDraft()
            } catch (e: Exception) {
                Log.e("ClosetViewModel", "Error saving outfit: ${e.message}")
            }
        }
    }

    fun deleteOutfit(outfit: Outfit) {
        viewModelScope.launch {
            repository.deleteOutfit(outfit)
        }
    }

    fun clearDraft() {
        draftTop.value = null
        draftBottom.value = null
        draftShoes.value = null
        draftOuterwear.value = null
        draftAccessory.value = null
    }

    fun toggleFavoriteOutfit(outfit: Outfit) {
        viewModelScope.launch {
            val updated = outfit.copy(isFavorite = !outfit.isFavorite)
            repository.updateOutfit(updated)
        }
    }
}

sealed interface AiRecommendState {
    object Idle : AiRecommendState
    object Loading : AiRecommendState
    data class Success(val result: RecommendationResult) : AiRecommendState
    data class Error(val message: String) : AiRecommendState
}

sealed interface AddClothingState {
    object Idle : AddClothingState
    object AiAnalyzing : AddClothingState
    object Saving : AddClothingState
    object Success : AddClothingState
    data class Error(val message: String) : AddClothingState
}
