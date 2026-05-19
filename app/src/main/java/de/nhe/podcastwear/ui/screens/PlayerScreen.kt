package de.nhe.podcastwear.ui.screens

import android.content.ComponentName
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.material3.*
import de.nhe.podcastwear.playback.PlaybackService
import kotlinx.coroutines.guava.await

@Composable
fun PlayerScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val ctrl = MediaController.Builder(context, token).buildAsync().await()
        controller = ctrl
        isPlaying = ctrl.isPlaying
        title = ctrl.mediaMetadata.title?.toString() ?: ""
        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaMetadataChanged(metadata: androidx.media3.common.MediaMetadata) {
                title = metadata.title?.toString() ?: ""
            }
        })
    }

    DisposableEffect(Unit) {
        onDispose { controller?.release() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title.ifBlank { "Nothing playing" },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { controller?.seekBack() },
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("«")
            }
            Button(
                onClick = {
                    val ctrl = controller ?: return@Button
                    if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
                },
                modifier = Modifier.size(52.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(if (isPlaying) "⏸" else "▶")
            }
            OutlinedButton(
                onClick = { controller?.seekForward() },
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("»")
            }
        }
    }
}
