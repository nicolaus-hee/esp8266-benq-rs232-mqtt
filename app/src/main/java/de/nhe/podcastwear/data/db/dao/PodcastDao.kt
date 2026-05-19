package de.nhe.podcastwear.data.db.dao

import androidx.room.*
import de.nhe.podcastwear.data.db.entities.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    fun observeAll(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun getByUrl(feedUrl: String): PodcastEntity?

    @Query("SELECT feedUrl FROM podcasts")
    suspend fun getAllUrls(): List<String>

    @Upsert
    suspend fun upsert(podcast: PodcastEntity)

    @Upsert
    suspend fun upsertAll(podcasts: List<PodcastEntity>)

    @Delete
    suspend fun delete(podcast: PodcastEntity)

    @Query("DELETE FROM podcasts WHERE feedUrl NOT IN (:activeUrls)")
    suspend fun deleteRemovedSubscriptions(activeUrls: List<String>)
}
