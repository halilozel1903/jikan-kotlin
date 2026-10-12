package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.jikan.JikanException
import kotlinx.coroutines.CancellationException

/** The state of something loading. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>

    data class Ready<T>(val value: T) : Load<T>

    data class Failed(val message: String) : Load<Nothing>
}

/** Runs [block] and wraps its result or a readable error. */
suspend fun <T> load(isDemo: Boolean, block: suspend () -> T): Load<T> = try {
    Load.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: JikanException.NotFound) {
    Load.Failed(
        if (isDemo) "This title is not part of the offline demo data." else "MyAnimeList has no entry for this.",
    )
} catch (e: JikanException.RateLimited) {
    Load.Failed("Jikan asked us to slow down. Wait a moment and try again.")
} catch (e: JikanException.Network) {
    Load.Failed("Could not reach the Jikan API. Check the connection.")
} catch (e: JikanException) {
    Load.Failed(e.message ?: "The Jikan API returned an error.")
}

/** Waits one frame, so lazy rows have laid out their items, then moves the focus to [requester]. */
@Composable
fun FocusOnce(requester: FocusRequester, key: Any?) {
    LaunchedEffect(key) {
        withFrameNanos { }
        withFrameNanos { }
        runCatching { requester.requestFocus() }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Loading…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    onUseDemo: (() -> Unit)?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val retryFocus = remember { FocusRequester() }
    FocusOnce(retryFocus, message)
    Column(
        modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = message,
            modifier = Modifier.widthIn(max = 520.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PillButton(text = "Try again", onClick = onRetry, modifier = Modifier.focusRequester(retryFocus), primary = true)
            if (onUseDemo != null) PillButton(text = "Use offline demo data", onClick = onUseDemo)
            if (onBack != null) PillButton(text = "Back", onClick = onBack)
        }
    }
}
