package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.JikanFormat
import io.github.halilozel1903.jikan.displayName
import io.github.halilozel1903.jikan.model.Anime
import io.github.halilozel1903.jikan.model.AnimeCharacter
import io.github.halilozel1903.jikan.model.Episode
import io.github.halilozel1903.jikan.tv.GenreChips
import io.github.halilozel1903.jikan.tv.JikanTvDefaults
import io.github.halilozel1903.jikan.tv.PosterCard
import io.github.halilozel1903.jikan.tv.PosterRail
import io.github.halilozel1903.jikan.tv.RailTitle
import io.github.halilozel1903.jikan.tv.ScoreStarColor
import io.github.halilozel1903.jikan.tv.StarIcon

/** One anime: synopsis, score and ranks, then episodes, characters with voice actors, recommendations and themes. */
@Composable
fun DetailScreen(
    id: Int,
    repository: AnimeRepository,
    onOpenAnime: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<Load<DetailData>>(Load.Loading, id, repository, attempt) {
        value = Load.Loading
        value = load(repository.isDemo) { repository.detail(id) }
    }
    when (val current = state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(current.message, onRetry = { attempt++ }, onUseDemo = null, onBack = onBack)
        is Load.Ready -> DetailContent(current.value, onOpenAnime)
    }
}

private enum class FirstFocus { Episodes, Characters, Recommendations }

@Composable
private fun DetailContent(data: DetailData, onOpenAnime: (Int) -> Unit) {
    val anime = data.anime
    val firstFocus = remember { FocusRequester() }
    val focusTarget = when {
        data.episodes.isNotEmpty() -> FirstFocus.Episodes
        data.characters.isNotEmpty() -> FirstFocus.Characters
        data.recommendations.isNotEmpty() -> FirstFocus.Recommendations
        else -> null
    }
    var focused by rememberSaveable { mutableStateOf(false) }
    if (!focused && focusTarget != null) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            withFrameNanos { }
            runCatching { firstFocus.requestFocus() }
            focused = true
        }
    }
    val horizontal = JikanTvDefaults.HorizontalPadding
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { Header(anime) }
        if (data.episodes.isNotEmpty()) {
            item(key = "episodes") {
                Column {
                    val total = anime.episodes
                    RailTitle(
                        text = "Episodes",
                        trailing = listOfNotNull(
                            if (total != null && total > data.episodes.size) "${data.episodes.size} of $total aired" else "${data.episodes.size} episodes",
                            anime.broadcast?.string,
                        ).joinToString(" · "),
                        modifier = Modifier.padding(horizontal = horizontal),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontal, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(data.episodes, key = { _, episode -> episode.malId }) { index, episode ->
                            EpisodeCard(
                                episode = episode,
                                modifier = if (index == 0 && focusTarget == FirstFocus.Episodes) Modifier.focusRequester(firstFocus) else Modifier,
                            )
                        }
                    }
                }
            }
        }
        if (data.characters.isNotEmpty()) {
            item(key = "characters") {
                Column {
                    RailTitle(
                        text = "Characters & voice actors",
                        trailing = "${data.characters.size} characters",
                        modifier = Modifier.padding(horizontal = horizontal),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = horizontal, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(data.characters, key = { _, c -> c.character.malId }) { index, character ->
                            CharacterCard(
                                character = character,
                                modifier = if (index == 0 && focusTarget == FirstFocus.Characters) Modifier.focusRequester(firstFocus) else Modifier,
                            )
                        }
                    }
                }
            }
        }
        if (data.recommendations.isNotEmpty()) {
            item(key = "recommendations") {
                PosterRail(
                    title = "If you like this",
                    items = data.recommendations,
                    key = { it.entry.malId },
                    trailing = "Recommended by MyAnimeList users",
                ) { recommendation ->
                    val entry = recommendation.entry
                    val first = recommendation == data.recommendations.first() && focusTarget == FirstFocus.Recommendations
                    PosterCard(
                        title = entry.title,
                        onClick = { onOpenAnime(entry.malId) },
                        modifier = if (first) Modifier.focusRequester(firstFocus) else Modifier,
                        subtitle = "${recommendation.votes} votes",
                        width = 96.dp,
                    ) {
                        AnimeArtwork(entry.malId, entry.images.large, entry.title, Modifier.fillMaxSize())
                    }
                }
            }
        }
        val theme = anime.theme
        if (theme != null && (theme.openings.isNotEmpty() || theme.endings.isNotEmpty())) {
            item(key = "themes") {
                Column(Modifier.padding(horizontal = horizontal), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RailTitle(text = "Theme songs")
                    for (song in theme.openings) ThemeLine("OP", song)
                    for (song in theme.endings) ThemeLine("ED", song)
                }
            }
        }
    }
}

