package io.github.halilozel1903.jikan.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.JikanFormat
import io.github.halilozel1903.jikan.model.Anime

/**
 * A focusable 2:3 poster for Android TV: the [artwork] in a rounded frame that scales up, glows and gets a border
 * when focused, with [title] (two lines at most) and an optional [subtitle] below it.
 *
 * @param score shown as a [ScoreBadge] in the top corner when not `null`.
 * @param width the card width; pass `Modifier.fillMaxWidth()` in [modifier] to fill a grid cell instead.
 * @param artwork draws the picture, filling the card. Load the cover image here, or draw a placeholder.
 */
@Composable
public fun PosterCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    score: Double? = null,
    width: Dp = JikanTvDefaults.PosterWidth,
    focusedScale: Float = JikanTvDefaults.FocusedScale,
    interactionSource: MutableInteractionSource? = null,
    artwork: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    Column(modifier.width(width)) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(JikanTvDefaults.PosterAspectRatio),
            shape = CardDefaults.shape(shape = JikanTvDefaults.PosterShape),
            colors = CardDefaults.colors(containerColor = colors.surfaceVariant),
            scale = CardDefaults.scale(focusedScale = focusedScale),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(width = 3.dp, color = colors.onSurface),
                    shape = JikanTvDefaults.PosterShape,
                ),
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = colors.primary.copy(alpha = 0.5f), elevation = 16.dp),
            ),
            interactionSource = source,
        ) {
            Box(Modifier.fillMaxSize()) {
                artwork()
                if (score != null) {
                    ScoreBadge(score, Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
        Spacer(Modifier.height(if (focused) 12.dp else 8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            color = if (focused) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A [PosterCard] for an [Anime]: its English title (or the default one), its score, and `"TV · 12 eps"` below.
 *
 * ```kotlin
 * AnimePosterCard(anime, onClick = { open(anime) }) {
 *     AsyncImage(model = anime.images.large, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
 * }
 * ```
 */
@Composable
public fun AnimePosterCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = JikanTvDefaults.PosterWidth,
    showScore: Boolean = true,
    subtitle: String? = posterSubtitle(anime),
    interactionSource: MutableInteractionSource? = null,
    artwork: @Composable BoxScope.(Anime) -> Unit,
) {
    PosterCard(
        title = anime.displayTitle,
        onClick = onClick,
        modifier = modifier,
        subtitle = subtitle,
        score = if (showScore) anime.score else null,
        width = width,
        interactionSource = interactionSource,
        artwork = { artwork(anime) },
    )
}

/** `"TV · 12 eps"`, `"Movie"` or `"Winter 2027"` for upcoming entries. */
public fun posterSubtitle(anime: Anime): String {
    val upcoming = anime.score == null && !anime.airing && anime.seasonLabel != null
    val parts = if (upcoming) {
        listOfNotNull(anime.type, anime.seasonLabel)
    } else {
        listOfNotNull(anime.type, JikanFormat.episodes(anime.episodes))
    }
    return parts.joinToString(" · ")
}
