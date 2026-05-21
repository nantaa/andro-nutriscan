package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.ScanRepository
import com.example.data.repository.OcrRepositoryImpl
import com.example.domain.usecase.ProcessOcrUseCase

class NutriScanApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ScanRepository(database.scanHistoryDao()) }
    val ocrRepository by lazy { OcrRepositoryImpl() }
    val processOcrUseCase by lazy { ProcessOcrUseCase(ocrRepository) }
}
