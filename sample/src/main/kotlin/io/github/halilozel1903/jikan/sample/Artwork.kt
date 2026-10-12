package io.github.halilozel1903.jikan.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import io.github.halilozel1903.jikan.JikanFormat
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** `true` when the app shows bundled demo data: covers are then drawn instead of downloaded. */
val LocalDemoArtwork = staticCompositionLocalOf { false }

/**
 * A cover image: drawn [CoverArt] in demo mode, otherwise the MyAnimeList image from Jikan over the drawn art,
 * which stays visible as a placeholder while the image loads or when it fails.
 */
@Composable
fun AnimeArtwork(seed: Int, imageUrl: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(modifier) {
        CoverArt(seed, Modifier.fillMaxSize())
        if (!LocalDemoArtwork.current && !imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** A round portrait: the character or person image, or drawn initials in demo mode and while loading. */
@Composable
fun Avatar(seed: Int, name: String, imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    val palette = palette(seed)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.top, palette.bottom))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            color = Color.White,
            fontSize = (size.value * 0.34f).sp,
            fontWeight = FontWeight.Bold,
        )
        if (!LocalDemoArtwork.current && !imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun initials(name: String): String =
    JikanFormat.naturalName(name).split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }

/**
 * Original, procedurally drawn cover art: a gradient sky and one of five motifs (night sky with a crescent moon,
 * sea and setting sun, neon skyline, misty mountains with lanterns, ringed planet), all picked from [seed], so every
 * title keeps its own look. Used for the screenshots and as the placeholder of real images.
 */
@Composable
fun CoverArt(seed: Int, modifier: Modifier = Modifier) {
    val palette = palette(seed)
    val motif = Math.floorMod(seed / 3, 5)
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(palette.top, palette.bottom)))
        val random = Random(seed)
        when (motif) {
            0 -> nightSky(palette, random)
            1 -> seaAndSun(palette, random)
            2 -> neonSkyline(palette, random)
            3 -> mountainsAndLanterns(palette, random)
            else -> ringedPlanet(palette, random)
        }
        // A soft shade at the bottom, like printed key art.
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                startY = size.height * 0.6f,
                endY = size.height,
            ),
        )
    }
}

private class Palette(val top: Color, val bottom: Color, val accent: Color, val shade: Color)

private val Palettes = listOf(
    Palette(Color(0xFF141B3D), Color(0xFF4B3A78), Color(0xFFFFD98A), Color(0xFF0B0F24)),
    Palette(Color(0xFF2B5876), Color(0xFF4E4376), Color(0xFFFFB199), Color(0xFF172238)),
    Palette(Color(0xFF0F2027), Color(0xFF2C5364), Color(0xFF7FE7D3), Color(0xFF08161B)),
    Palette(Color(0xFF42275A), Color(0xFF734B6D), Color(0xFFFFC371), Color(0xFF22132F)),
    Palette(Color(0xFF1D4350), Color(0xFFA43931), Color(0xFFFFE29F), Color(0xFF10232B)),
    Palette(Color(0xFF16222A), Color(0xFF3A6073), Color(0xFFFF9ECF), Color(0xFF0B1217)),
    Palette(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B), Color(0xFF1E0E3B)),
    Palette(Color(0xFF134E5E), Color(0xFF71B280), Color(0xFFFFF3B0), Color(0xFF0A2A33)),
    Palette(Color(0xFF232526), Color(0xFF414345), Color(0xFFFF7A59), Color(0xFF111213)),
)

private fun palette(seed: Int): Palette = Palettes[Math.floorMod(seed, Palettes.size)]

private fun DrawScope.stars(random: Random, count: Int, color: Color, maxY: Float) {
    repeat(count) {
        val center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height * maxY)
        drawCircle(color.copy(alpha = 0.4f + random.nextFloat() * 0.6f), radius = size.minDimension * (0.004f + random.nextFloat() * 0.008f), center = center)
    }
}

private fun DrawScope.hills(color: Color, base: Float, amplitude: Float, phase: Float) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            lineTo(x, h * base + sin(x / w * 2.2f * PI.toFloat() + phase) * h * amplitude)
            x += w / 40f
        }
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.nightSky(palette: Palette, random: Random) {
    val u = size.minDimension
    stars(random, 40, Color.White, 0.7f)
    val moon = Offset(size.width * (0.55f + random.nextFloat() * 0.25f), size.height * 0.24f)
    drawCircle(
        Brush.radialGradient(listOf(palette.accent.copy(alpha = 0.45f), Color.Transparent), center = moon, radius = u * 0.42f),
        radius = u * 0.42f,
        center = moon,
    )
    drawCircle(palette.accent, radius = u * 0.17f, center = moon)
    drawCircle(palette.top, radius = u * 0.15f, center = moon + Offset(u * 0.07f, -u * 0.04f))
    hills(palette.shade.copy(alpha = 0.7f), 0.72f, 0.05f, random.nextFloat() * 6f)
    hills(palette.shade, 0.84f, 0.04f, random.nextFloat() * 6f)
}

