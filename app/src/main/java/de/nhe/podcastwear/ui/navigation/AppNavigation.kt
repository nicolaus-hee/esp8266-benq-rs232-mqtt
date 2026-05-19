package de.nhe.podcastwear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.nhe.podcastwear.ui.screens.*
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val SUBSCRIPTIONS = "subscriptions"
    const val PODCAST_DETAIL = "podcast/{feedUrl}"
    const val EPISODE_DETAIL = "episode/{episodeUrl}"
    const val PLAYER = "player"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"

    fun podcastDetail(feedUrl: String) = "podcast/${URLEncoder.encode(feedUrl, "UTF-8")}"
    fun episodeDetail(episodeUrl: String) = "episode/${URLEncoder.encode(episodeUrl, "UTF-8")}"
}

@Composable
fun AppNavigation(startDestination: String = Routes.HOME) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                onOpenSubscriptions = { navController.navigate(Routes.SUBSCRIPTIONS) },
                onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SUBSCRIPTIONS) {
            SubscriptionsScreen(
                onPodcastClick = { feedUrl -> navController.navigate(Routes.podcastDetail(feedUrl)) },
            )
        }
        composable(
            route = Routes.PODCAST_DETAIL,
            arguments = listOf(navArgument("feedUrl") { type = NavType.StringType }),
        ) { backStack ->
            val feedUrl = URLDecoder.decode(backStack.arguments?.getString("feedUrl") ?: "", "UTF-8")
            PodcastDetailScreen(
                feedUrl = feedUrl,
                onEpisodeClick = { url -> navController.navigate(Routes.episodeDetail(url)) },
            )
        }
        composable(
            route = Routes.EPISODE_DETAIL,
            arguments = listOf(navArgument("episodeUrl") { type = NavType.StringType }),
        ) { backStack ->
            val episodeUrl = URLDecoder.decode(backStack.arguments?.getString("episodeUrl") ?: "", "UTF-8")
            EpisodeDetailScreen(
                episodeUrl = episodeUrl,
                onPlayStream = { navController.navigate(Routes.PLAYER) },
                onPlayDownload = { navController.navigate(Routes.PLAYER) },
            )
        }
        composable(Routes.PLAYER) {
            PlayerScreen(onDismiss = { navController.popBackStack() })
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(
                onEpisodeClick = { url -> navController.navigate(Routes.episodeDetail(url)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onSaved = { navController.popBackStack() })
        }
        composable(Routes.ONBOARDING) {
            SettingsScreen(onSaved = { navController.navigate(Routes.HOME) })
        }
    }
}
