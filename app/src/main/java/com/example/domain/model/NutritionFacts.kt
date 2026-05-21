package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NutritionFacts(
    val productName: String,
    val calories: Int,
    val caloriesUnit: String = "kcal",
    val sugar: Float,
    val sugarUnit: String = "g",
    val sugarStatus: String, // "Rendah", "Sedang", "Tinggi"
    val fat: Float,
    val fatUnit: String = "g",
    val protein: Float,
    val proteinUnit: String = "g",
    val sodium: Float,
    val sodiumUnit: String = "mg",
    val healthScore: Int, // 0 - 100
    val healthSummary: String // Explanations in Bahasa Indonesia
)
