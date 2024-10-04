package org.selkie.kol.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.util.MagicIncompatibleHullmods
import org.selkie.kol.Utils
import org.selkie.kol.impl.helpers.ZeaStaticStrings.GfxCat

class Oracle : BaseHullMod() {
    val COMMAND_POINT_RATE_FLAT_BONUS = 250f // +250%
    val COORDINATED_MANEUVERS_FLAT_BONUS = 5f // +5%
    val ELECTRONIC_WARFARE_FLAT_BONUS = 5f // +5%
    val HRS_SENSOR_RANGE_MOD_BONUS = 150f // +150 overworld sensor range
    val SIGHT_RANGE_BONUS = 2500f // +2000 combat sight range
    val SURVEY_SUPPLY_REDUCTION = 40f // 40 supplies reduction
    val SURVEY_MACHINERY_REDUCTION = 40f // 40 heavy machinery reduction
    val MOD_ID = "logi_oracle"

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        val engine = Global.getCombatEngine() ?: return
        val manager = engine.getFleetManager(ship.originalOwner) ?: return
        val member = manager.getDeployedFleetMember(ship) ?: return

        val commander = member.member?.fleetCommanderForStats ?: member.member?.fleetCommander
        val apply = ship === engine.playerShip || (commander != null && ship.captain === commander)

        if (apply) {
            ship.mutableStats.dynamic.getMod(Stats.COMMAND_POINT_RATE_FLAT).modifyFlat(MOD_ID, COMMAND_POINT_RATE_FLAT_BONUS*0.01f)
        } else {
            ship.mutableStats.dynamic.getMod(Stats.COMMAND_POINT_RATE_FLAT).unmodify(MOD_ID)
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI, id: String?) {
        stats.dynamic.getMod(Stats.COORDINATED_MANEUVERS_FLAT).modifyFlat(id, COORDINATED_MANEUVERS_FLAT_BONUS)
        stats.dynamic.getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ELECTRONIC_WARFARE_FLAT_BONUS)
        stats.dynamic.getMod(Stats.HRS_SENSOR_RANGE_MOD).modifyFlat(id, HRS_SENSOR_RANGE_MOD_BONUS)
        stats.dynamic.getMod(Stats.getSurveyCostReductionId(Commodities.HEAVY_MACHINERY)).modifyFlat(id, SURVEY_MACHINERY_REDUCTION)
        stats.dynamic.getMod(Stats.getSurveyCostReductionId(Commodities.SUPPLIES)).modifyFlat(id, SURVEY_SUPPLY_REDUCTION)
        stats.sightRadiusMod.modifyFlat(id, SIGHT_RANGE_BONUS)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        val incompatibleList = listOf(HullMods.SURVEYING_EQUIPMENT, HullMods.OPERATIONS_CENTER, HullMods.NAV_RELAY, HullMods.ECM, "hiressensors")
        for(hullmod in incompatibleList){
            if (ship.variant.hasHullMod(hullmod)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.variant, hullmod, MOD_ID)
        }
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

        tooltip.addSectionHeading("Oracle Sensor Suite", activeHeaderTextColor, activeHeaderBannerColor , Alignment.MID, headingPad)
        val oracle = tooltip.beginImageWithText(Global.getSettings().getSpriteName(GfxCat.ICONS, "logi_oracle"), HEIGHT)

        oracle.setBulletWidth(15f)
        oracle.setBulletedListMode("•")
        oracle.addPara("Reduces the heavy machinery and supplies required to perform surveys by %s",listPad, activeTextColor, activeHighlightColor, SURVEY_SUPPLY_REDUCTION.toInt().toString())
        oracle.addPara("Increases the fleets's sensor range by %s",listPad, activeTextColor, activeHighlightColor, HRS_SENSOR_RANGE_MOD_BONUS.toInt().toString())
        oracle.setBulletedListMode(null)
        oracle.addPara("When deployed in combat:", underHeadingPad)
        oracle.setBulletedListMode("•")
        oracle.addPara("Increases nav rating of your fleet by %s",listPad, activeTextColor, activeHighlightColor, COORDINATED_MANEUVERS_FLAT_BONUS.toInt().toString()+"%")
        oracle.addPara("Increases ECM rating of your fleet by %s",listPad, activeTextColor, activeHighlightColor, ELECTRONIC_WARFARE_FLAT_BONUS.toInt().toString()+"%")
        oracle.addPara("Increases vision range by %s",listPad, activeTextColor, activeHighlightColor, SIGHT_RANGE_BONUS.toInt().toString())
        oracle.addPara("If this is the flagship, increases command point recovery rate by %s",listPad, activeTextColor, activeHighlightColor, COMMAND_POINT_RATE_FLAT_BONUS.toInt().toString()+"%")
        tooltip.addImageWithText(underHeadingPad)
    }
}