package org.selkie.kol.impl.combat.subsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.drones.DroneFormation
import org.magiclib.subsystems.drones.MagicDroneSubsystem
import org.magiclib.subsystems.drones.PIDController
import org.selkie.kol.combat.StarficzAIUtils
import org.selkie.kol.combat.StarficzAIUtils.FutureHit
import org.selkie.kol.combat.StarficzAIUtils.HungarianAlgorithm
import org.selkie.kol.impl.helpers.ZeaStaticStrings;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaDrops;
import org.selkie.kol.impl.helpers.ZeaStaticStrings.ZeaStarTypes;import java.awt.Color
import java.util.*
import kotlin.math.abs
import kotlin.math.min

/**
 * Spawns a drone with an Ion Beam. Has no usable key and doesn't take a key index. Blocks wing system, activating it if the ship is venting.
 */
class ShieldDronesSubsystem(ship: ShipAPI?, val numDrones: Int, val isSmart: Boolean) : MagicDroneSubsystem(ship!!) {
    override fun canAssignKey(): Boolean {
        return false
    }

    override fun getBaseActiveDuration(): Float {
        return 0f
    }

    override fun getBaseCooldownDuration(): Float {
        return 2f
    }

    override fun usesChargesOnActivate(): Boolean {
        return false
    }

    override fun getBaseChargeRechargeDuration(): Float {
        return 10f
    }

    override fun shouldActivateAI(amount: Float): Boolean {
        return canActivate()
    }

    override fun onActivate() {
        for (drone in activeWings.keys) {
            if (drone.fluxLevel > 0f) {
                drone.giveCommand(ShipCommand.VENT_FLUX, null, 0)
            }
        }
    }

    override fun getPIDController(): PIDController {
        return PIDController(15f, 3.5f, 10f, 2f)
    }

