package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import org.dark.shaders.light.StandardLight
import org.lwjgl.util.vector.Vector2f
import org.magiclib.subsystems.MagicSubsystemsManager.addSubsystemToShip
import org.magiclib.subsystems.drones.MagicDroneSubsystem
import org.magiclib.util.MagicUI
import org.selkie.kol.Utils
import org.selkie.kol.hullmods.HullmodBackgroundElement
import org.selkie.kol.impl.combat.activators.PDDroneActivator
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class ElysianBuiltin : BaseHullMod() {
    companion object {
        internal const val CAPACITY_FACTOR_KEY = "\$coronal_cap_modifier"
        internal const val CC_RATE_KEY = "\$zea_cc_recharge"
        internal const val CC_TIME_KEY = "\$zea_cc_times"

        internal const val MIN_CHARGEMULT = 0.2f // multiplier of chargerate in no-light conditions
        internal const val MAX_CHARGEMULT = 4f // multiplier of chargerate in full-light

        internal const val SPEED_BOOST = 0.3f
        internal const val DAMAGE_BOOST = 0.3f
        internal const val ROF_BOOST = 0.3f

        // flux or charge based
        internal const val USING_FLUX = true

        //Time based
        internal const val MAX_CHARGETIME = 10f // in seconds
        internal const val TIME_CHARGERATE = 0.5f // seconds gained per second
        internal const val CHARGE_DELAY = 1f // seconds before charge after ship has stopped firing

        //Flux based
        internal const val MAX_CHARGEFLUX = 1.0f // multiplier on ships max flux
        internal const val FLUX_CHARGERATE = 0.05f // base percentage of bar gained per second

        internal val STAR_LUX: Map<String, Float> = mapOf(
            "star_orange_giant" to 3f,
            "star_red_giant" to 2.5f,
            "star_red_supergiant" to 2.5f,
            "star_red_dwarf" to 2.5f,
            "star_browndwarf" to 0.25f,
            "star_orange" to 3f,
            "star_yellow" to 3f,
            "star_white" to 2f,
            "star_blue_giant" to 10f,
            "star_blue_supergiant" to 10f,
            "black_hole" to 0.1f,
            "star_neutron" to 25f,
            "nebula_center_old" to 0f,
            "nebula_center_average" to 0f,
            "nebula_center_young" to 0f,
            "zea_star_black_neutron" to 0f,
            "zea_white_hole" to 100f,
            "zea_red_hole" to 3f,
            "US_star_blue_giant" to 10f,
            "US_star_yellow" to 3f,
            "US_star_orange_giant" to 3f,
            "US_star_red_giant" to 2.5f,
            "US_star_white" to 2f,
            "US_star_browndwarf" to 0.25f,
            "tiandong_shaanxi" to 2f,
            "star_brstar" to 3f,
            "star_yellow_supergiant" to 3f,
            "quasar" to 8f
        )

        internal fun getSystemStellarIntensity(ship: ShipAPI): Float {
            if (ship.fleetMember == null || ship.fleetMember.fleetData == null || ship.fleetMember.fleetData.fleet == null) return 1f
            val fleet = ship.fleetMember.fleetData.fleet
            if (fleet.isInHyperspace) return MIN_CHARGEMULT
            val system = fleet.containingLocation as StarSystemAPI
            if (system.star == null) return MIN_CHARGEMULT
            val loc = fleet.location
            var lux = 0f
            val primary = system.star
            lux += getStarIntensity(primary, loc)
            var secondary: PlanetAPI
            var tertiary: PlanetAPI
            if (system.secondary != null) {
                lux += getStarIntensity(system.secondary, loc)
            }
            if (system.tertiary != null) {
                lux += getStarIntensity(system.tertiary, loc)
            }
            return max(MIN_CHARGEMULT.toDouble(), min(lux.toDouble(), MAX_CHARGEMULT.toDouble())).toFloat()
        }

        internal fun getSystemStellarIntensity(fleet: CampaignFleetAPI): Float {
            if (fleet.isInHyperspace) return MIN_CHARGEMULT
            val system = fleet.containingLocation as StarSystemAPI
            if (system.star == null) return MIN_CHARGEMULT
            val loc = fleet.location
            var lux = 0f
            val primary = system.star
            lux += getStarIntensity(primary, loc)
            var secondary: PlanetAPI
            var tertiary: PlanetAPI
            if (system.secondary != null) {
                lux += getStarIntensity(system.secondary, loc)
            }
            if (system.tertiary != null) {
                lux += getStarIntensity(system.tertiary, loc)
            }
            return max(MIN_CHARGEMULT.toDouble(), min(lux.toDouble(), MAX_CHARGEMULT.toDouble())).toFloat()
        }

        internal fun getStarIntensity(star: PlanetAPI, fleetLoc: Vector2f?): Float {
            val baseDistance = star.radius * 1f //Intensity values are fairly high, so tweaking this down
            var lux = 0f
            for ((key, value) in STAR_LUX!!) {
                if (key == star.typeId) {
                    lux = value
                }
            }
            if (lux == 0f) lux = 1f // Unknown star type
            if (Misc.getDistance(star.location, fleetLoc) < star.radius + 500) { // Very close, inside corona for most stars
                return lux * 5f // Almost always full rate
            }
            lux *= baseDistance / Misc.getDistance(star.location, fleetLoc)
            return lux
        }

        internal fun refreshRechargeRate(): Float {
            if (Global.getSector() == null || Global.getSector().playerFleet == null) return FLUX_CHARGERATE
            val rechargeRate = FLUX_CHARGERATE * getSystemStellarIntensity(Global.getSector().playerFleet)
            Global.getSector().playerFleet.customData[CC_RATE_KEY] = rechargeRate
            Global.getSector().playerFleet.customData[CC_TIME_KEY] = Global.getSector().clock.timestamp
            return rechargeRate
        }

        internal fun getRechargeRate(): Float {
            return Global.getSector()?.playerFleet?.customData?.get(CC_RATE_KEY) as? Float ?: FLUX_CHARGERATE
        }
    }

    class ElysianBuiltinListener internal constructor(private val ship: ShipAPI) : AdvanceableListener {

        private val engine = Global.getCombatEngine()
        private val fluxCounted = HashMap<WeaponAPI, Boolean>()
        private val beams: MutableList<WeaponAPI> = ArrayList()
        private var engines: MutableList<Pair<EngineSlotAPI, Pair<Color, Color>>> = java.util.ArrayList()

        private var capacitorFactor = 1f // 0-1
        private var capacitorAmount = 0f // Gets verified

        protected var STATUSKEY1 = Any()
        protected var STATUSKEY2 = Any()
        protected var STATUSKEY3 = Any()
        protected var STATUSKEY4 = Any()
        var lightInterval = IntervalUtil(1.5f, 1.5f)
        var glow: StandardLight? = null

        init {
            for (weapon in ship.allWeapons) {
                if (!weapon.isDecorative) {
                    if (weapon.isBeam && !weapon.isBurstBeam) {
                        beams.add(weapon)
                    } else {
                        fluxCounted[weapon] = true
                    }
                }
            }
        }

        override fun advance(amount: Float) {
            var fluxPerSecond = 0f
            var flatFlux = 0f
            var charging = true

            if (engines.size == 0) {
                for (engine in ship.engineController.shipEngines) {
                    if (engine.engineSlot.color.alpha > 10) engines.add(Pair(engine.engineSlot, Pair(engine.engineSlot.color, engine.engineSlot.glowAlternateColor)))
                }
            }

            for (weapon in beams) {
                if (weapon.chargeLevel >= 0.2f) { //firing
                    fluxPerSecond += weapon.fluxCostToFire
                    if (!weapon.hasAIHint(WeaponAPI.AIHints.PD)) charging = false
                }
            }

            for (weapon in fluxCounted.keys) {
                if (weapon.chargeLevel >= 1f) { //firing
                    if (!fluxCounted[weapon]!!) {
                        fluxCounted[weapon] = true
                        flatFlux += weapon.fluxCostToFire
                        if (!weapon.hasAIHint(WeaponAPI.AIHints.PD)) charging = false
                    }
                } else if (!weapon.isInBurst) {
                    fluxCounted[weapon] = false
                }
            }

            var effectiveChargeRate: Float = ElysianBuiltin.getRechargeRate()
            if (ship.fluxTracker.isVenting) {
                effectiveChargeRate *= ship.mutableStats.ventRateMult.getModifiedValue() * 2
            }
            if (!charging) effectiveChargeRate = 0f

            if (USING_FLUX) {
                val fluxPool = MAX_CHARGEFLUX * ship.maxFlux
                val fluxUsed = flatFlux + fluxPerSecond * amount
                capacitorAmount += fluxUsed * -1 + fluxPool * effectiveChargeRate * amount
                capacitorAmount = max(0.0, min(capacitorAmount.toDouble(), fluxPool.toDouble())).toFloat()
                capacitorFactor = capacitorAmount / fluxPool
                MagicUI.drawInterfaceStatusBar(ship, capacitorFactor, Misc.getPositiveHighlightColor(), null, 0f, "BOOST", Math.round(capacitorFactor * 100))
            } else {
                //MagicUI.drawInterfaceStatusBar(ship, capacitorFactor, Misc.getPositiveHighlightColor(), null, 0, "BOOST", Math.round(capacitorFactor*MAX_CHARGETIME));
                //if(charging) {
                //    chargeTime += amount;
                //    if(chargeTime >= CHARGE_DELAY)
                //        capacitorFactor += amount/MAX_CHARGETIME*TIME_CHARGERATE;
                //} else {
                //    chargeTime = 0;
                //    capacitorFactor -= amount/MAX_CHARGETIME;
                //}
            }

            capacitorFactor = max(0.0, min(1.0, capacitorFactor.toDouble())).toFloat()
            for (engineData in engines!!) {
                engineData.one.color = Utils.OKLabInterpolateColor(engineData.two.one, Color(235, 165, 20, 150), capacitorFactor)
                if (engineData.two.two != null) engineData.one.glowAlternateColor = Utils.OKLabInterpolateColor(engineData.two.two, Color(215, 155, 55, 100), capacitorFactor)
            }

            ship.setJitterUnder(
                "coronal_cap" + ship.id, Color(235, 165, 20, 100),
                Utils.linMap(0f, 0.6f, 0.5f, 1f, capacitorFactor), 3, 0f, 15f
            )

            ship.setCustomData(CAPACITY_FACTOR_KEY, capacitorFactor)
            val stats = ship.mutableStats
            stats.maxSpeed.modifyPercent(CAPACITY_FACTOR_KEY, 100 * SPEED_BOOST * capacitorFactor)
            stats.acceleration.modifyPercent(CAPACITY_FACTOR_KEY, 100 * SPEED_BOOST * capacitorFactor)
            stats.deceleration.modifyPercent(CAPACITY_FACTOR_KEY, 100 * SPEED_BOOST * capacitorFactor)
            stats.maxTurnRate.modifyPercent(CAPACITY_FACTOR_KEY, 100 * SPEED_BOOST * capacitorFactor)
            stats.turnAcceleration.modifyPercent(CAPACITY_FACTOR_KEY, 100 * SPEED_BOOST * capacitorFactor)
            stats.energyWeaponDamageMult.modifyMult(CAPACITY_FACTOR_KEY, 1 + DAMAGE_BOOST * capacitorFactor)
            stats.ballisticRoFMult.modifyMult(CAPACITY_FACTOR_KEY, 1 + ROF_BOOST * capacitorFactor)
            stats.ballisticWeaponFluxCostMod.modifyMult(CAPACITY_FACTOR_KEY, 1 / (1 + ROF_BOOST * capacitorFactor))

            if (engine.playerShip === ship) {
                engine.maintainStatusForPlayerShip(
                    STATUSKEY1, Global.getSettings().getSpriteName("icons", "coronal_cap_bottom"),
                    "+" + Math.round(100 * SPEED_BOOST * capacitorFactor) + "% top speed", "improved maneuverability", false
                )
                engine.maintainStatusForPlayerShip(
                    STATUSKEY2, Global.getSettings().getSpriteName("icons", "coronal_cap_middle"),
                    "+" + Math.round(100 * (ROF_BOOST * capacitorFactor)) + "% ballistic rate of fire",
                    "-" + Math.round(100 * (1 - 1 / (1 + ROF_BOOST * capacitorFactor))) + "% ballistic flux use", false
                )
                engine.maintainStatusForPlayerShip(
                    STATUSKEY3, Global.getSettings().getSpriteName("icons", "coronal_cap_top"), "Coronal Capacitor",
                    "+" + Math.round(100 * (DAMAGE_BOOST * capacitorFactor)) + "% energy weapon damage", false
                )
                //engine.maintainStatusForPlayerShip(STATUSKEY4, Global.getSettings().getSpriteName("icons", "coronal_cap_top"),
                //        "", "Local stellar recharge rate: " + effectiveChargeRate*100, false);
            }
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {

        //For save compatibility with old version
        if (stats.variant.hasHullMod("zea_edf_coronal_capacitor")) {
            stats.variant.removeMod("zea_edf_coronal_capacitor")
        }
        if (stats.variant.hasHullMod("zea_edf_pd_drones")) {
            stats.variant.removeMod("zea_edf_pd_drones")
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        if (!ship.hasListenerOfClass(ElysianBuiltinListener::class.java)) ship.addListener(ElysianBuiltinListener(ship))
        if (ship.hullSize != HullSize.FIGHTER) addSubsystemToShip(ship, PDDroneActivator(ship))
    }

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        Global.getSector()?.playerFleet?.let { playerFleet ->
            if (!playerFleet.customData.containsKey(CC_TIME_KEY) || Global.getSector().clock.timestamp - playerFleet.customData[CC_TIME_KEY] as Long > 1024){
                refreshRechargeRate()
            }
        }
    }


    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI, width: Float, isForModSpec: Boolean) {
        val HEIGHT = 64f
        val headingPad = 20f
        val underHeadingPad = 10f
        val listPad = 5f

        val activeTextColor = Misc.getTextColor()
        val activePositiveColor = Misc.getPositiveHighlightColor()
        val activeNegativeColor = Misc.getNegativeHighlightColor()
        val activeHeaderBannerColor = Misc.getDarkPlayerColor()
        val activeHeaderTextColor = Utils.brighter(Misc.getButtonTextColor(), 0.8f)
        val activeHighlightColor = Misc.getHighlightColor()

        val inactiveTextColor = Misc.getGrayColor().darker()
        val inactivePositiveColor = Misc.getGrayColor().darker()
        val inactiveNegativeColor = Misc.getGrayColor().darker()
        val inactiveHeaderBannerColor = Misc.getDarkPlayerColor().darker().darker()
        val inactiveHeaderTextColor = Misc.getGrayColor().darker()
        val inactiveHighlightColor = Misc.getGrayColor().darker()


        val rate = getRechargeRate()
        HullmodBackgroundElement(tooltip, Global.getSettings().getSprite("kol_ui", "zea_edf_hmod"), 0.6f)
        tooltip.addSectionHeading("Coronal Capacitor", activeHeaderTextColor, activeHeaderBannerColor, Alignment.MID, headingPad)
        val coronalCapacitor = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "edf_coronal_cap"), HEIGHT)
        coronalCapacitor.setBulletedListMode("•")
        coronalCapacitor.setBulletWidth(15f)
        //coronalCapacitor.addPara("Utilizes energy from local stars to \nsupercharge weapons and engines.", listPad, activeTextColor, activePositiveColor, "supercharge weapons and engines");
        val para1 = coronalCapacitor.addPara(
            "Stellar energy charges at a rate equal to \n%s in this location. \nActive venting increases this rate by %s.",
            listPad, activeHighlightColor, String.format("%.2f", rate * 100) + "% per second", String.format("%.2f", ship.mutableStats.ventRateMult.getModifiedValue() * 2) + "x"
        )
        para1.setHighlightColors(activeHighlightColor, activePositiveColor)
        coronalCapacitor.addPara("Firing weapons deletes energy at a rate proportional to their flux cost.", listPad)
        val para2 = coronalCapacitor.addPara(
            "The boost from a full charge results in a %s to %s, %s, %s, and %s", 3f, activePositiveColor,
            "30% increase", "ballistic rate of fire", "energy weapon damage", "top speed", "acceleration"
        )
        para2.setHighlightColors(activePositiveColor, activeTextColor, activeTextColor, activeTextColor, activeTextColor)
        /*
        coronalCapacitor.addPara("The boost from a full charge results in a \n%s to:", 3f, activePositiveColor, "30% increase");
        coronalCapacitor.setBulletedListMode("");
        coronalCapacitor.addPara("  - Energy Weapon Damage", listPad, activeHighlightColor, "Energy Weapon Damage");
        coronalCapacitor.addPara("  - Ballistic Rate of Fire", listPad, activeHighlightColor, "Ballistic Rate of Fire");
        coronalCapacitor.addPara("  - Top Speed", listPad, activeHighlightColor, "Top Speed");
        coronalCapacitor.addPara("  - Acceleration", listPad, activeHighlightColor, "Acceleration");*/tooltip.addImageWithText(underHeadingPad)
        val hasShieldDrones = hullSize != HullSize.FIGHTER
        val activator: MagicDroneSubsystem = PDDroneActivator(ship)
        val maxDrones = activator.getMaxDeployedDrones().toString()
        val recharge = Math.round(activator.baseChargeRechargeDuration).toString()
        tooltip.addSectionHeading(
            "Shachi PD Drones", if (hasShieldDrones) activeHeaderTextColor else inactiveHeaderTextColor,
            if (hasShieldDrones) activeHeaderBannerColor else inactiveHeaderBannerColor, Alignment.MID, headingPad
        )
        val PDDrones = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", if (hasShieldDrones) "edf_shachi" else "edf_shachi"), HEIGHT)
        PDDrones.setBulletedListMode("•")
        PDDrones.setBulletWidth(15f)
        PDDrones.addPara(
            "Deploys %s PD drones behind the ship, each drone is armed with a single Burst PD Laser.", listPad,
            if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activeHighlightColor else inactiveHighlightColor, maxDrones, "Burst PD Laser"
        )
        PDDrones.addPara(
            "Drones regenerate once every %s seconds.", listPad,
            if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activeHighlightColor else inactiveHighlightColor, recharge
        )
        tooltip.addImageWithText(underHeadingPad)
    }

    override fun getTooltipWidth(): Float {
        return 400f
    }
}