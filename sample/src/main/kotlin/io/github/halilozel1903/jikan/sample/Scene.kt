package io.github.halilozel1903.jikan.sample

import io.github.halilozel1903.jikan.Season

/** Screenshot scenes, picked with the `scene` intent extra. They use the bundled fixtures, never the network. */
enum class Scene(val key: String) {
    Home("home"),
    Detail("detail"),
    Seasons("season"),
    ;

    /** The screens to open, bottom of the back stack first. */
    fun backStack(): List<Screen> = when (this) {
        Home -> listOf(Screen.Home)
        Detail -> listOf(Screen.Home, Screen.Detail(DETAIL_ID))
        Seasons -> listOf(Screen.Home, Screen.SeasonBrowser(SEASON_YEAR, SEASON))
    }

    companion object {
        /** "Harbor of a Thousand Bells", the fixture with episodes, characters and recommendations. */
        const val DETAIL_ID = 60201
        const val SEASON_YEAR = 2026
        val SEASON = Season.Summer

        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

/** One screen of the app's back stack. */
sealed interface Screen {
    data object Home : Screen

    data class Detail(val id: Int) : Screen

    data class SeasonBrowser(val year: Int, val season: Season) : Screen
}
