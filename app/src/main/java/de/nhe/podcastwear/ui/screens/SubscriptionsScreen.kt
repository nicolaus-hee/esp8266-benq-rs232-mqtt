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
import de.nhe.podcastwear.data.db.entities.PodcastEntity

@Composable
fun SubscriptionsScreen(onPodcastClick: (feedUrl: String) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val podcasts by db.podcastDao().observeAll().collectAsState(initial = emptyList())

    if (podcasts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No subscriptions yet.\nSync to load podcasts.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text("Podcasts", style = MaterialTheme.typography.titleSmall)
        }
        items(podcasts, key = { it.feedUrl }) { podcast ->
            PodcastChip(podcast = podcast, onClick = { onPodcastClick(podcast.feedUrl) })
        }
    }
}

@Composable
private fun PodcastChip(podcast: PodcastEntity, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = podcast.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
