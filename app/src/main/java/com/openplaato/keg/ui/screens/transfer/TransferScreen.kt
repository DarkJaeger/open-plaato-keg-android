package com.openplaato.keg.ui.screens.transfer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.openplaato.keg.data.model.TransferScale
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.OnSurfaceMuted
import com.openplaato.keg.ui.theme.PouringGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onConfigureTransferScale: (String) -> Unit,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.scales.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber500)
                    }
                }
                state.error != null && state.scales.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                            Text("Pull to retry", color = OnSurfaceMuted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                state.scales.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No transfer scales connected", color = OnSurfaceMuted)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    ) {
                        items(state.scales, key = { it.id }) { scale ->
                            TransferScaleCard(
                                scale = scale,
                                onConfigure = { onConfigureTransferScale(scale.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferScaleCard(
    scale: TransferScale,
    onConfigure: () -> Unit,
) {
    val fillPct = (scale.fill_percent ?: 0.0).coerceIn(0.0, 100.0)
    val isComplete = (scale.fill_percent ?: 0.0) >= 100.0
    val progressColor = if (isComplete) PouringGreen else Amber500

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
                Text(
                    text = scale.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FilledTonalIconButton(onClick = onConfigure) {
                    Icon(Icons.Default.Settings, contentDescription = "Configure transfer scale")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = scale.raw_weight?.let { "%.2f".format(it) } ?: "—",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber500,
                    lineHeight = 38.sp,
                )
                Text(
                    text = " kg",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (fillPct / 100.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${"%.1f".format(scale.fill_percent ?: 0.0)}% filled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isComplete) PouringGreen else OnSurfaceMuted,
                )
                if (isComplete) {
                    Text(
                        text = "✓ Transfer complete",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PouringGreen,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Empty keg: ${scale.empty_keg_weight?.let { "%.2f".format(it) } ?: "—"} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                )
                Text(
                    text = "Target: ${scale.target_weight?.let { "%.2f".format(it) } ?: "—"} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceMuted,
                )
            }
        }
    }
}
