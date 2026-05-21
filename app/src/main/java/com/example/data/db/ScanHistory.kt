package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "NUTRITION" or "HALAL"
    val productName: String,
    val contentJson: String, // JSON serialization of NutritionFacts or HalalCheckResult
    val statusText: String, // Display summaries: e.g. "Skor: 88" or "HALAL TERVERIFIKASI"
    val timestamp: Long = System.currentTimeMillis()
)
