package org.selkie.zea.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys
import org.selkie.zea.hullmods.DuskBuiltin
import java.awt.Color
import kotlin.math.*

class SparkleAIV2(val missile: MissileAPI) : MissileAIPlugin {

    companion object{
        const val SOURCE_REJOIN = 100f //200f distance added to the collision radius that determines target radius for sparkles returning
        const val SOURCE_REPEL = 50f //50f distance added to the collision radius that determines the radius below which sparkles
        const val SOURCE_COHESION = 600f //600f stickiness
        const val AVOID_RANGE = 50f
        const val COHESION_RANGE = 100f //100f Distance under which sparkles will cohere
        const val COHESION_STRENGTH = 0.3f

        const val MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD = 400f

        val baseColor = Color(100, 165, 255, 175)
        val baseEMPColor = Color(100, 165, 255, 255)

        val hfColor = Color(220, 80, 100, 225)
        val hfEMPColor = Color(255, 80, 100, 255)

        val DIST_MULT = mapOf(
            HullSize.FIGHTER to 0.125f,
            HullSize.FRIGATE to 0.17f,
            HullSize.DESTROYER to 0.25f,
            HullSize.CRUISER to 0.4f,
            HullSize.CAPITAL_SHIP to 0.6f
        )

        const val MAX_CATCHUP_SPEED_BONUS = 100f
        const val MAX_ANGLE_FOR_SPEED_BOOST = 45f
    }
    // NEW: Helper data class to hold ellipse parameters
    data class EllipseParameters(
        val center: Vector2f,
        val semiMajorAxis: Float,
        val semiMinorAxis: Float,
        val angle: Float // in degrees
    )

    var elapsed = 0f
    val tracker = IntervalUtil(0.05f, 0.1f)
    val randomFloat = Math.random().toFloat()
    val offset: Double = (Math.random() * 3.1415927410125732 * 2.0)
    var hfLevel = 0f
    var hfOverride = false
    var target: CombatEntityAPI? = null

    override fun advance(amount: Float) {
        if (missile.isFizzling) return
        if (missile.source == null) return

        elapsed += amount

        //HF color logic
        if (missile.source?.hasTag(DuskBuiltin.HF_TAG) == true || hfOverride){
            hfLevel = min(hfLevel + elapsed, 1f)
            missile.setJitter(this, hfColor, hfLevel, 1, missile.glowRadius)
            missile.engineController.fadeToOtherColor(this, hfColor, hfColor, 2f, 0.75f)
        } else{
            hfLevel = max(hfLevel - elapsed, 0f)
            missile.setJitter(this, baseColor, hfLevel, 1, missile.glowRadius)
            //missile.engineController.fadeToOtherColor(this, baseColor, baseColor, 2f, 0.75f)
        }

        if (elapsed >= 0.5f) {
            val wantToFlock: Boolean = !isTargetValid()
            if (wantToFlock) {
                doFlocking()
                hfOverride = false
            } else {
                val engine = Global.getCombatEngine()
                val targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50f)
                engine.headInDirectionWithoutTurning(missile, Misc.getAngleInDegrees(missile.location, targetLoc), 10000f)
                val correctAngle = VectorUtils.getAngle(missile.location, targetLoc)
                val aimAngle = MathUtils.getShortestRotation(missile.facing, correctAngle)
                if (aimAngle < 0.0f) {
                    missile.giveCommand(ShipCommand.TURN_RIGHT)
                } else {
                    missile.giveCommand(ShipCommand.TURN_LEFT)
                }
                if (abs(aimAngle.toDouble()) < abs(missile.angularVelocity.toDouble()) * 0.1f) {
                    missile.angularVelocity = aimAngle / 0.1f
                }
                missile.engineController.forceShowAccelerating()
            }
        }

