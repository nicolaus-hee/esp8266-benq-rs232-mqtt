package de.nhe.podcastwear.data.repository

import de.nhe.podcastwear.data.db.AppDatabase
import de.nhe.podcastwear.data.db.entities.EpisodeActionEntity
import de.nhe.podcastwear.data.db.entities.PodcastEntity
import de.nhe.podcastwear.data.prefs.UserPreferences
import de.nhe.podcastwear.network.EpisodeAction
import de.nhe.podcastwear.network.FeedParser
import de.nhe.podcastwear.network.GpodderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class SyncRepository(
    private val db: AppDatabase,
    private val prefs: UserPreferences,
    private val feedParser: FeedParser = FeedParser(),
) {
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)

    /**
     * Full sync cycle:
     * 1. Upload pending episode actions
     * 2. Fetch subscription changes from server → update local DB
     * 3. Refresh RSS feeds for updated subscriptions
     * 4. Fetch episode actions from server → apply played states locally
     */
    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cfg = prefs.gpodderConfig.first()
            check(cfg.isConfigured) { "Gpodder not configured" }

            val client = GpodderClient(cfg.serverUrl, cfg.username, cfg.password)

            client.registerDevice(cfg.deviceId)
            uploadPendingActions(client)
            syncSubscriptions(client, cfg.deviceId)
            syncEpisodeActions(client)
            prefs.updateLastSyncTimestamp(System.currentTimeMillis() / 1000)
        }
    }

    private suspend fun uploadPendingActions(client: GpodderClient) {
        val pending = db.episodeDao().getPendingActions()
        if (pending.isEmpty()) return

        val actions = pending.map { it.toEpisodeAction() }
        client.uploadEpisodeActions(actions)
        db.episodeDao().deleteActions(pending.map { it.id })
    }

    private suspend fun syncSubscriptions(client: GpodderClient, deviceId: String) {
        val lastSync = prefs.lastSyncTimestamp.first()
        val changes = client.getSubscriptionChanges(deviceId, lastSync)

        if (changes.add.isNotEmpty()) {
            val newPodcasts = changes.add.mapNotNull { url ->
                runCatching { feedParser.parseFeed(url) }.getOrNull()
            }
            newPodcasts.forEach { parsed ->
                db.podcastDao().upsert(parsed.podcast)
                db.episodeDao().upsertAll(parsed.episodes)
            }
        }

        if (changes.remove.isNotEmpty()) {
            changes.remove.forEach { url ->
                db.podcastDao().delete(PodcastEntity(feedUrl = url, title = "", description = ""))
            }
        }
    }

    /**
     * Refreshes RSS feeds for all subscribed podcasts to fetch new episodes.
     */
    suspend fun refreshFeeds(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val urls = db.podcastDao().getAllUrls()
            urls.forEach { url ->
                runCatching {
                    val parsed = feedParser.parseFeed(url)
                    db.podcastDao().upsert(parsed.podcast.copy(lastUpdated = System.currentTimeMillis()))
                    db.episodeDao().upsertAll(parsed.episodes)
                }
            }
        }
    }

    private suspend fun syncEpisodeActions(client: GpodderClient) {
        val lastTs = prefs.lastEpisodeSyncTimestamp.first()
        val response = client.getEpisodeActions(lastTs)

        response.actions.forEach { action ->
            if (action.action == "play") {
                val posMs = (action.position ?: 0L) * 1000
                val totalMs = (action.total ?: 0L) * 1000
                val played = totalMs > 0 && posMs >= totalMs - 5000
                db.episodeDao().updatePlayState(action.episode, played, posMs)
            }
        }
        prefs.updateLastEpisodeSyncTimestamp(response.timestamp)
    }

    private fun EpisodeActionEntity.toEpisodeAction() = EpisodeAction(
        podcast = podcastUrl,
        episode = episodeUrl,
        action = action.lowercase(),
        timestamp = isoFmt.format(Instant.ofEpochMilli(timestamp)),
        started = startedAt / 1000,
        position = positionMs / 1000,
        total = totalMs / 1000,
    )
}
