package io.github.halilozel1903.jikan.model

import io.github.halilozel1903.jikan.Season
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An anime from `/anime/{id}`, search, top, seasons and schedules. `/anime/{id}/full` adds [relations], [theme],
 * [external] and [streaming]; they are empty elsewhere.
 *
 * Text fields keep MyAnimeList's wording: [type] is `"TV"`, [status] is `"Currently Airing"`, [rating] is
 * `"PG-13 - Teens 13 or older"`.
 */
@Serializable
public data class Anime(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    val images: Images = Images(),
    val trailer: Trailer? = null,
    val approved: Boolean = true,
    val titles: List<Title> = emptyList(),
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_synonyms") val titleSynonyms: List<String> = emptyList(),
    val type: String? = null,
    val source: String? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val airing: Boolean = false,
    val aired: DateRange = DateRange(),
    val duration: String? = null,
    val rating: String? = null,
    val score: Double? = null,
    @SerialName("scored_by") val scoredBy: Int? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val favorites: Int? = null,
    val synopsis: String? = null,
    val background: String? = null,
    val season: String? = null,
    val year: Int? = null,
    val broadcast: Broadcast? = null,
    val producers: List<MalUrl> = emptyList(),
    val licensors: List<MalUrl> = emptyList(),
    val studios: List<MalUrl> = emptyList(),
    val genres: List<MalUrl> = emptyList(),
    @SerialName("explicit_genres") val explicitGenres: List<MalUrl> = emptyList(),
    val themes: List<MalUrl> = emptyList(),
    val demographics: List<MalUrl> = emptyList(),
    val relations: List<Relation> = emptyList(),
    val theme: ThemeSongs? = null,
    val external: List<NamedLink> = emptyList(),
    val streaming: List<NamedLink> = emptyList(),
) {
    /** The English title when there is one, otherwise the default (romanized) title. */
    public val displayTitle: String get() = titleEnglish?.takeIf { it.isNotBlank() } ?: titles.ofType("English") ?: title

    /** [season] as a [Season], or `null`. */
    public val parsedSeason: Season? get() = Season.fromApi(season)

    /** `"Fall 2026"`, or `null` when the season is unknown. */
    public val seasonLabel: String? get() {
        val s = parsedSeason ?: return null
        return if (year != null) "${s.label} $year" else s.label
    }

    /** Genres, themes and demographics together, in that order. */
    public val allGenres: List<MalUrl> get() = genres + explicitGenres + themes + demographics
}

/** Opening and ending songs from `/anime/{id}/full`. */
@Serializable
public data class ThemeSongs(
    val openings: List<String> = emptyList(),
    val endings: List<String> = emptyList(),
)

/** One episode of `/anime/{id}/episodes`. [score] is the users' 1 to 5 episode rating. */
@Serializable
public data class Episode(
    @SerialName("mal_id") val malId: Int,
    val url: String? = null,
    val title: String = "",
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_romanji") val titleRomanji: String? = null,
    val aired: String? = null,
    val score: Double? = null,
    val filler: Boolean = false,
    val recap: Boolean = false,
    @SerialName("forum_url") val forumUrl: String? = null,
) {
    /** The air date, or `null`. */
    public val airedDate: java.time.LocalDate? get() = parseDate(aired)
}

/** One episode from `/anime/{id}/episodes/{episode}`, with its synopsis and duration in seconds. */
@Serializable
public data class EpisodeDetail(
    @SerialName("mal_id") val malId: Int,
    val url: String? = null,
    val title: String = "",
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_romanji") val titleRomanji: String? = null,
    val duration: Int? = null,
    val aired: String? = null,
    val filler: Boolean = false,
    val recap: Boolean = false,
    val synopsis: String? = null,
)

/** A character of `/anime/{id}/characters`, with role and voice actors. */
@Serializable
public data class AnimeCharacter(
    val character: CharacterRef,
    val role: String = "",
    val favorites: Int = 0,
    @SerialName("voice_actors") val voiceActors: List<VoiceActor> = emptyList(),
) {
    /** `true` for main characters. */
    public val isMain: Boolean get() = role.equals("Main", ignoreCase = true)

    /** The voice actor in [language] (`"Japanese"` by default), or `null`. */
    public fun voiceActor(language: String = "Japanese"): PersonRef? =
        voiceActors.firstOrNull { it.language.equals(language, ignoreCase = true) }?.person
}

/** A person voicing a character in [language]. */
@Serializable
public data class VoiceActor(
    val person: PersonRef,
    val language: String = "",
)

/** A staff member of `/anime/{id}/staff`, such as `["Director", "Storyboard"]`. */
@Serializable
public data class StaffMember(
    val person: PersonRef,
    val positions: List<String> = emptyList(),
)

/** `/anime/{id}/statistics`: list counts and the score histogram. */
@Serializable
public data class AnimeStatistics(
    val watching: Int = 0,
    val completed: Int = 0,
    @SerialName("on_hold") val onHold: Int = 0,
    val dropped: Int = 0,
    @SerialName("plan_to_watch") val planToWatch: Int = 0,
    val total: Int = 0,
    val scores: List<ScoreVotes> = emptyList(),
)
