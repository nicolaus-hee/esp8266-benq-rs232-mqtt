package de.nhe.podcastwear.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import de.nhe.podcastwear.data.db.AppDatabase
import de.nhe.podcastwear.data.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val KEY_EPISODE_URL = "episode_url"

        fun enqueue(context: Context, episodeUrl: String, wifiOnly: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(KEY_EPISODE_URL to episodeUrl))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_$episodeUrl",
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeUrl = inputData.getString(KEY_EPISODE_URL)
            ?: return@withContext Result.failure()

        val db = AppDatabase.get(applicationContext)
        val episode = db.episodeDao().getByUrl(episodeUrl)
            ?: return@withContext Result.failure()

        val prefs = UserPreferences(applicationContext)
        val wifiOnly = prefs.wifiOnlyDownloads.first()
        if (wifiOnly && !isOnWifi()) return@withContext Result.retry()

        val destDir = File(applicationContext.filesDir, "episodes").also { it.mkdirs() }
        val fileName = episodeUrl.substringAfterLast('/').substringBefore('?').ifBlank {
            episode.mediaUrl.hashCode().toString() + ".mp3"
        }
        val destFile = File(destDir, fileName)

        if (destFile.exists()) {
            db.episodeDao().updateDownloadPath(episodeUrl, destFile.absolutePath, System.currentTimeMillis())
            return@withContext Result.success()
        }

        val req = Request.Builder().url(episodeUrl).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext Result.retry()
            resp.body?.byteStream()?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        db.episodeDao().updateDownloadPath(episodeUrl, destFile.absolutePath, System.currentTimeMillis())
        Result.success()
    }

    private fun isOnWifi(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        return cm.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
