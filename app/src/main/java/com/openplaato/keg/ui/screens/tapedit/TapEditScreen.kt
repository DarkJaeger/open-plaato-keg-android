package com.openplaato.keg.ui.screens.tapedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.OnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapEditScreen(
    tapId: String,
    onBack: () -> Unit,
    viewModel: TapEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState(initial = "")
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadHandle(context, it) }
    }
    val isNew = tapId == "new"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Tap" else "Edit Tap", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Load from beverage library
            if (state.beverages.isNotEmpty()) {
                SectionLabel("Load from Library")
                BeveragePickerDropdown(
                    beverages = state.beverages,
                    onSelect = { viewModel.fillFromBeverage(it) },
                )
            }

            SectionLabel("Tap Info")
            OutlinedTextField(
                value = state.tapNumber,
                onValueChange = { viewModel.update { copy(tapNumber = it) } },
                label = { Text("Tap Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Beer Info")
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.update { copy(name = it) } },
                label = { Text("Beer Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.brewery,
                onValueChange = { viewModel.update { copy(brewery = it) } },
                label = { Text("Brewery") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.style,
                onValueChange = { viewModel.update { copy(style = it) } },
                label = { Text("Style") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.abv,
                    onValueChange = { viewModel.update { copy(abv = it) } },
                    label = { Text("ABV %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.ibu,
                    onValueChange = { viewModel.update { copy(ibu = it) } },
                    label = { Text("IBU") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }

            // Color picker row
            SectionLabel("Color")
            ColorSwatchRow(
                selected = state.color,
                onSelect = { viewModel.update { copy(color = it) } },
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.update { copy(description = it) } },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.tastingNotes,
                onValueChange = { viewModel.update { copy(tastingNotes = it) } },
                label = { Text("Tasting Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Linked Keg")
            KegPickerDropdown(
                kegs = state.kegs,
                selectedKegId = state.kegId,
                onSelect = { viewModel.update { copy(kegId = it) } },
            )

            SectionLabel("Open-Tap Display")
            OutlinedTextField(
                value = state.deviceId,
                onValueChange = { if (it.length <= 6) viewModel.update { copy(deviceId = it) } },
                label = { Text("Device ID (max 6 chars)") },
                placeholder = { Text("e.g. tap1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("Tap Handle")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.handleImage != null) {
                    AsyncImage(
                        model = "$serverUrl/uploads/tap-handles/${state.handleImage}",
                        contentDescription = "Tap handle",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    TextButton(onClick = { viewModel.selectHandle(null) }) { Text("Remove") }
                }
                if (state.isUploadingHandle) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Amber500)
                } else {
                    OutlinedButton(onClick = { showPicker = true }) { Text("Change handle image") }
                }
            }

            state.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.save(onBack) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) {
                if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                else Text("Save Tap", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Tap") },
            text = { Text("Remove this tap configuration? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.delete(onBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showPicker) {
        HandlePickerDialog(
            serverUrl = serverUrl,
            tapHandles = state.tapHandles,
            isUploading = state.isUploadingHandle,
            onSelect = { viewModel.selectHandle(it); showPicker = false },
            onUpload = { galleryLauncher.launch("image/*") },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun HandlePickerDialog(
    serverUrl: String,
    tapHandles: List<String>,
    isUploading: Boolean,
    onSelect: (String?) -> Unit,
    onUpload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tap Handle Image") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (tapHandles.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardBackground)
                                    .clickable { onSelect(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "None", tint = OnSurfaceMuted)
                            }
                        }
                        items(tapHandles) { filename ->
                            AsyncImage(
                                model = "$serverUrl/uploads/tap-handles/$filename",
                                contentDescription = filename,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelect(filename) },
                            )
                        }
                    }
                }
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Amber500)
                } else {
                    OutlinedButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                        Text("Upload from gallery")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeveragePickerDropdown(beverages: List<Beverage>, onSelect: (Beverage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "— select to auto-fill —",
            onValueChange = {},
            readOnly = true,
            label = { Text("Beverage Library") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            beverages.forEach { bev ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(bev.displayName, fontWeight = FontWeight.Medium)
                            val sub = listOfNotNull(bev.brewery, bev.style).joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                        }
                    },
                    onClick = { onSelect(bev); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KegPickerDropdown(
    kegs: List<com.openplaato.keg.data.model.Keg>,
    selectedKegId: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDisplay = kegs.find { it.id == selectedKegId }
        ?.let { it.my_label?.takeIf { l -> l.isNotBlank() } ?: it.id.take(8) + "…" }
        ?: "None"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedDisplay,
            onValueChange = {},
            readOnly = true,
            label = { Text("Linked Keg") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = { onSelect(""); expanded = false },
            )
            kegs.forEach { keg ->
                val label = keg.my_label?.takeIf { it.isNotBlank() } ?: keg.id.take(8) + "…"
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(keg.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = OnSurfaceMuted,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private val colorSwatches = listOf(
    "#f59e0b", "#f97316", "#ef4444", "#ec4899",
    "#a855f7", "#3b82f6", "#06b6d4", "#10b981",
    "#84cc16", "#ffffff", "#9ca3af", "#c9a849",
)

@Composable
private fun ColorSwatchRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colorSwatches.forEach { hex ->
            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrElse { Color.Gray }
            val isSelected = selected.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(hex) },
            )
        }
    }
}