private fun DrawScope.seaAndSun(palette: Palette, random: Random) {
    val w = size.width
    val h = size.height
    val u = size.minDimension
    val sun = Offset(w * (0.35f + random.nextFloat() * 0.3f), h * 0.56f)
    drawCircle(palette.accent, radius = u * 0.26f, center = sun)
    // Retro stripes across the lower half of the sun.
    for (i in 0 until 5) {
        val y = sun.y - u * 0.02f + i * u * 0.05f
        drawRect(palette.bottom, topLeft = Offset(sun.x - u * 0.3f, y), size = Size(u * 0.6f, u * (0.008f + i * 0.006f)))
    }
    drawRect(palette.shade.copy(alpha = 0.92f), topLeft = Offset(0f, h * 0.62f), size = Size(w, h * 0.38f))
    for (i in 0 until 7) {
        val y = h * (0.66f + i * 0.05f)
        val half = w * (0.08f + i * 0.04f)
        drawLine(
            palette.accent.copy(alpha = 0.55f - i * 0.06f),
            start = Offset(sun.x - half, y),
            end = Offset(sun.x + half, y),
            strokeWidth = u * 0.012f,
        )
    }
}

private fun DrawScope.neonSkyline(palette: Palette, random: Random) {
    val w = size.width
    val h = size.height
    drawCircle(palette.accent.copy(alpha = 0.25f), radius = h * 0.45f, center = Offset(w * 0.5f, h * 0.55f))
    stars(random, 18, Color.White, 0.4f)
    val count = 9
    val bar = w / count
    for (i in 0 until count) {
        val height = h * (0.25f + random.nextFloat() * 0.45f)
        val top = h - height
        drawRect(palette.shade.copy(alpha = 0.85f), topLeft = Offset(i * bar, top), size = Size(bar * 0.92f, height))
        // Lit windows.
        var y = top + h * 0.03f
        while (y < h - h * 0.04f) {
            if (random.nextFloat() < 0.45f) {
                drawRect(
                    palette.accent.copy(alpha = 0.5f + random.nextFloat() * 0.5f),
                    topLeft = Offset(i * bar + bar * 0.2f, y),
                    size = Size(bar * 0.18f, h * 0.018f),
                )
            }
            if (random.nextFloat() < 0.45f) {
                drawRect(
                    palette.accent.copy(alpha = 0.4f + random.nextFloat() * 0.5f),
                    topLeft = Offset(i * bar + bar * 0.55f, y),
                    size = Size(bar * 0.18f, h * 0.018f),
                )
            }
            y += h * 0.045f
        }
    }
    drawLine(palette.accent, start = Offset(0f, h * 0.995f), end = Offset(w, h * 0.995f), strokeWidth = h * 0.01f)
}

private fun DrawScope.mountainsAndLanterns(palette: Palette, random: Random) {
    val w = size.width
    val h = size.height
    val u = size.minDimension
    for (layer in 0 until 3) {
        val base = h * (0.55f + layer * 0.14f)
        val peaks = 3 + layer
        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, base)
            for (p in 0 until peaks) {
                val x0 = w * p / peaks
                val x1 = w * (p + 1) / peaks
                lineTo((x0 + x1) / 2f, base - h * (0.12f + random.nextFloat() * 0.12f))
                lineTo(x1, base)
            }
            lineTo(w, h)
            close()
        }
        drawPath(path, palette.shade.copy(alpha = 0.45f + layer * 0.2f))
    }
    // A string of lanterns across the top.
    val sag = h * 0.06f
    val y0 = h * 0.12f
    val rope = Path().apply {
        moveTo(0f, y0)
        quadraticTo(w / 2f, y0 + sag * 2f, w, y0)
    }
    drawPath(rope, Color.Black.copy(alpha = 0.5f), style = Stroke(width = u * 0.008f))
    val lanterns = 5
    for (i in 1..lanterns) {
        val t = i / (lanterns + 1f)
        val x = w * t
        val y = y0 + sag * 4f * t * (1f - t) + u * 0.05f
        drawCircle(palette.accent.copy(alpha = 0.3f), radius = u * 0.07f, center = Offset(x, y))
        drawOval(palette.accent, topLeft = Offset(x - u * 0.035f, y - u * 0.045f), size = Size(u * 0.07f, u * 0.09f))
    }
}

private fun DrawScope.ringedPlanet(palette: Palette, random: Random) {
    val u = size.minDimension
    stars(random, 50, Color.White, 1f)
    val center = Offset(size.width * (0.4f + random.nextFloat() * 0.2f), size.height * 0.42f)
    val radius = u * 0.3f
    drawCircle(
        Brush.linearGradient(listOf(palette.accent, palette.bottom), start = center - Offset(radius, radius), end = center + Offset(radius, radius)),
        radius = radius,
        center = center,
    )
    rotate(-18f, pivot = center) {
        drawOval(
            palette.accent.copy(alpha = 0.8f),
            topLeft = Offset(center.x - radius * 1.7f, center.y - radius * 0.35f),
            size = Size(radius * 3.4f, radius * 0.7f),
            style = Stroke(width = u * 0.025f),
        )
    }
    drawCircle(palette.accent.copy(alpha = 0.9f), radius = u * 0.04f, center = Offset(size.width * 0.82f, size.height * 0.16f))
}