    override fun advance(amount: Float, isPaused: Boolean) {
        if (isPaused) return
        for (drone in activeWings.keys) {
            if (drone.shield.isOff && !drone.fluxTracker.isOverloadedOrVenting) {
                drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0)
            } else {
                drone.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK)
                val highestFluxLevelRatio = 0.75f
                val ratio = drone.fluxLevel
                val red = BASE_SHIELD_COLOR.red * MathUtils.clamp((1f - ratio) / highestFluxLevelRatio, 0f, 1f) + HIGHEST_FLUX_SHIELD_COLOR.red * MathUtils.clamp(ratio / highestFluxLevelRatio, 0f, 1f)
                val green = BASE_SHIELD_COLOR.green * MathUtils.clamp((1f - ratio) / highestFluxLevelRatio, 0f, 1f) + HIGHEST_FLUX_SHIELD_COLOR.green * MathUtils.clamp(ratio / highestFluxLevelRatio, 0f, 1f)
                val blue = BASE_SHIELD_COLOR.blue * MathUtils.clamp((1f - ratio) / highestFluxLevelRatio, 0f, 1f) + HIGHEST_FLUX_SHIELD_COLOR.blue * MathUtils.clamp(ratio / highestFluxLevelRatio, 0f, 1f)
                drone.shield.innerColor = Color(MathUtils.clamp(red / 255f, 0f, 1f),
                        MathUtils.clamp(green / 255f, 0f, 1f),
                        MathUtils.clamp(blue / 255f, 0f, 1f),
                        SHIELD_ALPHA)
            }
        }
    }

    override fun getDisplayText(): String {
        return "Chiwen Shield Drones"
    }

    public override fun getMaxCharges(): Int {
        return 0
    }

    override fun getMaxDeployedDrones(): Int {
        return numDrones
    }

    override fun getDroneVariant(): String {
        return ZeaStaticStrings.ZEA_DAWN_CHIWEN_WING;
    }

    override fun getDroneFormation(): DroneFormation {
        return if(isSmart) FacingSpinningCircleFormation() else SpinningCircleFormation()
    }

    private inner class FacingSpinningCircleFormation : DroneFormation() {

        private var currentRotation = 0f
        private val droneAIInterval = IntervalUtil(0.3f, 0.5f)
        private var lastUpdatedTime = 0f
        private var combinedHits: MutableList<FutureHit> = ArrayList()
        private var omniShieldDirections: List<Float> = ArrayList()
        override fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float) {
            val activeDrones: MutableList<ShipAPI> = ArrayList()
            val ventingDrones: MutableList<ShipAPI> = ArrayList()
            for (drone in drones.keys) {
                if (drone.fluxLevel < 0.8f && !drone.fluxTracker.isOverloadedOrVenting) activeDrones.add(drone) else {
                    ventingDrones.add(drone)
                    drone.fluxTracker.ventFlux()
                }
            }
            droneAIInterval.advance(amount)
            if (droneAIInterval.intervalElapsed()) {
                lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false)
                combinedHits = ArrayList()
                combinedHits.addAll(StarficzAIUtils.incomingProjectileHits(ship, ship.location))
                combinedHits.addAll(StarficzAIUtils.generatePredictedWeaponHits(ship, ship.location, 3f))

                // prefilter expensive one time functions
                val candidateShieldDirections: MutableList<Float> = ArrayList()
                val FUZZY_RANGE = 0.3f
                for (hit in combinedHits) {
                    var tooCLose = false
                    for (candidateDirection in candidateShieldDirections) {
                        if (abs((hit.angle - candidateDirection).toDouble()) < FUZZY_RANGE) {
                            tooCLose = true
                            break
                        }
                    }
                    if (!tooCLose) candidateShieldDirections.add(hit.angle)
                }
                if (candidateShieldDirections.isEmpty()) candidateShieldDirections.add(ship.facing)
                omniShieldDirections = candidateShieldDirections
            }

            // calculate how much damage the ship would take if shields went down
            val currentTime = Global.getCombatEngine().getTotalElapsedTime(false)
            val timeElapsed = currentTime - lastUpdatedTime
            val armor = StarficzAIUtils.getCurrentArmorRating(ship)
            val droneAngles: MutableList<Float> = ArrayList()
            droneAngles.addAll(omniShieldDirections)
            val bestLowestTime = Float.POSITIVE_INFINITY
            val blockedDirections: MutableList<Float> = ArrayList()

            // get as many drone angles as we have drones
            for (i in activeDrones.indices) {
                val potentialAngles: MutableList<Triple<Float, Float, Float>> = ArrayList()
                // for each angle where there is incoming damage
                for (droneAngle in droneAngles) {
                    var damageBlocked = 0f
                    var lowestTime = Float.POSITIVE_INFINITY
                    // for each future hit check if its blocked, if so at what time and how much
                    for (hit in combinedHits) {
                        // skip damage already blocked by other drones
                        var alreadyBlocked = false
                        for (block in blockedDirections) {
                            if (Misc.isInArc(block, DRONE_ARC, hit.angle)) {
                                alreadyBlocked = true
                                break
                            }
                        }
                        if (alreadyBlocked) continue

                        // if the drone can block it, note down that stats
                        if (Misc.isInArc(droneAngle, DRONE_ARC, hit.angle) && hit.timeToHit > timeElapsed + 0.15f) {
                            val trueDamage = StarficzAIUtils.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armor, ship)
                            damageBlocked += trueDamage.one + trueDamage.two
                            lowestTime = min(lowestTime.toDouble(), (hit.timeToHit - timeElapsed).toDouble()).toFloat()
                        }
                    }
                    // add the potential angles to a list
                    if (damageBlocked > 0) potentialAngles.add(Triple(droneAngle, lowestTime, damageBlocked))
                }

                // find the best direction and add it to blocked directions
                potentialAngles.sortWith(Comparator { (_, second, third), (_, second1, third1) -> // sort by closest damage, with every 1000 damage = 1 second closer
                    val damageTimeRatio = 1000f
                    if (second - third / damageTimeRatio < second1 - third1 / damageTimeRatio) {
                        -1
                    } else {
                        1
                    }
                })
                if (!potentialAngles.isEmpty()) {
                    blockedDirections.add(potentialAngles[0].first)
                }
            }

            // rotate idle
            currentRotation += ROTATION_SPEED * amount
            if (!activeDrones.isEmpty()) {
                // generate all locations by doubling up on locations if too many drones
                val droneLocations: MutableList<Vector2f> = ArrayList()
                var droneDistance = ship.shieldRadiusEvenIfNoShield * 0.95f
                for (i in activeDrones.indices) {
                    var droneAngle: Float = if (blockedDirections.isEmpty()) {
                        currentRotation + 360f / activeDrones.size * i
                    } else {
                        blockedDirections[i % blockedDirections.size]
                    }
                    droneLocations.add(MathUtils.getPointOnCircumference(ship.location, droneDistance, droneAngle))
                    if (!blockedDirections.isEmpty() && i % blockedDirections.size == blockedDirections.size - 1) droneDistance += 30f
                }

                // fill weights for Hungarian Algorithm
                val weights = Array(activeDrones.size) { FloatArray(droneLocations.size) }
                for (i in activeDrones.indices) {
                    val weightRow = FloatArray(droneLocations.size)
                    for (j in droneLocations.indices) {
                        weightRow[j] = Misc.getDistance(droneLocations[j], activeDrones[i].location)
                    }
                    weights[i] = weightRow
                }

                // execute and move drones
                val algo = HungarianAlgorithm(weights)
                val results = algo.execute()
                for (i in activeDrones.indices) {
                    val drone = activeDrones[i]
                    drones[drone]!!.move(droneLocations[results[i]], drone)
                    drones[drone]!!.rotate(Misc.getAngleInDegrees(ship.location, drone.location), drone)
                }
            }
            if (!ventingDrones.isEmpty()) {
                // deal with venting drones
                val droneVentingLocations: MutableList<Vector2f> = ArrayList()
                for (i in ventingDrones.indices) {
                    droneVentingLocations.add(MathUtils.getPointOnCircumference(ship.location, ship.shieldRadiusEvenIfNoShield * 0.3f, currentRotation + 360f / ventingDrones.size * i))
                }

                // move each drone to its closest location
                // fill weights for Hungarian Algorithm
                val ventingWeights = Array(ventingDrones.size) { FloatArray(droneVentingLocations.size) }
                for (i in ventingDrones.indices) {
                    val weightRow = FloatArray(droneVentingLocations.size)
                    for (j in droneVentingLocations.indices) {
                        weightRow[j] = Misc.getDistance(droneVentingLocations[j], ventingDrones[i].location)
                    }
                    ventingWeights[i] = weightRow
                }

                // execute and move drones
                val ventAlgo = HungarianAlgorithm(ventingWeights)
                val ventResults = ventAlgo.execute()
                for (i in ventingDrones.indices) {
                    val drone = ventingDrones[i]
                    drones[drone]!!.move(droneVentingLocations[ventResults[i]], drone)
                    drones[drone]!!.rotate(Misc.getAngleInDegrees(ship.location, drone.location), drone)
                }
            }
        }
    }


    private inner class SpinningCircleFormation : DroneFormation() {
        private var currentRotation = 0f
        override fun advance(ship: ShipAPI, drones: Map<ShipAPI, PIDController>, amount: Float) {

            // rotate idle
            currentRotation += ROTATION_SPEED * amount

            // generate all locations by doubling up on locations if too many drones
            val droneLocations: MutableList<Vector2f> = ArrayList()
            val droneDistance = ship.shieldRadiusEvenIfNoShield * 1.15f
            for (i in 0 until drones.size) {
                val droneAngle = currentRotation + 360f / drones.size * i
                droneLocations.add(MathUtils.getPointOnCircumference(ship.location, droneDistance, droneAngle))
            }
            val activeDrones: List<ShipAPI> = ArrayList(drones.keys)
            // fill weights for Hungarian Algorithm
            val weights = Array(activeDrones.size) { FloatArray(droneLocations.size) }
            for (i in activeDrones.indices) {
                val weightRow = FloatArray(droneLocations.size)
                for (j in droneLocations.indices) {
                    weightRow[j] = Misc.getDistance(droneLocations[j], activeDrones[i].location)
                }
                weights[i] = weightRow
            }

            // execute and move drones
            val algo = HungarianAlgorithm(weights)
            val results = algo.execute()
            for (i in activeDrones.indices) {
                val drone = activeDrones[i]
                drones[drone]!!.move(droneLocations[results[i]], drone)
                drones[drone]!!.rotate(Misc.getAngleInDegrees(ship.location, drone.location), drone)
            }
        }
    }


    companion object {
        private val BASE_SHIELD_COLOR = Color.cyan
        private val HIGHEST_FLUX_SHIELD_COLOR = Color.red
        private const val SHIELD_ALPHA = 0.25f

        private const val DRONE_ARC = 30f
        private const val ROTATION_SPEED = 20f
    }
}
