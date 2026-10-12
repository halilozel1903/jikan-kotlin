package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.JikanFormat
import io.github.halilozel1903.jikan.Season
import io.github.halilozel1903.jikan.model.SeasonArchive
import io.github.halilozel1903.jikan.tv.AnimePosterCard
import io.github.halilozel1903.jikan.tv.JikanTvDefaults

/** A season's anime in a grid, with a row of neighbouring seasons to switch to. */
@Composable
fun SeasonScreen(
    year: Int,
    season: Season,
    repository: AnimeRepository,
    onOpenAnime: (Int) -> Unit,
    onSelectSeason: (year: Int, season: Season) -> Unit,
    onBack: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<Load<SeasonData>>(Load.Loading, year, season, repository, attempt) {
        value = Load.Loading
        value = load(repository.isDemo) { repository.season(year, season) }
    }
    when (val current = state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(current.message, onRetry = { attempt++ }, onUseDemo = null, onBack = onBack)
        is Load.Ready -> SeasonContent(current.value, onOpenAnime, onSelectSeason)
    }
}

/** One choice of the season picker. */
data class SeasonChoice(val year: Int, val season: Season) {
    val label: String get() = "${season.label} $year"
}

/**
 * Up to [count] seasons around [year] and [season] from Jikan's season archive, newest first. The current choice is
 * always included, even when the archive is missing.
 */
fun seasonChoices(archive: List<SeasonArchive>, year: Int, season: Season, count: Int = 8): List<SeasonChoice> {
    val all = buildList<SeasonChoice> {
        for (entry in archive.sortedByDescending { it.year }) {
            for (s in entry.parsedSeasons.sortedByDescending { it.ordinal }) add(SeasonChoice(entry.year, s))
        }
    }
    val current = SeasonChoice(year, season)
    val index = all.indexOf(current)
    if (index < 0) return listOf(current)
    val start = (index - 2).coerceIn(0, (all.size - count).coerceAtLeast(0))
    return all.subList(start, minOf(all.size, start + count))
}

@Composable
private fun SeasonContent(
    data: SeasonData,
    onOpenAnime: (Int) -> Unit,
    onSelectSeason: (year: Int, season: Season) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val horizontal = JikanTvDefaults.HorizontalPadding
    val firstCard = remember { FocusRequester() }
    var focused by rememberSaveable { mutableStateOf(false) }
    if (!focused && data.anime.isNotEmpty()) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            withFrameNanos { }
            runCatching { firstCard.requestFocus() }
            focused = true
        }
    }
    val choices = remember(data) { seasonChoices(data.archive, data.year, data.season) }
    val scored = data.anime.mapNotNull { it.score }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = horizontal, end = horizontal, top = 28.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "SEASON",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                )
                Text(
                    text = SeasonChoice(data.year, data.season).label,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = listOfNotNull(
                    "${data.anime.size} titles",
                    if (scored.isNotEmpty()) "Average score ${JikanFormat.score(scored.average())}" else null,
                ).joinToString(" · "),
                modifier = Modifier.padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontal, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(choices, key = { it.label }) { choice ->
                PillButton(
                    text = choice.label,
                    onClick = { onSelectSeason(choice.year, choice.season) },
                    selected = choice.year == data.year && choice.season == data.season,
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            state = rememberLazyGridState(),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = horizontal, end = horizontal, top = 10.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(data.anime, key = { _, anime -> anime.malId }) { index, anime ->
                AnimePosterCard(
                    anime = anime,
                    onClick = { onOpenAnime(anime.malId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (index == 0) Modifier.focusRequester(firstCard) else Modifier),
                ) { item ->
                    AnimeArtwork(item.malId, item.images.large, item.displayTitle, Modifier.fillMaxSize())
                }
            }
        }
    }
}
