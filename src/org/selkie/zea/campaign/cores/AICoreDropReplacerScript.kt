package org.selkie.zea.campaign.cores

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.SpecialItemData
import org.selkie.zea.helpers.ZeaStaticStrings.BossCore

class AICoreDropReplacerScript : BaseCampaignEventListener(false) {
    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        super.reportEncounterLootGenerated(plugin, loot)

        if (loot == null) return
        for (stack in loot.stacksCopy) {
            if (stack.isCommodityStack) {
                var id = stack.commodityId
                var quantity = stack.size
                if (id in BossCore.ITEM_ID_LIST) {
                    loot.addSpecial(SpecialItemData(BossCore.SPECIAL_BOSS_CORE_ID, id), quantity)
                    loot.removeCommodity(id, quantity)
                }
            }
        }
    }
}