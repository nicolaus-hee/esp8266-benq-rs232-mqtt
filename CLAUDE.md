# AntennaPod-WearOS

Standalone Wear OS podcast app for Pixel Watch 4 (LTE). No phone companion required.

## Project overview

- **Package**: `de.nhe.podcastwear`
- **Min SDK**: 30 (Wear OS 3)
- **Language**: Kotlin + Jetpack Compose for Wear OS (Material3)
- **Sync backend**: gpodder / opodsync REST API v2

## Architecture

```
app/src/main/java/de/nhe/podcastwear/
├── MainActivity.kt            # Entry point; routes to Onboarding or Home
├── PodcastWearApp.kt          # Application class, WorkManager config
├── data/
│   ├── db/                    # Room database (AppDatabase, DAOs, Entities)
│   ├── prefs/UserPreferences  # DataStore: server URL, credentials, sync state
│   └── repository/SyncRepository  # Full gpodder sync cycle
├── network/
│   ├── GpodderClient.kt       # gpodder REST API v2 client (OkHttp)
│   └── FeedParser.kt          # RSS/Atom parser (XmlPullParser)
├── playback/PlaybackService   # Media3 MediaSessionService + ExoPlayer
├── sync/
│   ├── SyncWorker.kt          # WorkManager periodic sync job
│   └── DownloadWorker.kt      # Episode download worker (WiFi/LTE constraint)
└── ui/
    ├── navigation/AppNavigation.kt
    ├── screens/               # Home, Subscriptions, PodcastDetail, EpisodeDetail,
    │                          # Player, Downloads, Settings/Onboarding
    └── theme/Theme.kt
```

## Key dependencies

- `androidx.wear.compose:compose-material3` – WearOS UI
- `androidx.media3:media3-exoplayer` + `media3-session` – Playback
- `androidx.room` – Local database
- `androidx.datastore` – Preferences (credentials stored only on device, never in git)
- `androidx.work:work-runtime-ktx` – Background sync & downloads
- `com.squareup.okhttp3:okhttp` – HTTP (gpodder API + feed downloads)
- `kotlinx.serialization` – JSON parsing for gpodder API responses

## Sync flow

1. `SyncWorker` triggers `SyncRepository.sync()`
2. Upload pending `EpisodeActionEntity` records → gpodder server
3. Fetch subscription changes since last sync → update Room DB
4. Refresh RSS feeds for all subscriptions → upsert new episodes
5. Fetch episode actions from server → apply played/position states locally
6. Update `lastSyncTimestamp` in DataStore

## CI/CD

GitHub Actions (`.github/workflows/wearos-build.yml`):
- Push to `main` → build debug + signed release APK
- Release APK uploaded as GitHub Release artifact
- Signing via GitHub Secrets: `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`

## What's NOT in the repo (by design)

- `*.jks` / `*.keystore` – Signing keystore (GitHub Secret, never commit)
- `keystore.properties` – Local signing config (gitignored)
- `local.properties` – SDK paths (gitignored)
- gpodder credentials – Stored only in device EncryptedSharedPreferences at runtime

## TODO / next steps

- [ ] Gradle wrapper binary (`gradlew` script + `gradle-wrapper.jar`) – run `gradle wrapper` locally
- [ ] Replace placeholder icon with official AntennaPod icon
- [ ] Google Play Developer account + Play Store listing
- [ ] Play Store automated upload via Gradle Play Publisher
- [ ] Unit tests for GpodderClient sync logic
- [ ] Download progress indicator in UI
- [ ] Watch face complication (currently playing episode)
