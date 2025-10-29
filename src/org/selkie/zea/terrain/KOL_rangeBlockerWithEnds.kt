package org.selkie.zea.terrain

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import kotlin.arrayOf
import kotlin.collections.plus
import kotlin.math.*
import kotlin.math.roundToInt
import kotlin.ranges.coerceAtLeast

class KOL_rangeBlockerWithEnds(val resolution: Int, val maxRange: Float, val tag: String) {

    //	private LocationAPI location;
    //	private SectorEntityToken entity;
    //	private SectorEntityToken exclude;
    private val degreesPerUnit = 360f / resolution.toFloat()
    private val limits = FloatArray(resolution)
    val ends = FloatArray(resolution)
    private val curr = Array(resolution) { return@Array Pair(0f, 0f) }


    //private float [] alphas;
    private var wasUpdated = false
    private var isAnythingShortened = false

    fun wasEverUpdated(): Boolean {
        return wasUpdated
    }


    fun updateAndSync(entity: SectorEntityToken, exclude: SectorEntityToken?, diffMult: Float) {
        updateLimits(entity, exclude, diffMult)
        sync()
//		for (int i = 0; i < resolution; i++) {
//			alphas[i] = 1f;
//		}
    }

    fun sync() {
        for (i in 0..<resolution) {
            val pair = Pair<Float, Float>(limits[i], ends[i])
            curr[i] = pair
        }
    }

    /*fun getShortenAmountAt(angle: Float): Float {
        return maxRange - getCurrMaxAt(angle)
    }*/

    fun getBlockedRangeAt(angle: Float): Pair<Float, Float> {
        var angle = angle
        angle = Misc.normalizeAngle(angle)

        val index = angle / 360f * resolution
        var i1 = floor(index.toDouble()).toInt()
        var i2 = ceil(index.toDouble()).toInt()
        while (i1 >= resolution) i1 -= resolution
        while (i2 >= resolution) i2 -= resolution

        val v1 = curr[i1]
        val v2 = curr[i2]

        val start = v1.first + (v2.first - v1.first) * (index - index.toInt())
        val end = v1.second + (v2.second - v1.second) * (index - index.toInt())

        return Pair(start, end)

        //int index = getIndexForAngle(angle);
        //return curr[index];
    }

    fun getAlphaAt(angle: Float): Float {
        return 1f
//		int index = getIndexForAngle(angle);
//		return alphas[index];
    }

    fun advance(amount: Float, minApproachSpeed: Float, diffMult: Float) {
        for (i in 0..<resolution) {
            val newFirst = Misc.approach(curr[i].first, limits[i], minApproachSpeed, diffMult, amount)
            val newEnd = Misc.approach(curr[i].second, ends[i], minApproachSpeed, diffMult, amount)
            curr[i] = Pair(newFirst, newEnd)
        }
    }

    fun updateLimits(entity: SectorEntityToken, exclude: SectorEntityToken?, diffMult: Float) {
        //if (true) return;

        for (i in 0..<resolution) {
            limits[i] = maxRange
            ends[i] = 0f
        }

        isAnythingShortened = false

        for (iterEntity in entity.containingLocation.planets + entity.containingLocation.getEntitiesWithTag(tag)) {
            if (iterEntity == entity || iterEntity == exclude) continue
            val dist = Misc.getDistance(entity.location, iterEntity.location)
            if (dist > maxRange) continue

            var graceRadius: Float = iterEntity.radius + 100f
            var span = Misc.computeAngleSpan(iterEntity.radius + graceRadius, dist)

            var angle = Misc.getAngleInDegrees(entity.location, iterEntity.location)

            val offsetSize = maxRange * 0.2f

            var spanOffset = span * 0.4f / diffMult
            spanOffset = 0f

            val orbit = iterEntity.orbit
            /*if (orbit != null) {
                angle += ((span * 0.4f) * sign(orbit.orbitalPeriod)) * diffMult
            }*/

            var f = angle - span / 2f - spanOffset
            while (f <= angle + span / 2f - spanOffset) {
                var offset: Float = abs((f - angle).toFloat()) / (span / 2f)
                if (offset > 1) offset = 1f
                offset = (1f - cos((offset * 3.1416f / 2f).toDouble()).toFloat())
                //offset = (float) Math.sqrt(offset);
//				offset += 1f;
                offset *= offset
                //				offset *= offset;
//				offset -= 1f;
                //offset *= planet.getRadius() + graceRadius;
                offset *= offsetSize


                val index = getIndexForAngle(f)
                val limit = min((dist - (iterEntity.radius) + offset).toDouble(), limits[index].toDouble()).toFloat()
                limits[index] = limit
                ends[index] = max(limit + (iterEntity.radius * 5f), ends[index])

                isAnythingShortened = true
                f += degreesPerUnit
            }
        }

        wasUpdated = true
    }

    /*fun block(angle: Float, arc: Float, limit: Float) {
        //float radius = Misc.computeAngleRadius(angle, limit);
        val offsetSize = max(limit.toDouble(), (maxRange * 0.1f).toDouble()).toFloat()

        var f = angle - arc / 2f
        while (f <= angle + arc / 2f) {
            var offset: Float = abs((f - angle).toFloat()) / (arc / 2f)
            if (offset > 1) offset = 1f
            offset = (1f - cos((offset * 3.1416f / 2f).toDouble()).toFloat())
            offset += 1f
            offset *= offset
            offset -= 1f
            offset *= offsetSize
            //offset = 0f;
            offset = abs(offset.toDouble()).toFloat()

            val index = getIndexForAngle(f)
            limits[index] = min((limit + offset).toDouble(), limits[index].toDouble()).toFloat()
            isAnythingShortened = true
            f += degreesPerUnit
        }
    }*/

    fun getIndexForAngle(angle: Float): Int {
        var angle = angle
        angle = Misc.normalizeAngle(angle)

        var index = (angle / 360f * resolution.toFloat()).roundToInt()
        if (index < 0) index = 0
        //if (index > resolution - 1) index = resolution - 1;
        while (index >= resolution) index -= resolution

        return index
    }

    fun isAnythingShortened(): Boolean {
        return isAnythingShortened
    }

}