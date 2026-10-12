package io.github.halilozel1903.jikan

/** A season of the year, as used by `/seasons/{year}/{season}`. */
public enum class Season(public val apiValue: String, public val label: String) {
    Winter("winter", "Winter"),
    Spring("spring", "Spring"),
    Summer("summer", "Summer"),
    Fall("fall", "Fall"),
    ;

    public companion object {
        /** Parses `"fall"`, `"Fall"` or `"autumn"`, or returns `null`. */
        public fun fromApi(value: String?): Season? {
            val key = value?.trim()?.lowercase() ?: return null
            if (key == "autumn") return Fall
            return entries.firstOrNull { it.apiValue == key }
        }

        /** The anime season [month] (1 to 12) falls in: January to March is winter, and so on. */
        public fun ofMonth(month: Int): Season {
            require(month in 1..12) { "month must be 1 to 12, was $month" }
            return entries[(month - 1) / 3]
        }
    }
}

/** The `type` filter of anime search and the `filter` of season lists. */
public enum class AnimeType(public val apiValue: String) {
    Tv("tv"),
    Movie("movie"),
    Ova("ova"),
    Special("special"),
    Ona("ona"),
    Music("music"),
    Cm("cm"),
    Pv("pv"),
    TvSpecial("tv_special"),
}

/** The `status` filter of anime search. */
public enum class AnimeStatus(public val apiValue: String) {
    Airing("airing"),
    Complete("complete"),
    Upcoming("upcoming"),
}

/** The `rating` filter: MyAnimeList's age ratings. */
public enum class AnimeRating(public val apiValue: String) {
    /** All ages. */
    G("g"),

    /** Children. */
    Pg("pg"),

    /** Teens 13 or older. */
    Pg13("pg13"),

    /** 17+ (violence and profanity). */
    R17("r17"),

    /** Mild nudity. */
    R("r"),

    /** Hentai. Only returned with `sfw = false`. */
    Rx("rx"),
}

/** `order_by` of anime search. */
public enum class AnimeOrderBy(public val apiValue: String) {
    MalId("mal_id"),
    Title("title"),
    StartDate("start_date"),
    EndDate("end_date"),
    Episodes("episodes"),
    Score("score"),
    ScoredBy("scored_by"),
    Rank("rank"),
    Popularity("popularity"),
    Members("members"),
    Favorites("favorites"),
}

/** The `filter` of `/top/anime`. */
public enum class TopAnimeFilter(public val apiValue: String) {
    Airing("airing"),
    Upcoming("upcoming"),
    ByPopularity("bypopularity"),
    Favorite("favorite"),
}

/** The `type` filter of manga search and `/top/manga`. */
public enum class MangaType(public val apiValue: String) {
    Manga("manga"),
    Novel("novel"),
    LightNovel("lightnovel"),
    OneShot("oneshot"),
    Doujin("doujin"),
    Manhwa("manhwa"),
    Manhua("manhua"),
}

/** The `status` filter of manga search. */
public enum class MangaStatus(public val apiValue: String) {
    Publishing("publishing"),
    Complete("complete"),
    Hiatus("hiatus"),
    Discontinued("discontinued"),
    Upcoming("upcoming"),
}

/** `order_by` of manga search. */
public enum class MangaOrderBy(public val apiValue: String) {
    MalId("mal_id"),
    Title("title"),
    StartDate("start_date"),
    EndDate("end_date"),
    Chapters("chapters"),
    Volumes("volumes"),
    Score("score"),
    ScoredBy("scored_by"),
    Rank("rank"),
    Popularity("popularity"),
    Members("members"),
    Favorites("favorites"),
}

/** The `filter` of `/top/manga`. */
public enum class TopMangaFilter(public val apiValue: String) {
    Publishing("publishing"),
    Upcoming("upcoming"),
    ByPopularity("bypopularity"),
    Favorite("favorite"),
}

/** `sort` of search results. */
public enum class SortOrder(public val apiValue: String) {
    Ascending("asc"),
    Descending("desc"),
}

/** The `filter` of `/schedules`: a day of the week, or entries without a fixed day. */
public enum class ScheduleDay(public val apiValue: String) {
    Monday("monday"),
    Tuesday("tuesday"),
    Wednesday("wednesday"),
    Thursday("thursday"),
    Friday("friday"),
    Saturday("saturday"),
    Sunday("sunday"),
    Unknown("unknown"),
    Other("other"),
}

/** The `filter` of `/genres/anime` and `/genres/manga`. */
public enum class GenreFilter(public val apiValue: String) {
    Genres("genres"),
    ExplicitGenres("explicit_genres"),
    Themes("themes"),
    Demographics("demographics"),
}

