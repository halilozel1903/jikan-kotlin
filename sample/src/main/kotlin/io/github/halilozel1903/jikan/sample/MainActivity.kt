package io.github.halilozel1903.jikan.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.darkColorScheme

/**
 * An Android TV anime browser built on jikan-kotlin. A normal launch loads the live Jikan API (with Coil for the
 * cover images) and offers bundled demo data when Jikan cannot be reached.
 *
 * D-pad presses can't be timed reliably through adb on a fresh emulator, so `scripts/screenshots.sh` starts the app
 * with `--es scene <scene>` to open a fixed screen. Scenes read the bundled JSON fixtures, draw original cover art
 * and never touch the network:
 *
 * - `home`: the hero for the current season's top show and the rails, "More info" focused
 * - `detail`: "Harbor of a Thousand Bells" with its synopsis, score, episodes and characters
 * - `season`: the summer 2026 season grid with the season picker
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            MaterialTheme(colorScheme = SampleColors) {
                // The Surface gives every Text a readable default color on the dark background.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                ) {
                    SampleApp(
                        scene = scene,
                        liveRepository = { Repositories.live(applicationContext) },
                        demoRepository = { Repositories.demo(applicationContext) },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}

private val SampleColors = darkColorScheme(
    primary = Color(0xFFFF8A7A),
    onPrimary = Color(0xFF3B0A05),
    secondary = Color(0xFF8FD3FF),
    onSecondary = Color(0xFF00243A),
    background = Color(0xFF0C0E16),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF0C0E16),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF1D2130),
    onSurfaceVariant = Color(0xFFA9AFC2),
)
