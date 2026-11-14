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
 * The artwork's streaks run along its X axis and its cross-section (transparent, opaque, transparent) along Y, so
 * texture U goes around the ring and V across the band. Swapping them sweeps the cross-section around the ring once
 * per repeat, which shows as [TEXTURE_REPEATS] breaks and smears the pale edges into a white wash.
 *
 * Angles in here are radians counter-clockwise of [RING_FACING] unless noted.
 */
object AbyssSeaWaveRenderer {
    /** Vertices around the full ring where nothing is in the way: an upper bound, the step itself comes from
     * [COLUMN_SPACING] like the tear meshes, so both sample the texture at the same rate. */
    const val RING_POINTS = 380
    /** The angle the texture starts at, in degrees. */
    const val RING_FACING = 90f
    /** How many times the texture repeats around the ring. Static on purpose: deriving it from the circumference makes
     * it grow as the wave travels, which rescales the pattern every frame and outruns any mesh drawn at a fixed
     * resolution. */
    const val TEXTURE_REPEATS = 25
    /** The texture is sampled across the band over this fraction of its width. */
    const val ACROSS_SPAN = 1f
    const val SCROLL_SPEED = 500f

    /** Width of the soft edge of a tear, in SU. */
    const val EDGE_FADE = 60f
    /**
     * The wake behind each shelter is a Kármán vortex street: eddies shed alternately off either side of the obstacle,
     * swirling in opposite directions, drifting downstream in a staggered double row and dying out past the tail.
     * They swirl the wave's texture and, a little, the tear's edge. Sizes are in shelter radii.
     */
    /** Distance between consecutive eddies along the street (one per side every two of these). */
    const val VORTEX_SPACING = 3.2f
    /** Radius of an eddy's swirl. */
    const val VORTEX_SIZE = 1.2f
    /** How far each row sits off the wake's centre line once the tear has closed. */
    const val VORTEX_STREET_HALF_WIDTH = 0.6f
    /** How far the street runs past the tip of the tear before the eddies have died out completely; they are already
     * faint well before that (see [wake]). */
    const val WAKE_LENGTH = 5f
    /** Turn at an eddy's core, in radians. */
    const val VORTEX_SWIRL = 2.6f
    /** Where along the street the eddies start fading, and where they are gone, as fractions of the street's length
     * (tail plus [WAKE_LENGTH]; the tear itself ends at about 0.55). */
    const val VORTEX_FADE_START = 0.2f
    const val VORTEX_FADE_END = 0.6f
    /** How fast the eddies drift downstream, in SU per second. */
    const val VORTEX_DRIFT = 60f
    /** The share of the swirl that moves the mesh itself, scalloping the tear's edge; the texture always gets all of it. */
    const val VORTEX_SHAPE = 0.2f
    /** Swirls fade out over this many SU before the end of a patch, so the patch never shows as a seam. */
    const val VORTEX_PATCH_FADE = 120f
    /** Columns at these distances from the tear's edge (SU, past the soft edge), dense enough to draw the eddies. */
    private val WAKE_STEPS = floatArrayOf(10f, 22f, 38f, 58f, 82f, 110f, 145f, 185f, 230f, 280f, 340f, 400f, 470f, 550f, 640f)

    /** How far to either side of a tear its mesh reaches, in tear widths. */
    const val PATCH_WIDTH_MULT = 2f
    /** Spacing of the mesh around a tear, in SU: along the wave's travel, and across it. */
    const val ROW_SPACING = 30f
    const val COLUMN_SPACING = 60f
    const val MAX_ROWS = 120
    /** Columns per side of a tear. At least two, since the first column is the tear's edge and the second sits one fade
     * width out from it. The cap only guards against degenerate patches; the step normally comes from
     * [COLUMN_SPACING] and [RING_POINTS]. */
    const val MIN_COLUMNS = 18
    const val MAX_COLUMNS = 256

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
        sprite: SpriteAPI, alpha: Float, time: Float = 0f
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
            renderPatch(loc, patch, innerRadius, outerRadius, repeats, sprite, alpha, time)
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
            val farthest = shelter.distance + shelter.tailLength + shelter.radius * WAKE_LENGTH
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

