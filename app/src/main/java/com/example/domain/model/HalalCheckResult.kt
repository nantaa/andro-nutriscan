package com.example.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HalalCheckResult(
    val productName: String,
    val halalStatus: String, // "HALAL TERVERIFIKASI", "HARAM", "SYUBHAT" (MUI Standards)
    val muiCertificateId: String? = null,
    val ingredientsCount: Int,
    val flaggedIngredients: List<FlaggedIngredient> = emptyList(),
    val generalExplanation: String // Summary explanation in Bahasa Indonesia containing MUI references
)

@JsonClass(generateAdapter = true)
data class FlaggedIngredient(
    val name: String,
    val status: String, // "HARAM" or "SYUBHAT"
    val reason: String // MUI standards reason in Bahasa Indonesia
)
