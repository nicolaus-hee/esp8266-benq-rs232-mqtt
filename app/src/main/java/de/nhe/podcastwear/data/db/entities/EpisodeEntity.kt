package de.nhe.podcastwear.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["feedUrl"],
            childColumns = ["podcastFeedUrl"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("podcastFeedUrl"), Index("mediaUrl", unique = true)],
)
data class EpisodeEntity(
    @PrimaryKey val mediaUrl: String,
    val podcastFeedUrl: String,
    val title: String,
    val description: String?,
    val pubDate: Long,
    val durationMs: Long,
    val mimeType: String,
    /** Absolute path to the locally downloaded file, or null if not downloaded. */
    val localFilePath: String? = null,
    val downloadedAt: Long? = null,
    val playPositionMs: Long = 0L,
    val isPlayed: Boolean = false,
    val isFavorite: Boolean = false,
    /** Episode GUID from the RSS feed, used for deduplication. */
    val guid: String? = null,
)
