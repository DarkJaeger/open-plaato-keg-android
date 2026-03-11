package com.openplaato.keg.ui.screens.scales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.OnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleConfigScreen(
    onBack: () -> Unit,
    viewModel: ScaleConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val keg = state.keg

    var showTareDialog by remember { mutableStateOf(false) }
    var showEmptyKegDialog by remember { mutableStateOf(false) }
    var showResetPourDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val label = keg?.my_label?.takeIf { it.isNotBlank() }
                        ?: viewModel.kegId.take(8) + "…"
                    Text("Scale: $label", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber500)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Live readings ──────────────────────────────────────────
            item {
                SectionCard(title = "Live Readings") {
                    keg?.let { k ->
                        val amountLabel = if (k.measure_unit == "1") "Weight" else "Volume"
                        val amountUnit = when (k.beer_left_unit) { "litre" -> "L"; else -> k.beer_left_unit ?: "" }
                        ReadingRow(amountLabel, "${k.amount_left?.let { "%.2f".format(it) } ?: "—"} $amountUnit")
                        ReadingRow("Remaining", "${k.percent_of_beer_left?.let { "%.1f".format(it) } ?: "—"}%")
                        ReadingRow("Temperature", "${k.keg_temperature?.let { "%.1f".format(it) } ?: "—"}${k.temperature_unit ?: "°C"}")
                        k.weight_raw?.takeIf { it.isNotBlank() }?.let { ReadingRow("Raw weight", "$it kg") }
                        k.last_pour?.takeIf { it > 0 }?.let { ReadingRow("Last pour", "%.0f ml".format(it * 1000)) }
                    } ?: Text("No data", color = OnSurfaceMuted)
                }
            }

            // ── Display units ──────────────────────────────────────────
            item {
                SectionCard(title = "Display Units") {
                    ConfigLabel("Unit system")
                    ToggleRow(
                        options = listOf("Metric" to "metric", "US Imperial" to "us"),
                        selected = if (keg?.unit == "2") "us" else "metric",
                        onSelect = { viewModel.setUnit(it) },
                    )
                    Spacer(Modifier.height(12.dp))
                    ConfigLabel("Measure as")
                    ToggleRow(
                        options = listOf("Volume" to "volume", "Weight" to "weight"),
                        selected = if (keg?.measure_unit == "1") "weight" else "volume",
                        onSelect = { viewModel.setMeasureUnit(it) },
                    )
                }
            }

            // ── Keg mode ───────────────────────────────────────────────
            item {
                SectionCard(title = "Keg Mode") {
                    ToggleRow(
                        options = listOf("Beer" to "beer", "CO₂" to "co2"),
                        selected = if (keg?.keg_mode == "2") "co2" else "beer",
                        onSelect = { viewModel.setKegMode(it) },
                    )
                }
            }

            // ── Sensitivity ────────────────────────────────────────────
            item {
                SectionCard(title = "Pour Sensitivity") {
                    ToggleRow(
                        options = listOf(
                            "Very Low" to "very_low",
                            "Low" to "low",
                            "Medium" to "medium",
                            "High" to "high",
                        ),
                        selected = when (keg?.sensitivity) {
                            "1" -> "very_low"; "2" -> "low"; "3" -> "medium"; "4" -> "high"; else -> "medium"
                        },
                        onSelect = { viewModel.setSensitivity(it) },
                    )
                }
            }

            // ── Calibration inputs ─────────────────────────────────────
            item {
                SectionCard(title = "Calibration") {
                    InputRow(
                        label = "Empty keg weight",
                        hint = "kg",
                        value = state.emptyKegWeightInput,
                        onValueChange = { viewModel.updateInput { copy(emptyKegWeightInput = it) } },
                        onSet = { viewModel.saveEmptyKegWeight() },
                    )
                    Spacer(Modifier.height(10.dp))
                    InputRow(
                        label = "Max keg volume",
                        hint = if (keg?.unit == "2") "lbs / gal" else "kg / L",
                        value = state.maxVolumeInput,
                        onValueChange = { viewModel.updateInput { copy(maxVolumeInput = it) } },
                        onSet = { viewModel.saveMaxVolume() },
                    )
                    Spacer(Modifier.height(10.dp))
                    InputRow(
                        label = "Calibrate with known weight",
                        hint = "kg",
                        value = state.calWeightInput,
                        onValueChange = { viewModel.updateInput { copy(calWeightInput = it) } },
                        onSet = { viewModel.saveCalibrateKnownWeight() },
                        setLabel = "Calibrate",
                    )
                    Spacer(Modifier.height(10.dp))
                    InputRow(
                        label = "Temperature offset",
                        hint = "°",
                        value = state.tempOffsetInput,
                        onValueChange = { viewModel.updateInput { copy(tempOffsetInput = it) } },
                        onSet = { viewModel.saveTemperatureOffset() },
                    )
                }
            }

            // ── Actions ────────────────────────────────────────────────
            item {
                SectionCard(title = "Actions") {
                    // Tare
                    val isTaring = state.tareState == TareState.TARING || state.tareState == TareState.RELEASING
                    ActionButton(
                        label = when (state.tareState) {
                            TareState.TARING -> "Taring…"
                            TareState.RELEASING -> "Holding (3 s)…"
                            TareState.DONE -> "Tare complete ✓"
                            TareState.ERROR -> "Tare failed"
                            TareState.IDLE -> "Tare Scale"
                        },
                        enabled = state.tareState == TareState.IDLE,
                        loading = isTaring,
                        onClick = { showTareDialog = true },
                    )
                    Spacer(Modifier.height(10.dp))

                    // Set empty keg
                    val isSettingEmpty = state.emptyKegState == TareState.TARING || state.emptyKegState == TareState.RELEASING
                    ActionButton(
                        label = when (state.emptyKegState) {
                            TareState.TARING -> "Sending…"
                            TareState.RELEASING -> "Holding (3 s)…"
                            TareState.DONE -> "Empty keg set ✓"
                            TareState.ERROR -> "Command failed"
                            TareState.IDLE -> "Set Empty Keg"
                        },
                        enabled = state.emptyKegState == TareState.IDLE,
                        loading = isSettingEmpty,
                        onClick = { showEmptyKegDialog = true },
                    )
                    Spacer(Modifier.height(10.dp))

                    // Reset last pour
                    OutlinedButton(
                        onClick = { showResetPourDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reset Last Pour")
                    }
                }
            }

            // Feedback banner
            item {
                state.feedback?.let { msg ->
                    Text(
                        msg,
                        color = if (msg.startsWith("Failed")) MaterialTheme.colorScheme.error else Amber500,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Confirmation dialogs
    if (showTareDialog) {
        ConfirmDialog(
            title = "Tare Scale",
            text = "Place the empty keg on the scale. The app will hold the tare command for 3 seconds then release automatically.",
            confirmLabel = "Start Tare",
            onConfirm = { showTareDialog = false; viewModel.tare() },
            onDismiss = { showTareDialog = false },
        )
    }
    if (showEmptyKegDialog) {
        ConfirmDialog(
            title = "Set Empty Keg",
            text = "This calibrates the empty keg weight on the scale hardware. The command will hold for 3 seconds then release.",
            confirmLabel = "Set Empty Keg",
            onConfirm = { showEmptyKegDialog = false; viewModel.setEmptyKeg() },
            onDismiss = { showEmptyKegDialog = false },
        )
    }
    if (showResetPourDialog) {
        ConfirmDialog(
            title = "Reset Last Pour",
            text = "Clear the last pour reading?",
            confirmLabel = "Reset",
            onConfirm = { showResetPourDialog = false; viewModel.resetLastPour() },
            onDismiss = { showResetPourDialog = false },
        )
    }
}

// ── Shared composables ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = Amber500,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        content()
    }
}

@Composable
private fun ConfigLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun ReadingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ToggleRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            val isSelected = selected == value
            if (isSelected) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) { Text(label, fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(
                    onClick = { onSelect(value) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun InputRow(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSet: () -> Unit,
    setLabel: String = "Set",
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted, modifier = Modifier.padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(hint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSet,
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) { Text(setLabel, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Amber500),
            ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
