package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ClothingItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    /**
     * Check if the API key is configured
     */
    fun hasValidKey(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Convert Bitmap to Base64 String
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Analyzes clothing image or name details and returns structured tags and classification.
     */
    suspend fun analyzeClothingItem(
        name: String,
        notes: String,
        bitmap: Bitmap? = null
    ): AttributeResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasValidKey()) {
            return@withContext AttributeResult(
                category = ClothingItem.CATEGORY_TOPS,
                color = "White",
                tags = listOf("经典", "简约"),
                error = "API Key is missing. Please set your GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        val promptText = """
            你是一个专业时尚设计师与智能试衣助手。请分析这件衣服的信息（名称：“$name”，备注：“$notes”）。
            确定其最符合的属性。你的返回必须是一个且仅含有一个JSON格式的原始字符串，不要任何markdown格式包裹（不要```json，不要任何前后文本注解）。
            
            JSON结构必须严格如下：
            {
              "category": "Tops" 或 "Bottoms" 或 "Shoes" 或 "Outerwear" 或 "Accessories",
              "color": "其最亮眼或主要的颜色，采用英文如 White/Black/Navy/Beige/Gray/Brown/Khaki/Blue/Green/Red/Orange/Pink/Purple/Yellow",
              "season": "Spring" 或 "Summer" 或 "Autumn" 或 "Winter" 或 "All",
              "tags": ["三个代表材质或风格甚至版型的中文短标签1", "标签2", "标签3"],
              "clothing_notes": "一句话推荐搭配意见或面料特点（中文）"
            }
            
            注意类别必须是以下五者之一：Tops (上衣), Bottoms (下装), Shoes (鞋子), Outerwear (外套), Accessories (配饰)。
        """.trimIndent()

        try {
            // Build root content block
            val partsArray = JSONArray()

            // 1. Text part
            val textPart = JSONObject().put("text", promptText)
            partsArray.put(textPart)

            // 2. Inline Image part if bitmap is provided
            if (bitmap != null) {
                val base64Data = bitmap.toBase64()
                val inlineDataObj = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Data)
                val imagePart = JSONObject().put("inlineData", inlineDataObj)
                partsArray.put(imagePart)
            }

            val contentsElement = JSONObject()
                .put("parts", partsArray)

            val rootReq = JSONObject()
                .put("contents", JSONArray().put(contentsElement))

            val requestBodyJson = rootReq.toString()
            Log.d(TAG, "Request payload built successfully. Image included: ${bitmap != null}")

            val request = Request.Builder()
                .url("$API_URL?key=$apiKey")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API call failed code: ${response.code}, message: ${response.message}")
                return@withContext AttributeResult(
                    category = "Tops",
                    color = "White",
                    tags = listOf("经典"),
                    error = "请求API失败（状态码: ${response.code}）"
                )
            }

            val responseStr = response.body?.string() ?: ""
            Log.d(TAG, "Raw response: $responseStr")

            val jsonResponse = JSONObject(responseStr)
            val candidates = jsonResponse.getJSONArray("candidates")
            val rawText = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            // Clean any trailing/leading md bounds if generated
            val cleanedJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d(TAG, "Cleaned AI response: $cleanedJson")

            val parsedObj = JSONObject(cleanedJson)
            val category = parsedObj.optString("category", "Tops")
            val color = parsedObj.optString("color", "White")
            val season = parsedObj.optString("season", "All")
            val tagsArr = parsedObj.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    tags.add(tagsArr.getString(i))
                }
            } else {
                tags.add("智能")
                tags.add("简约")
            }
            val clothingNotes = parsedObj.optString("clothing_notes", "衣服搭配极佳")

            return@withContext AttributeResult(
                category = category,
                color = color,
                season = season,
                tags = tags,
                notesText = clothingNotes
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing clothing: ", e)
            return@withContext AttributeResult(
                category = "Tops",
                color = "White",
                tags = listOf("分析出错"),
                error = e.localizedMessage ?: "未知错误"
            )
        }
    }

    /**
     * Request outfits recommendations using Gemini API based on a closet listing and user filters
     */
    suspend fun getSmartRecommendation(
        closetItems: List<ClothingItem>,
        weatherContext: String,
        scenarioContext: String
    ): RecommendationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasValidKey()) {
            return@withContext RecommendationResult(
                error = "API Key is missing. Please configured GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        if (closetItems.isEmpty()) {
            return@withContext RecommendationResult(
                error = "您的衣橱里还没有任何衣服哦，请先添加一些衣服再尝试推荐吧！"
            )
        }

        val itemsDesc = StringBuilder()
        closetItems.forEach { item ->
            itemsDesc.append("ID: ${item.id} | 名称: ${item.name} | 类别: ${item.category} | 颜色: ${item.color} | 季节: ${item.season} | 标签: ${item.tags}\n")
        }

        val promptText = """
            你是一个极高尚的私人服装管家与美学穿搭大师。请根据用户当前的衣橱物品：
            
            ${itemsDesc.toString()}
            
            根据当前环境：
            - 天气/气温/情况: $weatherContext
            - 场景风格: $scenarioContext
            
            为你挑选衣服以组成一套最合理的穿搭。在选择时，必须保证上衣(Tops)和下装(Bottoms)是合理的搭配，鞋子(Shoes)必须合称。外套(Outerwear)和配饰(Accessories)是可选的。
            注意：挑选的物品ID必须存在于衣橱列表中。尽量使颜色风格呼应。
            
            你的返回必须且仅能是一个最纯净的原始JSON结构，不要包含任何前后的Markdown标签、```json包裹或解释文本。
            
            JSON结果格式严格如下:
            {
              "outfitName": "例如：极简干练职场风",
              "topId": 你选配的上衣的ID（Int，如无合适则为null）,
              "bottomId": 你选配的下装的ID（Int，如无合适则为null）,
              "shoesId": 你选配的鞋子的ID（Int，如无合适则为null）,
              "outerwearId": 你选配的外套的ID（Int，如无合适则为null，可选）,
              "accessoryId": 你选配的配饰的ID（Int，如无合适则为null，可选）,
              "suggestionReason": "详细告诉用户你为什么这么选，这套颜色怎么好看，如何配合该天气和穿搭场景，字数在150字以内。（采用温馨、鼓励性的中文）"
            }
        """.trimIndent()

        try {
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", promptText))
            
            val contentsElement = JSONObject().put("parts", partsArray)
            val rootReq = JSONObject().put("contents", JSONArray().put(contentsElement))

            val requestBodyJson = rootReq.toString()
            val request = Request.Builder()
                .url("$API_URL?key=$apiKey")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext RecommendationResult(
                    error = "请求API失败（状态码: ${response.code}）"
                )
            }

            val responseStr = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseStr)
            val candidates = jsonResponse.getJSONArray("candidates")
            val rawText = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            val cleanedJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d(TAG, "Outfit recommendation raw: $cleanedJson")

            val parsedObj = JSONObject(cleanedJson)
            val outfitName = parsedObj.optString("outfitName", "AI智能推荐穿搭")
            
            val topIdVal = if (parsedObj.isNull("topId")) null else parsedObj.optInt("topId")
            val bottomIdVal = if (parsedObj.isNull("bottomId")) null else parsedObj.optInt("bottomId")
            val shoesIdVal = if (parsedObj.isNull("shoesId")) null else parsedObj.optInt("shoesId")
            val outerwearIdVal = if (parsedObj.isNull("outerwearId")) null else parsedObj.optInt("outerwearId")
            val accessoryIdVal = if (parsedObj.isNull("accessoryId")) null else parsedObj.optInt("accessoryId")
            
            val reason = parsedObj.optString("suggestionReason", "这套衣服颜色配搭和谐，是您绝妙的选择。")

            return@withContext RecommendationResult(
                outfitName = outfitName,
                topId = topIdVal,
                bottomId = bottomIdVal,
                shoesId = shoesIdVal,
                outerwearId = outerwearIdVal,
                accessoryId = accessoryIdVal,
                suggestionReason = reason
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendation: ", e)
            return@withContext RecommendationResult(
                error = "智能分析出错，请重试或者是检查您的网络连接。"
            )
        }
    }
}

data class AttributeResult(
    val category: String,
    val color: String,
    val season: String = "All",
    val tags: List<String>,
    val notesText: String = "",
    val error: String? = null
)

data class RecommendationResult(
    val outfitName: String = "推荐穿搭",
    val topId: Int? = null,
    val bottomId: Int? = null,
    val shoesId: Int? = null,
    val outerwearId: Int? = null,
    val accessoryId: Int? = null,
    val suggestionReason: String = "",
    val error: String? = null
)
