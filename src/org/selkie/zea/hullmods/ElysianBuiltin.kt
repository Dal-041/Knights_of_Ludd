package org.selkie.zea.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.subsystems.MagicSubsystemsManager.addSubsystemToShip
import org.magiclib.subsystems.drones.MagicDroneSubsystem
import org.selkie.kol.Utils
import org.selkie.kol.hullmods.HullmodBackgroundElement
import org.selkie.zea.combat.subsystems.AmmoFeedSubsystem
import org.selkie.zea.combat.subsystems.PDDroneSubsystem
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat
import kotlin.math.roundToInt

class ElysianBuiltin : BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        if (ship.hullSize != HullSize.FIGHTER) {
            addSubsystemToShip(ship, AmmoFeedSubsystem(ship))
            addSubsystemToShip(ship, PDDroneSubsystem(ship))
        }
    }


    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
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


        HullmodBackgroundElement(tooltip, Global.getSettings().getSprite(GfxCat.UI, "zea_edf_hmod"), 0.6f)
        val hasFeeder = hullSize != HullSize.FIGHTER
        tooltip.addSectionHeading(
            "Accelerated Ammo Feeder", if (hasFeeder) activeHeaderTextColor else inactiveHeaderTextColor,
            if (hasFeeder) activeHeaderBannerColor else inactiveHeaderBannerColor, Alignment.MID, headingPad
        )
        val ammoFeeder = tooltip.beginImageWithText("graphics/icons/hullsys/ammo_feeder.png", HEIGHT)
        ammoFeeder.setBulletedListMode("•")
        ammoFeeder.setBulletWidth(15f)
        ammoFeeder.addPara(
            "On activation, increases ballistic rate of fire by %s and reduces ballistic flux use by %s.", listPad,
            if (hasFeeder) activeTextColor else inactiveTextColor, if (hasFeeder) activePositiveColor else inactiveHighlightColor,
            "${(AmmoFeedSubsystem.ROF_BONUS * 100).roundToInt()}%", "${(AmmoFeedSubsystem.FLUX_REDUCTION * 100).roundToInt()}%"
        )
        ammoFeeder.addPara(
            "Lasts %s seconds for frigates and destroyers, %s seconds for cruisers and capital ships.", listPad,
            if (hasFeeder) activeTextColor else inactiveTextColor, if (hasFeeder) activeHighlightColor else inactiveHighlightColor,
            AmmoFeedSubsystem.activeDuration(HullSize.FRIGATE).roundToInt().toString(),
            AmmoFeedSubsystem.activeDuration(HullSize.CRUISER).roundToInt().toString()
        )
        ammoFeeder.addPara(
            "Cooldown: %s seconds.", listPad,
            if (hasFeeder) activeTextColor else inactiveTextColor, if (hasFeeder) activeHighlightColor else inactiveHighlightColor,
            AmmoFeedSubsystem.COOLDOWN.roundToInt().toString()
        )
        tooltip.addImageWithText(underHeadingPad)

        if (ship == null) return


        val hasShieldDrones = hullSize != HullSize.FIGHTER
        val activator: MagicDroneSubsystem = PDDroneSubsystem(ship)
        val maxDrones = activator.getMaxDeployedDrones().toString()
        val recharge = activator.baseChargeRechargeDuration.roundToInt().toString()
        tooltip.addSectionHeading(
            "Shachi PD Drones", if (hasShieldDrones) activeHeaderTextColor else inactiveHeaderTextColor,
            if (hasShieldDrones) activeHeaderBannerColor else inactiveHeaderBannerColor, Alignment.MID, headingPad
        )
        val PDDrones = tooltip.beginImageWithText(Global.getSettings().getSpriteName(GfxCat.ICONS, if (hasShieldDrones) "edf_shachi" else "edf_shachi"), HEIGHT)
        PDDrones.setBulletedListMode("•")
        PDDrones.setBulletWidth(15f)
        PDDrones.addPara(
            "Deploys %s PD drones behind the ship, each drone is armed with a single Burst PD Laser.", listPad,
            if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activeHighlightColor else inactiveHighlightColor, maxDrones, "Burst PD Laser"
        )
        PDDrones.addPara(
            "PD drones launch 3 smart flares when parent vessel active vents.", listPad,
            if (hasShieldDrones) activeTextColor else inactiveTextColor, if (hasShieldDrones) activeHighlightColor else inactiveHighlightColor, maxDrones, "3"
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

    override fun getDisplaySortOrder(): Int {
        return 0
    }
}