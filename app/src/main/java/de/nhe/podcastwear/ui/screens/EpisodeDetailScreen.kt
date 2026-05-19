package de.nhe.podcastwear.ui.screens

import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.*
import de.nhe.podcastwear.data.db.AppDatabase
import de.nhe.podcastwear.data.db.entities.EpisodeEntity
import de.nhe.podcastwear.playback.PlaybackService
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@Composable
fun EpisodeDetailScreen(
    episodeUrl: String,
    onPlayStream: () -> Unit,
    onPlayDownload: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()

    var episode by remember { mutableStateOf<EpisodeEntity?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(episodeUrl) {
        episode = db.episodeDao().getByUrl(episodeUrl)
    }

    val ep = episode ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = ep.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch { startPlayback(context, ep, useLocal = false) }
                    onPlayStream()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Stream")
            }
        }
        if (ep.localFilePath != null) {
            item {
                Button(
                    onClick = {
                        scope.launch { startPlayback(context, ep, useLocal = true) }
                        onPlayDownload()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Play download")
                }
            }
        } else {
            item {
                OutlinedButton(
                    onClick = { /* TODO: trigger DownloadWorker */ isDownloading = true },
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isDownloading) "Downloading…" else "Download")
                }
            }
        }
    }
}

private suspend fun startPlayback(context: Context, episode: EpisodeEntity, useLocal: Boolean) {
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val controller = MediaController.Builder(context, token).buildAsync().await()
    val item = PlaybackService.buildMediaItem(
        episodeUrl = episode.mediaUrl,
        podcastUrl = episode.podcastFeedUrl,
        title = episode.title,
        localFilePath = if (useLocal) episode.localFilePath else null,
    )
    controller.setMediaItem(item)
    controller.seekTo(episode.playPositionMs)
    controller.prepare()
    controller.play()
}
