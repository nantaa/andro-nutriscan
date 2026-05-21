package com.example.data.repository

import com.example.domain.model.OcrBlock
import com.example.domain.repository.OcrRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrRepositoryImpl : OcrRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyzeImage(image: InputImage): List<OcrBlock> = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.map { block ->
                    val rawText = block.text
                    val processedText = fixOcrText(rawText)
                    OcrBlock(
                        text = processedText,
                        boundingBox = block.boundingBox
                    )
                }
                continuation.resume(blocks)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    override fun fixOcrText(text: String): String {
        // Correct issues where '9' is misread as 'g' next to numbers
        // E.g., '129' -> '12g', '4.5 9' -> '4.5g'
        val numberUnitRegex = """\b(\d+(?:[.,]\d+)?)\s*[9q]\b""".toRegex()
        var fixedText = text.replace(numberUnitRegex) { matchResult ->
            val number = matchResult.groupValues[1]
            "${number}g"
        }

        // Fix compound patterns like "gula 15 9" -> "gula 15g" or "protein 8 9" -> "protein 8g"
        val compoundRegex = """(?i)\b(gula|lemak|protein|karbohidrat|sodium)\s+(\d+(?:[.,]\d+)?)\s*[9q]\b""".toRegex()
        fixedText = fixedText.replace(compoundRegex) { matchResult ->
            val category = matchResult.groupValues[1]
            val value = matchResult.groupValues[2]
            val unit = if (category.lowercase() == "sodium") "mg" else "g"
            "$category $value$unit"
        }

        return fixedText
    }
}
