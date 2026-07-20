package com.newritage.app.network

import com.newritage.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gemini(gemini-3.5-flash) generateContent API 호출만 담당하는 클라이언트.
 * 프롬프트 구성이나 DB 접근은 하지 않으며, 실패 시 예외를 던져 호출자([com.newritage.app.data.GeminiRepository])가
 * 로컬 로직으로 폴백할 수 있게 한다.
 */
object GeminiApi {

    private const val MODEL = "gemini-3.5-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * [systemPrompt]/[userPrompt]로 콘텐츠 생성 요청을 보내고 첫 응답 후보의 텍스트를 반환한다.
     * [imageBase64]를 함께 넘기면 이미지([imageMimeType])를 같은 user content의 parts에 추가해
     * Gemini의 비전 기능으로 이미지를 함께 해석하도록 요청한다.
     */
    suspend fun chatCompletion(
        systemPrompt: String,
        userPrompt: String,
        imageBase64: String? = null,
        imageMimeType: String = "image/png"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalStateException("GEMINI_API_KEY가 비어 있습니다. local.properties에 키를 설정해주세요.")
        }

        val userParts = JSONArray().put(JSONObject().apply { put("text", userPrompt) })
        if (imageBase64 != null) {
            userParts.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", imageMimeType)
                    put("data", imageBase64)
                })
            })
        }

        val requestBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
            })
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", userParts)
                }
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.5)
                put("maxOutputTokens", 512)
                // 사고(thinking) 과정에 출력 토큰 예산이 소모되어 답변이 중간에 잘리는 것을 막기 위해 비활성화한다.
                put("thinkingConfig", JSONObject().apply { put("thinkingBudget", 0) })
            })
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Gemini API 호출 실패 (${response.code}): $bodyStr")
            }
            JSONObject(bodyStr)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        }
    }
}
