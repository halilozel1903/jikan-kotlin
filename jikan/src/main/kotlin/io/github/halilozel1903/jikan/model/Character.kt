package io.github.halilozel1903.jikan.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A character from `/characters/{id}`. `/characters/{id}/full` adds [anime], [manga] and [voices]. */
@Serializable
public data class Character(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    val images: Images = Images(),
    val name: String = "",
    @SerialName("name_kanji") val nameKanji: String? = null,
    val nicknames: List<String> = emptyList(),
    val favorites: Int = 0,
    val about: String? = null,
    val anime: List<CharacterAnimeRole> = emptyList(),
    val manga: List<CharacterMangaRole> = emptyList(),
    val voices: List<CharacterVoice> = emptyList(),
)

/** A short reference to a character inside another response. */
@Serializable
public data class CharacterRef(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    val images: Images = Images(),
    val name: String = "",
)

/** A character's role in an anime. */
@Serializable
public data class CharacterAnimeRole(
    val role: String = "",
    val anime: EntryRef,
)

/** A character's role in a manga. */
@Serializable
public data class CharacterMangaRole(
    val role: String = "",
    val manga: EntryRef,
)

/** A person voicing the character. */
@Serializable
public data class CharacterVoice(
    val person: PersonRef,
    val language: String = "",
)

/** A person (voice actor, staff, author) from `/people/{id}`. */
@Serializable
public data class Person(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    @SerialName("website_url") val websiteUrl: String? = null,
    val images: Images = Images(),
    val name: String = "",
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    @SerialName("alternate_names") val alternateNames: List<String> = emptyList(),
    val birthday: String? = null,
    val favorites: Int = 0,
    val about: String? = null,
)

/** A short reference to a person inside another response. */
@Serializable
public data class PersonRef(
    @SerialName("mal_id") val malId: Int,
    val url: String = "",
    val images: Images = Images(),
    val name: String = "",
)
