package io.github.halilozel1903.jikan.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.model.MalUrl

/**
 * Up to [maxCount] genre, theme or demographic names as small pills. They are not focusable: on TV, put
 * focusable filters in their own row.
 */
@Composable
public fun GenreChips(
    genres: List<MalUrl>,
    modifier: Modifier = Modifier,
    maxCount: Int = 4,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (genres.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (genre in genres.take(maxCount)) {
            Text(
                text = genre.name,
                modifier = Modifier
                    .background(containerColor, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
