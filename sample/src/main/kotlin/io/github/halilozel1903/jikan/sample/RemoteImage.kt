package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/**
 * An image from the Jikan API (a MyAnimeList cover or portrait) loaded with Coil, on a plain placeholder while it
 * loads. A failed load is retried a few times. Screenshot scenes wait until every one of these has loaded.
 */
@Composable
fun RemoteImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(if (url.isNullOrEmpty()) null else ImageStatus.Loading) }
    if (monitor != null) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        // snapshotFlow starts with the current value, so a load that finished before this effect ran still counts.
        // A failure that will be retried still counts as loading.
        LaunchedEffect(monitor, id, url) {
            snapshotFlow { if (status == ImageStatus.Failed && attempt < ImageRetries) ImageStatus.Loading else status }
                .collect { monitor.image(id, it) }
        }
    }
    if (status == ImageStatus.Failed && attempt < ImageRetries) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!url.isNullOrEmpty()) {
            key(url, attempt) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onState = { state ->
                        status = when (state) {
                            is AsyncImagePainter.State.Success -> ImageStatus.Loaded
                            is AsyncImagePainter.State.Error -> ImageStatus.Failed
                            else -> ImageStatus.Loading
                        }
                    },
                )
            }
        }
    }
}

/** A round character or person portrait. */
@Composable
fun Avatar(name: String, imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    RemoteImage(
        url = imageUrl,
        contentDescription = name,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}
