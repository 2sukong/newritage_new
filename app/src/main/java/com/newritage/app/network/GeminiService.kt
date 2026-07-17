package com.newritage.app.network

import android.util.Log
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

/**
 * Gemini API 연동 테스트용 서비스.
 * 현재는 API 연결 확인이 목적이며, 명상 데이터/일기/매듭 추천 등 실제 기능에는 사용하지 않는다.
 */
object GeminiService {

    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-2.5-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient()

    /** [prompt]를 Gemini에 보내고 응답 내용을 Logcat에 출력한다. */
    suspend fun sendTestPrompt(prompt: String = "테스트입니다.") = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            Log.e(TAG, "GEMINI_API_KEY가 비어 있습니다. local.properties에 키를 설정해주세요.")
            return@withContext
        }

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().apply {
                    put("parts", JSONArray().put(
                        JSONObject().apply { put("text", prompt) }
                    ))
                }
            ))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("x-goog-api-key", apiKey)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API 호출 실패 (${response.code}): $bodyStr")
                    return@withContext
                }
                val content = JSONObject(bodyStr)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                Log.d(TAG, "Gemini 응답: $content")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Gemini API 네트워크 오류", e)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API 응답 처리 오류", e)
        }
    }
}
