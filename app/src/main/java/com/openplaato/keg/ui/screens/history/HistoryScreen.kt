package com.openplaato.keg.ui.screens.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.OnSurfaceMuted
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.shapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape as VicoShape
import com.patrykandpatrick.vico.core.common.component.Component
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.common.shape.dashedShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timestampKey = ExtraStore.Key<List<Long>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    title: String,
    id: String,
    type: String, // "keg" or "airlock"
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }

    var showPercent by remember { mutableStateOf(true) }
    var showWeight by remember { mutableStateOf(true) }
    var showTemp by remember { mutableStateOf(true) }

    val dateTimeFormatter = remember(state.currentRange) {
        val pattern = when (state.currentRange) {
            "1h", "6h", "24h" -> "HH:mm"
            "7d", "30d" -> "MMM d"
            else -> "MM/dd"
        }
        SimpleDateFormat(pattern, Locale.getDefault())
    }

    LaunchedEffect(id, type) {
        if (type == "keg") {
            viewModel.loadKegHistory(id)
        } else {
            viewModel.loadAirlockHistory(id)
        }
    }

    LaunchedEffect(state.kegHistory, state.airlockHistory, showPercent, showWeight, showTemp) {
        if (type == "keg" && state.kegHistory.isNotEmpty()) {
            val p = if (showPercent) state.kegHistory.map { it.percent_of_beer_left ?: 0.0 } else emptyList()
            val w = if (showWeight) state.kegHistory.map { it.amount_left ?: 0.0 } else emptyList()
            val t = if (showTemp) state.kegHistory.map { it.keg_temperature ?: 0.0 } else emptyList()

            if (p.isNotEmpty() || w.isNotEmpty() || t.isNotEmpty()) {
                modelProducer.runTransaction {
                    lineSeries {
                        if (p.isNotEmpty()) series(p)
                        if (w.isNotEmpty()) series(w)
                        if (t.isNotEmpty()) series(t)
                    }
                    val timestamps = state.kegHistory.map { it.timestamp * 1000L }
                    extras {
                        it[timestampKey] = timestamps
                    }
                }
            }
        } else if (type == "airlock" && state.airlockHistory.isNotEmpty()) {
            val b = state.airlockHistory.map { it.bubbles_per_min ?: 0.0 }
            if (b.isNotEmpty()) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(b)
                    }
                    val timestamps = state.airlockHistory.map { it.timestamp * 1000L }
                    extras {
                        it[timestampKey] = timestamps
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val marker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            color = Color.Black,
            background = rememberShapeComponent(
                fill = fill(Amber500),
                shape = CorneredShape.Pill
            ),
            padding = insets(8.dp, 4.dp),
        ),
        indicator = { color ->
            shapeComponent(fill = fill(color), shape = CorneredShape.Pill)
        },
        guideline = rememberAxisGuidelineComponent(),
    )

    Scaffold(
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        contentWindowInsets = if (isLandscape) WindowInsets(0) else ScaffoldDefaults.contentWindowInsets
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Amber500)
            } else if (state.error != null) {
                Text(state.error!!, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            } else {
                val count = if (type == "keg") state.kegHistory.size else state.airlockHistory.size

                Column(modifier = Modifier.fillMaxSize()) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                if (type == "keg") "History" else "Bubbles per minute",
                                style = MaterialTheme.typography.titleMedium,
                                color = Amber500,
                                modifier = Modifier.weight(1f)
                            )
                            if (type == "keg") {
                                HistoryFilterChips(
                                    showPercent = showPercent,
                                    onPercentClick = { showPercent = !showPercent },
                                    showWeight = showWeight,
                                    onWeightClick = { showWeight = !showWeight },
                                    showTemp = showTemp,
                                    onTempClick = { showTemp = !showTemp }
                                )
                            }
                            RangeSelector(
                                currentRange = state.currentRange,
                                onRangeSelected = { r ->
                                    if (type == "keg") viewModel.loadKegHistory(id, r)
                                    else viewModel.loadAirlockHistory(id, r)
                                }
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        if (type == "keg") "History" else "Bubbles per minute",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Amber500
                                    )
                                }

                                RangeSelector(
                                    currentRange = state.currentRange,
                                    onRangeSelected = { r ->
                                        if (type == "keg") viewModel.loadKegHistory(id, r)
                                        else viewModel.loadAirlockHistory(id, r)
                                    }
                                )
                            }

                            if (type == "keg") {
                                Spacer(Modifier.height(8.dp))
                                HistoryFilterChips(
                                    showPercent = showPercent,
                                    onPercentClick = { showPercent = !showPercent },
                                    showWeight = showWeight,
                                    onWeightClick = { showWeight = !showWeight },
                                    showTemp = showTemp,
                                    onTempClick = { showTemp = !showTemp }
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    val hasData = if (type == "keg") {
                        (showPercent && state.kegHistory.any { it.percent_of_beer_left != null }) ||
                                (showWeight && state.kegHistory.any { it.amount_left != null }) ||
                                (showTemp && state.kegHistory.any { it.keg_temperature != null })
                    } else {
                        state.airlockHistory.any { it.bubbles_per_min != null }
                    }

                    if (count == 0 || !hasData) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history data found for this period.", color = OnSurfaceMuted)
                        }
                    } else {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = VerticalAxis.rememberStart(
                                    label = rememberAxisLabelComponent(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                ),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    label = rememberAxisLabelComponent(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    valueFormatter = { context, x, _ ->
                                        val timestamps = context.model.extraStore[timestampKey]
                                        val timestamp = timestamps.getOrNull(x.toInt())
                                        if (timestamp != null) {
                                            dateTimeFormatter.format(Date(timestamp))
                                        } else {
                                            ""
                                        }
                                    }
                                ),
                                marker = marker,
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(bottom = if (isLandscape) 0.dp else 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(
    currentRange: String,
    onRangeSelected: (String) -> Unit
) {
    val ranges = listOf("1h", "6h", "24h", "7d", "30d")
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(currentRange)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ranges.forEach { r ->
                DropdownMenuItem(
                    text = { Text(r) },
                    onClick = {
                        expanded = false
                        onRangeSelected(r)
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryFilterChips(
    showPercent: Boolean,
    onPercentClick: () -> Unit,
    showWeight: Boolean,
    onWeightClick: () -> Unit,
    showTemp: Boolean,
    onTempClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = showPercent,
            onClick = onPercentClick,
            label = { Text("Percent") }
        )
        FilterChip(
            selected = showWeight,
            onClick = onWeightClick,
            label = { Text("Weight") }
        )
        FilterChip(
            selected = showTemp,
            onClick = onTempClick,
            label = { Text("Temp") }
        )
    }
}
