package com.example.domain.usecase

import com.example.domain.model.OcrBlock
import com.example.domain.repository.OcrRepository
import com.google.mlkit.vision.common.InputImage

class ProcessOcrUseCase(private val ocrRepository: OcrRepository) {
    suspend operator fun invoke(image: InputImage): List<OcrBlock> {
        return ocrRepository.analyzeImage(image)
    }
}
