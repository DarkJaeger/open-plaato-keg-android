package com.openplaato.keg.ui.screens.scales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.LowRed
import com.openplaato.keg.ui.theme.OnSurfaceMuted
import com.openplaato.keg.ui.theme.PouringGreen
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScalesScreen(
    onConfigureScale: (String) -> Unit,
    onShowHistory: (String, String) -> Unit,
    viewModel: ScalesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kegs", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.isLoading && state.kegs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber500)
                    }
                }
                state.error != null && state.kegs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                            Text("Pull to retry", color = OnSurfaceMuted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                state.kegs.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No scales connected", color = OnSurfaceMuted)
                    }
                }
                else -> {
                    ReorderableScaleList(
                        kegs = state.kegs,
                        onMove = { from, to -> viewModel.moveKeg(from, to) },
                        onConfigure = onConfigureScale,
                        onShowHistory = onShowHistory,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorderableScaleList(
    kegs: List<Keg>,
    onMove: (from: Int, to: Int) -> Unit,
    onConfigure: (String) -> Unit,
    onShowHistory: (String, String) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onMove(from.index, to.index)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        items(kegs, key = { it.id }) { keg ->
            ReorderableItem(reorderState, key = keg.id) { isDragging ->
                ScaleCard(
                    keg = keg,
                    onConfigure = { onConfigure(keg.id) },
                    onShowHistory = { 
                        val label = keg.my_label?.takeIf { it.isNotBlank() } ?: keg.id
                        onShowHistory(keg.id, label)
                    },
                    // Pass the drag handle modifier so only the handle icon initiates dragging.
                    dragHandleModifier = Modifier.draggableHandle(),
                    isDragging = isDragging,
                )
            }
        }
    }
}

@Composable
fun ScaleCard(
    keg: Keg,
    onConfigure: () -> Unit,
    onShowHistory: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    isDragging: Boolean = false,
) {
    val label = keg.my_label?.takeIf { it.isNotBlank() } ?: (keg.id.take(8) + "…")
    val amount = keg.amount_left ?: 0.0
    val pct = (keg.percent_of_beer_left ?: 0.0).coerceIn(0.0, 100.0)
    val volumeUnit = when (keg.beer_left_unit) { "litre" -> "L"; else -> keg.beer_left_unit ?: "L" }
    val isPouring = keg.is_pouring?.let { it != "0" && it.isNotBlank() } ?: false
    val isLow = pct in 0.01..19.99
    val unitLabel = if (keg.unit == "2") "US" else "Metric"
    val isCo2Mode = when (keg.keg_mode) {
        "2" -> true
        "1" -> false
        else -> {
            // Heuristics for older firmware or if mode isn't explicitly set
            keg.my_label?.contains("CO2", ignoreCase = true) == true ||
            keg.beer_left_unit?.contains("CO2", ignoreCase = true) == true ||
            (keg.amount_left ?: 0.0) < -0.1 // CO2 scales often show negative weight if not tared correctly but labeled
        }
    }
    val modeLabel = if (isCo2Mode) "CO₂" else "Beer"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable { onShowHistory() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {},
                    modifier = dragHandleModifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = OnSurfaceMuted,
                    )
                }

                Spacer(Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Pill(unitLabel)
                        Pill(modeLabel, color = if (isCo2Mode) PouringGreen else null)
                        keg.sensitivity?.let { s ->
                            val sens = when (s) { "1" -> "Very Low"; "2" -> "Low"; "3" -> "Medium"; "4" -> "High"; else -> null }
                            sens?.let { Pill(it) }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPouring) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(PouringGreen))
                        Spacer(Modifier.width(8.dp))
                    }
                    keg.keg_temperature?.let { temp ->
                        Text(
                            "${"%.1f".format(temp)}${keg.temperature_unit ?: "°C"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Amber500,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    FilledTonalIconButton(onClick = onConfigure) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure scale")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isCo2Mode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Co2BottleVisual(
                        percent = pct.toFloat(),
                        modifier = Modifier.size(width = 60.dp, height = 100.dp)
                    )
                    
                    Spacer(Modifier.width(24.dp))
                    
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "%.1f".format(pct),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber500,
                                lineHeight = 34.sp,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "% CO₂ REMAINING",
                                style = MaterialTheme.typography.titleSmall,
                                color = OnSurfaceMuted,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }

                        Text(
                            "${"%.2f".format(amount)} $volumeUnit",
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurfaceMuted,
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.1f".format(pct),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber500,
                        lineHeight = 38.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "%",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                Text(
                    "${"%.2f".format(amount)} $volumeUnit",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceMuted,
                )

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (pct / 100.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (isLow) LowRed else Amber500,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun Co2BottleVisual(
    percent: Float,
    modifier: Modifier = Modifier
) {
    val strokeColor = PouringGreen.copy(alpha = 0.8f)
    val fillColor = if (percent < 20f) LowRed else PouringGreen.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        
        // Define the bottle path (matching the reference image shape better)
        val bottlePath = Path().apply {
            val corner = 8.dp.toPx()
            val shoulderY = h * 0.35f
            val neckW = w * 0.25f
            val capH = h * 0.08f
            
            // Start from bottom left corner
            moveTo(w * 0.15f + corner, h * 0.95f)
            // Bottom edge
            lineTo(w * 0.85f - corner, h * 0.95f)
            // Bottom right corner
            quadraticTo(w * 0.85f, h * 0.95f, w * 0.85f, h * 0.95f - corner)
            // Right side up to shoulder
            lineTo(w * 0.85f, shoulderY + corner)
            // Right shoulder
            quadraticTo(w * 0.85f, shoulderY, w * 0.85f - corner, shoulderY - corner * 0.5f)
            // To neck
            lineTo(w / 2 + neckW / 2, h * 0.2f)
            // Neck up
            lineTo(w / 2 + neckW / 2, capH)
            // Cap top
            lineTo(w / 2 - neckW / 2, capH)
            // Neck down
            lineTo(w / 2 - neckW / 2, h * 0.2f)
            // To left shoulder
            lineTo(w * 0.15f + corner, shoulderY - corner * 0.5f)
            // Left shoulder
            quadraticTo(w * 0.15f, shoulderY, w * 0.15f, shoulderY + corner)
            // Left side down
            lineTo(w * 0.15f, h * 0.95f - corner)
            // Bottom left corner
            quadraticTo(w * 0.15f, h * 0.95f, w * 0.15f + corner, h * 0.95f)
            close()
        }

        // Draw fill (inside out)
        clipPath(bottlePath) {
            val usableHeight = h * 0.85f
            val fillHeight = usableHeight * (percent / 100f)
            drawRect(
                color = fillColor,
                topLeft = Offset(0f, h * 0.95f - fillHeight),
                size = Size(w, fillHeight)
            )
        }

        // Draw outline
        drawPath(
            path = bottlePath,
            color = strokeColor,
            style = Stroke(width = strokeWidth)
        )
        
        // Draw cap
        val neckW = w * 0.25f
        val capH = h * 0.08f
        drawRoundRect(
            color = OnSurfaceMuted.copy(alpha = 0.6f),
            topLeft = Offset(w / 2 - (neckW * 0.6f), 0f),
            size = Size(neckW * 1.2f, capH * 1.2f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

@Composable
private fun Pill(text: String, color: androidx.compose.ui.graphics.Color? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text, 
            style = MaterialTheme.typography.labelLarge, 
            color = color ?: OnSurfaceMuted,
            fontWeight = if (color != null) FontWeight.Bold else FontWeight.Normal
        )
    }
}
