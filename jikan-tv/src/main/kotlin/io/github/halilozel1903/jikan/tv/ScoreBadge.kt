package io.github.halilozel1903.jikan.tv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.JikanFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A small pill with a star and MyAnimeList's score, such as `★ 8.91`. Draws `N/A` for entries without a score
 * (upcoming ones), or nothing when [hideWhenMissing] is set.
 */
@Composable
public fun ScoreBadge(
    score: Double?,
    modifier: Modifier = Modifier,
    hideWhenMissing: Boolean = false,
    containerColor: Color = Color.Black.copy(alpha = 0.62f),
    contentColor: Color = Color.White,
    starColor: Color = ScoreStarColor,
) {
    if (score == null && hideWhenMissing) return
    val text = JikanFormat.score(score)
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Score $text" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StarIcon(color = starColor, size = 11.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/** A five pointed star drawn with a path, so no icon library is needed. */
@Composable
public fun StarIcon(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val outer = this.size.minDimension / 2f
        val inner = outer * 0.45f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f + outer * 0.06f
        val path = Path()
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

/** The star color of [ScoreBadge]. */
public val ScoreStarColor: Color = Color(0xFFFFC94D)
