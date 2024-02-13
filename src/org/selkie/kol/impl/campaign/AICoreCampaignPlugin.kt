package org.selkie.kol.impl.campaign

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.CampaignPlugin
import org.selkie.kol.impl.campaign.cores.DawnBossCoreOfficerPlugin

class AICoreCampaignPlugin : BaseCampaignPlugin() {

    override fun isTransient(): Boolean {
        return true
    }

    override fun pickAICoreOfficerPlugin(commodityId: String?): PluginPick<AICoreOfficerPlugin>? {

        if (commodityId == "zea_dawn_boss_core") return PluginPick(DawnBossCoreOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)

        return null
    }

}