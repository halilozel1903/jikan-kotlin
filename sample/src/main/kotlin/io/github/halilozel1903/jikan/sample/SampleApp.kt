package io.github.halilozel1903.jikan.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * The app: a small back stack of [Screen]s. Scenes always use the bundled demo data; a normal launch uses the live
 * API and can switch to the demo data from the error screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SampleApp(
    scene: Scene?,
    liveRepository: () -> AnimeRepository,
    demoRepository: () -> AnimeRepository,
) {
    var demo by remember { mutableStateOf(scene != null) }
    val repository = remember(demo) { if (demo) demoRepository() else liveRepository() }
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
        LocalDemoArtwork provides demo,
        LocalBringIntoViewSpec provides bringIntoView,
    ) {
        stateHolder.SaveableStateProvider(screen.toString()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    repository = repository,
                    onOpenAnime = { open(Screen.Detail(it)) },
                    onOpenSeasons = {
                        val (year, season) = AnimeRepository.currentSeason()
                        open(Screen.SeasonBrowser(if (demo) Scene.SEASON_YEAR else year, if (demo) Scene.SEASON else season))
                    },
                    onUseDemo = if (demo) null else ({ demo = true }),
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
