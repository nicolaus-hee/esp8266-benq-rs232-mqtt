package de.nhe.podcastwear.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending gpodder EpisodeActions that have not yet been uploaded to the sync server.
 * Flushed by SyncWorker after a successful upload.
 */
@Entity(tableName = "pending_episode_actions")
data class EpisodeActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val podcastUrl: String,
    val episodeUrl: String,
    /** Action type: PLAY, PAUSE, DOWNLOAD, DELETE */
    val action: String,
    val timestamp: Long = System.currentTimeMillis(),
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val positionMs: Long = 0L,
    val totalMs: Long = 0L,
)
