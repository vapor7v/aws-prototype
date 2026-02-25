package com.arinterior.engine.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for API Gateway endpoints.
 */
interface AwsApiService {

    @POST("/intent")
    suspend fun extractIntent(@Body request: IntentRequest): IntentResponse

    @POST("/voice-cmd")
    suspend fun parseVoiceCommand(@Body request: VoiceCommandRequest): VoiceCommandResponse

    @POST("/label")
    suspend fun labelObject(@Body request: LabelRequest): LabelResponse
}

// ─── Request DTOs ────────────────────────────────────────────

data class IntentRequest(
    val prompt: String
)

data class VoiceCommandRequest(
    val text: String
)

data class LabelRequest(
    val image: String  // base64 encoded
)

// ─── Response DTOs ───────────────────────────────────────────

data class IntentResponse(
    @SerializedName("design_intent") val designIntent: Map<String, Any>,
    @SerializedName("raw_prompt") val rawPrompt: String
)

data class VoiceCommandResponse(
    val command: Map<String, Any>,
    @SerializedName("raw_text") val rawText: String
)

data class LabelResponse(
    val labels: List<LabelItem>,
    @SerializedName("matched_label") val matchedLabel: String?,
    @SerializedName("matched_category") val matchedCategory: String?,
    @SerializedName("catalog_match") val catalogMatch: Map<String, Any>?,
    @SerializedName("model_url") val modelUrl: String?
)

data class LabelItem(
    val name: String,
    val confidence: Float
)
