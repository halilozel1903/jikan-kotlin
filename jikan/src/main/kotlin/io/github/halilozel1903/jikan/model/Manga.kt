package io.github.halilozel1903.jikan.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A manga, light novel, manhwa or other print entry. `/manga/{id}/full` adds [relations] and [external]. */
@Serializable
public data class Manga(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    val images: Images = Images(),
    val approved: Boolean = true,
    val titles: List<Title> = emptyList(),
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_synonyms") val titleSynonyms: List<String> = emptyList(),
    val type: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val publishing: Boolean = false,
    val published: DateRange = DateRange(),
    val score: Double? = null,
    @SerialName("scored_by") val scoredBy: Int? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val favorites: Int? = null,
    val synopsis: String? = null,
    val background: String? = null,
    val authors: List<MalUrl> = emptyList(),
    val serializations: List<MalUrl> = emptyList(),
    val genres: List<MalUrl> = emptyList(),
    @SerialName("explicit_genres") val explicitGenres: List<MalUrl> = emptyList(),
    val themes: List<MalUrl> = emptyList(),
    val demographics: List<MalUrl> = emptyList(),
    val relations: List<Relation> = emptyList(),
    val external: List<NamedLink> = emptyList(),
) {
    /** The English title when there is one, otherwise the default title. */
    public val displayTitle: String get() = titleEnglish?.takeIf { it.isNotBlank() } ?: titles.ofType("English") ?: title
}

/** A character of `/manga/{id}/characters`. */
@Serializable
public data class MangaCharacter(
    val character: CharacterRef,
    val role: String = "",
)
