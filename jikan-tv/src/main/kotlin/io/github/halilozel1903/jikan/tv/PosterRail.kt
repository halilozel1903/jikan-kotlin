package io.github.halilozel1903.jikan.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.model.Anime

/**
 * The title above a rail, with an optional [trailing] hint such as `"12 titles"`. A long title is ellipsized
 * and never pushes the hint off screen. Marked as a heading for accessibility services.
 */
@Composable
public fun RailTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * A titled, horizontally scrolling row of focusable items. Nothing is drawn for an empty list.
 *
 * @param key a stable key per item, such as the MyAnimeList id.
 * @param itemContent draws one item, usually a [PosterCard].
 */
@Composable
public fun <T> PosterRail(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = JikanTvDefaults.RailContentPadding,
    spacing: Dp = JikanTvDefaults.CardSpacing,
    itemContent: @Composable (T) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier) {
        RailTitle(
            text = title,
            trailing = trailing,
            modifier = Modifier.padding(horizontal = JikanTvDefaults.HorizontalPadding),
        )
        Spacer(Modifier.height(4.dp))
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            lazyItems(items = items, key = key) { item -> itemContent(item) }
        }
    }
}

/**
 * A [PosterRail] of [AnimePosterCard]s. Anime that appear twice in [anime] (Jikan can repeat an entry across
 * pages) are shown once.
 */
@Composable
public fun AnimeRail(
    title: String,
    anime: List<Anime>,
    onClick: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    cardWidth: Dp = JikanTvDefaults.PosterWidth,
    state: LazyListState = rememberLazyListState(),
    artwork: @Composable BoxScope.(Anime) -> Unit,
) {
    PosterRail(
        title = title,
        items = anime.distinctBy { it.malId },
        key = { it.malId },
        modifier = modifier,
        trailing = trailing,
        state = state,
    ) { item ->
        AnimePosterCard(anime = item, onClick = { onClick(item) }, width = cardWidth, artwork = artwork)
    }
}
