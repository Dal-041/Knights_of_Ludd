package org.selkie.zea.campaign

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.CampaignPlugin
import org.selkie.zea.campaign.coffinlink.CoffinLinkOfficerPlugin
import org.selkie.zea.campaign.cores.BossAICoreOfficerPlugin
import org.selkie.zea.helpers.ZeaStaticStrings.BossCore
import org.selkie.zea.helpers.ZeaStaticStrings.CoffinLink

class AICoreCampaignPlugin : BaseCampaignPlugin() {

    override fun isTransient(): Boolean {
        return true
    }

    override fun pickAICoreOfficerPlugin(commodityId: String?): PluginPick<AICoreOfficerPlugin>? {

        if (commodityId in BossCore.ITEM_ID_LIST)
            return PluginPick(BossAICoreOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)

        if (commodityId == CoffinLink.COMMODITY_ID)
            return PluginPick(CoffinLinkOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)

        return null
    }

}