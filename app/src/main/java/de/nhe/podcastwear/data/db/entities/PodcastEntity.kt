package de.nhe.podcastwear.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val feedUrl: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val link: String?,
    val subscribedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = 0L,
)
