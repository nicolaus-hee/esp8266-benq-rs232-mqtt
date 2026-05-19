package de.nhe.podcastwear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import de.nhe.podcastwear.data.prefs.UserPreferences
import de.nhe.podcastwear.ui.navigation.AppNavigation
import de.nhe.podcastwear.ui.navigation.Routes
import de.nhe.podcastwear.ui.theme.PodcastWearTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val cfg = UserPreferences(this@MainActivity).gpodderConfig.first()
            val start = if (cfg.isConfigured) Routes.HOME else Routes.ONBOARDING

            setContent {
                PodcastWearTheme {
                    AppNavigation(startDestination = start)
                }
            }
        }
    }
}
