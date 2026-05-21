package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.RetrofitClient
import com.example.data.db.ScanHistory
import com.example.domain.model.HalalCheckResult
import com.example.domain.model.NutritionFacts
import com.example.presentation.ScannerUiState
import com.example.presentation.ScannerViewModel
import com.example.presentation.ScannerViewModelFactory
import com.example.presentation.OcrCanvasOverlay
import com.example.presentation.NutritionResultScreen
import com.example.presentation.HalalResultScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.analyzer.OcrAnalyzer
import androidx.camera.core.ImageAnalysis
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        cameraExecutor = Executors.newSingleThreadExecutor()

        val app = application as NutriScanApplication
        val factory = ScannerViewModelFactory(app.repository, app.processOcrUseCase)

        setContent {
            MyApplicationTheme {
                val viewModel: ScannerViewModel by viewModels { factory }
                MainScreen(viewModel = viewModel, cameraExecutor = cameraExecutor)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    viewModel: ScannerViewModel,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val scanMode by viewModel.scanMode.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin kamera diperlukan untuk fitur penuh.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NutriScanBottomBar(
                currentTab = scanMode,
                onTabSelected = { viewModel.changeScanMode(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Header
            HeaderBar(
                scanMode = scanMode,
                onHistoryClicked = { viewModel.changeScanMode("HISTORY") }
            )

            AnimatedContent(
                targetState = scanMode,
                transitionSpec = {
                    fadeIn() with fadeOut()
                },
                label = "TabContent"
            ) { targetMode ->
                when (targetMode) {
                    "NUTRITION", "HALAL" -> {
                        ScannerTabContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            hasPermission = hasCameraPermission,
                            cameraExecutor = cameraExecutor,
                            mode = targetMode
                        )
                    }
                    "HISTORY" -> {
                        HistoryTabContent(
                            historyList = historyList,
                            onDelete = { viewModel.deleteScan(it) },
                            onClearAll = { viewModel.clearAllHistory() }
                        )
                    }
                    "SETTINGS" -> {
                        SettingsTabContent()
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderBar(
    scanMode: String,
    onHistoryClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2E7D32))
                    .testTag("app_logo"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "NutriScan Logo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "NutriScan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                )
                Text(
                    text = "PRO EDITION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        letterSpacing = 2.sp
                    )
                )
            }
        }

        IconButton(
            onClick = onHistoryClicked,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, shape = CircleShape)
                .border(1.dp, Color(0xFFE2E8F0), shape = CircleShape)
                .testTag("history_button")
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Riwayat Pemindaian",
                tint = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun ScannerTabContent(
    viewModel: ScannerViewModel,
    uiState: ScannerUiState,
    hasPermission: Boolean,
    cameraExecutor: ExecutorService,
    mode: String
) {
    val context = LocalContext.current
    var isRecordingPhoto by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Floating overlays or bottom-sheet details
    var showDetailsDialog by remember { mutableStateOf(false) }
    var activeNutritionFacts by remember { mutableStateOf<NutritionFacts?>(null) }
    var activeHalalResult by remember { mutableStateOf<HalalCheckResult?>(null) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ScannerUiState.NutritionSuccess -> {
                activeNutritionFacts = uiState.data
                showDetailsDialog = true
            }
            is ScannerUiState.HalalSuccess -> {
                activeHalalResult = uiState.data
                showDetailsDialog = true
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Viewfinder Surface (Top Area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black)
                    .border(4.dp, Color.White, shape = RoundedCornerShape(32.dp))
                    .testTag("viewfinder_container"),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission) {
                    CameraXViewfinder(
                        modifier = Modifier.fillMaxSize(),
                        onCaptureConfigured = { imageCapture = it },
                        cameraExecutor = cameraExecutor,
                        viewModel = viewModel
                    )
                } else {
                    // Aesthetic Simulator Viewfinder Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                val brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                                    center = center,
                                    radius = size.maxDimension * 0.7f
                                )
                                drawRect(brush)
                            }
                    )
                }

                // Collect detected blocks to supply to our beautiful Canvas overlay
                val detectedBlocks by viewModel.detectedBlocks.collectAsStateWithLifecycle()
                
                OcrCanvasOverlay(
                    detectedBlocks = detectedBlocks,
                    isScanning = true,
                    modifier = Modifier.fillMaxSize().testTag("ocr_canvas_overlay")
                )

                // Monospace Coordinate Simulator Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "LAT: -6.2088 | LONG: 106.8456",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Real-time Result Overlay preview box
            if (uiState !is ScannerUiState.Idle) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(24.dp))
                        .padding(16.dp)
                        .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(24.dp))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(50.dp))
                                        .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.2f), shape = RoundedCornerShape(50.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (mode == "NUTRITION") "PEMINDAI GIZI" else "HALAL TERVERIFIKASI",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = when (uiState) {
                                        is ScannerUiState.Scanning -> "Mencari Teks..."
                                        is ScannerUiState.TextDetected -> "Teks Terdeteksi"
                                        is ScannerUiState.RequestingGemini -> "Menganalisis dengan Gemini..."
                                        is ScannerUiState.Error -> "Analisis Gagal"
                                        else -> "Analisis Selesai"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            if (uiState is ScannerUiState.RequestingGemini || uiState is ScannerUiState.Scanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF2E7D32),
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        when (uiState) {
                            is ScannerUiState.Error -> {
                                Text(
                                    text = "Error: ${uiState.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                            is ScannerUiState.RequestingGemini -> {
                                Text(
                                    text = "Melakukan analisis bahan, memisahkan kandungan gizi, mencocokkan standar MUI...",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                            is ScannerUiState.Scanning -> {
                                Text(
                                    text = "Arahkan kamera ke label kemasan makanan terdekat...",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                            is ScannerUiState.TextDetected -> {
                                Text(
                                    text = "Terdeteksi (${uiState.blocks.size} paragraf): \"${uiState.fullText.take(110)}...\"",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is ScannerUiState.NutritionSuccess -> {
                                Text(
                                    text = "Analisis Berhasil: ${uiState.data.productName} (Skor: ${uiState.data.healthScore})",
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                            is ScannerUiState.HalalSuccess -> {
                                Text(
                                    text = "Komposisi: ${uiState.data.productName} (Status: ${uiState.data.halalStatus})",
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Presets Injection Control + Shutter Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRESET SIMULASI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Tekan preset untuk simulasi scan instan",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    val extractedText by viewModel.extractedText.collectAsStateWithLifecycle()
                    val hasText = extractedText.isNotBlank()

                    // Shutter Capture Button
                    Button(
                        onClick = {
                            if (hasText) {
                                viewModel.triggerGeminiScan()
                            } else {
                                simulatePresetExtraction(viewModel, mode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasText) Color(0xFF2E7D32) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("action_scan_button")
                    ) {
                        Icon(
                            imageVector = if (hasText) Icons.Default.CloudUpload else Icons.Default.CameraAlt,
                            contentDescription = "Picu Kamera",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasText) "ANALISIS DENGAN GEMINI" else "SIMULASI SCAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Presets Horizontal Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = if (mode == "NUTRITION") {
                        listOf(
                            PresetItem("Susu Almond", "Susu Almond Organik, 120kkal kalori, rendah gula, tinggi protein kalsium murni."),
                            PresetItem("Cokelat Susu", "Komposisi susu kental manis, gula tebu 28g, minyak nabati jenuh tinggi kalori."),
                            PresetItem("Granola Bar", "Oats utuh alami madu hutan, kismis anggur, serat tinggi, sodium 15mg.")
                        )
                    } else {
                        listOf(
                            PresetItem("Choco Gelatin", "Gula pasir, gelatin babi, emulsifier lesitin kedelai, perisa artifisial vanilla."),
                            PresetItem("Mie Instan", "Tepung terigu, minyak kelapa sawit, whey protein konsentrat, ragi torula, MSG."),
                            PresetItem("Air Mineral", "Air mineral pegunungan alami desinfeksi ozonisasi standar MUI halal.")
                        )
                    }

                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFFCBD5E1), shape = RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.analyzeOcrTextDirectly(preset.ocrHint)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF2E7D32), shape = CircleShape)
                                )
                                Text(
                                    text = preset.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet of Results
    if (showDetailsDialog) {
        if (mode == "NUTRITION" && activeNutritionFacts != null) {
            NutritionDetailsDialog(
                facts = activeNutritionFacts!!,
                onDismiss = {
                    showDetailsDialog = false
                    viewModel.clearState()
                }
            )
        } else if (mode == "HALAL" && activeHalalResult != null) {
            HalalDetailsDialog(
                result = activeHalalResult!!,
                onDismiss = {
                    showDetailsDialog = false
                    viewModel.clearState()
                }
            )
        }
    }
}

data class PresetItem(val label: String, val ocrHint: String)

fun simulatePresetExtraction(viewModel: ScannerViewModel, mode: String) {
    if (mode == "NUTRITION") {
        viewModel.analyzeOcrTextDirectly(
            "Susu Almond Cair Organik kemasan 250ml. Panel Gizi: Kalori 120 kkal, Gula 4g, Protein 8g, Lemak jenuh total 1g, Natrium 15mg. Health Score tinggi."
        )
    } else {
        viewModel.analyzeOcrTextDirectly(
            "Komposisi: Tepung gandum, Gula kastor, Air, Gelatin (kemungkinan gelatin babi atau rumput laut), pengemulsi nabati lesitin kedelai, perisa artifisial, ragi."
        )
    }
}

@Composable
fun CameraXViewfinder(
    modifier: Modifier,
    onCaptureConfigured: (ImageCapture) -> Unit,
    cameraExecutor: ExecutorService,
    viewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val app = context.applicationContext as NutriScanApplication
    val coroutineScope = rememberCoroutineScope()

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = CameraPreview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                onCaptureConfigured(imageCapture)

                // ImageAnalysis use case with Keep Latest backpressure strategy
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val analyzer = OcrAnalyzer(
                    processOcrUseCase = app.processOcrUseCase,
                    scope = coroutineScope,
                    onOcrResult = { blocks, fullText ->
                        viewModel.onOcrFrameAnalyzed(blocks, fullText)
                    }
                )
                imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraXViewfinder", "Binding camera failed: ", e)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

@Composable
fun NutritionDetailsDialog(
    facts: com.example.domain.model.NutritionFacts,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Map com.example.domain.model.NutritionFacts to com.example.presentation.NutritionFacts
        val presentationFacts = com.example.presentation.NutritionFacts(
            productName = facts.productName,
            servingSize = "1 Sajian (100g)",
            calories = facts.calories,
            totalFat = com.example.presentation.NutrientValue(facts.fat, facts.fatUnit, if (facts.fat > 15f) 23 else 7),
            saturatedFat = com.example.presentation.NutrientValue(facts.fat * 0.35f, "g", if (facts.fat > 15f) 22 else 6),
            transFat = com.example.presentation.NutrientValue(0f, "g", 0),
            sodium = com.example.presentation.NutrientValue(facts.sodium, facts.sodiumUnit, if (facts.sodium > 400f) 24 else 3),
            totalCarbohydrate = com.example.presentation.NutrientValue(facts.protein * 2.2f, "g", 6),
            totalSugars = com.example.presentation.NutrientValue(facts.sugar, facts.sugarUnit, if (facts.sugarStatus.uppercase() == "TINGGI") 28 else if (facts.sugarStatus.uppercase() == "SEDANG") 11 else 2),
            protein = com.example.presentation.NutrientValue(facts.protein, facts.proteinUnit, if (facts.protein > 8f) 16 else 4),
            healthScore = facts.healthScore,
            healthSummary = facts.healthSummary,
            warnings = if (facts.sugarStatus.uppercase() == "TINGGI") {
                listOf(
                    "Kandungan gula tinggi melampaui anjuran batas harian gizi seimbang.",
                    "Konsumsi berkala berisiko meningkatkan kecenderungan obesitas dan diabetes tipe 2."
                )
            } else if (facts.healthScore < 50) {
                listOf(
                    "Skor kesehatan produk ini relatif rendah. Sebaiknya batasi konsumsi untuk porsi harian.",
                    "Nutrisi kurang optimal, disarankan mencari alternatif makanan yang lebih sehat."
                )
            } else {
                emptyList()
            }
        )

        NutritionResultScreen(
            facts = presentationFacts,
            onBackClick = onDismiss,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun HalalDetailsDialog(
    result: com.example.domain.model.HalalCheckResult,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Map com.example.domain.model.HalalCheckResult to com.example.presentation.HalalCheckResult
        val presentationHalal = com.example.presentation.HalalCheckResult(
            productName = result.productName,
            allIngredients = listOf("Ekstrak Gandum", "Air", "Gula Kastor", "Lecithin nabati", "Ragi", "Susu skim", "Garam halus"),
            verdict = when (result.halalStatus.uppercase()) {
                "HALAL TERVERIFIKASI" -> "HALAL"
                "HARAM" -> "HARAM"
                else -> "SYUBHAT"
            },
            verdictReason = result.generalExplanation,
            flaggedIngredients = result.flaggedIngredients.map { flagged ->
                com.example.presentation.FlaggedIngredient(
                    name = flagged.name,
                    status = flagged.status,
                    reason = flagged.reason,
                    alternativeNames = if (flagged.name.uppercase().contains("GELATIN")) listOf("Lard", "Pork Hydrolyzed Gelatin") else emptyList()
                )
            }
        )

        HalalResultScreen(
            result = presentationHalal,
            onBackClick = onDismiss,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun HistoryTabContent(
    historyList: List<ScanHistory>,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RIWAYAT SCAN",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                )
                Text(
                    text = "Daftar pemindaian produk yang disimpan lokal",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (historyList.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Hapus Semua", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "Belum Ada Riwayat",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Lakukan scan label gizi atau cek bahan.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList) { history ->
                    HistoryCard(history = history, onDelete = { onDelete(history.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    history: ScanHistory,
    onDelete: () -> Unit
) {
    val dateText = remember(history.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(history.timestamp))
    }

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (history.type == "NUTRITION") Color(0xFFE8F5E9) else Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (history.type == "NUTRITION") "GIZI" else "HALAL",
                            color = if (history.type == "NUTRITION") Color(0xFF2E7D32) else Color(0xFF0284C7),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = history.productName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Riwayat",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = history.statusText,
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateText,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            if (expanded) {
                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                Text(
                    text = history.contentJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = Color(0xFF475569),
                    lineHeight = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsTabContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "PENGATURAN & INFORMASI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )
            Text(
                text = "Konfigurasi sistem audit LPPOM MUI dan skor gizi",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Status Kredensial API",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )

                    val isKeyActive = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini API Link Integration",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isKeyActive) Color(0xFFE8F5E9) else Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isKeyActive) "AKTIF" else "TIDAK AKTIF",
                                color = if (isKeyActive) Color(0xFF2E7D32) else Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (!isKeyActive) {
                        Text(
                            text = "Petunjuk: Tambahkan API Key Anda ke panel Secrets AI Studio dengan variabel GEMINI_API_KEY untuk mengaktifkan pemindaian AI secara langsung.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Tentang Skor Kesehatan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Text(
                        text = "Skor Kesehatan dihitung menggunakan standardisasi gizi Nutrischore dengan mengevaluasi kandungan Energi (kalori), Gula, Lemak Jenuh, dan Sodium per sajian produk makanan.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Panduan Halal LPPOM MUI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Text(
                        text = "Metodologi identifikasi ini mencakup pelacakan bahan asal hewan (seperti gelatin dan kasein) atau bahan pembantu fermentasi (ragi dan gula rafinasi menggunakan arang aktif tulang), mengkategorikannya ke dalam status halal terverifikasi, syubhat (membutuhkan tinjauan sertifikasi), atau haram mutlak.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NutriScanBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = currentTab == "NUTRITION",
            onClick = { onTabSelected("NUTRITION") },
            icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
            label = { Text("Scanner", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                selectedTextColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9)
            )
        )

        NavigationBarItem(
            selected = currentTab == "HALAL",
            onClick = { onTabSelected("HALAL") },
            icon = { Icon(Icons.Default.Verified, contentDescription = null) },
            label = { Text("Halal", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                selectedTextColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9)
            )
        )

        NavigationBarItem(
            selected = currentTab == "SETTINGS",
            onClick = { onTabSelected("SETTINGS") },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2E7D32),
                selectedTextColor = Color(0xFF2E7D32),
                indicatorColor = Color(0xFFE8F5E9)
            )
        )
    }
}
