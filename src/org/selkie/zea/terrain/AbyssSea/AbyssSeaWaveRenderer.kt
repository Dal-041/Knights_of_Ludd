package org.selkie.zea.terrain.AbyssSea

import com.fs.starfarer.api.graphics.SpriteAPI
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.OpenGlUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Draws an [AbyssSeaWave]: a textured ring that tears open around each [AbyssSeaShelter].
 *
 * Away from obstacles the ring is a plain strip. Around a shelter it is a mesh stitched to the shelter's outline, so
 * the obstacle cuts the wave's crests off cleanly along the tear's edge; the crests themselves are never bent.
 *
 * Angles in here are radians counter-clockwise of [RING_FACING] unless noted.
 */
object AbyssSeaWaveRenderer {
    /** Vertices around the full ring where nothing is in the way. */
    const val RING_POINTS = 380
    /** The angle the texture starts at, in degrees. */
    const val RING_FACING = 90f
    /** How many times the texture repeats around the ring. One: the artwork is laid round the whole way once, so there
     * is nothing to tile and nothing to match up at the seam. */
    const val TEXTURE_REPEATS = 1
    /** The texture is sampled across the band over this fraction of its width, stopping short of the far edge so the
     * last row of texels (or the wrapped first row, with GL_REPEAT) never lands on the band's edge. */
    const val ACROSS_SPAN = 0.999f
    const val SCROLL_SPEED = 500f

    /** Width of the soft edge of a tear, in SU. */
    const val EDGE_FADE = 40f
    /** How far to either side of a tear its mesh reaches, in tear widths. */
    const val PATCH_WIDTH_MULT = 1.5f
    /** Spacing of the mesh around a tear, in SU: along the wave's travel, and across it. */
    const val ROW_SPACING = 30f
    const val COLUMN_SPACING = 60f
    const val MAX_ROWS = 120
    const val MIN_COLUMNS = 8
    const val MAX_COLUMNS = 48

    private const val TWO_PI = (PI * 2.0).toFloat()
    private val FACING_RAD = Math.toRadians(RING_FACING.toDouble()).toFloat()
    private val MAX_HALF_SPAN = Math.toRadians(100.0).toFloat()

    /** A shelter the wave is currently passing, and the stretch of the ring its mesh covers. */
    private class Tear(val shelter: AbyssSeaShelter, var center: Float, val halfSpan: Float)

    /** A stretch of the ring drawn as one mesh, holding every tear whose influence overlaps. */
    private class Patch(var start: Float, var end: Float) {
        val tears = ArrayList<Tear>()
    }

    fun render(
        loc: Vector2f, innerRadius: Float, outerRadius: Float, shelters: List<AbyssSeaShelter>,
        sprite: SpriteAPI, alpha: Float
    ) {
        if (outerRadius - innerRadius < 1f) return
        val patches = buildPatches(shelters, innerRadius, outerRadius)
        val repeats = TEXTURE_REPEATS

        if (patches.isEmpty()) {
            renderCalm(loc, 0f, TWO_PI, innerRadius, outerRadius, repeats, sprite, alpha)
            return
        }
        for (i in patches.indices) {
            val patch = patches[i]
            renderPatch(loc, patch, innerRadius, outerRadius, repeats, sprite, alpha)
            val nextStart = if (i + 1 < patches.size) patches[i + 1].start else patches[0].start + TWO_PI
            renderCalm(loc, patch.end, nextStart, innerRadius, outerRadius, repeats, sprite, alpha)
        }
    }

