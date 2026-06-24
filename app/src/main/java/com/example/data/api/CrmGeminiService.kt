package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CrmGeminiService {
    private const val TAG = "CrmGeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Evaluates a chat conversation between a client and the business.
     * Generates a suggested response and an analysis score.
     */
    suspend fun generateSuggestionAndScore(
        leadName: String,
        category: String,
        conversationHistory: List<Pair<Boolean, String>> // true for client, false for business
    ): GenerationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is empty or placeholder! Returning mock results.")
            return@withContext getFallbackResult(leadName)
        }

        val prompt = buildPrompt(leadName, category, conversationHistory)

        try {
            // Build Gemini JSON request body
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API failed with code: ${response.code}, message: ${response.message}")
                    return@withContext getFallbackResult(leadName)
                }

                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Response: $responseBodyStr")

                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        Log.d(TAG, "Parsed raw text: $text")

                        // Gemini returned JSON as requested in the prompt
                        val innerJson = JSONObject(text.trim())
                        return@withContext GenerationResult(
                            suggestion = innerJson.optString("suggestion", "Thanks for reaching out! We will review this and get back to you shortly."),
                            matchPercentage = innerJson.optInt("matchPercentage", 90),
                            leadScore = innerJson.optInt("leadScore", 85),
                            reason = innerJson.optString("reason", "Standard follow up suggestion")
                        )
                    }
                }
                getFallbackResult(leadName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            getFallbackResult(leadName)
        }
    }

    private fun buildPrompt(
        leadName: String,
        category: String,
        history: List<Pair<Boolean, String>>
    ): String {
        val historyStr = StringBuilder()
        history.forEach { (isClient, msg) ->
            if (isClient) {
                historyStr.append("Client ($leadName): \"$msg\"\n")
            } else {
                historyStr.append("Business Representative: \"$msg\"\n")
            }
        }

        return """
            You are FollowUp AI, a smart WhatsApp CRM and sales assistant for local businesses.
            Analyze the following WhatsApp chat conversation between a client and a local business specializing in "$category".
            
            Conversation history:
            $historyStr
            
            Task:
            1. Draft an elite, professional, friendly, and persuasive response to the client's last message. Use a tone that matches a premium local business. Use a call-to-action that encourages completing a booking, site visit, or quote.
            2. Compute a match/intent percentage (0-100) indicating how aligned the drafted response is with the client's explicit requests.
            3. Evaluate a Lead Score (0-100) indicating how hot this lead is based on their interest level, budget indicators, and intent to buy.
            4. Provide a brief 1-sentence reason explaining why this response is suitable.
            
            You MUST return ONLY a JSON object with the following exact keys:
            {
              "suggestion": "Your highly customized, beautifully written response draft",
              "matchPercentage": 95,
              "leadScore": 85,
              "reason": "Brief 1-sentence reason for the response"
            }
            Do not include any markdown styling like ```json or any other text outside the JSON object.
        """.trimIndent()
    }

    private fun getFallbackResult(leadName: String): GenerationResult {
        return GenerationResult(
            suggestion = "Hi $leadName, thank you for your query! We would love to help. Could we schedule a brief call this afternoon or tomorrow to discuss the details and arrange a quote?",
            matchPercentage = 95,
            leadScore = 80,
            reason = "Addresses query directly with an actionable call to schedule a phone call."
        )
    }

    data class GenerationResult(
        val suggestion: String,
        val matchPercentage: Int,
        val leadScore: Int,
        val reason: String
    )
}
