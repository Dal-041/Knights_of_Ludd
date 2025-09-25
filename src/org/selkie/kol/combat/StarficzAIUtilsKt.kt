package org.selkie.kol.combat

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.*
import org.lwjgl.util.vector.Vector2f
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object StarficzAIUtilsKt {

    /**
     * A self-contained object representing the pre-calculated, ship-relative DPS profile.
     * Once created via the top-level `createShipDpsProfile` function, you can call
     * `getInterpolatedDpsForLocation` on this object to get a danger value.
     *
     * @param dpsGrid The pre-calculated grid of DPS values [Range -> [Angle -> DPS]].
     * @param maxRange The maximum range of the ship's relevant weaponry.
     * @param angleStep The angular resolution of the grid.
     * @param rangeStep The range resolution of the grid.
     */
    class DpsProfile(
        val ship: ShipAPI,
        private val dpsGrid: Map<Int, Map<Int, Float>>, // int indices
        val maxRange: Float,
        private val angleStep: Float,
        private val rangeStep: Float
    ) {
        private val maxRangeIndex = (maxRange / rangeStep).toInt()
        private val numAngleIndices = (360f / angleStep).toInt()

        fun getDpsDangerAtLocation(testPoint: Vector2f): Float {
            val relativeVector = testPoint - ship.location
            val range = relativeVector.length()
            if (range > this.maxRange) return 0f

            val worldAngle = VectorUtils.getAngle(ship.location, testPoint)
            val relativeAngle = Misc.normalizeAngle(worldAngle - ship.facing)

            val rangePos = range / this.rangeStep
            val r1 = floor(rangePos).toInt()
            val rangeFraction = rangePos - r1

            val anglePos = relativeAngle / this.angleStep
            val a1 = floor(anglePos).toInt()
            val angleFraction = anglePos - a1

            // Get the four corner points of the grid cell we are in
            val r2 = min(r1 + 1, maxRangeIndex)
            val a2 = (a1 + 1) % numAngleIndices

            // Get the DPS values at the four corners
            val q11 = dpsGrid[r1]?.get(a1) ?: 0f // (range1, angle1)
            val q12 = dpsGrid[r1]?.get(a2) ?: 0f // (range1, angle2)
            val q21 = dpsGrid[r2]?.get(a1) ?: 0f // (range2, angle1)
            val q22 = dpsGrid[r2]?.get(a2) ?: 0f // (range2, angle2)

            // Interpolate along the angle axis first
            val dpsAtRange1 = Misc.interpolate(q11, q12, angleFraction)
            val dpsAtRange2 = Misc.interpolate(q21, q22, angleFraction)

            // Then interpolate along the range axis for the final result
            return Misc.interpolate(dpsAtRange1, dpsAtRange2, rangeFraction)
        }
    }


    /**
     * Factory function that performs the one-time, expensive calculation of a ship's intrinsic DPS profile.
     *
     * @param ship The ship to create a profile for.
     * @param angleStep The angular resolution of the profile grid in degrees.
     * @param rangeStep The range resolution of the profile grid in world units.
     * @return A DpsProfile object which can be queried repeatedly for interpolated danger values.
     */
    fun createShipDpsProfile(
        ship: ShipAPI,
        angleStep: Float = 10f,
        rangeStep: Float = 100f
    ): DpsProfile {
        val dpsGrid = mutableMapOf<Int, MutableMap<Int, Float>>()
        var maxRange = 0f

        for (weapon in ship.allWeapons) {
            if (!weapon.isDecorative &&
                weapon.type != WeaponAPI.WeaponType.MISSILE &&
                !weapon.hasAIHint(WeaponAPI.AIHints.PD)
            ) {
                maxRange = max(weapon.range, maxRange)
            }
        }

        val maxRangeIndex = (maxRange / rangeStep).toInt()
        val maxAngleIndex = (360f / angleStep).toInt()

        for (r in 0..maxRangeIndex) {
            val dpsAtRange = mutableMapOf<Int, Float>()
            for (a in 0 until maxAngleIndex) {
                val currentRange = r * rangeStep
                val currentAngle = a * angleStep
                var potentialDPS = 0f

                for (weapon in ship.allWeapons) {
                    if (!weapon.isDecorative && weapon.type != WeaponAPI.WeaponType.MISSILE) {
                        val shipRelativeWeaponFacing = Misc.normalizeAngle(weapon.arcFacing - ship.facing)
                        if (Misc.isInArc(shipRelativeWeaponFacing, weapon.arc, currentAngle) &&
                            currentRange <= weapon.range
                        ) {
                            potentialDPS += if (weapon.spec.burstSize <= 1 || weapon.derivedStats.burstFireDuration <= 0f) {
                                weapon.derivedStats.dps
                            } else {
                                weapon.derivedStats.burstDamage / weapon.derivedStats.burstFireDuration
                            }
                        }
                    }
                }
                dpsAtRange[a] = potentialDPS
            }
            dpsGrid[r] = dpsAtRange
        }

        return DpsProfile(ship, dpsGrid, maxRange, angleStep, rangeStep)
    }
}