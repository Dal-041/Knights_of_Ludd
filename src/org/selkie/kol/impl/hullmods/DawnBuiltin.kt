package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.addLunaElement
import org.lazywizard.lazylib.MathUtils
import org.magiclib.subsystems.MagicSubsystemsManager.addSubsystemToShip
import org.magiclib.util.MagicIncompatibleHullmods
import org.selkie.kol.impl.combat.activators.SimpleShieldDronesActivator
import java.awt.Color
import java.util.*


class DawnBuiltin : BaseHullMod() {

    //smooth instead of flat
    //public static final float HULL_PERCENTAGE = 0.5f;
    private val ID = "CascadeTargetingProtocol"

    val RANGE_BONUS = 10f // 200su
    val ENMITY_BONUS_ROF_RELOAD = 30f
    val ENMITY_BONUS_FLUX_REDUCTION = 15f

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if (!ship.isAlive) {
            return
        }
        val engine = Global.getCombatEngine()
        //val HPLeftRatio = ship.hitpoints / ship.maxHitpoints
        var HPLeftRatio =  (ship.hitpoints - ship.maxHitpoints * 0.5f) / (ship.maxHitpoints - ship.maxHitpoints * 0.5f)
        HPLeftRatio = MathUtils.clamp(HPLeftRatio, 0f, 1f)

