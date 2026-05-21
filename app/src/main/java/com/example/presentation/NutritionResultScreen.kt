package com.example.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Re-declare exact user data structures inside the file to prevent model conflicts and guarantee build success
data class NutritionFacts(
    val productName: String?,
    val servingSize: String,
    val calories: Int,
    val totalFat: NutrientValue,
    val saturatedFat: NutrientValue,
    val transFat: NutrientValue,
    val sodium: NutrientValue,
    val totalCarbohydrate: NutrientValue,
    val totalSugars: NutrientValue,
    val protein: NutrientValue,
    val healthScore: Int,        // 1–100
    val healthSummary: String,   // Bahasa Indonesia
    val warnings: List<String>   // Bahasa Indonesia
)

data class NutrientValue(
    val amount: Float,
    val unit: String,
    val dailyValuePercent: Int?
)

@Composable
fun NutritionResultScreen(
    facts: NutritionFacts,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = facts) {
        animationTriggered = true
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                // Return Navigation
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info, // Back button icon or similar
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Hasil Analisis Gizi",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        containerColor = Color(0xFF0F0F0F), // Ultimate dark theme
        modifier = modifier.testTag("nutrition_result_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("scrollable_container"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Product Name and Basic Overview Hero
            item {
                ProductHeroSection(facts = facts)
            }

            // Health Score Card containing Circular Gauge
            item {
                HealthScoreRingCard(
                    facts = facts,
                    animationTriggered = animationTriggered
                )
            }

            // Health Summary Bahasa Indonesia
            item {
                HealthSummaryCard(summary = facts.healthSummary)
            }

            // Warning Alerts Card (rendered only if warnings exist)
            if (facts.warnings.isNotEmpty()) {
                item {
                    WarningsCard(warnings = facts.warnings)
                }
            }

            // Nutrient Info Section Header
            item {
                Text(
                    text = "KANDUNGAN GIZI",
                    color = Color(0xFF00C9A7), // Accent teal
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Items of specific nutrients
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NutrientRowItem(
                            label = "Lemak Total (Total Fat)",
                            value = facts.totalFat
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        NutrientRowItem(
                            label = "Lemak Jenuh (Saturated Fat)",
                            value = facts.saturatedFat
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        NutrientRowItem(
                            label = "Lemak Trans (Trans Fat)",
                            value = facts.transFat
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        NutrientRowItem(
                            label = "Karbohidrat Total (Total Carb)",
                            value = facts.totalCarbohydrate
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        NutrientRowItem(
                            label = "Gula Total (Total Sugars)",
                            value = facts.totalSugars
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        NutrientRowItem(
                            label = "Protein",
                            value = facts.protein
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                        NutrientRowItem(
                            label = "Natrium (Sodium)",
                            value = facts.sodium
                        )
                    }
                }
            }

            // Reference guidelines footer
            item {
                Text(
                    text = "* Persen AKG (% Daily Value) berdasarkan kebutuhan energi 2150 kkal. Kebutuhan energi Anda mungkin lebih tinggi atau lebih rendah.",
                    color = Color.Gray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
fun ProductHeroSection(facts: NutritionFacts) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = facts.productName ?: "Produk Tidak Dikenal",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Serving size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = Color(0xFF00C9A7),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sajian: ${facts.servingSize}",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }

                // Calories Pill
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFE53935).copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE53935).copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${facts.calories} kkal",
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HealthScoreRingCard(
    facts: NutritionFacts,
    animationTriggered: Boolean
) {
    // Dynamic ring color depending on the designated safety levels
    val scoreColor = when (facts.healthScore) {
        in 80..100 -> Color(0xFF00C9A7) // Pure Green/Teal
        in 50..79 -> Color(0xFFF59E0B)  // Yellow
        else -> Color(0xFFEF4444)       // Red
    }

    val progressAnimated by animateFloatAsState(
        targetValue = if (animationTriggered) facts.healthScore / 100f else 0f,
        animationSpec = tween(durationMillis = 1100),
        label = "score_arc_anim"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .testTag("health_score_ring"),
                contentAlignment = Alignment.Center
            ) {
                // Background Track Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 12.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        style = Stroke(width = strokeW)
                    )
                }

                // Dynamic Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 12.dp.toPx()
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progressAnimated,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Inner score text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = facts.healthScore.toString(),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 42.sp
                    )
                    Text(
                        text = "dari 100",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "Skor Kesehatan",
                color = Color.LightGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HealthSummaryCard(summary: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Analisis Ringkas",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary,
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun WarningsCard(warnings: List<String>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFEF4444).copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Peringatan",
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Peringatan Kesehatan",
                    color = Color(0xFFFF8A80),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                warnings.forEach { warning ->
                    Text(
                        text = "• $warning",
                        color = Color(0xFFFFCDD2),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NutrientRowItem(
    label: String,
    value: NutrientValue
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${if (value.amount % 1f == 0f) value.amount.toInt() else value.amount} ${value.unit}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            // Daily value badge if available
            value.dailyValuePercent?.let { percent ->
                val badgeColor = when {
                    percent < 10 -> Color(0xFF00C9A7)  // Safe / low value
                    percent <= 20 -> Color(0xFFF59E0B) // Moderately high
                    else -> Color(0xFFEF4444)          // Extreme high
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = badgeColor.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percent% AKG",
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Horizontal visual balance meter bar
        value.dailyValuePercent?.let { percent ->
            val fillRatio = (percent / 100f).coerceIn(0f, 1f)
            val barColor = when {
                percent < 10 -> Color(0xFF00C9A7)
                percent <= 20 -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillRatio)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }
    }
}
