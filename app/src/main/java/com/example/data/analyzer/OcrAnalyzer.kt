package com.example.data.analyzer

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.model.OcrBlock
import com.example.domain.usecase.ProcessOcrUseCase
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OcrAnalyzer(
    private val processOcrUseCase: ProcessOcrUseCase,
    private val scope: CoroutineScope,
    private val onOcrResult: (List<OcrBlock>, String) -> Unit
) : ImageAnalysis.Analyzer {

    private var isAnalyzing = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isAnalyzing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isAnalyzing = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scope.launch {
            try {
                val blocks = processOcrUseCase(inputImage)
                val fullText = blocks.joinToString("\n") { it.text }
                withContext(Dispatchers.Main) {
                    onOcrResult(blocks, fullText)
                }
            } catch (e: Exception) {
                // Silently drop analysis failures or pass placeholder
            } finally {
                imageProxy.close()
                isAnalyzing = false
            }
        }
    }
}
