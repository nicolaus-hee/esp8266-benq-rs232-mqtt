package de.nhe.podcastwear.network

import android.util.Xml
import de.nhe.podcastwear.data.db.entities.EpisodeEntity
import de.nhe.podcastwear.data.db.entities.PodcastEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ParsedFeed(
    val podcast: PodcastEntity,
    val episodes: List<EpisodeEntity>,
)

class FeedParser(private val http: OkHttpClient = defaultClient()) {

    companion object {
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        private val RSS_DATE_FORMATS = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
        )

        fun parseDate(value: String): Long {
            for (fmt in RSS_DATE_FORMATS) {
                runCatching { return fmt.parse(value.trim())?.time ?: 0L }
            }
            return 0L
        }
    }

    /**
     * Downloads and parses the RSS feed at [feedUrl].
     * Throws [IOException] on network errors.
     */
    fun parseFeed(feedUrl: String): ParsedFeed {
        val req = Request.Builder().url(feedUrl).get().build()
        val stream = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Feed HTTP ${resp.code}: $feedUrl")
            resp.body?.byteStream() ?: throw IOException("Empty body: $feedUrl")
        }
        return parse(feedUrl, stream)
    }

    private fun parse(feedUrl: String, stream: InputStream): ParsedFeed {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(stream, null)
        }

        var podcastTitle = ""
        var podcastDescription = ""
        var podcastImage: String? = null
        var podcastLink: String? = null
        val episodes = mutableListOf<EpisodeEntity>()

        var inChannel = false
        var inItem = false
        var episodeTitle = ""
        var episodeDescription: String? = null
        var episodePubDate = 0L
        var episodeMediaUrl: String? = null
        var episodeDurationMs = 0L
        var episodeMime = "audio/mpeg"
        var episodeGuid: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name

            when (event) {
                XmlPullParser.START_TAG -> when {
                    tag == "channel" -> inChannel = true
                    tag == "item" && inChannel -> inItem = true
                    !inItem && tag == "title" -> podcastTitle = parser.nextText()
                    !inItem && tag == "description" -> podcastDescription = parser.nextText()
                    !inItem && tag == "link" -> podcastLink = parser.nextText()
                    !inItem && tag == "image" -> {
                        // <image><url>...</url></image>
                    }
                    !inItem && tag == "url" -> if (podcastImage == null) podcastImage = parser.nextText()
                    inItem && tag == "title" -> episodeTitle = parser.nextText()
                    inItem && tag == "description" -> episodeDescription = parser.nextText()
                    inItem && tag == "pubDate" -> episodePubDate = parseDate(parser.nextText())
                    inItem && tag == "guid" -> episodeGuid = parser.nextText()
                    inItem && (tag == "duration" || tag == "itunes:duration") -> {
                        val raw = parser.nextText().trim()
                        episodeDurationMs = parseDuration(raw)
                    }
                    inItem && tag == "enclosure" -> {
                        episodeMediaUrl = parser.getAttributeValue(null, "url")
                        episodeMime = parser.getAttributeValue(null, "type") ?: "audio/mpeg"
                    }
                    // itunes:image for podcast artwork
                    !inItem && tag == "itunes:image" -> {
                        val href = parser.getAttributeValue(null, "href")
                        if (href != null) podcastImage = href
                    }
                }

                XmlPullParser.END_TAG -> when {
                    tag == "item" && inItem -> {
                        val url = episodeMediaUrl
                        if (url != null && episodeTitle.isNotBlank()) {
                            episodes.add(
                                EpisodeEntity(
                                    mediaUrl = url,
                                    podcastFeedUrl = feedUrl,
                                    title = episodeTitle,
                                    description = episodeDescription,
                                    pubDate = episodePubDate,
                                    durationMs = episodeDurationMs,
                                    mimeType = episodeMime,
                                    guid = episodeGuid,
                                )
                            )
                        }
                        // Reset episode state
                        episodeTitle = ""; episodeDescription = null; episodePubDate = 0L
                        episodeMediaUrl = null; episodeDurationMs = 0L; episodeMime = "audio/mpeg"
                        episodeGuid = null; inItem = false
                    }
                    tag == "channel" -> inChannel = false
                }
            }
            event = parser.next()
        }

        val podcast = PodcastEntity(
            feedUrl = feedUrl,
            title = podcastTitle,
            description = podcastDescription,
            imageUrl = podcastImage,
            link = podcastLink,
        )
        return ParsedFeed(podcast, episodes)
    }

    private fun parseDuration(raw: String): Long {
        // Formats: HH:MM:SS, MM:SS, or plain seconds
        return if (raw.contains(':')) {
            val parts = raw.split(':').map { it.toLongOrNull() ?: 0L }
            when (parts.size) {
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                2 -> (parts[0] * 60 + parts[1]) * 1000
                else -> 0L
            }
        } else {
            (raw.toLongOrNull() ?: 0L) * 1000
        }
    }
}
