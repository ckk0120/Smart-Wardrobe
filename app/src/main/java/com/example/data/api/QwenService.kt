package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ClothingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "QwenService"
    private const val MODEL_NAME = "qwen-plus"
    private const val API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    fun hasValidKey(): Boolean {
        val key = BuildConfig.QWEN_API_KEY
        return !key.isNullOrEmpty() && key != "MY_QWEN_API_KEY"
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeClothingItem(
        name: String,
        notes: String,
        bitmap: Bitmap? = null
    ): AttributeResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.QWEN_API_KEY
        if (!hasValidKey()) {
            return@withContext AttributeResult(
                category = ClothingItem.CATEGORY_TOPS,
                color = "White",
                tags = listOf("经典", "简约"),
                error = "API Key is missing. Please set your QWEN_API_KEY in secrets."
            )
        }

        val promptText = """
            你是一个专业时尚设计师与智能试衣助手。请分析这件衣服的信息（名称：$name，备注：$notes）。
            你的返回必须只是一个 JSON 字符串，不要 markdown，不要解释。
            JSON 结构如下：
            {
              "category": "Tops" or "Bottoms" or "Shoes" or "Outerwear" or "Accessories",
              "color": "White/Black/Navy/Beige/Gray/Brown/Khaki/Blue/Green/Red/Orange/Pink/Purple/Yellow",
              "season": "Spring" or "Summer" or "Autumn" or "Winter" or "All",
              "tags": ["标签1", "标签2", "标签3"],
              "clothing_notes": "一句中文穿搭建议"
            }
        """.trimIndent()

        try {
            val messages = JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put(
                        "content",
                        buildMessageContent(promptText, bitmap)
                    )
            )

            val rootReq = JSONObject()
                .put("model", MODEL_NAME)
                .put("messages", messages)
                .put("temperature", 0.2)

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(rootReq.toString().toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext AttributeResult(
                    category = "Tops",
                    color = "White",
                    tags = listOf("经典"),
                    error = "请求API失败（状态码: ${response.code}）"
                )
            }

            val responseStr = response.body?.string().orEmpty()
            val jsonResponse = JSONObject(responseStr)
            val rawText = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val cleanedJson = rawText.replace("```json", "").replace("```", "").trim()
            val parsedObj = JSONObject(cleanedJson)
            val tagsArr = parsedObj.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) tags.add(tagsArr.getString(i))
            }

            return@withContext AttributeResult(
                category = parsedObj.optString("category", "Tops"),
                color = parsedObj.optString("color", "White"),
                season = parsedObj.optString("season", "All"),
                tags = if (tags.isNotEmpty()) tags else listOf("智能", "简约"),
                notesText = parsedObj.optString("clothing_notes", "衣服搭配很协调")
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

    suspend fun getSmartRecommendation(
        closetItems: List<ClothingItem>,
        weatherContext: String,
        scenarioContext: String
    ): RecommendationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.QWEN_API_KEY
        if (!hasValidKey()) {
            return@withContext RecommendationResult(
                error = "API Key is missing. Please set your QWEN_API_KEY in secrets."
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
            你是一个专业穿搭顾问。请根据以下衣橱信息为用户推荐一套搭配：

            ${itemsDesc.toString()}

            当前环境：
            - 天气/气温/场景: $weatherContext
            - 场景风格: $scenarioContext

            请只返回 JSON，不要 markdown，不要解释。
            JSON 格式：
            {
              "outfitName": "示例",
              "topId": 1,
              "bottomId": 2,
              "shoesId": 3,
              "outerwearId": null,
              "accessoryId": null,
              "suggestionReason": "50字内中文理由"
            }
        """.trimIndent()

        try {
            val rootReq = JSONObject()
                .put("model", MODEL_NAME)
                .put(
                    "messages",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", promptText)
                    )
                )
                .put("temperature", 0.2)

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(rootReq.toString().toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext RecommendationResult(
                    error = "请求API失败（状态码: ${response.code}）"
                )
            }

            val responseStr = response.body?.string().orEmpty()
            val jsonResponse = JSONObject(responseStr)
            val rawText = jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val cleanedJson = rawText.replace("```json", "").replace("```", "").trim()
            val parsedObj = JSONObject(cleanedJson)

            return@withContext RecommendationResult(
                outfitName = parsedObj.optString("outfitName", "AI智能推荐穿搭"),
                topId = if (parsedObj.isNull("topId")) null else parsedObj.optInt("topId"),
                bottomId = if (parsedObj.isNull("bottomId")) null else parsedObj.optInt("bottomId"),
                shoesId = if (parsedObj.isNull("shoesId")) null else parsedObj.optInt("shoesId"),
                outerwearId = if (parsedObj.isNull("outerwearId")) null else parsedObj.optInt("outerwearId"),
                accessoryId = if (parsedObj.isNull("accessoryId")) null else parsedObj.optInt("accessoryId"),
                suggestionReason = parsedObj.optString("suggestionReason", "这套搭配很协调。")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendation: ", e)
            return@withContext RecommendationResult(
                error = "智能分析出错，请重试或检查网络连接。"
            )
        }
    }

    private fun buildMessageContent(promptText: String, bitmap: Bitmap?): Any {
        if (bitmap == null) return promptText
        return JSONArray()
            .put(JSONObject().put("type", "text").put("text", promptText))
            .put(
                JSONObject()
                    .put("type", "image_url")
                    .put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,${bitmap.toBase64()}")
                    )
            )
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
