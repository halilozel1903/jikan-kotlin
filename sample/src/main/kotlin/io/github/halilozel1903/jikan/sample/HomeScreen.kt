package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.JikanFormat
import io.github.halilozel1903.jikan.model.Anime
import io.github.halilozel1903.jikan.tv.AnimeRail
import io.github.halilozel1903.jikan.tv.GenreChips
import io.github.halilozel1903.jikan.tv.JikanTvDefaults
import io.github.halilozel1903.jikan.tv.ScoreStarColor
import io.github.halilozel1903.jikan.tv.StarIcon

/** Width of the posters in the home rails: small enough that the hero and a whole rail fit on screen. */
private val HomeCardWidth = 96.dp

/** The hero for the current season's top show, then rails: top airing, this season, upcoming and two genres. */
@Composable
fun HomeScreen(
    repository: AnimeRepository,
    onOpenAnime: (Int) -> Unit,
    onOpenSeasons: () -> Unit,
    onUseDemo: (() -> Unit)?,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<Load<HomeData>>(Load.Loading, repository, attempt) {
        value = Load.Loading
        value = load(repository.isDemo) { repository.home() }
    }
    when (val current = state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(current.message, onRetry = { attempt++ }, onUseDemo = onUseDemo, onBack = null)
        is Load.Ready -> HomeContent(current.value, onOpenAnime, onOpenSeasons)
    }
}

@Composable
private fun HomeContent(data: HomeData, onOpenAnime: (Int) -> Unit, onOpenSeasons: () -> Unit) {
    val heroFocus = remember { FocusRequester() }
    // Focus the hero once; coming back from a detail screen keeps the scroll position instead.
    var focused by rememberSaveable { mutableStateOf(false) }
    if (!focused && data.hero != null) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            withFrameNanos { }
            runCatching { heroFocus.requestFocus() }
            focused = true
        }
    }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        val hero = data.hero
        if (hero != null) {
            item(key = "hero") {
                Hero(
                    anime = hero,
                    onMoreInfo = { onOpenAnime(hero.malId) },
                    onOpenSeasons = onOpenSeasons,
                    buttonModifier = Modifier.focusRequester(heroFocus),
                )
            }
        }
        rail("top-airing", "Top airing", "Ranked by score", data.topAiring, onOpenAnime)
        rail("this-season", "This season", data.hero?.seasonLabel, data.thisSeason, onOpenAnime)
        rail("upcoming", "Upcoming", data.upcoming.firstOrNull()?.seasonLabel, data.upcoming, onOpenAnime)
        for (genre in data.genres) {
            rail("genre-${genre.genreId}", genre.name, "Top rated", genre.anime, onOpenAnime)
        }
    }
}

private fun LazyListScope.rail(
    key: String,
    title: String,
    trailing: String?,
    anime: List<Anime>,
    onOpenAnime: (Int) -> Unit,
) {
    if (anime.isEmpty()) return
    item(key = key) {
        AnimeRail(
            title = title,
            anime = anime,
            onClick = { onOpenAnime(it.malId) },
            trailing = trailing,
            cardWidth = HomeCardWidth,
        ) { item ->
            AnimeArtwork(item.malId, item.images.large, item.displayTitle, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Hero(anime: Anime, onMoreInfo: () -> Unit, onOpenSeasons: () -> Unit, buttonModifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(284.dp),
    ) {
        AnimeArtwork(
            seed = anime.malId,
            imageUrl = anime.images.large,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.62f),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to colors.background,
                        0.38f to colors.background,
                        0.7f to colors.background.copy(alpha = 0.4f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.background)),
        )
        Column(
            Modifier
                .padding(start = JikanTvDefaults.HorizontalPadding, top = 18.dp)
                .width(560.dp),
        ) {
            Wordmark()
            Spacer(Modifier.height(12.dp))
            Text(
                text = listOfNotNull("#1 this season", anime.seasonLabel).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                maxLines = 1,
            )
            Text(
                text = anime.displayTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarIcon(color = ScoreStarColor, size = 14.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = JikanFormat.score(anime.score),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                anime.scoredBy?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${JikanFormat.compactCount(it)} users",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = JikanFormat.summary(anime),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            GenreChips(anime.allGenres, maxCount = 4)
            Spacer(Modifier.height(10.dp))
            Text(
                text = anime.synopsis.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PillButton(text = "More info", onClick = onMoreInfo, modifier = buttonModifier, primary = true)
                PillButton(text = "Browse seasons", onClick = onOpenSeasons)
            }
        }
    }
}

@Composable
private fun Wordmark() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "Jikan TV",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Anime data from MyAnimeList via the Jikan API",
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(bottom = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