        tracker.advance(amount)
        if (tracker.intervalElapsed()) {
            if (elapsed >= 0.5f) {
                acquireNewTargetIfNeeded()
            }
        }
    }

    private fun getAllShips(source: ShipAPI): List<ShipAPI> {
        if (source.isShipWithModules) {
            return source.childModulesCopy + source
        } else if(source.isStationModule){
            return source.parentStation.childModulesCopy + source.parentStation
        } else{
            return listOf(source)
        }
    }

    // REVISED function to calculate the enclosing ellipse using the AI's constants
    private fun calculateEnclosingEllipse(ships: List<ShipAPI>, buffer: Float): EllipseParameters {
        if (ships.isEmpty()) {
            return EllipseParameters(Vector2f(), 0f, 0f, 0f)
        }
        if (ships.size == 1) {
            val ship = ships.first()
            val radius = ship.collisionRadius + buffer
            return EllipseParameters(ship.location, radius, radius, 0f)
        }

        // Find the two ships that are furthest apart to define the major axis.
        var maxDistSq = -1f
        var head: ShipAPI = ships[0]
        var tail: ShipAPI = ships[1]

        for (i in ships.indices) {
            for (j in i + 1 until ships.size) {
                val distSq = Misc.getDistanceSq(ships[i].location, ships[j].location)
                if (distSq > maxDistSq) {
                    maxDistSq = distSq
                    head = ships[i]
                    tail = ships[j]
                }
            }
        }

        val center = Vector2f.add(head.location, tail.location, null).scale(0.5f) as Vector2f
        val angle = Misc.getAngleInDegrees(head.location, tail.location)
        // The semi-major axis is half the distance between the end-points, plus their radii, plus the buffer.
        val semiMajorAxis = sqrt(maxDistSq) / 2f + (head.collisionRadius + tail.collisionRadius) / 2f + buffer

        // Find the ship furthest from the major axis to define the minor axis.
        var maxDistFromMajorAxis = 0f
        for (ship in ships) {
            // Consider the ship's own size in the calculation
            val dist = Misc.distanceFromLineToPoint(head.location, tail.location, ship.location) + ship.collisionRadius
            if (dist > maxDistFromMajorAxis) {
                maxDistFromMajorAxis = dist
            }
        }

        // The semi-minor axis is the max distance found, plus the buffer.
        val semiMinorAxis = maxDistFromMajorAxis + buffer

        return EllipseParameters(center, semiMajorAxis, semiMinorAxis, angle)
    }

    private fun doFlocking() {
        if (missile.source == null) return
        val source = missile.source
        val engine = Global.getCombatEngine()
        val hullSize: HullSize = source.hullSize
        var avoidRange = AVOID_RANGE
        val cohesionRange = COHESION_RANGE
        val sin = sin(Global.getCombatEngine().getTotalElapsedTime(false).toDouble()).toFloat()
        val mult = 1f + sin * 0.25f
        avoidRange *= mult
        val total = Vector2f()

        var bonus = 0f // Default to zero bonus.
        val sourceVelocity = if(source.isStationModule) source.parentStation.velocity else source.velocity
        val sourceMaxSpeed = if(source.isStationModule) source.parentStation.maxSpeed else source.maxSpeed

        // Check if the source ship is moving meaningfully.
        if (sourceVelocity.length() > 20f) {
            val sourceVelAngle = VectorUtils.getFacing(sourceVelocity)
            val moteVelAngle = VectorUtils.getFacing(missile.velocity)
            val angleDiff = MathUtils.getShortestRotation(moteVelAngle, sourceVelAngle)

            // Calculate how aligned the mote is, from 1.0 (perfectly aligned) to 0.0 (at the max angle).
            // This is the "throttle" percentage for our bonus.
            val alignmentFactor = MathUtils.clamp(1f - (abs(angleDiff) / MAX_ANGLE_FOR_SPEED_BOOST), 0f, 1f)

            // Calculate the maximum possible bonus based on how fast the source ship is moving.
            val speedRatio = sourceVelocity.length() / sourceMaxSpeed
            val maxPossibleBonus = MAX_CATCHUP_SPEED_BONUS * MathUtils.clamp(speedRatio, 0f, 1f)

            // The final bonus is the max possible bonus, scaled by the alignment factor.
            bonus = maxPossibleBonus * alignmentFactor
        }

        // Apply the final calculated bonus. If conditions aren't met, bonus will be 0,
        // which effectively removes any existing bonus from this source.
        missile.engineStats.maxSpeed.modifyPercent("motecatchup", bonus)

        //Mote-mote logic
        for (otherMissile in DuskBuiltin.getAllMotes()) {
            if (otherMissile === missile) continue
            var dist = Misc.getDistanceSq(missile.location, otherMissile.location)
            if(dist > max(avoidRange, cohesionRange).pow(2)) continue
            dist = sqrt(dist)

            if (dist < avoidRange) {
                val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(otherMissile.location, missile.location))
                val f = 1f - dist / avoidRange
                dir.scale(f)
                Vector2f.add(total, dir, total)
            }
            if (dist < cohesionRange) {
                val dir = Vector2f(otherMissile.velocity)
                Misc.normalise(dir)
                val f = 1f - dist / cohesionRange
                dir.scale(f*COHESION_STRENGTH)
                Vector2f.add(total, dir, total)
            }
        }


        // --- MODIFIED: Mote-Snake interaction logic using original constants ---
        val snakeModules = getAllShips(source)

        // 1. Define the elliptical boundaries based on the original constants.
        // The "rejoin" ellipse is the hard outer boundary.
        val rejoinBuffer = SOURCE_REJOIN * (DIST_MULT[hullSize] ?: 1f)
        val rejoinEllipse = calculateEnclosingEllipse(snakeModules, rejoinBuffer)

        // The "cohesion" ellipse is a larger, softer boundary for velocity matching.
        val cohesionBuffer = SOURCE_COHESION * (DIST_MULT[hullSize] ?: 1f)
        val cohesionEllipse = calculateEnclosingEllipse(snakeModules, cohesionBuffer)

        // 2. Calculate mote's position relative to the REJOIN ellipse.
        val localPos = Vector2f.sub(missile.location, rejoinEllipse.center, null)
        VectorUtils.rotate(localPos, -rejoinEllipse.angle, localPos) // Un-rotate the point

        val normalizedDistance = if (rejoinEllipse.semiMajorAxis > 1f && rejoinEllipse.semiMinorAxis > 1f) {
            (localPos.x.pow(2) / rejoinEllipse.semiMajorAxis.pow(2)) +
                    (localPos.y.pow(2) / rejoinEllipse.semiMinorAxis.pow(2))
        } else {
            0f // Avoid division by zero if ellipse is tiny
        }

        // 3. Apply forces based on elliptical boundaries.

        // REJOIN: If outside the outer ellipse, pull it back in.
        if (normalizedDistance > 1f) {
            val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.location, rejoinEllipse.center))
            // This force mimics the original: strength increases the further out you are.
            val f = sqrt(normalizedDistance) - 1f
            dir.scale(f * 0.5f) // Using the 0.5f factor from the original code.
            Vector2f.add(total, dir, total)
        }

        // REPEL: Repel from the NEAREST module to avoid clipping through the snake.
        var closestModule: ShipAPI? = null
        var minDistSq = Float.MAX_VALUE
        for (module in snakeModules) {
            val distSq = Misc.getDistanceSq(missile.location, module.location)
            if (distSq < minDistSq) {
                minDistSq = distSq
                closestModule = module
            }
        }

        if (closestModule != null) {
            val repelRadius = closestModule.collisionRadius + (SOURCE_REPEL * (DIST_MULT[hullSize] ?: 1f))
            val dist = sqrt(minDistSq)
            if (dist < repelRadius) {
                val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(closestModule.location, missile.location))
                val f = 1f - dist / repelRadius
                dir.scale(f * 5f) // Strong repulsion, same as original.
                Vector2f.add(total, dir, total)
            }
        }

        // COHESION: If inside the larger cohesion ellipse, match the source ship's velocity.
        val distToCenter = Misc.getDistance(missile.location, cohesionEllipse.center)
        // We use the average radius as a simple check for "am I inside the cohesion zone?"
        val avgCohesionRadius = (cohesionEllipse.semiMajorAxis + cohesionEllipse.semiMinorAxis) / 2f
        if (distToCenter < avgCohesionRadius && sourceVelocity.length() > 20f) {
            val dir = Vector2f(sourceVelocity)
            Misc.normalise(dir)
            val f = 1f - distToCenter / avgCohesionRadius
            dir.scale(f * 1f) // Same as original.
            Vector2f.add(total, dir, total)
        }

        // ORBITING: This is the original tangential steering force, now relative to the ellipse center.
        // This provides the "circling" instruction without adding artificial velocity.
        val offset = if (randomFloat > 0.5f) 90f else -90f
        val dir = Misc.getUnitVectorAtDegreeAngle(
            Misc.getAngleInDegrees(rejoinEllipse.center, missile.location) + offset
        )
        val f = 0.2f
        // This logic prevents the orbiting force from overpowering other commands, same as original.
        if (total.length() > 0.2f){
            dir.scale(min(f * 1f/total.length(), 1f))
        } else {
            dir.scale(f)
        }
        Vector2f.add(total, dir, total)

        if (total.length() > 0) {
            val dir = Misc.getAngleInDegrees(total)
            engine.headInDirectionWithoutTurning(missile, dir, 10000f)
            val aimAngle = MathUtils.getShortestRotation(this.missile.facing, dir)
            if (aimAngle < 0.0f) {
                this.missile.giveCommand(ShipCommand.TURN_RIGHT)
            } else {
                this.missile.giveCommand(ShipCommand.TURN_LEFT)
            }
            if (abs(aimAngle.toDouble()) < abs(this.missile.angularVelocity.toDouble()) * 0.1f) {
                this.missile.angularVelocity = aimAngle / 0.1f
            }
            missile.engineController.forceShowAccelerating()
        }
    }

    private fun acquireNewTargetIfNeeded() {
        val engine = Global.getCombatEngine()

        // want to: target nearest missile that is not targeted by another two motes already
        val owner = missile.owner
        var maxMotesPerMissile = 2
        val maxDistFromSourceShip = MAX_DIST_FROM_SOURCE_TO_ENGAGE_AS_PD
        var minDist = Float.MAX_VALUE
        var closest: CombatEntityAPI? = null
        for (other in engine.missiles) {
            val isNinevehMine = other.isMine && other.source?.hullSpec?.hullId?.equals(ZeaStaticStrings.ZEA_BOSS_NINEVENH) == true
            maxMotesPerMissile = if (isNinevehMine) 4 else 2
            if (other.owner == owner && !isNinevehMine) continue
            if (other.owner == 100) continue
            val distToTarget = Misc.getDistance(missile.location, other.location)
            if (distToTarget > minDist) continue
            if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue
            val distFromSource = Misc.getDistance(other.location, missile.source.location)
            if (distFromSource > maxDistFromSourceShip && !isNinevehMine) continue
            if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue
            if (distToTarget < minDist) {
                closest = other
                minDist = distToTarget
            }
        }

        for (other in engine.ships) {
            if (other.owner == owner) continue
            if (other.owner == 100) continue
            if (!other.isFighter) continue
            val distToTarget = Misc.getDistance(missile.location, other.location)
            if (distToTarget > minDist) continue
            if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue
            val distFromSource = Misc.getDistance(other.location, missile.source.location)
            if (distFromSource > maxDistFromSourceShip) continue
            if (getNumMotesTargeting(other) >= maxMotesPerMissile) continue
            if (distToTarget < minDist) {
                closest = other
                minDist = distToTarget
            }
        }
        target = closest

        // hf nineveh boss mines
        hfOverride = target is MissileAPI && (target as MissileAPI).isMine && (target as MissileAPI).source?.hullSpec?.hullId?.equals(ZeaStaticStrings.ZEA_BOSS_NINEVENH) == true &&
                ((target as MissileAPI).source?.variant?.hasTag(ZeaMemKeys.ZEA_BOSS_TAG) == true || Global.getSettings().isDevMode)
    }

    private fun isTargetValid(): Boolean {
        if (target == null || target is ShipAPI && (target as ShipAPI).isPhased) {
            return false
        }
        val engine = Global.getCombatEngine()
        if (target != null && target is ShipAPI && (target as ShipAPI).isHulk) return false
        var list: List<*>? = null
        var isNinevehMine = false
        if (target is ShipAPI) {
            list = engine.ships
        } else {
            list = engine.missiles
            isNinevehMine = (target as MissileAPI).isMine && (target as MissileAPI).source?.hullSpec?.hullId?.equals(ZeaStaticStrings.ZEA_BOSS_NINEVENH) == true
        }
        return target != null && list!!.contains(target) && (target!!.owner != missile.owner || isNinevehMine)
    }

    private fun getNumMotesTargeting(other: CombatEntityAPI): Int {
        var count = 0
        for (mote in DuskBuiltin.getAllMotes()) {
            if (mote === missile) continue
            if (mote.unwrappedMissileAI is SparkleAIV2) {
                val ai = mote.unwrappedMissileAI as SparkleAIV2
                if (ai.target === other) {
                    count++
                }
            }
        }
        return count
    }
}