    /** Groups the shelters the band is passing into non-overlapping stretches of the ring, sorted by angle. */
    private fun buildPatches(shelters: List<AbyssSeaShelter>, innerRadius: Float, outerRadius: Float): List<Patch> {
        val tears = ArrayList<Tear>()
        for (shelter in shelters) {
            val nearest = shelter.distance - shelter.radius
            if (nearest <= 1f) continue // the wave starts inside this one; there is no front to tear
            val farthest = shelter.distance + shelter.tailLength
            if (nearest >= outerRadius || farthest <= innerRadius) continue

            // widest angle the tear can take up, seen from the wave's source
            val widest = asin((shelter.radius / nearest.coerceAtLeast(shelter.radius)).coerceAtMost(0.95f))
            val halfSpan = (widest * PATCH_WIDTH_MULT).coerceAtMost(MAX_HALF_SPAN)
            val center = ((shelter.angle - FACING_RAD) % TWO_PI + TWO_PI) % TWO_PI
            tears += Tear(shelter, center, halfSpan)
        }
        if (tears.isEmpty()) return emptyList()
        tears.sortBy { it.center }

        val patches = ArrayList<Patch>()
        for (tear in tears) {
            val start = tear.center - tear.halfSpan
            val end = tear.center + tear.halfSpan
            val last = patches.lastOrNull()
            if (last != null && start <= last.end) {
                last.end = maxOf(last.end, end)
                last.tears += tear
            } else {
                patches += Patch(start, end).also { it.tears += tear }
            }
        }

        // the ring is a circle: fold the first patch into the last if they meet across the seam
        while (patches.size > 1 && patches.last().end - TWO_PI >= patches.first().start) {
            val first = patches.removeAt(0)
            val last = patches.last()
            for (tear in first.tears) tear.center += TWO_PI
            last.tears += first.tears
            last.end = maxOf(last.end, first.end + TWO_PI)
        }
        if (patches.size == 1 && patches[0].end - patches[0].start > TWO_PI) {
            patches[0].end = patches[0].start + TWO_PI
        }
        return patches
    }

    /** Draws an unbroken stretch of the ring as a single strip. */
    private fun renderCalm(
        loc: Vector2f, start: Float, end: Float, innerRadius: Float, outerRadius: Float, repeats: Int,
        sprite: SpriteAPI, alpha: Float
    ) {
        if (end - start < 0.0001f) return
        val step = TWO_PI / RING_POINTS
        val angles = ArrayList<Float>()
        angles += start
        var a = (floor(start / step) + 1f) * step
        while (a < end) {
            angles += a
            a += step
        }
        angles += end
        OpenGlUtils.drawTexturedRingGrid(
            loc,
            floatArrayOf(innerRadius, outerRadius),
            FloatArray(angles.size) { RING_FACING + Math.toDegrees(angles[it].toDouble()).toFloat() },
            null,
            repeats,
            SCROLL_SPEED,
            sprite,
            RING_FACING,
            alpha,
            true,
        )
    }

