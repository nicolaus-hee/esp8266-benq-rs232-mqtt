package de.nhe.podcastwear.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

// ── JSON models ──────────────────────────────────────────────────────────────

@Serializable
data class SubscriptionChanges(
    val add: List<String> = emptyList(),
    val remove: List<String> = emptyList(),
)

@Serializable
data class SubscriptionChangesResponse(
    val timestamp: Long,
    val updateUrls: List<List<String>> = emptyList(),
)

@Serializable
data class EpisodeAction(
    val podcast: String,
    val episode: String,
    val action: String,           // "play" | "pause" | "download" | "delete" | "new"
    val timestamp: String? = null,
    val started: Long? = null,
    val position: Long? = null,
    val total: Long? = null,
)

@Serializable
data class EpisodeActionsResponse(
    val actions: List<EpisodeAction>,
    val timestamp: Long,
)

@Serializable
data class EpisodeActionsUploadResponse(
    val timestamp: Long,
    @SerialName("update_urls") val updateUrls: List<List<String>> = emptyList(),
)

@Serializable
data class DeviceRegistration(
    val caption: String,
    val type: String = "mobile",
)

// ── Client ───────────────────────────────────────────────────────────────────

/**
 * Implements the gpodder.net / opodsync REST API (v2).
 *
 * All methods throw [IOException] on network or HTTP errors.
 */
class GpodderClient(
    private val serverUrl: String,
    private val username: String,
    password: String,
    debug: Boolean = false,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Authorization", Credentials.basic(username, password))
                .build()
            chain.proceed(req)
        }
        .also { builder ->
            if (debug) builder.addNetworkInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        .build()

    private val jsonMime = "application/json; charset=utf-8".toMediaType()

    // ── Device ───────────────────────────────────────────────────────────────

    /**
     * Registers (or updates) this device on the sync server.
     * Safe to call on every launch; server is idempotent.
     */
    fun registerDevice(deviceId: String, deviceName: String = "PodcastWear") {
        val body = json.encodeToString(DeviceRegistration(caption = deviceName))
            .toRequestBody(jsonMime)
        val req = Request.Builder()
            .url("$serverUrl/api/2/devices/$username/$deviceId.json")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("registerDevice HTTP ${resp.code}")
        }
    }

    // ── Subscriptions ────────────────────────────────────────────────────────

    /**
     * Fetches the full subscription list for [deviceId].
     * Returns a list of feed URLs.
     */
    fun getSubscriptions(deviceId: String): List<String> {
        val req = Request.Builder()
            .url("$serverUrl/api/2/subscriptions/$username/$deviceId.json")
            .get()
            .build()
        val responseBody = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("getSubscriptions HTTP ${resp.code}")
            resp.body?.string() ?: "[]"
        }
        return json.decodeFromString(responseBody)
    }

    /**
     * Uploads subscription changes (add/remove) to the server.
     * Returns the server timestamp to be stored for subsequent diff calls.
     */
    fun uploadSubscriptionChanges(deviceId: String, add: List<String>, remove: List<String>): Long {
        val payload = json.encodeToString(SubscriptionChanges(add = add, remove = remove))
            .toRequestBody(jsonMime)
        val req = Request.Builder()
            .url("$serverUrl/api/2/subscriptions/$username/$deviceId.json")
            .post(payload)
            .build()
        val responseBody = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("uploadSubscriptions HTTP ${resp.code}")
            resp.body?.string() ?: "{\"timestamp\":0}"
        }
        return json.decodeFromString<SubscriptionChangesResponse>(responseBody).timestamp
    }

    /**
     * Fetches subscription changes since [sinceTimestamp] (epoch seconds).
     * Pass 0 to get all subscriptions.
     */
    fun getSubscriptionChanges(deviceId: String, sinceTimestamp: Long): SubscriptionChanges {
        val url = "$serverUrl/api/2/subscriptions/$username/$deviceId.json?since=$sinceTimestamp"
        val req = Request.Builder().url(url).get().build()
        val responseBody = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("getSubscriptionChanges HTTP ${resp.code}")
            resp.body?.string() ?: "{\"add\":[],\"remove\":[]}"
        }
        return json.decodeFromString(responseBody)
    }

    // ── Episode actions ───────────────────────────────────────────────────────

    /**
     * Uploads a batch of episode actions.
     * Returns the server timestamp to store for future incremental fetches.
     */
    fun uploadEpisodeActions(actions: List<EpisodeAction>): Long {
        val payload = json.encodeToString(actions).toRequestBody(jsonMime)
        val req = Request.Builder()
            .url("$serverUrl/api/2/episodes/$username.json")
            .post(payload)
            .build()
        val responseBody = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("uploadEpisodeActions HTTP ${resp.code}")
            resp.body?.string() ?: "{\"timestamp\":0}"
        }
        return json.decodeFromString<EpisodeActionsUploadResponse>(responseBody).timestamp
    }

    /**
     * Fetches episode actions recorded on the server since [sinceTimestamp] (epoch seconds).
     * Use 0 on first run to get the full history.
     */
    fun getEpisodeActions(sinceTimestamp: Long): EpisodeActionsResponse {
        val url = "$serverUrl/api/2/episodes/$username.json?since=$sinceTimestamp"
        val req = Request.Builder().url(url).get().build()
        val responseBody = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("getEpisodeActions HTTP ${resp.code}")
            resp.body?.string() ?: "{\"actions\":[],\"timestamp\":0}"
        }
        return json.decodeFromString(responseBody)
    }
}
