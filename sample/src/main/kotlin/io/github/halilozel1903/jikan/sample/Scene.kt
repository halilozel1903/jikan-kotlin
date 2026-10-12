package io.github.halilozel1903.jikan.sample

import io.github.halilozel1903.jikan.Season

/** Screenshot scenes, picked with the `scene` intent extra. They load live data from the Jikan API. */
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
        /** Fullmetal Alchemist: Brotherhood, a finished show whose page does not change from week to week. */
        const val DETAIL_ID = 5114

        /** A past season, so the grid stays the same between runs. */
        const val SEASON_YEAR = 2024
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
