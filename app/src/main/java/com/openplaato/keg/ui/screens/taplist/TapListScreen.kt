package com.openplaato.keg.ui.screens.taplist

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapWithKeg
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.LowRed
import com.openplaato.keg.ui.theme.OnSurfaceMuted
import com.openplaato.keg.ui.theme.PouringGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapListScreen(
    viewModel: TapListViewModel,
    onEditTap: (Tap) -> Unit,
    onNewTap: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tap List", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTap, containerColor = Amber500) {
                Icon(Icons.Default.Add, contentDescription = "New tap", tint = Color.Black)
            }
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
                state.isLoading && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Amber500)
                    }
                }
                state.error != null && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.error ?: "Error",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Pull to retry",
                                color = OnSurfaceMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No taps configured", color = OnSurfaceMuted)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp, vertical = 16.dp
                        ),
                    ) {
                        items(state.items, key = { it.tap.id }) { item ->
                            TapCard(
                                item = item,
                                serverUrl = serverUrl,
                                onEdit = { onEditTap(item.tap) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TapCard(item: TapWithKeg, serverUrl: String = "", onEdit: () -> Unit) {
    val tap = item.tap
    val hasKeg = item.keg != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable { onEdit() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (tap.handle_image != null) {
                    AsyncImage(
                        model = "$serverUrl/uploads/tap-handles/${tap.handle_image}",
                        contentDescription = "Tap handle",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val meta = listOfNotNull(
                        tap.brewery?.takeIf { it.isNotBlank() },
                        tap.style?.takeIf { it.isNotBlank() },
                        tap.abv?.takeIf { it.isNotBlank() }?.let { "$it%" },
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasKeg) {
                        if (item.isPouring) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(PouringGreen),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        item.temperature?.let { temp ->
                            Text(
                                text = "${"%.1f".format(temp)}${item.tempUnit}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Amber500,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OnSurfaceMuted)
                    }
                }
            }

            if (hasKeg) {
                Spacer(Modifier.height(12.dp))

                // Volume
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%.2f".format(item.amountLeft),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber500,
                        lineHeight = 38.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.volumeUnit,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Progress bar
                val progressColor = if (item.isLow) LowRed else Amber500
                LinearProgressIndicator(
                    progress = { (item.percentLeft / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${"%.1f".format(item.percentLeft)}% remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isLow) LowRed else OnSurfaceMuted,
                )

                // Last pour
                if (item.lastPour > 0) {
                    val (mult, unit) = when (item.tap.keg_id?.let { _ -> item.keg?.beer_left_unit }) {
                        "lbs" -> 16.0 to "oz"
                        "kg" -> 1000.0 to "g"
                        "gal" -> 128.0 to "oz"
                        else -> 1000.0 to "ml"
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Last pour · ${(item.lastPour * mult).toInt()} $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceMuted,
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No keg linked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceMuted,
                )
            }
        }
    }
}
