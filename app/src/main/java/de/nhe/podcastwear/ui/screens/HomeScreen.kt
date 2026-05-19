package de.nhe.podcastwear.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.*

@Composable
fun HomeScreen(
    onOpenPlayer: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "PodcastWear",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        item {
            Button(onClick = onOpenPlayer, modifier = Modifier.fillMaxWidth()) {
                Text("Now Playing")
            }
        }
        item {
            Button(onClick = onOpenSubscriptions, modifier = Modifier.fillMaxWidth()) {
                Text("Podcasts")
            }
        }
        item {
            Button(onClick = onOpenDownloads, modifier = Modifier.fillMaxWidth()) {
                Text("Downloads")
            }
        }
        item {
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Settings")
            }
        }
    }
}
