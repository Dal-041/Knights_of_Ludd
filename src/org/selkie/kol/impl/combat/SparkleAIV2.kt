package org.selkie.kol.impl.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.impl.helpers.ZeaStaticStrings
import org.selkie.kol.impl.hullmods.DuskBuiltin
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
    }

    var elapsed = 0f
    val tracker = IntervalUtil(0.05f, 0.1f)
    val randomFloat = Math.random().toFloat()
    val offset = (Math.random() * 3.1415927410125732 * 2.0).toFloat()
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

    private fun doFlocking() {
        if (missile.source == null) return
        val source = missile.source
        val engine = Global.getCombatEngine()
        val hullSize: HullSize = source.hullSize
        var avoidRange = AVOID_RANGE
        val cohesionRange = COHESION_RANGE
        val sourceRejoin = source.collisionRadius + SOURCE_REJOIN * DIST_MULT[hullSize] as Float
        val sourceRepel = source.collisionRadius + SOURCE_REPEL * DIST_MULT[hullSize] as Float
        val sourceCohesion = source.collisionRadius + SOURCE_COHESION * DIST_MULT[hullSize] as Float
        val sin = sin(Global.getCombatEngine().getTotalElapsedTime(false).toDouble()).toFloat()
        val mult = 1f + sin * 0.25f
        avoidRange *= mult
        val total = Vector2f()

        //Mote-mote logic
        for (otherMissile in DuskBuiltin.allMotes) {
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

        //Mote-Source logic
        val dist = Misc.getDistance(missile.location, source.location)
        if (dist > sourceRejoin) {
            val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.location, source.location))
            val f = dist / (sourceRejoin + 400f * DIST_MULT[hullSize] as Float) - 1f
            dir.scale(f * 0.5f)
            Vector2f.add(total, dir, total)
        }
        if (dist < sourceRepel) {
            val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.location, missile.location))
            val f = 1f - dist / sourceRepel
            dir.scale(f * 5f)
            Vector2f.add(total, dir, total)
        }
        if (dist < sourceCohesion && source.velocity.length() > 20f) {
            val dir = Vector2f(source.velocity)
            Misc.normalise(dir)
            val f = 1f - dist / sourceCohesion
            dir.scale(f * 1f)
            Vector2f.add(total, dir, total)
        }

        // if not strongly going anywhere, circle the source ship; only kicks in for lone motes
        if (true) {
            val offset = if (randomFloat > 0.5f) 90f else -90f
            val dir = Misc.getUnitVectorAtDegreeAngle(
                Misc.getAngleInDegrees(missile.location, source.location) + offset
            )
            val f = 0.2f
            if (total.length() > 0.2f){
                dir.scale(min(f * 1f/total.length(), 1f))
            }
            Vector2f.add(total, dir, total)
        }

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
                ((target as MissileAPI).source?.variant?.hasTag(ZeaStaticStrings.BOSS_TAG) == true || Global.getSettings().isDevMode)
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
        for (mote in DuskBuiltin.allMotes) {
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