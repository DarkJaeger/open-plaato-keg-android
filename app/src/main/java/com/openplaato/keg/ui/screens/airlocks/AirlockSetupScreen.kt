package com.openplaato.keg.ui.screens.airlocks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.OnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirlockSetupScreen(
    onBack: () -> Unit,
    viewModel: AirlockSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Airlock Setup", fontWeight = FontWeight.Bold) },
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
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::load,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.isLoading && state.airlocks.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber500)
                    }
                }
                state.airlocks.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No airlocks connected", color = OnSurfaceMuted)
                            Text(
                                "Pull to refresh",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceMuted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.airlocks, key = { it.id }) { airlock ->
                            AirlockSetupCard(
                                airlock = airlock,
                                labelInput = state.labelInputs[airlock.id] ?: "",
                                grainfatherInputs = state.grainfatherInputs[airlock.id] ?: GrainfatherInputs(),
                                brewfatherInputs = state.brewfatherInputs[airlock.id] ?: BrewfatherInputs(),
                                onLabelChange = { viewModel.updateLabel(airlock.id, it) },
                                onSaveLabel = { keyboard?.hide(); viewModel.saveLabel(airlock.id) },
                                onGrainfatherChange = { viewModel.updateGrainfather(airlock.id, it) },
                                onSaveGrainfather = { keyboard?.hide(); viewModel.saveGrainfather(airlock.id) },
                                onBrewfatherChange = { viewModel.updateBrewfather(airlock.id, it) },
                                onSaveBrewfather = { keyboard?.hide(); viewModel.saveBrewfather(airlock.id) },
                            )
                        }
                        state.feedback?.let { msg ->
                            item {
                                Text(
                                    msg,
                                    color = if (msg.startsWith("Failed")) MaterialTheme.colorScheme.error else Amber500,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AirlockSetupCard(
    airlock: Airlock,
    labelInput: String,
    grainfatherInputs: GrainfatherInputs,
    brewfatherInputs: BrewfatherInputs,
    onLabelChange: (String) -> Unit,
    onSaveLabel: () -> Unit,
    onGrainfatherChange: (GrainfatherInputs.() -> GrainfatherInputs) -> Unit,
    onSaveGrainfather: () -> Unit,
    onBrewfatherChange: (BrewfatherInputs.() -> BrewfatherInputs) -> Unit,
    onSaveBrewfather: () -> Unit,
) {
    var grainfatherExpanded by remember { mutableStateOf(false) }
    var brewfatherExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Device ID
        Text(airlock.id, style = MaterialTheme.typography.labelLarge, color = OnSurfaceMuted)

        // Live stats
        if (airlock.temperature != null || airlock.bubbles_per_min != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                airlock.temperature?.let { StatItem("Temp", "${"%.1f".format(it)}°C") }
                airlock.bubbles_per_min?.let { StatItem("BPM", "%.1f".format(it)) }
            }
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // ── Label ─────────────────────────────────────────────────
        Text("Label", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceMuted,
            modifier = Modifier.padding(bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = labelInput,
                onValueChange = onLabelChange,
                placeholder = { Text("e.g. Primary Fermentor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSaveLabel() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onSaveLabel,
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        }

        // ── Grainfather ────────────────────────────────────────────
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().clickable { grainfatherExpanded = !grainfatherExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Grainfather",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (grainfatherInputs.enabled) Amber500 else MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                if (grainfatherExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceMuted,
            )
        }
        if (grainfatherExpanded) {
            Spacer(Modifier.height(10.dp))
            IntegrationToggleRow(
                label = "Enable Grainfather",
                checked = grainfatherInputs.enabled,
                onCheckedChange = { onGrainfatherChange { copy(enabled = it) } },
            )
            Spacer(Modifier.height(8.dp))
            UnitToggleRow(
                selected = grainfatherInputs.unit,
                onSelect = { onGrainfatherChange { copy(unit = it) } },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = grainfatherInputs.sg,
                onValueChange = { onGrainfatherChange { copy(sg = it) } },
                label = { Text("Specific Gravity") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = grainfatherInputs.url,
                onValueChange = { onGrainfatherChange { copy(url = it) } },
                label = { Text("Grainfather URL (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSaveGrainfather,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) { Text("Save Grainfather", fontWeight = FontWeight.Bold) }
        }

        // ── Brewfather ─────────────────────────────────────────────
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().clickable { brewfatherExpanded = !brewfatherExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Brewfather",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (brewfatherInputs.enabled) Amber500 else MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                if (brewfatherExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceMuted,
            )
        }
        if (brewfatherExpanded) {
            Spacer(Modifier.height(10.dp))
            IntegrationToggleRow(
                label = "Enable Brewfather",
                checked = brewfatherInputs.enabled,
                onCheckedChange = { onBrewfatherChange { copy(enabled = it) } },
            )
            Spacer(Modifier.height(8.dp))
            UnitToggleRow(
                selected = brewfatherInputs.unit,
                onSelect = { onBrewfatherChange { copy(unit = it) } },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = brewfatherInputs.url,
                onValueChange = { onBrewfatherChange { copy(url = it) } },
                label = { Text("Brewfather Stream URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = brewfatherInputs.sg,
                onValueChange = { onBrewfatherChange { copy(sg = it) } },
                label = { Text("Specific Gravity") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = brewfatherInputs.og,
                onValueChange = { onBrewfatherChange { copy(og = it) } },
                label = { Text("Original Gravity (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = brewfatherInputs.batchVolume,
                onValueChange = { onBrewfatherChange { copy(batchVolume = it) } },
                label = { Text("Batch Volume in L (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSaveBrewfather,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) { Text("Save Brewfather", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun IntegrationToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Amber500),
        )
    }
}

@Composable
private fun UnitToggleRow(selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Celsius" to "celsius", "Fahrenheit" to "fahrenheit").forEach { (label, value) ->
            val isSelected = selected == value
            if (isSelected) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) { Text(label, fontWeight = FontWeight.Bold) }
            } else {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onSelect(value) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) { Text(label) }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = Amber500)
    }
}
