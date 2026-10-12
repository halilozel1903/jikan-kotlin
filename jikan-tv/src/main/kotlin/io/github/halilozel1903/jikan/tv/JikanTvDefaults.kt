package io.github.halilozel1903.jikan.tv

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Default sizes of the jikan-tv components, tuned for a 960 x 540 dp TV screen. */
public object JikanTvDefaults {
    /** Width of a poster card; the poster is 2:3 like MyAnimeList's cover images. */
    public val PosterWidth: Dp = 116.dp

    /** MyAnimeList covers are 2:3 (225 x 350 px or so). */
    public const val PosterAspectRatio: Float = 2f / 3f

    /** How much a focused card grows. Keep it small so neighbours are not covered. */
    public const val FocusedScale: Float = 1.08f

    /** Corner shape of poster cards. */
    public val PosterShape: Shape = RoundedCornerShape(10.dp)

    /** Space between cards in a rail. */
    public val CardSpacing: Dp = 16.dp

    /** Start and end padding of rails, matching the TV safe area. */
    public val HorizontalPadding: Dp = 48.dp

    /** Padding of a rail's row: room for the focus scale above and below the cards. */
    public val RailContentPadding: PaddingValues = PaddingValues(horizontal = HorizontalPadding, vertical = 10.dp)
}
