package org.selkie.zea.hullmods

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
import org.selkie.kol.ReflectionUtils
import org.selkie.kol.Utils
import org.selkie.zea.combat.subsystems.ShieldDronesSubsystem
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys
import java.awt.Color
import java.util.*
import kotlin.math.roundToInt


class DawnBuiltin : BaseHullMod() {
    //smooth instead of flat
    //public static final float HULL_PERCENTAGE = 0.5f;
    companion object{
        const val ID = "CascadeTargetingProtocol"

        const val RANGE_BONUS = 10f // 200su
        const val ENMITY_BONUS_ROF_RELOAD = 30f
        const val ENMITY_BONUS_FLUX_REDUCTION = 15f
        const val ENMITY_HP_THRESHOLD = 50f
        const val DRONE_ADDED_KEY = "DawnHullmodDronesAdded"
        fun getNumDrones(ship: ShipAPI): Int {
            if((ship.hullSpec.baseHullId == ZeaStaticStrings.ZEA_BOSS_NIAN)){
                return 5
            }
            else if(ship.hullSize == HullSize.CRUISER){
                return 3
            }
            else if(ship.hullSize == HullSize.CAPITAL_SHIP) {
                return 4
            }
            return 0
        }
        fun getNumDrones(hullsize: HullSize): Int {
            if(hullsize == HullSize.CRUISER){
                return 3
            }
            else if(hullsize == HullSize.CAPITAL_SHIP) {
                return 4
            }
            return 0
        }
    }


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
            "+" + (RANGE_BONUS * HPLeftRatio).roundToInt() + "% weapon range",
            false)
        engine.maintainStatusForPlayerShip((ID + "_TOOLTIP_FEED") as Any,
            "graphics/icons/campaign/sensor_strength.png",
            "Cascade Targeting Protocol",
            "+" + (ENMITY_BONUS_ROF_RELOAD * (1 - HPLeftRatio)).roundToInt() + "% weapon ROF and recharge rate",
            false)
        engine.maintainStatusForPlayerShip((ID + "_TOOLTIP_SEED") as Any,
            "graphics/icons/campaign/sensor_strength.png",
            "Cascade Targeting Protocol",
            "-" + (ENMITY_BONUS_FLUX_REDUCTION * (1 - HPLeftRatio)).roundToInt() + "% weapon flux cost",
            false)
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)
        stats.energyShieldDamageTakenMult.modifyMult(id, 0.9f)
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
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        super.applyEffectsAfterShipCreation(ship, id)
        ship.setCustomData(DRONE_ADDED_KEY, true)
        val isBoss = ship.variant.hasTag(ZeaMemKeys.ZEA_BOSS_TAG) || ship.fleetMember != null && ship.fleetMember.fleetData != null && ship.fleetMember.fleetData.fleet != null && ship.fleetMember.fleetData.fleet.memoryWithoutUpdate.contains(ZeaMemKeys.ZEA_BOSS_TAG)
        if(!isBoss){
            addSubsystemToShip(ship, ShieldDronesSubsystem(ship, getNumDrones(ship), false))
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
        val activeHeaderTextColor = Utils.brighter(Misc.getButtonTextColor(), 0.8f)
        val activeHighlightColor = Misc.getHighlightColor()

        val inactiveTextColor = Misc.getGrayColor().darker()
        val inactivePositiveColor = Misc.getGrayColor().darker()
        val inactiveNegativeColor = Misc.getGrayColor().darker()
        val inactiveHeaderBannerColor = Misc.getDarkPlayerColor().darker().darker()
        val inactiveHeaderTextColor = Misc.getGrayColor().darker()
        val inactiveHighlightColor = Misc.getGrayColor().darker()

        //var initialHeight = tooltip!!.heightSoFar
        val background = tooltip!!.addLunaElement(0f, 0f)

        tooltip.addSectionHeading("Cascade Protocol", activeHeaderTextColor, activeHeaderBannerColor , Alignment.MID, headingPad)
        val cascadeProtocol = tooltip.beginImageWithText(Global.getSettings().getSpriteName(GfxCat.ICONS, "dawn_cascade"), HEIGHT)
        cascadeProtocol.setBulletedListMode("•")
        cascadeProtocol.setBulletWidth(15f)
        val para1 = cascadeProtocol.addPara("Increases the range of ballistic and energy weapons by ${RANGE_BONUS.toInt()}%% while over ${ENMITY_HP_THRESHOLD.toInt()}%% hull.",
            listPad, activeTextColor, activePositiveColor, "${RANGE_BONUS.toInt()}%", "${ENMITY_HP_THRESHOLD.toInt()}%"
        )
        para1.setHighlightColors(activePositiveColor, activeHighlightColor)
        val para2 = cascadeProtocol.addPara("Increases rate of fire of ballistic and energy weapons by ${ENMITY_BONUS_ROF_RELOAD.toInt()}%% with a ${ENMITY_BONUS_FLUX_REDUCTION.toInt()}%% flux cost reduction while under ${ENMITY_HP_THRESHOLD.toInt()}%% hull.",
            listPad, activeTextColor, activePositiveColor, "${ENMITY_BONUS_ROF_RELOAD.toInt()}%", "${ENMITY_BONUS_FLUX_REDUCTION.toInt()}%", "${ENMITY_HP_THRESHOLD.toInt()}%"
        )
        para2.setHighlightColors(activePositiveColor, activePositiveColor, activeHighlightColor)
        tooltip.addImageWithText(underHeadingPad)


        val hasShieldDrones = hullSize == HullSize.CAPITAL_SHIP || hullSize == HullSize.CRUISER
        val activator = ShieldDronesSubsystem(ship, getNumDrones(ship), false)
        val drone = Global.getSettings().getVariant(activator.getDroneVariant()).hullSpec
        val health = (drone.fluxCapacity / drone.shieldSpec.fluxPerDamageAbsorbed + drone.armorRating + drone.hitpoints).roundToInt().toString()
        val maxDrones = activator.getMaxDeployedDrones().toString()
        val recharge = activator.baseChargeRechargeDuration.roundToInt().toString()
        tooltip.addSectionHeading(
            "Chiwen Shield Drones", if (hasShieldDrones) activeHeaderTextColor else inactiveHeaderTextColor,
            if (hasShieldDrones) activeHeaderBannerColor else inactiveHeaderBannerColor, Alignment.MID, headingPad
        )
        val shieldDrones = tooltip.beginImageWithText(Global.getSettings().getSpriteName(GfxCat.ICONS, if (hasShieldDrones) "dawn_chiwen" else "dawn_chiwen_grey"), HEIGHT)
        shieldDrones.setBulletedListMode("•")
        shieldDrones.setBulletWidth(15f)

        val para3 = shieldDrones.addPara("%s %s/%s/%s/%s %s %s %s", listPad, inactiveTextColor, activeTextColor,
            "Deploys",
            "${getNumDrones(HullSize.FRIGATE)}",
            "${getNumDrones(HullSize.DESTROYER)}",
            "${getNumDrones(HullSize.CRUISER)}",
            "${getNumDrones(HullSize.CAPITAL_SHIP)}",
            "shield drones around the ship, each drone is capable of absorbing",
            health,
            "damage.")

        ReflectionUtils.invoke("setColor", shieldDrones.prev, if (hasShieldDrones) activeTextColor else inactiveTextColor)

        para3.setHighlightColors(if (hasShieldDrones) activeTextColor else inactiveTextColor,
            if(ship.hullSize == HullSize.FRIGATE && hasShieldDrones) activeHighlightColor else inactiveTextColor,
            if(ship.hullSize == HullSize.DESTROYER && hasShieldDrones) activeHighlightColor else inactiveTextColor,
            if(ship.hullSize == HullSize.CRUISER && hasShieldDrones) activeHighlightColor else inactiveTextColor,
            if(ship.hullSize == HullSize.CAPITAL_SHIP && hasShieldDrones) activeHighlightColor else inactiveTextColor,
            if (hasShieldDrones) activeTextColor else inactiveTextColor,
            if (hasShieldDrones) activePositiveColor else inactivePositiveColor,
            if (hasShieldDrones) activeTextColor else inactiveTextColor)

        shieldDrones.addPara("Drones regenerate once every %s seconds.",
            listPad, if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activeHighlightColor else inactiveHighlightColor, recharge
        )
        tooltip.addImageWithText(underHeadingPad)


        tooltip.addSectionHeading("Advanced Solar Plating", activeHeaderTextColor, activeHeaderBannerColor , Alignment.MID, headingPad)
        val solarPlating = tooltip.beginImageWithText(Global.getSettings().getSpriteName(GfxCat.ICONS, "dawn_solar"), HEIGHT)
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

        var sprite = Global.getSettings().getSprite(GfxCat.UI, "zea_dawn_hmod")
        background.render {
            sprite.setSize(tooltip.widthSoFar + 20, tooltip.heightSoFar + 10)
            sprite.setAdditiveBlend()
            sprite.alphaMult = 0.9f
            sprite.render(tooltip.position.x, tooltip.position.y)
        }
    }

    override fun getDisplaySortOrder(): Int {
        return 0
    }

}