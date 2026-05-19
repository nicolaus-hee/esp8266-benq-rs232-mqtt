package de.nhe.podcastwear.sync

import android.content.Context
import androidx.work.*
import de.nhe.podcastwear.data.db.AppDatabase
import de.nhe.podcastwear.data.prefs.UserPreferences
import de.nhe.podcastwear.data.repository.SyncRepository
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_ONCE = "sync_once"

        /** Schedules periodic sync; replaces any existing schedule. */
        fun schedulePeriodicSync(context: Context, intervalHours: Long = 4L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /** Triggers an immediate one-off sync. */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val prefs = UserPreferences(applicationContext)
        val repo = SyncRepository(db, prefs)

        return repo.sync().fold(
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < 3) Result.retry() else Result.failure() },
        )
    }
}
