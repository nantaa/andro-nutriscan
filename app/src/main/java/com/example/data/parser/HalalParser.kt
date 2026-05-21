package com.example.data.parser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parsers that safely process raw JSON strings from Gemini into target robust Kotlin Halal models.
 */
object HalalParser {

    // User-requested clean Kotlin data structures
    data class HalalCheckResult(
        val productName: String?,
        val allIngredients: List<String>,
        val verdict: String,   // "HALAL", "HARAM", "SYUBHAT", "UNKNOWN"
        val verdictReason: String,
        val flaggedIngredients: List<FlaggedIngredient>
    )

    data class FlaggedIngredient(
        val name: String,
        val status: String,
        val reason: String,
        val alternativeNames: List<String>
    )

    // Shadow `@Serializable` classes to ensure parsing is highly resilient to null/missing fields
    @Serializable
    private data class RawHalalCheckResult(
        val productName: String? = null,
        val allIngredients: List<String?>? = null,
        val verdict: String? = null,
        val verdictReason: String? = null,
        val flaggedIngredients: List<RawFlaggedIngredient?>? = null
    )

    @Serializable
    private data class RawFlaggedIngredient(
        val name: String? = null,
        val status: String? = null,
        val reason: String? = null,
        val alternativeNames: List<String?>? = null
    )

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /**
     * Parses raw JSON string into a reliable, non-null guaranteed HalalCheckResult structure.
     * Handles null fields gracefully.
     * Returns a [Result] wrapper containing the output.
     */
    fun parse(rawJson: String): Result<HalalCheckResult> {
        return runCatching {
            val cleanedJson = cleanJsonString(rawJson)
            val raw = jsonHelper.decodeFromString<RawHalalCheckResult>(cleanedJson)
            
            HalalCheckResult(
                productName = raw.productName,
                allIngredients = raw.allIngredients?.filterNotNull() ?: emptyList(),
                verdict = raw.verdict ?: "UNKNOWN",
                verdictReason = raw.verdictReason ?: "",
                flaggedIngredients = raw.flaggedIngredients?.filterNotNull()?.map {
                    FlaggedIngredient(
                        name = it.name ?: "Unknown Ingredient",
                        status = it.status ?: "UNKNOWN",
                        reason = it.reason ?: "",
                        alternativeNames = it.alternativeNames?.filterNotNull() ?: emptyList()
                    )
                } ?: emptyList()
            )
        }
    }

    private fun cleanJsonString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```json") && trimmed.endsWith("```")) {
            return trimmed.substring(7, trimmed.length - 3).trim()
        }
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            return trimmed.substring(3, trimmed.length - 3).trim()
        }
        return trimmed
    }
}