@Composable
private fun Header(anime: Anime) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize()) {
            AnimeArtwork(
                seed = anime.malId,
                imageUrl = anime.images.large,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to colors.background,
                            0.5f to colors.background,
                            0.8f to colors.background.copy(alpha = 0.55f),
                            1f to colors.background.copy(alpha = 0.2f),
                        ),
                    ),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to colors.background)),
            )
        }
        Row(
            Modifier.padding(start = JikanTvDefaults.HorizontalPadding, end = JikanTvDefaults.HorizontalPadding, top = 28.dp),
        ) {
            AnimeArtwork(
                seed = anime.malId,
                imageUrl = anime.images.large,
                contentDescription = anime.displayTitle,
                modifier = Modifier
                    .width(150.dp)
                    .aspectRatio(JikanTvDefaults.PosterAspectRatio)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(anime.type, anime.status, anime.rating?.substringBefore(" - ")).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = anime.displayTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val otherTitles = listOfNotNull(anime.title.takeIf { it != anime.displayTitle }, anime.titleJapanese)
                if (otherTitles.isNotEmpty()) {
                    Text(
                        text = otherTitles.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Stats(anime)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = JikanFormat.summary(anime),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                GenreChips(anime.allGenres, maxCount = 5)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = anime.synopsis ?: "No synopsis yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Score with the number of votes, then rank, popularity and members. */
@Composable
private fun Stats(anime: Anime) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        StarIcon(color = ScoreStarColor, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = JikanFormat.score(anime.score),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        anime.scoredBy?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${JikanFormat.compactCount(it)} users",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(20.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            anime.rank?.let { StatPill("Ranked #$it") }
            anime.popularity?.let { StatPill("Popularity #$it") }
            anime.members?.let { StatPill("${JikanFormat.compactCount(it)} members") }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
    )
}

/** A focusable card for one episode: number, recap or filler tag, title and air date. */
@Composable
private fun EpisodeCard(episode: Episode, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = {},
        modifier = modifier
            .width(224.dp)
            .height(86.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.onSurface,
            focusedContentColor = colors.background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "EP ${episode.malId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )
                val tag = when {
                    episode.recap -> "Recap"
                    episode.filler -> "Filler"
                    else -> null
                }
                if (tag != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondary,
                    )
                }
                Spacer(Modifier.weight(1f))
                formatAired(episode.aired)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A focusable card for a character: portrait, name, role and Japanese voice actor. */
@Composable
private fun CharacterCard(character: AnimeCharacter, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = {},
        modifier = modifier.width(236.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.onSurface,
            focusedContentColor = colors.background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val ref = character.character
            Avatar(seed = ref.malId, name = ref.name, imageUrl = ref.images.jpg.imageUrl, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ref.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = character.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (character.isMain) colors.primary else colors.secondary,
                    maxLines = 1,
                )
                character.voiceActor()?.let { actor ->
                    Text(
                        text = "CV: ${actor.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeLine(kind: String, song: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = kind,
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = song,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * `"2026-07-03T00:00:00+00:00"` as `"Jul 3, 2026"`. Parsed by hand: java.time needs API 26 (or desugaring) and the
 * sample runs from API 24.
 */
fun formatAired(value: String?): String? {
    if (value == null || value.length < 10) return null
    val year = value.substring(0, 4).toIntOrNull() ?: return null
    val month = value.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = value.substring(8, 10).toIntOrNull() ?: return null
    return "${Months[month - 1]} $day, $year"
}
