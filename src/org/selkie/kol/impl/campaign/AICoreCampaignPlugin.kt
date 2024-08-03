package org.selkie.kol.impl.campaign

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.CampaignPlugin
import org.selkie.kol.impl.campaign.cores.BossAICoreOfficerPlugin
import org.selkie.kol.impl.helpers.ZeaStaticStrings.BossCore

class AICoreCampaignPlugin : BaseCampaignPlugin() {

    override fun isTransient(): Boolean {
        return true
    }

    override fun pickAICoreOfficerPlugin(commodityId: String?): PluginPick<AICoreOfficerPlugin>? {

        if (commodityId in BossCore.ITEM_ID_LIST)
            return PluginPick(BossAICoreOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)

        return null
    }

}