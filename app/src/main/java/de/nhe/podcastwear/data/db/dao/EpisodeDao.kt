package de.nhe.podcastwear.data.db.dao

import androidx.room.*
import de.nhe.podcastwear.data.db.entities.EpisodeActionEntity
import de.nhe.podcastwear.data.db.entities.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE podcastFeedUrl = :feedUrl ORDER BY pubDate DESC")
    fun observeByPodcast(feedUrl: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE isPlayed = 0 ORDER BY pubDate DESC LIMIT 50")
    fun observeQueue(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE localFilePath IS NOT NULL ORDER BY downloadedAt DESC")
    fun observeDownloaded(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE mediaUrl = :url")
    suspend fun getByUrl(url: String): EpisodeEntity?

    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>)

    @Query("UPDATE episodes SET isPlayed = :played, playPositionMs = :positionMs WHERE mediaUrl = :url")
    suspend fun updatePlayState(url: String, played: Boolean, positionMs: Long)

    @Query("UPDATE episodes SET localFilePath = :path, downloadedAt = :time WHERE mediaUrl = :url")
    suspend fun updateDownloadPath(url: String, path: String, time: Long)

    @Query("UPDATE episodes SET localFilePath = NULL, downloadedAt = NULL WHERE mediaUrl = :url")
    suspend fun clearDownload(url: String)

    // EpisodeActions (gpodder pending queue)

    @Insert
    suspend fun insertAction(action: EpisodeActionEntity)

    @Query("SELECT * FROM pending_episode_actions ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<EpisodeActionEntity>

    @Query("DELETE FROM pending_episode_actions WHERE id IN (:ids)")
    suspend fun deleteActions(ids: List<Long>)
}
