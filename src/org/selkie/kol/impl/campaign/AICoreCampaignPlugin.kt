package org.selkie.kol.impl.campaign

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.CampaignPlugin
import org.selkie.kol.impl.campaign.cores.DawnBossCoreOfficerPlugin
import org.selkie.kol.impl.campaign.cores.DuskBossCoreOfficerPlugin
import org.selkie.kol.impl.campaign.cores.ElysiaBossCoreOfficerPlugin

class AICoreCampaignPlugin : BaseCampaignPlugin() {

    override fun isTransient(): Boolean {
        return true
    }

    override fun pickAICoreOfficerPlugin(commodityId: String?): PluginPick<AICoreOfficerPlugin>? {

        if (commodityId == "zea_dawn_boss_core") return PluginPick(DawnBossCoreOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)
        if (commodityId == "zea_dusk_boss_core") return PluginPick(DuskBossCoreOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)
        if (commodityId == "zea_elysia_boss_core") return PluginPick(ElysiaBossCoreOfficerPlugin(), CampaignPlugin.PickPriority.HIGHEST)

        return null
    }

}