        ship.mutableStats.ballisticWeaponRangeBonus.modifyPercent(ID, RANGE_BONUS * HPLeftRatio)
        ship.mutableStats.energyWeaponRangeBonus.modifyPercent(ID, RANGE_BONUS * HPLeftRatio)
        ship.mutableStats.missileWeaponRangeBonus.modifyPercent(ID, RANGE_BONUS * HPLeftRatio)
        ship.mutableStats.ballisticRoFMult.modifyPercent(ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio))
        ship.mutableStats.energyRoFMult.modifyPercent(ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio))
        ship.mutableStats.ballisticAmmoRegenMult.modifyPercent(ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio))
        ship.mutableStats.energyAmmoRegenMult.modifyPercent(ID, ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio))
        ship.mutableStats.ballisticWeaponFluxCostMod.modifyPercent(ID, -ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio))
        ship.mutableStats.energyWeaponFluxCostMod.modifyPercent(ID, -ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio))

        // fuckin glow
        val WEAPON_TYPES = EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY)
        ship.setWeaponGlow(1f, Color(235, 20, 25, 250), WEAPON_TYPES)
        if (engine.playerShip !== ship) {
            return
        }

        engine.maintainStatusForPlayerShip((ID + "_TOOLTIP_SNEEDS") as Any,
            "graphics/icons/campaign/sensor_strength.png",
            "Cascade Targeting Protocol",
            "+" + Math.round(RANGE_BONUS * HPLeftRatio) + "% weapon range",
            false)
        engine.maintainStatusForPlayerShip((ID + "_TOOLTIP_FEED") as Any,
            "graphics/icons/campaign/sensor_strength.png",
            "Cascade Targeting Protocol",
            "+" + Math.round(ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio)) + "% weapon ROF and recharge rate",
            false)
        engine.maintainStatusForPlayerShip((ID + "_TOOLTIP_SEED") as Any,
            "graphics/icons/campaign/sensor_strength.png",
            "Cascade Targeting Protocol",
            "-" + Math.round(ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio)) + "% weapon flux cost",
            false)
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)
        stats.energyShieldDamageTakenMult.modifyMult(id, 0f)
        stats.energyDamageTakenMult.modifyMult(ID, 0.9f)
        if (stats.variant.hullMods.contains(HullMods.SOLAR_SHIELDING)) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(stats.variant,
                HullMods.SOLAR_SHIELDING,
                "zea_dawn_builtin")
        }

        //For save compatibility with old version
        if (stats.variant.hasHullMod(HullMods.SOLAR_SHIELDING)) {
            stats.variant.removeMod(HullMods.SOLAR_SHIELDING)
        }
        if (stats.variant.hasHullMod("zea_cascade_targeting")) {
            stats.variant.removeMod("zea_cascade_targeting")
        }
        if (stats.variant.hasHullMod("zea_dawn_simple_shield_drones")) {
            stats.variant.removeMod("zea_dawn_simple_shield_drones")
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        super.applyEffectsAfterShipCreation(ship, id)
        if (ship.hullSize == HullSize.CRUISER || ship.hullSize == HullSize.CAPITAL_SHIP) {
            addSubsystemToShip(ship, SimpleShieldDronesActivator(ship))
        }
    }

    override fun shouldAddDescriptionToTooltip(hullSize: HullSize, ship: ShipAPI, isForModSpec: Boolean): Boolean {
        return false
    }

    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI, width: Float, isForModSpec: Boolean) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)

        var sprite = Global.getSettings().getSprite("kol_ui", "kol_dawn_hmod")

        var initialHeight = tooltip!!.heightSoFar
        var element = tooltip!!.addLunaElement(0f, 0f)

        tooltip.addSpacer(10f)
        tooltip.addPara(spec.getDescription(hullSize), 0f)

        tooltip.addSpacer(10f)
        tooltip.addSectionHeading("Cascade Protocol", Alignment.MID, 0f)
        tooltip.addSpacer(10f)

        tooltip.addPara("A dynamic co-targeting unit supplements this ship by increasing range by 10%% while over 50%% hull. \n\n" +
                "If the vessel goes below 50%% hull, power is rerouted to improve ballistic and energy weaponry, allowing them to fire 30%% faster while generating 15%% less flux.",
            0f, Misc.getTextColor(), Misc.getHighlightColor(), "10%", "50%", "below 50%", "30%", "15%")

        tooltip.addSpacer(10f)
        tooltip.addSectionHeading("Chiwen Shield Drones", Alignment.MID, 0f)
        tooltip.addSpacer(10f)

        var tc = Misc.getTextColor()
        var hc = Misc.getHighlightColor()

        if (hullSize !== HullSize.CAPITAL_SHIP && hullSize !== HullSize.CRUISER) {
            tc = Misc.getGrayColor()
            hc = Misc.getGrayColor()
        }

        val activator = SimpleShieldDronesActivator(ship)
        val drone = Global.getSettings().getVariant(activator.getDroneVariant()).hullSpec
        val health =
            Math.round(drone.fluxCapacity / drone.shieldSpec.fluxPerDamageAbsorbed + drone.armorRating + drone.hitpoints)
                .toString()
        val maxDrones = activator.getMaxDeployedDrones().toString()
        val recharge = Math.round(activator.baseChargeRechargeDuration).toString()

        val label: LabelAPI = tooltip.addPara("Deploys $maxDrones shield drones that can each absorb $health damage around the ship. Drones regenerate once every $recharge seconds. " +
                    "Only activates on Cruisers and Capitals.",
                0f, tc, hc)

        label.setHighlight(maxDrones, health, recharge, "Only activates on Cruisers and Capitals.")
        label.setHighlightColors(hc, hc, hc, Misc.getHighlightColor())

        tooltip.addSpacer(10f)
        tooltip.addSectionHeading("Advanced Solar Plating", Alignment.MID, 0f)
        tooltip.addSpacer(10f)

        tooltip.addPara("Decreases the CR-damaging effects of operating in star corona and hyperspace storms by 100%%. \n\n" +
                "In combat, reduces energy damage taken by 10%%", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "100%", "10%")

        element.render {
            sprite.setSize(tooltip.widthSoFar + 20, tooltip.heightSoFar + 10)
            sprite.setAdditiveBlend()
            sprite.render(tooltip.position.x, tooltip.position.y)
        }
    }

    override fun getDisplaySortOrder(): Int {
        return 0
    }

}