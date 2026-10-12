package io.github.halilozel1903.jikan.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * The app: a small back stack of [Screen]s, all loaded from the live Jikan API. A screenshot [scene] opens a fixed
 * back stack and reports to a [SceneMonitor] when its data and images have loaded.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SampleApp(scene: Scene?, repository: AnimeRepository) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()
    val stack = remember {
        mutableStateListOf<Screen>().apply { addAll(scene?.backStack() ?: listOf(Screen.Home)) }
    }
    val stateHolder = rememberSaveableStateHolder()
    val marginPx = with(LocalDensity.current) { 48.dp.toPx() }
    val bringIntoView = remember(marginPx) { MinimalBringIntoViewSpec(marginPx) }

    val open: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    val screen = stack.last()
    CompositionLocalProvider(
        LocalSceneMonitor provides monitor,
        LocalBringIntoViewSpec provides bringIntoView,
    ) {
        stateHolder.SaveableStateProvider(screen.toString()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    repository = repository,
                    onOpenAnime = { open(Screen.Detail(it)) },
                    onOpenSeasons = {
                        val (year, season) = AnimeRepository.currentSeason()
                        open(Screen.SeasonBrowser(year, season))
                    },
                )
                is Screen.Detail -> DetailScreen(
                    id = screen.id,
                    repository = repository,
                    onOpenAnime = { open(Screen.Detail(it)) },
                    onBack = back,
                )
                is Screen.SeasonBrowser -> SeasonScreen(
                    year = screen.year,
                    season = screen.season,
                    repository = repository,
                    onOpenAnime = { open(Screen.Detail(it)) },
                    onSelectSeason = { year, season ->
                        // Switching seasons replaces this screen instead of growing the back stack.
                        stack[stack.lastIndex] = Screen.SeasonBrowser(year, season)
                    },
                    onBack = back,
                )
            }
        }
    }
}