    /** Angle between vertices around the ring, in radians: [COLUMN_SPACING] at the band's middle, but never finer than
     * [RING_POINTS] to the full circle. Shared by calm stretches and tear meshes so both bend the same. */
    private fun calmStep(innerRadius: Float, outerRadius: Float): Float =
        (COLUMN_SPACING / ((innerRadius + outerRadius) / 2f)).coerceAtLeast(TWO_PI / RING_POINTS)

    /** Draws an unbroken stretch of the ring as a single strip. */
    private fun renderCalm(
        loc: Vector2f, start: Float, end: Float, innerRadius: Float, outerRadius: Float, repeats: Int,
        sprite: SpriteAPI, alpha: Float
    ) {
        if (end - start < 0.0001f) return
        val step = calmStep(innerRadius, outerRadius)
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
            false,
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
        alpha: Float, time: Float
    ) {
        val tears = patch.tears
        val thickness = outerRadius - innerRadius
        val radii = buildRows(tears, innerRadius, outerRadius)
        val rows = radii.size
        if (rows < 2) return

        // a side can run nearly the whole patch, so size its columns for that, at the calm ring's step: any coarser and
        // the band turns into straight chords that kink where they meet
        val columns = ceil((patch.end - patch.start) / calmStep(innerRadius, outerRadius)).toInt()
            .coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val perRow = columns + 1

        val scroll = OpenGlUtils.getTextureScroll(SCROLL_SPEED)

        // vertex grids for the two sides of the tear, indexed [side][row * perRow + column]
        val xs = Array(2) { FloatArray(rows * perRow) }
        val ys = Array(2) { FloatArray(rows * perRow) }
        val us = Array(2) { FloatArray(rows * perRow) }
        val vs = Array(2) { FloatArray(rows * perRow) }
        val alphas = Array(2) { FloatArray(rows * perRow) }

        val widths = FloatArray(tears.size)
        // each tear's half-angle on its low and high side; they differ once the edges ripple
        val spansLow = FloatArray(tears.size)
        val spansHigh = FloatArray(tears.size)

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
                val maxSpan = tear.halfSpan * 0.9f
                spansLow[k] = asin((width / radius).coerceIn(0f, 1f)).coerceAtMost(maxSpan)
                spansHigh[k] = spansLow[k]
                val span = maxOf(spansLow[k], spansHigh[k])
                if (span > widest) {
                    widest = span
                    low = tear.center - spansLow[k]
                    high = tear.center + spansHigh[k]
                }
            }
            low = low.coerceIn(patch.start, patch.end)
            high = high.coerceIn(patch.start, patch.end)

