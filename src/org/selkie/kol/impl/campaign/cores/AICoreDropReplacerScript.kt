package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.SpecialItemData

class AICoreDropReplacerScript : BaseCampaignEventListener(false) {
    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        super.reportEncounterLootGenerated(plugin, loot)

        if (loot == null) return
        for (stack in loot.stacksCopy) {
            if (stack.isCommodityStack) {
                var id = stack.commodityId
                var quantity = stack.size
                if (BossCoreSpecialItemPlugin.cores.containsKey(id)) {
                    loot.addSpecial(SpecialItemData("zea_boss_core_special", id), quantity)
                    loot.removeCommodity(id, quantity)
                }
            }
        }
    }
}