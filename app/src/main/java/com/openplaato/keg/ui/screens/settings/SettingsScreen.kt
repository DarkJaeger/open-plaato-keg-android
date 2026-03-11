package com.openplaato.keg.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.OnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSetupAirlocks: () -> Unit = {},
    onBrowseBrewfatherBatches: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Server connection ─────────────────────────────────────
            Text("Server Connection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Enter the IP address or hostname of your Open Plaato Keg server.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
            )
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onUrlChange,
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.10:4000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); viewModel.save() }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.saved) {
                Text(
                    "Saved — restart the app for the connection to take effect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Amber500,
                )
            }
            Button(
                onClick = { keyboard?.hide(); viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) { Text("Save", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))

            // ── Airlocks ──────────────────────────────────────────────
            Text("Airlocks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable airlock support", style = MaterialTheme.typography.bodyLarge)
                    Text("Allow Plaato Airlock devices to connect.",
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                }
                Switch(
                    checked = state.airlockEnabled,
                    onCheckedChange = viewModel::toggleAirlockEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Amber500),
                )
            }
            OutlinedButton(onClick = onSetupAirlocks, modifier = Modifier.fillMaxWidth()) {
                Text("Set up Airlocks", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(8.dp))

            // ── Notifications ─────────────────────────────────────────
            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pour notifications", style = MaterialTheme.typography.bodyLarge)
                    Text("Notify when a pour is detected on a keg.",
                        style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                }
                Switch(
                    checked = state.pourNotificationsEnabled,
                    onCheckedChange = { viewModel.togglePourNotifications() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Amber500),
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Brewfather batch import ────────────────────────────────
            Text("Brewfather Import", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Enter your Brewfather credentials to import batches into the beverage library.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceMuted,
            )
            if (state.brewfatherConfigured) {
                Text(
                    "Credentials configured \u2713",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Amber500,
                    fontWeight = FontWeight.Medium,
                )
            }
            OutlinedTextField(
                value = state.brewfatherUserId,
                onValueChange = viewModel::onBrewfatherUserIdChange,
                label = { Text("Brewfather User ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.brewfatherApiKey,
                onValueChange = viewModel::onBrewfatherApiKeyChange,
                label = { Text("Brewfather API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); viewModel.saveBrewfatherCreds() }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.brewfatherSaved) {
                Text("Credentials saved", style = MaterialTheme.typography.bodyMedium, color = Amber500)
            }
            Button(
                onClick = { keyboard?.hide(); viewModel.saveBrewfatherCreds() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.Black),
            ) { Text("Save Credentials", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onBrowseBrewfatherBatches,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.brewfatherConfigured,
            ) { Text("Browse Batches", fontWeight = FontWeight.Medium) }
        }
    }
}
