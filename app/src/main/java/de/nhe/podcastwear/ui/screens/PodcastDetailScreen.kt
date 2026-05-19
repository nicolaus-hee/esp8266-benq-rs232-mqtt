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
import de.nhe.podcastwear.data.db.entities.EpisodeEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PodcastDetailScreen(feedUrl: String, onEpisodeClick: (episodeUrl: String) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val episodes by db.episodeDao().observeByPodcast(feedUrl).collectAsState(initial = emptyList())
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(episodes, key = { it.mediaUrl }) { episode ->
            EpisodeChip(
                episode = episode,
                dateFmt = dateFmt,
                onClick = { onEpisodeClick(episode.mediaUrl) },
            )
        }
    }
}

@Composable
private fun EpisodeChip(episode: EpisodeEntity, dateFmt: SimpleDateFormat, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = episode.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            val date = if (episode.pubDate > 0) dateFmt.format(Date(episode.pubDate)) else ""
            val played = if (episode.isPlayed) " ✓" else ""
            val downloaded = if (episode.localFilePath != null) " ↓" else ""
            Text(text = "$date$played$downloaded")
        },
    )
}
