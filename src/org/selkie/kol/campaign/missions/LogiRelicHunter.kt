package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color


class LogiRelicHunter : HubMissionWithBarEvent() {

    enum class Stage {
        TALK_TO_MGA_SCIENTIST,
        HUNTING_RELIC_SHIPS,
        COMPLETED,
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        setStartingStage(Stage.TALK_TO_MGA_SCIENTIST)
        addSuccessStages(Stage.COMPLETED)
        //(Global.getSector().economy.getMarket("market id here").memoryWithoutUpdate.get("\$BarCMD_shownEvents") as ArrayList<*>).add("bar_event_id_here")

        triggerSetGlobalMemoryValueAfterDelay(10f, "\$logi_MGA_bar_not_encountered", true) // what value is this delay???
        return true
    }

    // description when selected in intel screen
    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val opad = 10f
        if (currentStage == Stage.TALK_TO_MGA_SCIENTIST) {
            info.addPara("Find the Mbaye-Gogol Autofab Scientist looking for you.", opad)
        } else if (currentStage == Stage.HUNTING_RELIC_SHIPS) {
            info.addPara("You've talked to TempName, who hired you to figure out the whereabouts of several " +
                    "Domain-era relic exploration ships. TempName wants them recovered no matter the condition, " +
                    "and returned back to him for reverse engineering.", opad)
        }
    }

    // short description in popups and the intel entry
    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        val h = Misc.getHighlightColor()
        if (currentStage == Stage.TALK_TO_MGA_SCIENTIST) {
            info.addPara("Find the Mbaye-Gogol Autofab Scientist looking for you.", tc, pad)
            return true
        } else if (currentStage == Stage.HUNTING_RELIC_SHIPS) {
            info.addPara("Find and Recover the Domain-era relic ships.", tc, pad)
            return true
        }
        return false
    }

    // mission name
    override fun getBaseName(): String {
        return "Forgotten Vanguards"
    }
}