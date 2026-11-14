package org.selkie.zea.terrain.AbyssSea

import org.lwjgl.util.vector.Vector2f
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The calm water an obstacle leaves in a [AbyssSeaWave], like a rock in a stream: the obstacle's upstream half-disc,
 * tapering to a point [tailLength] downstream. The wave tears open on the front and rejoins behind the tip.
 *
 * Everything is measured in the frame of the wave: "along" is distance downstream of [center] (away from the wave's
 * source), "perp" is distance off that axis.
 */
class AbyssSeaShelter(source: Vector2f, val center: Vector2f, val radius: Float) {
    companion object {
        /** How far the shelter reaches past the obstacle, in obstacle radii. */
        const val TAIL_LENGTH_MULT = 6f
    }

    val tailLength = radius * TAIL_LENGTH_MULT
    /** Distance from the wave's source to [center]. */
    val distance: Float
    /** Direction from the wave's source to [center], in radians. */
    val angle: Float
    private val dirX: Float
    private val dirY: Float

    init {
        val dx = center.x - source.x
        val dy = center.y - source.y
        distance = sqrt(dx * dx + dy * dy)
        dirX = if (distance > 0f) dx / distance else 1f
        dirY = if (distance > 0f) dy / distance else 0f
        angle = atan2(dirY, dirX)
    }

    /** Half the shelter's width at [along] SU downstream of [center]; 0 outside its length. */
    fun halfWidth(along: Float): Float = when {
        along < -radius || along > tailLength -> 0f
        along <= 0f -> sqrt(radius * radius - along * along)
        else -> radius * (1f - along / tailLength)
    }

    /** How far inside the shelter the point is, in SU from its edge. Positive inside, zero or negative outside. */
    fun depth(x: Float, y: Float): Float {
        val dx = x - center.x
        val dy = y - center.y
        val along = dx * dirX + dy * dirY
        val perp = abs(dx * dirY - dy * dirX)
        return halfWidth(along) - perp
    }

    /** A point on the shelter's outline [along] SU downstream of [center], on the given side (+1 or -1). */
    fun edgePoint(along: Float, side: Float): Vector2f {
        val width = halfWidth(along) * side
        return Vector2f(center.x + dirX * along - dirY * width, center.y + dirY * along + dirX * width)
    }

    /** Unit vector pointing from the shelter's axis out through the given side (+1 or -1). */
    fun sideNormal(side: Float): Vector2f = Vector2f(-dirY * side, dirX * side)

    /** Unit vector pointing downstream, away from the wave's source. */
    fun downstream(): Vector2f = Vector2f(dirX, dirY)

    fun contains(point: Vector2f): Boolean = depth(point.x, point.y) > 0f
}
