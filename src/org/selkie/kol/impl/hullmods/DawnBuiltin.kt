package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.addLunaElement
import org.lazywizard.lazylib.MathUtils
import org.magiclib.subsystems.MagicSubsystemsManager.addSubsystemToShip
import org.magiclib.util.MagicIncompatibleHullmods
import org.selkie.kol.impl.combat.activators.SimpleShieldDronesActivator
import org.selkie.kol.impl.combat.activators.SmartShieldDronesActivator
import java.awt.Color
import java.util.*


class DawnBuiltin : BaseHullMod() {
    //smooth instead of flat
    //public static final float HULL_PERCENTAGE = 0.5f;
    private val ID = "CascadeTargetingProtocol"

    val RANGE_BONUS = 10f // 200su
    val ENMITY_BONUS_ROF_RELOAD = 30f
    val ENMITY_BONUS_FLUX_REDUCTION = 15f
    val ENMITY_HP_THRESHOLD = 50f

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
        if (stats.variant.hasHullMod("zea_dawn_smart_shield_drones")) {
            stats.variant.removeMod("zea_dawn_smart_shield_drones")
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        super.applyEffectsAfterShipCreation(ship, id)
        if (ship.hullSize == HullSize.CRUISER || ship.hullSize == HullSize.CAPITAL_SHIP) {
            if (ship.hullSpec.baseHullId == "zea_boss_nian") {
                addSubsystemToShip(ship, SmartShieldDronesActivator(ship))
            }else{
                addSubsystemToShip(ship, SimpleShieldDronesActivator(ship))
            }
        }
    }

    override fun getTooltipWidth(): Float {
        return 410f
    }

    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI, width: Float, isForModSpec: Boolean) {

        val HEIGHT = 64f
        val headingPad = 20f
        val underHeadingPad = 10f
        val listPad = 3f

        val activeTextColor = Misc.getTextColor()
        val activePositiveColor = Misc.getPositiveHighlightColor()
        val activeNegativeColor = Misc.getNegativeHighlightColor()
        val activeHeaderBannerColor = Misc.getDarkPlayerColor()
        val activeHighlightColor = Misc.getHighlightColor()

        val inactiveTextColor = Misc.getGrayColor().darker()
        val inactivePositiveColor = Misc.getGrayColor().darker()
        val inactiveNegativeColor = Misc.getGrayColor().darker()
        val inactiveHeaderBannerColor = Misc.getDarkPlayerColor().darker().darker()
        val inactiveHighlightColor = Misc.getGrayColor().darker()

        //var initialHeight = tooltip!!.heightSoFar
        val background = tooltip!!.addLunaElement(0f, 0f)

        tooltip.addSectionHeading("Cascade Protocol", activeHighlightColor, activeHeaderBannerColor , Alignment.MID, headingPad)
        val cascadeProtocol = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "dawn_cascade"), HEIGHT)
        cascadeProtocol.setBulletedListMode("•")
        cascadeProtocol.setBulletWidth(15f)
        val para1 = cascadeProtocol.addPara("Increases the range of ballistic and energy weapons by ${ENMITY_BONUS_ROF_RELOAD.toInt()}%% while over ${ENMITY_HP_THRESHOLD.toInt()}%% hull.",
            listPad, activeTextColor, activePositiveColor
        )
        para1.setHighlight("${ENMITY_BONUS_ROF_RELOAD.toInt()}%", "${ENMITY_HP_THRESHOLD.toInt()}%")
        para1.setHighlightColors(activePositiveColor, activeHighlightColor)
        val para2 = cascadeProtocol.addPara("Increases rate of fire of ballistic and energy weapons by ${ENMITY_BONUS_ROF_RELOAD.toInt()}%% with a ${ENMITY_BONUS_FLUX_REDUCTION.toInt()}%% flux cost reduction while under ${ENMITY_HP_THRESHOLD.toInt()}%% hull.",
            listPad, activeTextColor, activePositiveColor
        )
        para2.setHighlight("${ENMITY_BONUS_ROF_RELOAD.toInt()}%", "${ENMITY_BONUS_FLUX_REDUCTION.toInt()}%", "${ENMITY_HP_THRESHOLD.toInt()}%")
        para2.setHighlightColors(activePositiveColor, activePositiveColor, activeHighlightColor)
        tooltip.addImageWithText(underHeadingPad)


        val hasShieldDrones = hullSize == HullSize.CAPITAL_SHIP || hullSize == HullSize.CRUISER
        val activator = if (ship.hullSpec.baseHullId == "zea_boss_nian") SmartShieldDronesActivator(ship) else SimpleShieldDronesActivator(ship)
        val drone = Global.getSettings().getVariant(activator.getDroneVariant()).hullSpec
        val health = Math.round(drone.fluxCapacity / drone.shieldSpec.fluxPerDamageAbsorbed + drone.armorRating + drone.hitpoints).toString()
        val maxDrones = activator.getMaxDeployedDrones().toString()
        val recharge = Math.round(activator.baseChargeRechargeDuration).toString()
        tooltip.addSectionHeading(
            "Chiwen Shield Drones", if (hasShieldDrones) activeHighlightColor else inactiveHighlightColor,
            if (hasShieldDrones) activeHeaderBannerColor else inactiveHeaderBannerColor, Alignment.MID, headingPad
        )
        val capacitorShields = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", if (hasShieldDrones) "dawn_chiwen" else "dawn_chiwen_grey"), HEIGHT)
        capacitorShields.setBulletedListMode("•")
        capacitorShields.setBulletWidth(15f)
        capacitorShields.addPara(
            "Deploys %s shield drones around the ship, each drone is capable of absorbing %s damage.",
            listPad, if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activePositiveColor else inactivePositiveColor, maxDrones, health
        )
        capacitorShields.addPara("Drones regenerate once every %s seconds.",
            listPad, if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activePositiveColor else inactivePositiveColor, recharge
        )
        capacitorShields.addPara("Only activates on Cruisers and Capitals.",
            listPad, if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activePositiveColor else inactivePositiveColor
        )
        tooltip.addImageWithText(underHeadingPad)


        tooltip.addSectionHeading("Advanced Solar Plating", activeHighlightColor, activeHeaderBannerColor , Alignment.MID, headingPad)
        val solarPlating = tooltip.beginImageWithText(Global.getSettings().getSpriteName("icons", "dawn_solar"), HEIGHT)
        solarPlating.setBulletedListMode("•")
        solarPlating.setBulletWidth(15f)
        solarPlating.addPara("Decreases the CR-damaging effects of operating in star corona and hyperspace storms by %s.",
            listPad, activeTextColor, activePositiveColor, "100%"
        )
        solarPlating.addPara(
            "In combat, reduces energy damage taken by %s",
            listPad, activeTextColor, activePositiveColor, "10%"
        )
        tooltip.addImageWithText(underHeadingPad)

        var sprite = Global.getSettings().getSprite("kol_ui", "kol_dawn_hmod")
        background.render {
            sprite.setSize(tooltip.widthSoFar + 20, tooltip.heightSoFar + 10)
            sprite.setAdditiveBlend()
            sprite.alphaMult = 0.8f
            sprite.render(tooltip.position.x, tooltip.position.y)
        }
    }

    override fun getDisplaySortOrder(): Int {
        return 0
    }

}