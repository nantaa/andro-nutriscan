package com.example.data.api

import com.example.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A Ktor-based client that sends multimodal inputs (base64 JPEG + prompt) to the
 * Gemini 2.0 Flash API and returns the generated content as a raw JSON string.
 */
class GeminiApiClient {

    private val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
        }
    }

    @Serializable
    private data class GeminiRequest(
        val contents: List<Content>,
        val generationConfig: GenerationConfig? = null
    )

    @Serializable
    private data class Content(
        val parts: List<Part>
    )

    @Serializable
    private data class Part(
        val text: String? = null,
        val inlineData: InlineData? = null
    )

    @Serializable
    private data class InlineData(
        val mimeType: String,
        val data: String
    )

    @Serializable
    private data class GenerationConfig(
        val responseMimeType: String? = null,
        val temperature: Float? = null
    )

    @Serializable
    private data class GeminiResponse(
        val candidates: List<Candidate>? = null
    )

    @Serializable
    private data class Candidate(
        val content: Content? = null
    )

    /**
     * Executes generateContent to Gemini 2.0 Flash API with given parameters.
     */
    suspend fun generateContent(base64Image: String, prompt: String): String {
        // Access key secure through BuildConfig injected inside .env or local.properties
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key Gemini belum dikonfigurasi. Harap atur GEMINI_API_KEY.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        if (base64Image.isNotBlank()) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)))
        }

        val request = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("HTTP Error ${response.status.value}: ${response.bodyAsText()}")
        }

        val responseBodyString = response.bodyAsText()
        
        // Parse the top-level Gemini output nesting safely to get the core JSON text
        val parsedResponse = Json { ignoreUnknownKeys = true }.decodeFromString<GeminiResponse>(responseBodyString)
        val candidateText = parsedResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Konten respons kosong dari Gemini GenerateContent API.")

        return candidateText
    }
}
