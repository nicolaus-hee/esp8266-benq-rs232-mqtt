package de.nhe.podcastwear.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import de.nhe.podcastwear.MainActivity
import de.nhe.podcastwear.data.db.AppDatabase
import de.nhe.podcastwear.data.db.entities.EpisodeActionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus= */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionIntent)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) recordPlayAction(finished = true)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) recordPlayAction(finished = false)
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun recordPlayAction(finished: Boolean) {
        val item = player.currentMediaItem ?: return
        val episodeUrl = item.mediaId.ifBlank { item.localConfiguration?.uri?.toString() } ?: return
        val podcastUrl = item.mediaMetadata.extras?.getString("podcast_url") ?: return
        val positionMs = player.currentPosition
        val totalMs = player.duration.takeIf { it > 0L } ?: 0L

        val action = EpisodeActionEntity(
            podcastUrl = podcastUrl,
            episodeUrl = episodeUrl,
            action = if (finished) "PLAY" else "PAUSE",
            startedAt = 0L,
            positionMs = positionMs,
            totalMs = totalMs,
        )
        serviceScope.launch(Dispatchers.IO) {
            AppDatabase.get(applicationContext).episodeDao().insertAction(action)
        }
    }

    // ── Helper used by UI to build a MediaItem ────────────────────────────────

    companion object {
        fun buildMediaItem(
            episodeUrl: String,
            podcastUrl: String,
            title: String,
            localFilePath: String? = null,
        ): MediaItem {
            val uri = if (localFilePath != null) "file://$localFilePath" else episodeUrl
            return MediaItem.Builder()
                .setMediaId(episodeUrl)
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .setExtras(android.os.Bundle().apply {
                            putString("podcast_url", podcastUrl)
                        })
                        .build()
                )
                .build()
        }
    }
}
