package com.example.domain.repository

import com.example.domain.model.OcrBlock
import com.google.mlkit.vision.common.InputImage

interface OcrRepository {
    suspend fun analyzeImage(image: InputImage): List<OcrBlock>
    fun fixOcrText(text: String): String
}
