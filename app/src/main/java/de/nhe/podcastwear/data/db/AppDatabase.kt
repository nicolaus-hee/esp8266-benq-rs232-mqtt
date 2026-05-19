package de.nhe.podcastwear.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.nhe.podcastwear.data.db.dao.EpisodeDao
import de.nhe.podcastwear.data.db.dao.PodcastDao
import de.nhe.podcastwear.data.db.entities.EpisodeActionEntity
import de.nhe.podcastwear.data.db.entities.EpisodeEntity
import de.nhe.podcastwear.data.db.entities.PodcastEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, EpisodeActionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "podcastwear.db",
                ).build().also { instance = it }
            }
    }
}
