package com.example.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// User-provided local robust model declarations for Halal compliance assessments
data class HalalCheckResult(
    val productName: String?,
    val allIngredients: List<String>,
    val verdict: String,   // "HALAL", "HARAM", "SYUBHAT", "UNKNOWN"
    val verdictReason: String,
    val flaggedIngredients: List<FlaggedIngredient>
)

data class FlaggedIngredient(
    val name: String,
    val status: String,   // "HARAM" or "SYUBHAT"
    val reason: String,
    val alternativeNames: List<String>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HalalResultScreen(
    result: HalalCheckResult,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animateInTriggered by remember { mutableStateOf(false) }
    var allIngredientsExpanded by remember { mutableStateOf(false) }

    // Track which flagged ingredients are expanded by their indexes
    val expandedIngredients = remember { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(key1 = result) {
        animateInTriggered = true
    }

    // Colors mapping based on the MUI verdict state
    val verdictColor = when (result.verdict.uppercase()) {
        "HALAL" -> Color(0xFF00C9A7)    // Emerald/Teal green
        "HARAM" -> Color(0xFFEF4444)    // Red
        "SYUBHAT" -> Color(0xFFF59E0B)   // Amber/Orange
        else -> Color(0xFF6B7280)        // Gray
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
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Status Kebersihan & Halal",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        containerColor = Color(0xFF0F0F0F), // Absolute dark style background
        modifier = modifier.testTag("halal_result_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("scrollable_container"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Vertical Slide + Fade Verdict Banner at the top
            item {
                AnimatedVisibility(
                    visible = animateInTriggered,
                    enter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = tween(durationMillis = 600)
                    ) + fadeIn(animationSpec = tween(durationMillis = 600)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VerdictBanner(verdict = result.verdict, bannerColor = verdictColor)
                }
            }

            // Product Meta Header Area
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NAMA PRODUK",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = result.productName ?: "Produk Tanpa Label",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. Verdict Reason Card (matches verdict status colors in soft transparency)
            item {
                VerdictReasonCard(
                    reasonText = result.verdictReason,
                    verdictColor = verdictColor
                )
            }

            // 3. Flagged Ingredients collapsible section (Expand/Collapse on click)
            if (result.flaggedIngredients.isNotEmpty()) {
                item {
                    Text(
                        text = "BAHAN YANG PERLU DIPERHATIKAN",
                        color = Color(0xFFF59E0B), // Warm yellow warning header color
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                itemsIndexed(result.flaggedIngredients) { index, flagged ->
                    val isExpanded = expandedIngredients[index] ?: false
                    
                    FlaggedIngredientCard(
                        flagged = flagged,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedIngredients[index] = !isExpanded
                        }
                    )
                }
            }

            // 4. Collapsible full list of All Ingredients of the item as small chips
            if (result.allIngredients.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        // Header tap area to toggle list expand/collapse state
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { allIngredientsExpanded = !allIngredientsExpanded }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAFTAR SEMUA BAHAN (${result.allIngredients.size})",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Icon(
                                imageVector = if (allIngredientsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (allIngredientsExpanded) "Tutup" else "Buka",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = allIngredientsExpanded,
                            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
                        ) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                result.allIngredients.forEach { ingredient ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = Color(0xFF1C1C1E),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = ingredient,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MUI Reference / Halal Certification Note
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Referensi kehalalan berdasarkan fatwa MUI & standar kehalalan global. Selalu periksa tanda logo halal resmi pada kemasan fisik.",
                    color = Color.Gray.copy(alpha = 0.7f),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VerdictBanner(
    verdict: String,
    bannerColor: Color
) {
    val (titleText, icon, descText) = when (verdict.uppercase()) {
        "HALAL" -> Triple("HALAL", Icons.Default.CheckCircle, "Bahan-bahan aman & memenuhi kriteria halal.")
        "HARAM" -> Triple("HARAM", Icons.Default.Cancel, "Terdeteksi kandungan bahan yang dilarang (non-halal).")
        "SYUBHAT" -> Triple("PERLU DIPERHATIKAN", Icons.Default.ReportProblem, "Terdapat bahan yang meragukan / butuh verifikasi pabrik.")
        else -> Triple("TIDAK DAPAT DITENTUKAN", Icons.Default.Help, "Teks label kurang jelas untuk menentukan kehalalan.")
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bannerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = titleText,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = titleText,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            Text(
                text = descText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun VerdictReasonCard(
    reasonText: String,
    verdictColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = verdictColor.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = verdictColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Penjelasan Detil",
                color = verdictColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = reasonText,
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun FlaggedIngredientCard(
    flagged: FlaggedIngredient,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val statusColor = if (flagged.status.uppercase() == "HARAM") Color(0xFFEF4444) else Color(0xFFF59E0B)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name + Badge + Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flagged.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), shape = CircleShape)
                            .border(1.dp, statusColor.copy(alpha = 0.4f), shape = CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = flagged.status.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Expand toggle arrow
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Sembunyikan detail" else "Tampilkan detail",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Area containing reasoning and alternatives
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Divider(color = Color.White.copy(alpha = 0.05f))

                    Text(
                        text = "Alasan Keamanan:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = flagged.reason,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    if (flagged.alternativeNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Nama Lain / Sinonim:",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = flagged.alternativeNames.joinToString(", "),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
