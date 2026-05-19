package de.nhe.podcastwear

import android.app.Application
import androidx.work.Configuration
import de.nhe.podcastwear.data.db.AppDatabase

class PodcastWearApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Eagerly open DB on startup to trigger schema creation
        AppDatabase.get(this)
    }
}
