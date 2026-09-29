package org.selkie.zea.campaign.coffinlink

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.helpers.ZeaStaticStrings.CoffinLink

/**
 * The COFFIN Link item: right-click to grant the player the COFFIN Link skill, which lets them
 * take command of automated ships. Adapted from Tahlan Shipworks' Neural Uplink (by Nia).
 */
class CoffinLinkItemPlugin : BaseSpecialItemPlugin() {

    private fun isIntegrated(): Boolean = Global.getSector().playerStats.hasSkill(CoffinLink.SKILL_ID)

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, transferHandler: CargoTransferHandlerAPI?, stackSource: Any?) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource)
        val opad = 10f
        addCostLabel(tooltip, opad, transferHandler, stackSource)
        if (isIntegrated()) {
            tooltip.addPara("Already integrated", Misc.getGrayColor(), opad)
        } else {
            tooltip.addPara("Right-click to integrate", Misc.getPositiveHighlightColor(), opad)
        }
    }

    override fun hasRightClickAction(): Boolean = true

    override fun shouldRemoveOnRightClickAction(): Boolean = !isIntegrated()

    override fun performRightClickAction() {
        val messages = Global.getSector().campaignUI.messageDisplay
        if (isIntegrated()) {
            messages.addMessage("${spec.name}: already integrated")
            return
        }
        Global.getSector().playerStats.setSkillLevel(CoffinLink.SKILL_ID, 1f)
        Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1f, 1f)
        messages.addMessage("${spec.name} integrated: you can now take command of automated ships")
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int = 0
}
