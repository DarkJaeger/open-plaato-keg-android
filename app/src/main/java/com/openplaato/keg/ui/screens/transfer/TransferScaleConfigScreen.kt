package com.openplaato.keg.ui.screens.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.OnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScaleConfigScreen(
    onBack: () -> Unit,
    viewModel: TransferScaleConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val label = state.scale?.label?.takeIf { it.isNotBlank() }
                        ?: viewModel.scaleId.take(8) + "…"
                    Text("Transfer Scale: $label", fontWeight = FontWeight.Bold)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionCard(title = "Configuration") {
                    LabeledField(
                        label = "Label",
                        value = state.labelInput,
                        onValueChange = viewModel::updateLabel,
                        placeholder = "Scale name",
                        keyboardType = KeyboardType.Text,
                    )
                    Spacer(Modifier.height(12.dp))
                    LabeledField(
                        label = "Empty keg weight (kg)",
                        value = state.emptyKegWeightInput,
                        onValueChange = viewModel::updateEmptyKegWeight,
                        placeholder = "e.g. 4.000",
                        keyboardType = KeyboardType.Decimal,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::loadKegsForAutofill,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Auto-fill from keg")
                    }
                    Spacer(Modifier.height(12.dp))
                    LabeledField(
                        label = "Target weight (kg)",
                        value = state.targetWeightInput,
                        onValueChange = viewModel::updateTargetWeight,
                        placeholder = "e.g. 24.000",
                        keyboardType = KeyboardType.Decimal,
                    )
                }
            }

            item {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(18.dp)
                                .width(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            state.feedback?.let { msg ->
                item {
                    Text(
                        msg,
                        color = if (msg.startsWith("Failed") || msg.startsWith("Delete")) MaterialTheme.colorScheme.error else Amber500,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = viewModel::showDeleteDialog,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete Scale")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Delete Transfer Scale") },
            text = { Text("Remove this transfer scale? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(onDeleted = onBack) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text("Cancel") }
            },
        )
    }

    state.kegsForAutofill?.let { kegs ->
        KegPickerDialog(
            kegs = kegs,
            onPick = viewModel::autofillFromKeg,
            onDismiss = viewModel::dismissAutofillDialog,
        )
    }
}

@Composable
private fun KegPickerDialog(
    kegs: List<Keg>,
    onPick: (Keg) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Keg") },
        text = {
            if (kegs.isEmpty()) {
                Text("No kegs found", color = OnSurfaceMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    kegs.forEach { keg ->
                        val label = keg.my_label?.takeIf { it.isNotBlank() }
                            ?: (keg.id.take(8) + "…")
                        val weightDisplay = keg.empty_keg_weight?.toDoubleOrNull()
                            ?.let { " (${"%.3f".format(it)} kg)" } ?: ""
                        TextButton(
                            onClick = { onPick(keg) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("$label$weightDisplay")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

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
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceMuted,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
