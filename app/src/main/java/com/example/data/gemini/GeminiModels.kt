package com.example.data.gemini

import com.squareup.moshi.Json

data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @Json(name = "role") val role: String = "user",
    @Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @Json(name = "text") val text: String
)

data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Double = 0.7,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 1000
)

data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @Json(name = "error") val error: GeminiError? = null
)

data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

data class GeminiError(
    @Json(name = "message") val message: String? = null,
    @Json(name = "code") val code: Int? = null,
    @Json(name = "status") val status: String? = null
)
