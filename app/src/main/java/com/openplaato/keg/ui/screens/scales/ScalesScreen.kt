package com.openplaato.keg.ui.screens.scales

import androidx.compose.foundation.background
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
    val modeLabel = if (keg.keg_mode == "2") "CO₂" else "Beer"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Drag handle — leftmost. draggableHandle() must be applied here inside
                // the ReorderableItem scope (passed in as dragHandleModifier).
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
                        Pill(modeLabel)
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

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.2f".format(amount),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber500,
                    lineHeight = 38.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    volumeUnit,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (isLow) LowRed else Amber500,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.1f".format(pct)}% remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLow) LowRed else OnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = OnSurfaceMuted)
    }
}
