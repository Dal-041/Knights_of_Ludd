package org.selkie.kol.impl.campaign.cores

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.campaign.ui.trade.CargoItemStack

//Loosely based on things done in Tahlans Digital Soul
class AICoreReplacerScript : EveryFrameScript {

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    var addedCommodities = false

    var quantities = mutableMapOf<String, Float>()

    var interval = IntervalUtil(0.25f, 0.25f)

    override fun advance(amount: Float) {

        var cargo = Global.getSector().playerFleet.cargo

        //Add the commodities while the player isnt looking at the inventory
        if (Global.getSector().campaignUI.currentCoreTab == CoreUITabId.FLEET || Global.getSector().campaignUI.currentCoreTab == CoreUITabId.REFIT) {

            interval.advance(amount)
            if (!addedCommodities && interval.intervalElapsed()) {
                for (stack in cargo.stacksCopy) {
                    if (stack.isSpecialStack) {
                        var plugin = stack.plugin
                        if (plugin is BossCoreSpecialItemPlugin) {
                            var commId = plugin.commoditySpec.id
                            var commStack = Global.getFactory().createCargoStack(CargoAPI.CargoItemType.RESOURCES, commId, cargo)
                            commStack.size = stack.size
                            cargo.addFromStack(commStack)

                            var current = quantities.get(commStack.commodityId) ?: 0f
                            quantities.set(commStack.commodityId, current + commStack.size)
                        }
                    }
                }

                addedCommodities = true
            }

        }
        //Remove the commodities and update sizes
        else {
            addedCommodities = false
            interval = IntervalUtil(0.25f, 0.25f)

            for ((commodity, quantity) in quantities)  {
                var newQuantity = cargo.getCommodityQuantity(commodity)
                var diff = quantity - newQuantity

                while (diff > 0) {
                    var stack = cargo.stacksCopy.find { it.isSpecialStack && it.specialDataIfSpecial.data == commodity } ?: break

                    stack.subtract(1f)
                    if (stack.size < 0.1f) {
                        stack.cargo.removeStack(stack)
                    }

                    diff--
                }

                cargo.removeCommodity(commodity, quantity)
            }

            quantities.clear()

            var cores = listOf("zea_dusk_boss_core", "zea_dawn_boss_core", "zea_elysia_boss_core")
            for (core in cores) {
                var quantity = cargo.getCommodityQuantity(core)
                if (quantity > 0) {
                    var stack = cargo.stacksCopy.find { it.isSpecialStack && it.specialDataIfSpecial.data == core }
                    if (stack != null) {
                        stack.add(quantity)
                    }
                    else {
                        cargo.addSpecial(SpecialItemData("zea_boss_core_special", core), quantity)
                    }
                }
                cargo.removeCommodity(core, quantity)
            }
        }
    }
}