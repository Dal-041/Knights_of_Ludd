package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import lunalib.lunaRefit.BaseRefitButton
import java.util.*

class DuskCoreRefitButton : BaseRefitButton() {

    override fun getButtonName(member: FleetMemberAPI?, variant: ShipVariantAPI?): String {
        if (isIntegrated(member!!)) return "Remove Core"
        return "Transcendence"
    }

    override fun getIconName(member: FleetMemberAPI?, variant: ShipVariantAPI?): String {
        return Global.getSettings().getSpriteName("characters", DuskBossCoreOfficerPlugin.portraitID)
    }

    override fun shouldShow(member: FleetMemberAPI?, variant: ShipVariantAPI?, market: MarketAPI?): Boolean {
        if (isIntegrated(member!!)) return true
        if (Misc.isAutomated(member)) return false

        if (Global.getSector().playerFleet.fleetData.membersListCopy
                .any { member != it && it.captain != null && it.captain.aiCoreId == "zea_dusk_boss_core" }) return false

        return getCommodityStack() != null || getSpecialStack() != null
    }

    override fun isClickable(member: FleetMemberAPI?, variant: ShipVariantAPI?, market: MarketAPI?): Boolean {
        return (member!!.captain == null || member.captain.isDefault) || isIntegrated(member)
    }

    override fun getOrder(member: FleetMemberAPI?, variant: ShipVariantAPI?): Int {
        return 200
    }

    override fun getToolipWidth(member: FleetMemberAPI?, variant: ShipVariantAPI?, market: MarketAPI?): Float {
        return 400f
    }

    override fun hasTooltip(member: FleetMemberAPI?, variant: ShipVariantAPI?, market: MarketAPI?): Boolean {
        return true
    }

    override fun addTooltip(tooltip: TooltipMakerAPI?, member: FleetMemberAPI?, variant: ShipVariantAPI?, market: MarketAPI?) {

        var name = Global.getSettings().getCommoditySpec("zea_dusk_boss_core").name

        if (isIntegrated(member!!)) {
            tooltip!!.addPara("Click to remove the integrated $name.", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "$name")
        } else {
            tooltip!!.addPara("Integrate the $name in to the ship. This type of integration is solely for crewed ships. " +
                    "The skills of the core can not be configured when assigned to crewed ships.", 0f, Misc.getTextColor(), Misc.getHighlightColor(), "$name")

            tooltip.addSpacer(10f)

            if (member != null && !member.captain.isDefault) {
                tooltip.addPara("Can not install the core while an officer is assigned to the ship. ", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor())
            }
        }


    }

    fun isIntegrated(member: FleetMemberAPI) : Boolean {
        return member.captain != null && member.captain.aiCoreId == "zea_dusk_boss_core"
    }

    override fun onClick(member: FleetMemberAPI?, variant: ShipVariantAPI?, event: InputEventAPI?, market: MarketAPI?) {

        if (isIntegrated(member!!)) {
            Misc.setUnremovable(member.captain, true)
            member!!.captain = null as PersonAPI?

            if (getSpecialStack() == null) {

                Global.getSector().playerFleet.cargo.addCommodity("zea_dusk_boss_core", 1f)
            }


        } else {

            var core = DuskBossCoreOfficerPlugin().createPerson("zea_dusk_boss_core", Factions.PLAYER, Random())
            member.captain = core
            Misc.setUnremovable(core, true)

            /*var commStack = getCommodityStack()
            if (commStack != null) {
                commStack.subtract(1f)
                if (commStack.size < 0.1f) {
                    commStack.cargo.removeStack(commStack)
                }
            }*/

            var specialStack = getSpecialStack()
            if (specialStack != null) {
                specialStack.subtract(1f)
                if (specialStack.size < 0.1f) {
                    specialStack.cargo.removeStack(specialStack)
                }
            }

        }

        refreshVariant()
        refreshButtonList()
    }


    companion object {

        fun getCommodityStack() : CargoStackAPI? {
            return Global.getSector().playerFleet.cargo.stacksCopy.find { it.commodityId == "zea_dusk_boss_core" }

        }

        fun getSpecialStack() : CargoStackAPI? {
            return Global.getSector().playerFleet.cargo.stacksCopy.find { it.specialItemSpecIfSpecial?.id == "zea_boss_core_special" && it.specialDataIfSpecial.data == "zea_dusk_boss_core" }
        }

    }

}

