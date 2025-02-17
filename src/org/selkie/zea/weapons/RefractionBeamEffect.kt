package org.selkie.zea.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.setAlpha
import java.awt.Color
import java.awt.geom.Line2D
import kotlin.math.abs
import kotlin.math.atan2

class RefractionBeamEffect : BeamEffectPlugin {
    val Interval = IntervalUtil(0.1f, 0.1f)
    val SecondaryTargets = mutableMapOf<ShipAPI, ShipAPI>()
    val ReflectionOrderKey = "ReflectionOrder"
    var RefectionOrder = 0
    val reflectionColors = listOf<Color>(Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED)

    override fun advance(amount: Float, engine: CombatEngineAPI, beam: BeamAPI) {


        // limit to 2 reflections
        if(beam.source.isDrone && ReflectionOrderKey in beam.source.customData){
            RefectionOrder = beam.source.customData[ReflectionOrderKey] as Int
        }
        if (RefectionOrder > 0){
            beam.coreColor = reflectionColors[RefectionOrder-1].darker()
            beam.fringeColor = reflectionColors[RefectionOrder-1].brighter()
            beam.fringeColor.setAlpha(155)
            beam.width = 70f - RefectionOrder*9f
        }


        if(RefectionOrder >= 6) return


        val target = beam.damageTarget
        if (target is ShipAPI) {
            val hitShield = target.shield != null && target.shield.isWithinArc(beam.to)
            Interval.advance(amount)
            if(Interval.intervalElapsed() && hitShield){
                val allShips = Global.getCombatEngine().ships
                val blockers = allShips.filter{it.hullSize != ShipAPI.HullSize.FIGHTER && it != target && !it.isDrone &&
                    MathUtils.getDistanceSquared(it.location, beam.to) < beam.weapon.range*beam.weapon.range}
                val targets = blockers.filter{it.owner != beam.source.owner}
                var currentSecondaryTargets = getValidSecondaryTargets(beam.to, target.shieldCenterEvenIfNoShield, targets, blockers)
                val sortedSecondaryTargets = currentSecondaryTargets.sortedBy { -abs(it.second) }
                // limit each reflection to 1 ray
                if(sortedSecondaryTargets.isNotEmpty()) currentSecondaryTargets = mutableListOf(sortedSecondaryTargets[0])

                // add all new secondary targets
                for(currentTarget in currentSecondaryTargets){
                    if(currentTarget.first !in SecondaryTargets){
                        SecondaryTargets[currentTarget.first] = org.selkie.zea.combat.utils.makeNewRedirectionDrone(beam.source, beam.weapon)
                        SecondaryTargets[currentTarget.first]!!.mutableStats.beamWeaponDamageMult.modifyMult("reflectionDecay", (7-RefectionOrder)/7f)
                        SecondaryTargets[currentTarget.first]!!.setCustomData(ReflectionOrderKey, RefectionOrder+1)
                    }
                }

                // remove all invalid targets
                val currentSecondaryShipAPIs = currentSecondaryTargets.map { it.first }.toSet()
                SecondaryTargets.keys.retainAll{
                    if(it !in currentSecondaryShipAPIs) {
                        Global.getCombatEngine().removeEntity(SecondaryTargets[it])
                    }
                    it in currentSecondaryShipAPIs
                }
            }

            if(!hitShield){
                // remove all drones
                SecondaryTargets.keys.retainAll{
                    Global.getCombatEngine().removeEntity(SecondaryTargets[it])
                    false
                }
            }

            for((beamTarget, beamDrone) in SecondaryTargets){
                beamDrone.facing = VectorUtils.getFacing(VectorUtils.getDirectionalVector(beam.to, beamTarget.location))

                val weaponFirePointToBeamIntersect = Vector2f.sub(beam.to, beamDrone.allWeapons[0].getFirePoint(0), null)
                beamDrone.location.set(Vector2f.add(beamDrone.location, weaponFirePointToBeamIntersect, null))

                for(weapon in beamDrone.allWeapons){
                    weapon.setForceFireOneFrame(true)
                    weapon.setFacing(beamDrone.facing)
                    weapon.updateBeamFromPoints()
                }
            }
        }
    }

    fun getValidSecondaryTargets(beamTo: Vector2f, targetLocation: Vector2f,
                                 secondaryTargets: List<ShipAPI>, blockers: List<ShipAPI>): MutableList<Pair<ShipAPI, Float>> {
        val validTargets = mutableListOf<Pair<ShipAPI, Float>>()

        // Compute vector r = beamTo - targetLocation.
        // The tangent at beamTo is perpendicular to this vector.
        val normal = Vector2f()
        Vector2f.sub(beamTo, targetLocation, normal)
        normal.normalise(normal)

        // Iterate over each candidate in the secondary targets list.
        for (candidate in secondaryTargets) {
            // Condition 1: Check that the candidate is on the safe side of the tangent.
            // Compute the vector from beamTo to the candidate's location.
            val toCandidate = Vector2f()
            Vector2f.sub(candidate.location, beamTo, toCandidate)

            // If the dot product is not positive, the candidate is in front of (or too close to)
            // the tangent defined by the primary target.
            if (Vector2f.dot(toCandidate, normal) <= 0f) {
                continue
            }

            // Condition 2: Check for a clear line-of-sight between beamTo and candidate.location.
            var isBlocked = false
            for (blocker in blockers) {
                // Skip the candidate's own ShipAPI instance.
                if (blocker === candidate) continue
                // Compute the squared distance from the blocker's location to the line segment from beamTo to candidate.location.
                val distanceSq = Line2D.ptSegDistSq(
                        beamTo.x.toDouble(), beamTo.y.toDouble(),
                        candidate.location.x.toDouble(), candidate.location.y.toDouble(),
                        blocker.location.x.toDouble(), blocker.location.y.toDouble()
                )
                // Compare with the square of the blocker's collision radius.
                val blockerRadiusSq = blocker.collisionRadius * blocker.collisionRadius
                if (distanceSq <= blockerRadiusSq.toDouble()) {
                    isBlocked = true
                    break
                }
            }

            if (isBlocked) continue

            // Candidate passed both tests. Now compute the reflection angle.
            // First, normalize toCandidate.
            val toCandidateNorm = Vector2f(toCandidate)
            toCandidateNorm.normalise(toCandidateNorm)

            // Compute the 2D cross product (a scalar) and dot product.
            // The cross product in 2D is given by: cross = (normal.x * toCandidateNorm.y - normal.y * toCandidateNorm.x)
            val cross = normal.x * toCandidateNorm.y - normal.y * toCandidateNorm.x
            val dot = Vector2f.dot(normal, toCandidateNorm)

            // Use atan2(cross, dot) to get the signed angle in radians.
            val angleRad = atan2(cross.toDouble(), dot.toDouble())
            // Convert the angle to degrees.
            val angleDeg = Math.toDegrees(angleRad).toFloat()

            // Add the candidate and its reflection angle to the results.
            validTargets.add(Pair(candidate, angleDeg))
        }

        return validTargets
    }
}

