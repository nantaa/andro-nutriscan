package com.example.presentation

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ScanHistory
import com.example.data.repository.ScanRepository
import com.example.domain.model.HalalCheckResult
import com.example.domain.model.NutritionFacts
import com.example.domain.model.OcrBlock
import com.example.domain.usecase.ProcessOcrUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

sealed interface ScannerUiState {
    object Idle : ScannerUiState
    object Scanning : ScannerUiState
    data class TextDetected(val blocks: List<OcrBlock>, val fullText: String) : ScannerUiState
    object RequestingGemini : ScannerUiState
    data class NutritionSuccess(val data: NutritionFacts) : ScannerUiState
    data class HalalSuccess(val data: HalalCheckResult) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

class ScannerViewModel(
    private val repository: ScanRepository,
    private val processOcrUseCase: ProcessOcrUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _scanMode = MutableStateFlow("NUTRITION") // "NUTRITION", "HALAL", "HISTORY", "SETTINGS"
    val scanMode: StateFlow<String> = _scanMode.asStateFlow()

    val historyList: StateFlow<List<ScanHistory>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText.asStateFlow()

    private val _detectedBlocks = MutableStateFlow<List<OcrBlock>>(emptyList())
    val detectedBlocks: StateFlow<List<OcrBlock>> = _detectedBlocks.asStateFlow()

    fun changeScanMode(mode: String) {
        _scanMode.value = mode
        _uiState.value = ScannerUiState.Idle
        _capturedBitmap.value = null
        _extractedText.value = ""
        _detectedBlocks.value = emptyList()
    }

    fun clearState() {
        _uiState.value = ScannerUiState.Idle
        _capturedBitmap.value = null
        _extractedText.value = ""
        _detectedBlocks.value = emptyList()
    }

    fun deleteScan(id: Int) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Receives real-time frames feedback from text recognition analyzer
    fun onOcrFrameAnalyzed(blocks: List<OcrBlock>, fullText: String) {
        if (_uiState.value is ScannerUiState.RequestingGemini ||
            _uiState.value is ScannerUiState.NutritionSuccess ||
            _uiState.value is ScannerUiState.HalalSuccess) {
            return // Skip updates if actively calling Gemini or viewing details
        }
        _detectedBlocks.value = blocks
        _extractedText.value = fullText
        if (blocks.isNotEmpty()) {
            _uiState.value = ScannerUiState.TextDetected(blocks, fullText)
        } else {
            _uiState.value = ScannerUiState.Scanning
        }
    }

    // Active triggers the Gemini REST call when the user taps "Scan" 
    fun triggerGeminiScan() {
        val ocrText = _extractedText.value
        if (ocrText.isBlank()) {
            _uiState.value = ScannerUiState.Error("Arahkan kamera ke label makanan / bahan hingga teks terdeteksi.")
            return
        }

        _uiState.value = ScannerUiState.RequestingGemini
        viewModelScope.launch {
            try {
                if (_scanMode.value == "NUTRITION") {
                    val result = repository.scanNutritionLabel(null, ocrText)
                    _uiState.value = ScannerUiState.NutritionSuccess(result)
                } else {
                    val result = repository.checkHalalIngredients(null, ocrText)
                    _uiState.value = ScannerUiState.HalalSuccess(result)
                }
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.localizedMessage ?: "Gagal menganalisis label gizi dengan Gemini")
            }
        }
    }

    // Analyze raw text directly (such as preset mock scans)
    fun analyzeOcrTextDirectly(text: String) {
        _extractedText.value = text
        _uiState.value = ScannerUiState.RequestingGemini
        viewModelScope.launch {
            try {
                if (_scanMode.value == "NUTRITION") {
                    val result = repository.scanNutritionLabel(null, text)
                    _uiState.value = ScannerUiState.NutritionSuccess(result)
                } else {
                    val result = repository.checkHalalIngredients(null, text)
                    _uiState.value = ScannerUiState.HalalSuccess(result)
                }
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.localizedMessage ?: "Terjadi kesalahan analisis")
            }
        }
    }
}

class ScannerViewModelFactory(
    private val repository: ScanRepository,
    private val processOcrUseCase: ProcessOcrUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(repository, processOcrUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
