package io.github.halilozel1903.jikan.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** `{"data": ...}`: the envelope of every single resource response. */
@Serializable
public data class JikanResponse<T>(
    val data: T,
)

/**
 * `{"data": [...], "pagination": {...}}`: one page of a list response.
 *
 * Jikan pages start at 1. [Pagination.hasNextPage] tells whether another page follows.
 */
@Serializable
public data class JikanPage<T>(
    val data: List<T> = emptyList(),
    val pagination: Pagination = Pagination(),
) {
    /** The current page number, or 1 when the endpoint does not report it. */
    public val page: Int get() = pagination.currentPage ?: 1

    /** `true` when another page follows. */
    public val hasNextPage: Boolean get() = pagination.hasNextPage
}

/** Jikan's `pagination` object. Some endpoints (episodes, reviews) only send [lastVisiblePage] and [hasNextPage]. */
@Serializable
public data class Pagination(
    @SerialName("last_visible_page") val lastVisiblePage: Int = 1,
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
    @SerialName("current_page") val currentPage: Int? = null,
    val items: PaginationItems? = null,
)

/** `pagination.items`: how many results this page has, how many there are in total and the page size. */
@Serializable
public data class PaginationItems(
    val count: Int = 0,
    val total: Int = 0,
    @SerialName("per_page") val perPage: Int = 0,
)

/** An image in one format. Characters and people only have [imageUrl]; anime and manga have all three sizes. */
@Serializable
public data class ImageUrls(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_image_url") val smallImageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
    @SerialName("maximum_image_url") val maximumImageUrl: String? = null,
)

/** Jikan's `images` object: the same picture as JPEG and WebP. */
@Serializable
public data class Images(
    val jpg: ImageUrls = ImageUrls(),
    val webp: ImageUrls = ImageUrls(),
) {
    /** The largest available url, preferring WebP. */
    public val large: String?
        get() = webp.largeImageUrl ?: jpg.largeImageUrl ?: webp.imageUrl ?: jpg.imageUrl

    /** The default size, preferring WebP. */
    public val medium: String?
        get() = webp.imageUrl ?: jpg.imageUrl ?: large

    /** The smallest available url, preferring WebP. */
    public val small: String?
        get() = webp.smallImageUrl ?: jpg.smallImageUrl ?: medium
}

/** A link to another MyAnimeList entry: genres, studios, producers, authors, magazines. */
@Serializable
public data class MalUrl(
    @SerialName("mal_id") val malId: Int,
    val type: String = "",
    val name: String = "",
    val url: String = "",
)

/** One entry of `titles`: `{"type": "Default" | "Synonym" | "Japanese" | "English" | ..., "title": "..."}`. */
@Serializable
public data class Title(
    val type: String,
    val title: String,
)

/** The title of [type] in [this], such as `"English"` or `"Japanese"`. */
public fun List<Title>.ofType(type: String): String? = firstOrNull { it.type.equals(type, ignoreCase = true) }?.title

/** `aired` and `published`: ISO 8601 [from] and [to] instants plus MyAnimeList's own text, such as `"Oct 2, 2026 to ?"`. */
@Serializable
public data class DateRange(
    val from: String? = null,
    val to: String? = null,
    val prop: DateRangeProp? = null,
    val string: String? = null,
) {
    /** The start date, or `null` when unknown. */
    public val fromDate: LocalDate? get() = parseDate(from)

    /** The end date, or `null` while running or when unknown. */
    public val toDate: LocalDate? get() = parseDate(to)
}

/** `prop`: the parts of a [DateRange] that are known; MyAnimeList often only knows the year or month. */
@Serializable
public data class DateRangeProp(
    val from: DateParts = DateParts(),
    val to: DateParts = DateParts(),
)

/** A partial date. */
@Serializable
public data class DateParts(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
)

/** Parses a Jikan timestamp (`2026-10-02T00:00:00+00:00`) to a date, or returns `null`. */
internal fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).toLocalDate()
    } catch (e: DateTimeParseException) {
        try {
            LocalDate.parse(value.take(10))
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

/** An anime's weekly time slot in Japan. */
@Serializable
public data class Broadcast(
    val day: String? = null,
    val time: String? = null,
    val timezone: String? = null,
    val string: String? = null,
)

/** A YouTube trailer. */
@Serializable
public data class Trailer(
    @SerialName("youtube_id") val youtubeId: String? = null,
    val url: String? = null,
    @SerialName("embed_url") val embedUrl: String? = null,
    val images: TrailerImages? = null,
)

/** Thumbnails of a [Trailer]. */
@Serializable
public data class TrailerImages(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_image_url") val smallImageUrl: String? = null,
    @SerialName("medium_image_url") val mediumImageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
    @SerialName("maximum_image_url") val maximumImageUrl: String? = null,
)

/** A related entry group in `/full` responses, such as `{"relation": "Sequel", "entry": [...]}`. */
@Serializable
public data class Relation(
    val relation: String,
    val entry: List<MalUrl> = emptyList(),
)

/** A named link: `external` and `streaming` in `/full` responses. */
@Serializable
public data class NamedLink(
    val name: String,
    val url: String,
)

/** An entry that a recommendation, character role or person credit points to. */
@Serializable
public data class EntryRef(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    val images: Images = Images(),
    val title: String = "",
)

/** A recommendation: `{"entry": {...}, "url": "...", "votes": 12}`. */
@Serializable
public data class Recommendation(
    val entry: EntryRef,
    val url: String = "",
    val votes: Int = 0,
)

/** One picture of `/anime/{id}/pictures` or `/manga/{id}/pictures`. */
@Serializable
public data class Picture(
    val jpg: ImageUrls = ImageUrls(),
    val webp: ImageUrls = ImageUrls(),
)

/** A genre, theme or demographic from `/genres/anime` or `/genres/manga`, with the number of entries. */
@Serializable
public data class Genre(
    @SerialName("mal_id") val malId: Int,
    val name: String,
    val url: String = "",
    val count: Int = 0,
)

/** One year of `/seasons`: `{"year": 2026, "seasons": ["winter", "spring", "summer", "fall"]}`. */
@Serializable
public data class SeasonArchive(
    val year: Int,
    val seasons: List<String> = emptyList(),
) {
    /** [seasons] as [io.github.halilozel1903.jikan.Season] values, unknown names skipped. */
    public val parsedSeasons: List<io.github.halilozel1903.jikan.Season>
        get() = seasons.mapNotNull { io.github.halilozel1903.jikan.Season.fromApi(it) }
}

/** One bar of the score histogram in `/statistics`. */
@Serializable
public data class ScoreVotes(
    val score: Int,
    val votes: Int = 0,
    val percentage: Double = 0.0,
)
