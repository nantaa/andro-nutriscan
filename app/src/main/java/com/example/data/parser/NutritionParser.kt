package com.example.data.parser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parsers that safely process raw JSON strings from Gemini into target robust Kotlin models.
 */
object NutritionParser {

    // User-requested clean Kotlin data structures
    data class NutritionFacts(
        val productName: String?,
        val servingSize: String,
        val calories: Int,
        val totalFat: NutrientValue,
        val saturatedFat: NutrientValue,
        val transFat: NutrientValue,
        val sodium: NutrientValue,
        val totalCarbohydrate: NutrientValue,
        val totalSugars: NutrientValue,
        val protein: NutrientValue,
        val healthScore: Int,
        val healthSummary: String,
        val warnings: List<String>
    )

    data class NutrientValue(
        val amount: Float,
        val unit: String,
        val dailyValuePercent: Int?
    )

    // Shadow `@Serializable` classes to ensure parsing is highly resilient to null/missing fields
    @Serializable
    private data class RawNutritionFacts(
        val productName: String? = null,
        val servingSize: String? = null,
        val calories: Int? = null,
        val totalFat: RawNutrientValue? = null,
        val saturatedFat: RawNutrientValue? = null,
        val transFat: RawNutrientValue? = null,
        val sodium: RawNutrientValue? = null,
        val totalCarbohydrate: RawNutrientValue? = null,
        val totalSugars: RawNutrientValue? = null,
        val protein: RawNutrientValue? = null,
        val healthScore: Int? = null,
        val healthSummary: String? = null,
        val warnings: List<String?>? = null
    )

    @Serializable
    private data class RawNutrientValue(
        val amount: Float? = null,
        val unit: String? = null,
        val dailyValuePercent: Int? = null
    )

    private val jsonHelper = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /**
     * Parses raw JSON string into a reliable, non-null guaranteed NutritionFacts structure.
     * Handles null fields gracefully.
     * Returns a [Result] wrapper containing the output.
     */
    fun parse(rawJson: String): Result<NutritionFacts> {
        return runCatching {
            // Un-wrap any leftover markdown codeblock if the LLM outputted them despite format constraints
            val cleanedJson = cleanJsonString(rawJson)
            val raw = jsonHelper.decodeFromString<RawNutritionFacts>(cleanedJson)
            
            NutritionFacts(
                productName = raw.productName,
                servingSize = raw.servingSize ?: "N/A",
                calories = raw.calories ?: 0,
                totalFat = mapNutrient(raw.totalFat, "g"),
                saturatedFat = mapNutrient(raw.saturatedFat, "g"),
                transFat = mapNutrient(raw.transFat, "g"),
                sodium = mapNutrient(raw.sodium, "mg"),
                totalCarbohydrate = mapNutrient(raw.totalCarbohydrate, "g"),
                totalSugars = mapNutrient(raw.totalSugars, "g"),
                protein = mapNutrient(raw.protein, "g"),
                healthScore = raw.healthScore ?: 0,
                healthSummary = raw.healthSummary ?: "",
                warnings = raw.warnings?.filterNotNull() ?: emptyList()
            )
        }
    }

    private fun mapNutrient(raw: RawNutrientValue?, defaultUnit: String): NutrientValue {
        return NutrientValue(
            amount = raw?.amount ?: 0f,
            unit = raw?.unit ?: defaultUnit,
            dailyValuePercent = raw?.dailyValuePercent
        )
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
