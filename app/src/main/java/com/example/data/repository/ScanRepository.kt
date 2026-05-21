package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.ScanHistory
import com.example.data.db.ScanHistoryDao
import com.example.domain.model.HalalCheckResult
import com.example.domain.model.NutritionFacts
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScanRepository(
    private val scanHistoryDao: ScanHistoryDao,
    private val moshi: Moshi = RetrofitClient.moshiInstance
) {
    val allScans: Flow<List<ScanHistory>> = scanHistoryDao.getAllScans()

    suspend fun insertScan(type: String, productName: String, contentJson: String, statusText: String) {
        withContext(Dispatchers.IO) {
            val scan = ScanHistory(
                type = type,
                productName = productName,
                contentJson = contentJson,
                statusText = statusText
            )
            scanHistoryDao.insertScan(scan)
        }
    }

    suspend fun deleteScan(id: Int) {
        withContext(Dispatchers.IO) {
            scanHistoryDao.deleteScan(id)
        }
    }

    suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            scanHistoryDao.clearHistory()
        }
    }

    suspend fun scanNutritionLabel(bitmapBase64: String?, ocrText: String): NutritionFacts = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key Gemini belum dikonfigurasi. Silakan atur GEMINI_API_KEY di panel Secrets AI Studio.")
        }

        val promptText = """
            Analisis panel informasi nilai gizi (Nutrition Facts Panel) dari produk makanan berikut.
            Gunakan teks hasil OCR ini untuk membantu analisis jika gambar kurang jelas atau sebagai referensi utama:
            ---
            $ocrText
            ---
            
            Tolong kembalikan respons dalam bentuk JSON murni yang sesuai dengan format kelas data Kotlin NutritionFacts berikut:
            {
              "productName": "Nama Produk Gizi",
              "calories": 150,
              "caloriesUnit": "kcal",
              "sugar": 4.5,
              "sugarUnit": "g",
              "sugarStatus": "Rendah", // "Rendah" jika gula <= 5g/100g, "Sedang" jika 5g - 22.5g, "Tinggi" jika > 22.5g 
              "fat": 3.0,
              "fatUnit": "g",
              "protein": 8.0,
              "proteinUnit": "g",
              "sodium": 45.0,
              "sodiumUnit": "mg",
              "healthScore": 85, // Berikan skor gizi objektif skala 0-100 berdasarkan profil nutrisi (kadar lemak jenuh, gula, natrium, protein)
              "healthSummary": "Ringkasan penilaian gizi singkat, edukatif, dan praktis dalam Bahasa Indonesia."
            }
            
            Format respons Anda harus berupa valid JSON object tunggal, langsung mulai dengan karakter { dan diakhiri dengan }. Jangan sertakan pembungkus markdown ```json atau teks penjelasan tambahan lainnya di luar format JSON.
        """.trimIndent()

        val parts = mutableListOf<Part>()
        parts.add(Part(text = promptText))
        if (!bitmapBase64.isNullOrEmpty()) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmapBase64)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Anda adalah asisten ahli pangan dan gizi terakreditasi di Indonesia. Selalu memberikan hasil analisis dalam Bahasa Indonesia."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Tidak ada respons dari Gemini Flash API")

            val jsonString = extractJson(rawText)
            val adapter = moshi.adapter(NutritionFacts::class.java)
            val result = adapter.fromJson(jsonString) ?: throw Exception("Gagal melakukan deserialisasi data gizi")
            
            // Auto save successful scan to DB
            insertScan(
                type = "NUTRITION",
                productName = result.productName,
                contentJson = jsonString,
                statusText = "Skor Gizi: ${result.healthScore} (${result.sugarStatus} Gula)"
            )

            result
        } catch (e: Exception) {
            Log.e("ScanRepository", "Nutrition Scan Error: ", e)
            throw e
        }
    }

    suspend fun checkHalalIngredients(bitmapBase64: String?, ocrText: String): HalalCheckResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key Gemini belum dikonfigurasi. Silakan atur GEMINI_API_KEY di panel Secrets AI Studio.")
        }

        val promptText = """
            Analisis daftar bahan/komposisi komposisi produk berikut untuk mendeteksi kehalalan berdasarkan fatwa Majelis Ulama Indonesia (MUI).
            Identifikasi dan flag bahan-bahan yang tergolong HARAM mutlak (babi, alkohol, darah, bangkai) atau SYUBHAT (diragukan, membutuhkan pemeriksaan kritis seperti emulsifier, gelatin, ragi, lecithin, whey, pengental, enzim, perisa sintetis).
            
            Gunakan teks hasil OCR di bawah ini untuk memperjelas analisis atau jika gambar tidak tersedia:
            ---
            $ocrText
            ---
            
            Kembalikan respons analisis dalam struktur JSON Bahasa Indonesia yang persis seperti format kelas data Kotlin HalalCheckResult ini:
            {
              "productName": "Nama Produk Bahan",
              "halalStatus": "HALAL TERVERIFIKASI" | "HARAM" | "SYUBHAT", // Tentukan status utama produk
              "muiCertificateId": "ID Sertifikat MUI (jika tertera di label) atau null jika tidak ada",
              "ingredientsCount": 10,
              "flaggedIngredients": [
                {
                  "name": "Nama Bahan Terflag",
                  "status": "HARAM" | "SYUBHAT",
                  "reason": "Penjelasan berbasis standar LPPOM MUI dalam Bahasa Indonesia mengapa bahan ini tergolong haram atau syubhat."
                }
              ],
              "generalExplanation": "Kesimpulan kehalalan komprehensif dalam Bahasa Indonesia mengenai produk ini dengan merujuk standar kehalalan Majelis Ulama Indonesia."
            }
            
            Respons Anda harus berupa valid JSON object tunggal saja, tanpa pembungkus seperti ```json. Pastikan dimulai dengan '{' dan diakhiri dengan '}'.
        """.trimIndent()

        val parts = mutableListOf<Part>()
        parts.add(Part(text = promptText))
        if (!bitmapBase64.isNullOrEmpty()) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmapBase64)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Anda adalah auditor sertifikasi halal LPPOM MUI ahli pangan halal Indonesia. Selalu memberikan hasil dalam Bahasa Indonesia."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Tidak ada respons dari Gemini Flash API")

            val jsonString = extractJson(rawText)
            val adapter = moshi.adapter(HalalCheckResult::class.java)
            val result = adapter.fromJson(jsonString) ?: throw Exception("Gagal melakukan deserialisasi data kehalalan")

            // Auto save successful scan to DB
            insertScan(
                type = "HALAL",
                productName = result.productName,
                contentJson = jsonString,
                statusText = result.halalStatus
            )

            result
        } catch (e: Exception) {
            Log.e("ScanRepository", "Halal Scan Error: ", e)
            throw e
        }
    }

    private fun extractJson(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }
}
