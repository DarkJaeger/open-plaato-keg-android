package com.openplaato.keg.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Liquor
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.openplaato.keg.ui.theme.Amber500
import com.openplaato.keg.ui.theme.CardBackground
import com.openplaato.keg.ui.theme.OnSurfaceMuted
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val steps: List<String>,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Default.Settings,
        title = "Connect to Your Server",
        description = "The app connects to an Open Plaato Keg server on your local network. You'll need the server's IP address and port to get started.",
        steps = listOf(
            "Open the Settings tab",
            "Enter your server URL (e.g. http://192.168.1.10:8085)",
            "Tap Save — your scales will appear automatically",
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Liquor,
        title = "Set Up Keg Scales",
        description = "Once connected, your keg scales appear in the Kegs tab. Tap any scale to configure it.",
        steps = listOf(
            "Choose metric or US units, and weight or volume display",
            "Set the keg mode (Beer or CO₂)",
            "Enter the empty keg weight and max keg volume",
            "Place the empty keg on the scale and tap Tare",
        ),
    ),
    OnboardingPage(
        icon = Icons.Default.Scale,
        title = "Set Up Transfer Scales",
        description = "Transfer scales appear in the Transfer tab and help you hit a precise fill weight when transferring beer.",
        steps = listOf(
            "Tap a transfer scale to open its config",
            "Enter a label and the empty keg weight",
            "Set the target fill weight (or pick from an existing keg)",
            "Tap Save — the scale will track fill progress live",
        ),
    ),
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    fun finish() {
        viewModel.markSeen()
        onFinish()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                OnboardingPageContent(page = pages[index])
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (i == pagerState.currentPage) Amber500 else OnSurfaceMuted),
                        )
                    }
                }

                if (pagerState.currentPage < pages.lastIndex) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = ::finish,
                            modifier = Modifier.weight(1f),
                        ) { Text("Skip") }
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Amber500,
                                contentColor = Color.Black,
                            ),
                        ) { Text("Next", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    Button(
                        onClick = ::finish,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber500,
                            contentColor = Color.Black,
                        ),
                    ) { Text("Get Started", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = Amber500,
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            page.steps.forEachIndexed { i, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Amber500),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${i + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