    /**
     * Draws a stretch of the ring that tears around one or more shelters.
     *
     * The mesh is laid out in rows of constant distance from the wave's source. Each row is split at the tear into two
     * sides, each running from the tear's edge out to the end of the patch, so the tear's outline lies exactly on the
     * mesh. Texture coordinates and opacity depend only on where a vertex ends up in the world, so the layout of the
     * mesh never shows.
     */
    private fun renderPatch(
        loc: Vector2f, patch: Patch, innerRadius: Float, outerRadius: Float, repeats: Int, sprite: SpriteAPI,
        alpha: Float
    ) {
        val tears = patch.tears
        val thickness = outerRadius - innerRadius
        val radii = buildRows(tears, innerRadius, outerRadius)
        val rows = radii.size
        if (rows < 2) return

        val arc = (patch.end - patch.start) * 0.5f * outerRadius
        val columns = ceil(arc / COLUMN_SPACING).toInt().coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val perRow = columns + 1

        val scroll = OpenGlUtils.getTextureScroll(SCROLL_SPEED)

        // vertex grids for the two sides of the tear, indexed [side][row * perRow + column]
        val xs = Array(2) { FloatArray(rows * perRow) }
        val ys = Array(2) { FloatArray(rows * perRow) }
        val us = Array(2) { FloatArray(rows * perRow) }
        val vs = Array(2) { FloatArray(rows * perRow) }
        val alphas = Array(2) { FloatArray(rows * perRow) }

        val widths = FloatArray(tears.size)
        val spans = FloatArray(tears.size)

        for (row in 0 until rows) {
            val radius = radii[row]

            // each tear's shape at this distance from the source, and where to split the row
            var low = tears[0].center
            var high = low
            var widest = 0f
            for (k in tears.indices) {
                val tear = tears[k]
                val along = radius - tear.shelter.distance
                val width = tear.shelter.halfWidth(along)
                widths[k] = width
                spans[k] = asin((width / radius).coerceIn(0f, 1f)).coerceAtMost(tear.halfSpan * 0.9f)
                if (spans[k] > widest) {
                    widest = spans[k]
                    low = tear.center - spans[k]
                    high = tear.center + spans[k]
                }
            }
            low = low.coerceIn(patch.start, patch.end)
            high = high.coerceIn(patch.start, patch.end)

            for (side in 0..1) {
                val from = if (side == 0) low else high
                val to = if (side == 0) patch.start else patch.end
                val sideArc = abs(to - from) * radius
                // the second column sits one fade width from the edge, so the edge stays equally soft all along
                val fade = if (sideArc > 0f) (EDGE_FADE / sideArc).coerceAtMost(0.3f) else 0.3f

                for (column in 0 until perRow) {
                    val fraction = when (column) {
                        0 -> 0f
                        else -> fade + (1f - fade) * (column - 1) / (columns - 1)
                    }
                    val angle = from + (to - from) * fraction

                    var vertexAlpha = 1f
                    for (k in tears.indices) {
                        if (widths[k] > 0f) {
                            val offEdge = abs(angle - tears[k].center) - spans[k]
                            val outside = (offEdge * radius / EDGE_FADE).coerceIn(0f, 1f)
                            val open = (widths[k] / EDGE_FADE).coerceAtMost(1f)
                            vertexAlpha = minOf(vertexAlpha, 1f - open * (1f - outside))
                        }
                    }

                    val index = row * perRow + column
                    val worldAngle = FACING_RAD + angle
                    xs[side][index] = loc.x + radius * cos(worldAngle)
                    ys[side][index] = loc.y + radius * sin(worldAngle)
                    val around = scroll + repeats * angle / TWO_PI
                    val across = ((radius - innerRadius) / thickness).coerceIn(0f, 1f) * ACROSS_SPAN
                    us[side][index] = across
                    vs[side][index] = around
                    alphas[side][index] = alpha * vertexAlpha
                }
            }
        }

        // one quad per cell of each grid
        val quads = 2 * (rows - 1) * columns
        val vertices = FloatArray(quads * 8)
        val texCoords = FloatArray(quads * 8)
        val colors = FloatArray(quads * 16) { 1f }
        var v = 0
        for (side in 0..1) {
            for (row in 0 until rows - 1) {
                for (column in 0 until columns) {
                    val a = row * perRow + column
                    val b = a + 1
                    val c = b + perRow
                    val d = a + perRow
                    for (index in intArrayOf(a, b, c, d)) {
                        vertices[v * 2] = xs[side][index]
                        vertices[v * 2 + 1] = ys[side][index]
                        texCoords[v * 2] = us[side][index]
                        texCoords[v * 2 + 1] = vs[side][index]
                        colors[v * 4 + 3] = alphas[side][index]
                        v++
                    }
                }
            }
        }

        sprite.bindTexture()
        OpenGlUtils.drawPoints(
            vertices, texCoords, colors, GL11.GL_QUADS, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA
        )
    }

    /**
     * Distances from the wave's source to put mesh rows at: evenly spaced, plus extra rows across the rounded front of
     * each shelter, where its outline turns too fast for even spacing to follow.
     */
    private fun buildRows(tears: List<Tear>, start: Float, end: Float): FloatArray {
        val even = ceil((end - start) / ROW_SPACING).toInt().coerceIn(1, MAX_ROWS)
        val radii = ArrayList<Float>()
        for (i in 0..even) radii += start + (end - start) * i / even

        for (tear in tears) {
            val shelter = tear.shelter
            for (i in 0 until 10) {
                val radius = shelter.distance - shelter.radius * cos(Math.toRadians(i * 9.0)).toFloat()
                if (radius > start && radius < end) radii += radius
            }
        }
        radii.sort()
        return radii.toFloatArray()
    }
}
