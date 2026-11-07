package org.selkie.zea.combat.subsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Planets
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaStarTypes

/**
 * Stellar charge that powers the Elysian boss subsystems (Radiance, Passive IFF Disruptor), carried over from the
 * retired Coronal Capacitor: charges at a rate set by nearby stars, faster while venting, pauses while non-PD
 * weapons fire, and drains by the flux those weapons cost. [level] runs 0 (empty) to 1 (full); it starts empty.
 */
class CoronalCharge(private val ship: ShipAPI) {
    companion object {
        private const val MIN_CHARGEMULT = 0.2f // multiplier of charge rate in no-light conditions
        private const val MAX_CHARGEMULT = 4f // multiplier of charge rate in full light
        private const val MAX_CHARGEFLUX = 1.0f // capacity as a multiplier on the ship's max flux
        private const val FLUX_CHARGERATE = 0.05f // base fraction of capacity gained per second

        private val STAR_LUX: Map<String, Float> = mapOf(
            StarTypes.ORANGE_GIANT to 3f,
            StarTypes.RED_GIANT to 2.5f,
            StarTypes.RED_SUPERGIANT to 2.5f,
            StarTypes.RED_DWARF to 2.5f,
            StarTypes.BROWN_DWARF to 0.25f,
            StarTypes.ORANGE to 3f,
            StarTypes.YELLOW to 3f,
            StarTypes.WHITE_DWARF to 2f,
            StarTypes.BLUE_GIANT to 10f,
            StarTypes.BLUE_SUPERGIANT to 10f,
            StarTypes.BLACK_HOLE to 0.1f,
            StarTypes.NEUTRON_STAR to 25f,
            ZeaStarTypes.ZEA_WHITE_HOLE to 100f,
            ZeaStarTypes.ZEA_RED_HOLE to 3f,
            ZeaStarTypes.US_STAR_BLUE_GIANT to 10f,
            ZeaStarTypes.US_STAR_YELLOW to 3f,
            ZeaStarTypes.US_STAR_ORANGE_GIANT to 3f,
            ZeaStarTypes.US_STAR_RED_GIANT to 2.5f,
            ZeaStarTypes.US_STAR_WHITE to 2f,
            ZeaStarTypes.US_STAR_BROWNDWARF to 0.25f,
            ZeaStarTypes.TIANDONG_SHAANXI to 2f,
            ZeaStarTypes.STAR_BRSTAR to 3f,
            ZeaStarTypes.STAR_YELLOW_SUPERGIANT to 3f,
            ZeaStarTypes.QUASAR to 8f
        ) // unlisted types (nebula centres, black neutron star, ...) count as 1

        /** Charge rate (fraction of capacity per second) at the player fleet's current position. */
        private fun stellarChargeRate(): Float {
            val fleet = Global.getSector()?.playerFleet ?: return FLUX_CHARGERATE
            if (fleet.isInHyperspace) return FLUX_CHARGERATE * MIN_CHARGEMULT
            val system = fleet.containingLocation as? StarSystemAPI ?: return FLUX_CHARGERATE * MIN_CHARGEMULT
            if (system.star == null) return FLUX_CHARGERATE * MIN_CHARGEMULT
            val lux = listOfNotNull(system.star, system.secondary, system.tertiary).sumOf { starIntensity(it, fleet.location).toDouble() }.toFloat()
            return FLUX_CHARGERATE * lux.coerceIn(MIN_CHARGEMULT, MAX_CHARGEMULT)
        }

        private fun starIntensity(star: PlanetAPI, fleetLoc: Vector2f): Float {
            val lux = STAR_LUX[star.typeId] ?: 1f
            val distance = Misc.getDistance(star.location, fleetLoc)
            if (distance < star.radius + 500) return lux * 5f // inside the corona for most stars
            return lux * star.radius / distance
        }
    }

    private val chargeRate = stellarChargeRate()
    private val beams = mutableListOf<WeaponAPI>()
    private val fluxCounted = HashMap<WeaponAPI, Boolean>()
    private var stored = 0f

    var level = 0f
        private set

    init {
        for (weapon in ship.allWeapons) {
            if (weapon.isDecorative) continue
            if (weapon.isBeam && !weapon.isBurstBeam) beams.add(weapon) else fluxCounted[weapon] = true
        }
    }

    fun advance(elapsed: Float) {
        var fluxPerSecond = 0f
        var flatFlux = 0f
        var charging = true

        for (weapon in beams) {
            if (weapon.chargeLevel >= 0.2f) {
                fluxPerSecond += weapon.fluxCostToFire
                if (!weapon.hasAIHint(WeaponAPI.AIHints.PD)) charging = false
            }
        }
        for (weapon in fluxCounted.keys) {
            if (weapon.chargeLevel >= 1f) {
                if (!fluxCounted[weapon]!!) {
                    fluxCounted[weapon] = true
                    flatFlux += weapon.fluxCostToFire
                    if (!weapon.hasAIHint(WeaponAPI.AIHints.PD)) charging = false
                }
            } else if (!weapon.isInBurst) {
                fluxCounted[weapon] = false
            }
        }

        var rate = chargeRate
        if (ship.fluxTracker.isVenting) rate *= ship.mutableStats.ventRateMult.modifiedValue * 2
        if (!charging) rate = 0f

        val pool = MAX_CHARGEFLUX * ship.maxFlux
        if (pool <= 0f) return
        stored = (stored - flatFlux - fluxPerSecond * elapsed + pool * rate * elapsed).coerceIn(0f, pool)
        level = stored / pool
    }
}