/**
 * Parameters of `GET /anime` (search). Every field is optional; `null` leaves the parameter out.
 *
 * ```kotlin
 * AnimeSearchQuery(query = "space", type = AnimeType.Tv, genres = listOf(24), orderBy = AnimeOrderBy.Score, sort = SortOrder.Descending)
 * ```
 *
 * @property query the `q` text.
 * @property genres MyAnimeList genre ids to include (`genres=1,24`); [genresExclude] to exclude.
 * @property minScore and [maxScore] limit the score (0 to 10).
 * @property startDate and [endDate] are `yyyy-MM-dd`, `yyyy-MM` or `yyyy`.
 * @property sfw `true` filters out adult entries.
 * @property limit page size, 1 to 25.
 */
public data class AnimeSearchQuery(
    val query: String? = null,
    val type: AnimeType? = null,
    val status: AnimeStatus? = null,
    val rating: AnimeRating? = null,
    val genres: List<Int> = emptyList(),
    val genresExclude: List<Int> = emptyList(),
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val orderBy: AnimeOrderBy? = null,
    val sort: SortOrder? = null,
    val letter: String? = null,
    val producers: List<Int> = emptyList(),
    val startDate: String? = null,
    val endDate: String? = null,
    val sfw: Boolean? = null,
    val unapproved: Boolean? = null,
    val page: Int? = null,
    val limit: Int? = null,
) {
    internal fun parameters(): List<Pair<String, String>> = buildList<Pair<String, String>> {
        query?.takeIf { it.isNotBlank() }?.let { add("q" to it.trim()) }
        type?.let { add("type" to it.apiValue) }
        status?.let { add("status" to it.apiValue) }
        rating?.let { add("rating" to it.apiValue) }
        if (genres.isNotEmpty()) add("genres" to genres.joinToString(","))
        if (genresExclude.isNotEmpty()) add("genres_exclude" to genresExclude.joinToString(","))
        minScore?.let { add("min_score" to formatScore(it)) }
        maxScore?.let { add("max_score" to formatScore(it)) }
        orderBy?.let { add("order_by" to it.apiValue) }
        sort?.let { add("sort" to it.apiValue) }
        letter?.takeIf { it.isNotBlank() }?.let { add("letter" to it) }
        if (producers.isNotEmpty()) add("producers" to producers.joinToString(","))
        startDate?.let { add("start_date" to it) }
        endDate?.let { add("end_date" to it) }
        sfw?.let { add("sfw" to it.toString()) }
        if (unapproved == true) add("unapproved" to "true")
        page?.let { add("page" to checkPage(it).toString()) }
        limit?.let { add("limit" to checkLimit(it).toString()) }
    }
}

/**
 * Parameters of `GET /manga` (search). Every field is optional; `null` leaves the parameter out.
 *
 * @property magazines MyAnimeList magazine ids, such as Weekly Shounen Jump.
 */
public data class MangaSearchQuery(
    val query: String? = null,
    val type: MangaType? = null,
    val status: MangaStatus? = null,
    val genres: List<Int> = emptyList(),
    val genresExclude: List<Int> = emptyList(),
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val orderBy: MangaOrderBy? = null,
    val sort: SortOrder? = null,
    val letter: String? = null,
    val magazines: List<Int> = emptyList(),
    val startDate: String? = null,
    val endDate: String? = null,
    val sfw: Boolean? = null,
    val unapproved: Boolean? = null,
    val page: Int? = null,
    val limit: Int? = null,
) {
    internal fun parameters(): List<Pair<String, String>> = buildList<Pair<String, String>> {
        query?.takeIf { it.isNotBlank() }?.let { add("q" to it.trim()) }
        type?.let { add("type" to it.apiValue) }
        status?.let { add("status" to it.apiValue) }
        if (genres.isNotEmpty()) add("genres" to genres.joinToString(","))
        if (genresExclude.isNotEmpty()) add("genres_exclude" to genresExclude.joinToString(","))
        minScore?.let { add("min_score" to formatScore(it)) }
        maxScore?.let { add("max_score" to formatScore(it)) }
        orderBy?.let { add("order_by" to it.apiValue) }
        sort?.let { add("sort" to it.apiValue) }
        letter?.takeIf { it.isNotBlank() }?.let { add("letter" to it) }
        if (magazines.isNotEmpty()) add("magazines" to magazines.joinToString(","))
        startDate?.let { add("start_date" to it) }
        endDate?.let { add("end_date" to it) }
        sfw?.let { add("sfw" to it.toString()) }
        if (unapproved == true) add("unapproved" to "true")
        page?.let { add("page" to checkPage(it).toString()) }
        limit?.let { add("limit" to checkLimit(it).toString()) }
    }
}

internal fun formatScore(value: Double): String {
    require(value in 0.0..10.0) { "Scores are 0 to 10, was $value" }
    return if (value == Math.floor(value)) value.toInt().toString() else value.toString()
}

internal fun checkPage(page: Int): Int {
    require(page >= 1) { "Jikan pages start at 1, was $page" }
    return page
}

internal fun checkLimit(limit: Int): Int {
    require(limit in 1..JikanClient.MAX_PAGE_SIZE) { "limit must be 1 to ${JikanClient.MAX_PAGE_SIZE}, was $limit" }
    return limit
}
