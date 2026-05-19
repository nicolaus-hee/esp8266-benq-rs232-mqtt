package de.nhe.podcastwear.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.*
import de.nhe.podcastwear.data.db.AppDatabase

@Composable
fun DownloadsScreen(onEpisodeClick: (episodeUrl: String) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val episodes by db.episodeDao().observeDownloaded().collectAsState(initial = emptyList())

    if (episodes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloaded episodes", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Text("Downloads", style = MaterialTheme.typography.titleSmall) }
        items(episodes, key = { it.mediaUrl }) { episode ->
            Chip(
                onClick = { onEpisodeClick(episode.mediaUrl) },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}