            for (side in 0..1) {
                val from = if (side == 0) low else high
                val to = if (side == 0) patch.start else patch.end
                val sideArc = abs(to - from) * radius
                // column 1 sits one fade width from the edge, so the edge stays equally soft all along; the next ones
                // are packed near the edge where the eddies swirl, then the rest spread evenly to the end of the patch
                val zone = WAKE_STEPS.size
                val zoneWidth = EDGE_FADE + WAKE_STEPS.last()
                val squeeze = if (sideArc > 0f) (sideArc * 0.5f / zoneWidth).coerceAtMost(1f) else 0f
                val zoneEnd = if (sideArc > 0f) zoneWidth * squeeze / sideArc else 0f

                for (column in 0 until perRow) {
                    val fraction = when {
                        column == 0 || sideArc <= 0f -> 0f
                        column == 1 -> EDGE_FADE * squeeze / sideArc
                        column <= zone + 1 -> (EDGE_FADE + WAKE_STEPS[column - 2]) * squeeze / sideArc
                        else -> zoneEnd + (1f - zoneEnd) * (column - zone - 1) / (columns - zone - 1)
                    }
                    val angle = from + (to - from) * fraction
                    val distortion = wake(radius, angle, patch, time)

                    var vertexAlpha = 1f
                    for (k in tears.indices) {
                        if (widths[k] > 0f) {
                            val fromCenter = angle - tears[k].center
                            val offEdge = abs(fromCenter) - if (fromCenter < 0f) spansLow[k] else spansHigh[k]
                            val outside = (offEdge * radius / EDGE_FADE).coerceIn(0f, 1f)
                            val open = (widths[k] / EDGE_FADE).coerceAtMost(1f)
                            vertexAlpha = minOf(vertexAlpha, 1f - open * (1f - outside))
                        }
                    }

                    val index = row * perRow + column
                    // the mesh moves a little, the texture on it a lot: the water bends while the tear stays put
                    val worldAngle = FACING_RAD + angle + distortion[0] * VORTEX_SHAPE / radius
                    val worldRadius = radius + distortion[1] * VORTEX_SHAPE
                    xs[side][index] = loc.x + worldRadius * cos(worldAngle)
                    ys[side][index] = loc.y + worldRadius * sin(worldAngle)
                    val around = scroll + repeats * (angle + distortion[0] / radius) / TWO_PI
                    val across = ((radius + distortion[1] - innerRadius) / thickness).coerceIn(0f, 1f) * ACROSS_SPAN
                    us[side][index] = around
                    vs[side][index] = across
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

    private val wakeOut = FloatArray(2)

    /**
     * How far the vortex street behind the patch's shelters pushes the wave at a point, as {around, outward} in SU.
     * Each eddy turns the water around its core by up to [VORTEX_SWIRL], easing off with distance like a Gaussian.
     * Returns a shared array; read it before the next call.
     */
    private fun wake(radius: Float, angle: Float, patch: Patch, time: Float): FloatArray {
        var around = 0f
        var outward = 0f
        for (tear in patch.tears) {
            val shelter = tear.shelter
            val r = shelter.radius
            val spacing = r * VORTEX_SPACING
            val size = r * VORTEX_SIZE
            val shed = r * 0.3f // just past the obstacle's widest point
            val trail = shelter.tailLength + r * WAKE_LENGTH
            val drift = time * VORTEX_DRIFT

            val along = radius - shelter.distance
            val perp = (angle - tear.center) * radius
            if (along < shed - 2.5f * size || along > shed + trail + 2.5f * size) continue

            // eddy j sits at shed + drift - j * spacing: each one drifts downstream, a new one sheds every spacing,
            // and the sides alternate with j's parity, so an eddy keeps its side and its spin all its life
            val jMid = (shed + drift - along) / spacing
            val reach = 2.5f * size / spacing
            for (j in ceil(jMid - reach).toInt()..floor(jMid + reach).toInt()) {
                val center = shed + drift - j * spacing
                val life = (center - shed) / trail
                if (life < 0f || life > 1f) continue
                val side = if (j and 1 == 0) 1f else -1f
                val grow = ((center - shed) / (0.6f * spacing)).coerceAtMost(1f)
                // untouched near the obstacle, then dying off over the back half of the tear
                val t = ((life - VORTEX_FADE_START) / (VORTEX_FADE_END - VORTEX_FADE_START)).coerceIn(0f, 1f)
                val strength = grow * (1f - life) * (1f - t * t * (3f - 2f * t))

                // cores sit just outside the tear, in the water, where the swirl can be seen
                val offset = maxOf(shelter.halfWidth(center) + 0.5f * size, r * VORTEX_STREET_HALF_WIDTH)
                val dx = along - center
                val dy = perp - side * offset
                val falloff = (dx * dx + dy * dy) / (size * size)
                if (falloff > 6f) continue
                // the outer side of each row spins away from the wake's centre line
                val turn = -side * VORTEX_SWIRL * strength * kotlin.math.exp(-falloff)
                val c = cos(turn)
                val s = sin(turn)
                outward += dx * c - dy * s - dx
                around += dx * s + dy * c - dy
            }
        }
        val edge = (minOf(angle - patch.start, patch.end - angle) * radius / VORTEX_PATCH_FADE).coerceIn(0f, 1f)
        wakeOut[0] = around * edge
        wakeOut[1] = outward * edge
        return wakeOut
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
