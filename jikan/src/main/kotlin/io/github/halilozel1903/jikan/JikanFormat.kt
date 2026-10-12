package io.github.halilozel1903.jikan

import io.github.halilozel1903.jikan.model.Anime
import io.github.halilozel1903.jikan.model.Character
import io.github.halilozel1903.jikan.model.CharacterRef
import io.github.halilozel1903.jikan.model.Person
import io.github.halilozel1903.jikan.model.PersonRef
import java.util.Locale

/** Small formatting helpers for showing Jikan data in a UI. */
public object JikanFormat {

    /** MyAnimeList writes names as `"Family, Given"`; this returns `"Given Family"`. Other names are returned as is. */
    public fun naturalName(name: String): String {
        val parts = name.split(", ")
        return if (parts.size == 2 && parts.all { it.isNotBlank() }) "${parts[1]} ${parts[0]}" else name
    }

    /** `8.91`, or `"N/A"` without a score, like MyAnimeList. */
    public fun score(score: Double?): String = score?.let { String.format(Locale.US, "%.2f", it) } ?: "N/A"

    /** `950`, `12.4K`, `212K`, `1.2M`. */
    public fun compactCount(count: Int): String = when {
        count < 1_000 -> count.toString()
        count < 100_000 -> trim(count / 1_000.0) + "K"
        count < 1_000_000 -> (count / 1_000).toString() + "K"
        else -> trim(count / 1_000_000.0) + "M"
    }

    /** `"12 eps"`, `"1 ep"`, or `null` when the number of episodes is unknown. */
    public fun episodes(count: Int?): String? = when (count) {
        null, 0 -> null
        1 -> "1 ep"
        else -> "$count eps"
    }

    /**
     * One line of facts, such as `"TV · 12 eps · Fall 2026 · Studio Lumen"`. Unknown parts are left out.
     */
    public fun summary(anime: Anime, separator: String = " · "): String = listOfNotNull(
        anime.type?.takeIf { it.isNotBlank() },
        episodes(anime.episodes),
        anime.seasonLabel ?: anime.year?.toString(),
        anime.studios.firstOrNull()?.name,
    ).joinToString(separator)

    private fun trim(value: Double): String {
        val text = String.format(Locale.US, "%.1f", value)
        return text.removeSuffix(".0")
    }
}

/** The name as `"Given Family"`. */
public val CharacterRef.displayName: String get() = JikanFormat.naturalName(name)

/** The name as `"Given Family"`. */
public val Character.displayName: String get() = JikanFormat.naturalName(name)

/** The name as `"Given Family"`. */
public val PersonRef.displayName: String get() = JikanFormat.naturalName(name)

/** The name as `"Given Family"`. */
public val Person.displayName: String get() = JikanFormat.naturalName(name)
