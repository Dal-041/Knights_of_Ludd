package org.selkie.zea.campaign.cores

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.util.IntervalUtil
import org.selkie.zea.helpers.ZeaStaticStrings.BossCore

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
    var conversionCheckInterval = IntervalUtil(0.010f, 0.11f)

    override fun advance(amount: Float) {

        var cargo = Global.getSector().playerFleet.cargo

        //Add the commodities while the player isnt looking at the inventory
        if (Global.getSector().campaignUI.currentCoreTab == CoreUITabId.FLEET || Global.getSector().campaignUI.currentCoreTab == CoreUITabId.REFIT) {

            interval.advance(amount)
            if (!addedCommodities && interval.intervalElapsed()) {
                for (stack in cargo.stacksCopy) {
                    if (stack.isSpecialStack) {
                        var plugin = stack.plugin
                        if (plugin is BossCoreSpecialItemPlugin ) {
                            var commId = plugin.commoditySpec.id
                            var commStack = Global.getFactory().createCargoStack(CargoAPI.CargoItemType.RESOURCES, commId, cargo)
                            commStack.size = stack.size
                            cargo.addFromStack(commStack)

                            var current = quantities[commStack.commodityId] ?: 0f
                            quantities[commStack.commodityId] = current + commStack.size
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

            //Trying to avoid performance issues of checking every frame, getCommodityQuantity appears to be rather performance intensive
            var changedSomething = false

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
                changedSomething = true
            }

            quantities.clear()

            conversionCheckInterval.advance(amount)
            if (changedSomething || conversionCheckInterval.intervalElapsed()) {

                var quantities = getCargoQuantities(cargo)

                for (core in BossCore.ITEM_ID_LIST) {
                    //var quantity = cargo.getCommodityQuantity(core)
                    var quantity = quantities[core]!!
                    if (quantity > 0) {
                        var stack = cargo.stacksCopy.find { it.isSpecialStack && it.specialDataIfSpecial.data == core }
                        if (stack != null) {
                            stack.add(quantity)
                        }
                        else {
                            cargo.addSpecial(SpecialItemData(BossCore.SPECIAL_BOSS_CORE_ID, core), quantity)
                        }
                    }
                    cargo.removeCommodity(core, quantity)
                }
            }
        }


        /*//because of tranquility
        var duskStack = cargo.stacksCopy.find { it.specialItemSpecIfSpecial?.id == "zea_boss_core_special" && it.specialDataIfSpecial.data == "zea_dusk_boss_core" }
        if (duskStack != null) {
            if (duskStack.size >= 2f) {
                duskStack.subtract(1f)
            }
        }*/

        for(officer in Global.getSector().playerFleet.fleetData.officersCopy.map { it.person }){
            if(officer.stats.hasSkill(BossCore.DUSK_CORE.exclusiveSkillID) && officer.id != Global.getSector().memoryWithoutUpdate.getString("\$officer_"+BossCore.DUSK_CORE.exclusiveSkillID)){
                officer.stats.setSkillLevel(BossCore.DUSK_CORE.exclusiveSkillID, 0f)
            }
        }
    }

    //Custom method because iterating over the cargo for each core is inefficient
    fun getCargoQuantities(cargo: CargoAPI) : Map<String, Float> {

        var quantities = HashMap<String, Float>()
        quantities.put(BossCore.DUSK_CORE.itemID, 0f)
        quantities.put(BossCore.DAWN_CORE.itemID, 0f)
        quantities.put(BossCore.ELYSIAN_CORE.itemID, 0f)

        for (stack in cargo.stacksCopy) {
            if (stack.isCommodityStack && quantities.keys.contains(stack.commodityId)) {
                quantities.set(stack.commodityId, quantities.get(stack.commodityId)!! + stack.size)
            }
        }

        return quantities
    }
}