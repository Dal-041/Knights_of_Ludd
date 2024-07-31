package org.selkie.kol.impl.campaign.interactions

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI

class DuskStationInteraction : InteractionDialogPlugin {
    override fun init(dialog: InteractionDialogAPI?) {
        TODO("Not yet implemented")
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        TODO("Not yet implemented")
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
    }

    override fun advance(amount: Float) {
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {
    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? {
        return null
    }
}