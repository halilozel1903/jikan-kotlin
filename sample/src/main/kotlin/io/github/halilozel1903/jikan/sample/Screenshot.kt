package io.github.halilozel1903.jikan.sample

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Where an image of a screenshot scene is. */
enum class ImageStatus { Loading, Loaded, Failed }

/**
 * Tells `scripts/screenshots.sh` when a screenshot scene is complete: its data came from the live API and every
 * image on screen has loaded. The script waits for the `SCENE_READY <scene>` line in logcat before it captures,
 * and fails on `SCENE_FAILED`, so an error state or an empty poster never ends up in the README.
 */
class SceneMonitor(private val scene: Scene) {
    private val images = mutableStateMapOf<Any, ImageStatus>()
    private var content by mutableStateOf<Load<*>>(Load.Loading)

    fun image(id: Any, status: ImageStatus?) {
        if (status == null) images.remove(id) else images[id] = status
    }

    fun content(state: Load<*>) {
        content = state
    }

    private sealed interface Status {
        data object Waiting : Status
        data object Ready : Status
        data class Failed(val reason: String) : Status
    }

    private fun status(): Status {
        val current = content
        return when {
            current is Load.Failed -> Status.Failed(current.message)
            images.values.any { it == ImageStatus.Failed } -> Status.Failed("An image did not load.")
            current is Load.Ready && images.isNotEmpty() && images.values.all { it == ImageStatus.Loaded } -> Status.Ready
            else -> Status.Waiting
        }
    }

    @Composable
    fun Report() {
        LaunchedEffect(this) {
            snapshotFlow { status() }.collectLatest { status ->
                when (status) {
                    Status.Waiting -> Unit
                    // Ready for a moment without anything new loading: focus and layout have settled.
                    Status.Ready -> {
                        delay(1_500)
                        withFrameNanos { }
                        Log.i(TAG, "SCENE_READY ${scene.key}")
                    }
                    is Status.Failed -> Log.e(TAG, "SCENE_FAILED ${scene.key}: ${status.reason}")
                }
            }
        }
    }

    companion object {
        const val TAG = "JikanScene"
    }
}

/** The monitor of the screenshot scene being shown, or `null` in normal use. */
val LocalSceneMonitor = staticCompositionLocalOf<SceneMonitor?> { null }

/** Reports [state] to the scene monitor, if there is one. */
@Composable
fun ReportLoad(state: Load<*>) {
    val monitor = LocalSceneMonitor.current ?: return
    LaunchedEffect(monitor, state) { monitor.content(state) }
}
