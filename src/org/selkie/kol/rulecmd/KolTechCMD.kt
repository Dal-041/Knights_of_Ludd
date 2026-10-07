package org.selkie.kol.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoPickerListener
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.situations.KolTechSituationIntel
import org.selkie.kol.campaign.tech.KolTechData
import org.selkie.kol.campaign.tech.KolTechRequisition
import org.selkie.kol.campaign.tech.KolTechSettings
import org.selkie.kol.campaign.tech.KolTechValuation
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolTech
import java.util.Random
import kotlin.math.roundToInt

/**
 * Rules entry point for the Technology situation (Helensis at Cygnus).
 *   KolTechCMD publish                    local values for text and conditions ($kolTech_*)
 *   KolTechCMD handover                   cargo picker: hand over AI technology for scrip
 *   KolTechCMD handoverShips              fleet picker: hand over AI-faction ships (from TRUSTED)
 *   KolTechCMD requisition                production picker: ordained equipment for scrip
 *   KolTechCMD previewOfficer <personality>  prepare and show a candidate (cautious / steady / aggressive)
 *   KolTechCMD hireOfficer                   hire the previewed candidate (fires KolTechAfterPurchase)
 *   KolTechCMD clearOfficer                  forget the previewed candidate
 *   KolTechCMD buyUnique <slot>           one of the unique ships
 * After a picker closes it fires KolTechAfterHandover (or KolTechShrouded), then PopulateOptions.
 * After a purchase it sets $kolTech_purchaseType (weapons / fighters / ships / mixed / officer / unique) and fires
 * KolTechAfterPurchase for Helensis' comment.
 */
class KolTechCMD : BaseCommandPlugin() {

    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>,
                         memoryMap: MutableMap<String, MemoryAPI>): Boolean {
        dialog ?: return false
        return when (params.getOrNull(0)?.getString(memoryMap)) {
            "publish" -> { publish(memoryMap); true }
            "handover" -> { handover(dialog, memoryMap); true }
            "handoverShips" -> { handoverShips(dialog, memoryMap); true }
            "requisition" -> { KolTechRequisition.open(dialog, stageIndex()); true }
            "previewOfficer" -> { previewOfficer(dialog, params.getOrNull(1)?.getString(memoryMap) ?: Personalities.STEADY); true }
            "hireOfficer" -> hireOfficer(dialog, memoryMap)
            "clearOfficer" -> { candidate = null; true }
            "buyUnique" -> buyUnique(dialog, memoryMap, params.getOrNull(1)?.getInt(memoryMap) ?: 0)
            else -> false
        }
    }

    companion object {
        fun stageIndex(): Int = KolTechSituationIntel.get()?.stageIndex() ?: 0

        /** Credits of goods per scrip at the current stage. */
        fun storeRate(stage: Int = stageIndex()): Float {
            val rates = KolTechSettings.storeRates
            return rates[(stage - 1).coerceIn(0, rates.size - 1)]
        }

        /** Base officer price at the current stage: discounted less than equipment (settings officerRates). */
        fun officerBasePrice(stage: Int = stageIndex()): Float {
            val rates = KolTechSettings.officerRates
            return KolTechSettings.officerCreditValue / rates[(stage - 1).coerceIn(0, rates.size - 1)]
        }

        /** The candidate shown by previewOfficer and their price (base +/- variance); dialog-scoped, never saved. */
        private var candidate: PersonAPI? = null
        private var candidatePrice = 0

        /** Stage from which officers are unlimited (CONSECRATED). */
        const val OFFICERS_UNCAPPED_STAGE = 4

        /**
         * Officers available now: officersPerMilestone at each milestone from TRUSTED (unused ones carry forward),
         * unlimited from CONSECRATED. Returns Int.MAX_VALUE when uncapped.
         */
        fun officerAllowance(stage: Int = stageIndex()): Int =
            if (stage >= OFFICERS_UNCAPPED_STAGE) Int.MAX_VALUE
            else ((stage - 1) * KolTechSettings.officersPerMilestone - KolTechData.get().officersHired).coerceAtLeast(0)

        /** The previewed candidate's price, or the base price if none is being shown. */
        fun officerPrice(): Int = if (candidate != null) candidatePrice else officerBasePrice().roundToInt().coerceAtLeast(1)

        /** Helensis comments on a purchase (rules: KolTechAfterPurchase, keyed on $kolTech_purchaseType). */
        fun afterPurchase(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>, type: String) {
            memoryMap[MemKeys.LOCAL]?.set("\$kolTech_purchaseType", type, 0f)
            FireBest.fire(null, dialog, memoryMap, KolTech.TRIGGER_AFTER_PURCHASE)
        }
    }

    // --- publish ------------------------------------------------------------------------------------------

    private fun publish(memoryMap: Map<String, MemoryAPI>) {
        val local = memoryMap[MemKeys.LOCAL] ?: return
        val data = KolTechData.get()
        val fleet = Global.getSector().playerFleet
        val officerPrice = officerPrice()
        local.set("\$kolTech_balance", Misc.getWithDGS(data.balance.toFloat()), 0f)
        local.set("\$kolTech_officerPrice", Misc.getWithDGS(officerPrice.toFloat()), 0f)
        local.set("\$kolTech_canAffordOfficer", data.canAfford(officerPrice), 0f)
        local.set("\$kolTech_officerSlotFree", fleet.fleetData.officersCopy.size < Misc.getMaxOfficers(fleet), 0f)
        local.set("\$kolTech_officerAvailable", officerAllowance() > 0, 0f)
        val stage = stageIndex()
        for (slot in KolTechSettings.uniques) {
            val n = slot.slot
            val offered = stage >= slot.stage && n !in data.uniquesBought &&
                    Global.getSettings().doesVariantExist(slot.variant)
            local.set("\$kolTech_uniqueOffer$n", offered, 0f)
            if (!offered) continue
            local.set("\$kolTech_uniqueName$n", Global.getSettings().getVariant(slot.variant).hullSpec.nameWithDesignationWithDashClass, 0f)
            local.set("\$kolTech_uniquePrice$n", Misc.getWithDGS(slot.price.toFloat()), 0f)
            local.set("\$kolTech_canAffordUnique$n", data.canAfford(slot.price), 0f)
        }
    }

    // --- handover: items ----------------------------------------------------------------------------------

    private fun handover(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        val playerCargo = Global.getSector().playerFleet.cargo
        val offer = Global.getFactory().createCargo(true)
        for (stack in playerCargo.stacksCopy) if (KolTechValuation.isListed(stack)) offer.addFromStack(stack)
        offer.sort()
        val width = 310f
        dialog.showCargoPickerDialog("Hand over technology", "Confirm", "Cancel", true, width, offer,
            object : CargoPickerListener {
                override fun pickedCargo(cargo: CargoAPI) {
                    if (cargo.isEmpty) { done(dialog, memoryMap); return }
                    if (KolTechValuation.containsForbidden(cargo)) {
                        // Shrouded: the whole handover is void; only the abomination is taken
                        for (stack in cargo.stacksCopy.filter { KolTechValuation.isForbidden(it) }) {
                            playerCargo.removeItems(stack.type, stack.data, stack.size)
                            lossText(stack, dialog)
                        }
                        excommunicate(dialog, memoryMap)
                        return
                    }
                    val scrip = KolTechValuation.total(cargo)
                    for (stack in cargo.stacksCopy) {
                        playerCargo.removeItems(stack.type, stack.data, stack.size)
                        lossText(stack, dialog)
                    }
                    pay(scrip, dialog)
                    FireAll.fire(null, dialog, memoryMap, KolTech.TRIGGER_AFTER_HANDOVER)
                    done(dialog, memoryMap)
                }

                override fun cancelledCargoSelection() = done(dialog, memoryMap)

                override fun recreateTextPanel(panel: TooltipMakerAPI, cargo: CargoAPI?, pickedUp: CargoStackAPI?,
                                               pickedUpFromSource: Boolean, combined: CargoAPI) {
                    val h = Misc.getHighlightColor()
                    val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
                    panel.setParaFontOrbitron()
                    panel.addPara("The Order's Bounty", knights.baseUIColor, 1f)
                    panel.setParaFontDefault()
                    panel.addImage(knights.crest, width, 0f)
                    panel.addPara("Master Helensis pays scrip for technology taken from the AI fleets, by kind and " +
                            "grade rather than by market value.", 10f)
                    panel.addPara("These items will earn %s scrip.", 10f, h,
                        Misc.getWithDGS(KolTechValuation.total(combined).toFloat()))
                    panel.addPara("Current balance: %s scrip.", 3f, h, Misc.getWithDGS(KolTechData.get().balance.toFloat()))
                }
            })
    }

    private fun lossText(stack: CargoStackAPI, dialog: InteractionDialogAPI) {
        val text = dialog.textPanel
        val qty = stack.size.roundToInt()
        when {
            stack.isCommodityStack -> AddRemoveCommodity.addCommodityLossText(stack.commodityId, qty, text)
            stack.isWeaponStack -> AddRemoveCommodity.addWeaponLossText(stack.weaponSpecIfWeapon.weaponId, qty, text)
            stack.isFighterWingStack -> AddRemoveCommodity.addFighterLossText(stack.fighterWingSpecIfWing.id, qty, text)
            stack.isSpecialStack -> AddRemoveCommodity.addItemLossText(stack.specialDataIfSpecial, qty, text)
        }
    }

    private fun pay(scrip: Int, dialog: InteractionDialogAPI) {
        if (scrip <= 0) return
        val text = dialog.textPanel
        text.setFontSmallInsignia()
        text.addPara("Received %s scrip", Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
            Misc.getWithDGS(scrip.toFloat()))
        text.setFontInsignia()
        KolTechSituationIntel.get()?.credit(scrip, dialog) ?: KolTechData.get().earn(scrip)
    }

    private fun done(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        publish(memoryMap)
        FireAll.fire(null, dialog, memoryMap, "PopulateOptions")
    }

    // --- handover: ships ----------------------------------------------------------------------------------

    private fun handoverShips(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        val fleet = Global.getSector().playerFleet
        val pool = fleet.fleetData.membersListCopy.filter {
            !it.isFlagship && (KolTechValuation.isForbidden(it) || KolTechValuation.shipValue(it) > 0)
        }
        dialog.showFleetMemberPickerDialog("Hand over ships", "Confirm", "Cancel", 3, 7, 58f, true, true, pool,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: MutableList<FleetMemberAPI>?) {
                    if (members.isNullOrEmpty()) { done(dialog, memoryMap); return }
                    val forbidden = members.filter { KolTechValuation.isForbidden(it) }
                    if (forbidden.isNotEmpty()) {
                        for (m in forbidden) {
                            fleet.fleetData.removeFleetMember(m)
                            AddRemoveCommodity.addFleetMemberLossText(m, dialog.textPanel)
                        }
                        excommunicate(dialog, memoryMap)
                        return
                    }
                    var scrip = 0
                    for (m in members) {
                        scrip += KolTechValuation.shipValue(m)
                        fleet.fleetData.removeFleetMember(m)
                        AddRemoveCommodity.addFleetMemberLossText(m, dialog.textPanel)
                    }
                    pay(scrip, dialog)
                    FireAll.fire(null, dialog, memoryMap, KolTech.TRIGGER_AFTER_HANDOVER)
                    done(dialog, memoryMap)
                }

                override fun cancelledFleetMemberPicking() = done(dialog, memoryMap)
            })
    }

    // --- Shrouded -----------------------------------------------------------------------------------------

    private fun excommunicate(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        Global.getSector().memoryWithoutUpdate.set(KolTech.EXCOMMUNICATED, true)
        val level = runCatching { RepLevel.valueOf(KolTechSettings.excommunicationRep) }.getOrDefault(RepLevel.VENGEFUL)
        for (id in listOf(KolStaticStrings.kolFactionID, Factions.LUDDIC_CHURCH)) {
            Global.getSector().getFaction(id)?.setRelationship(Factions.PLAYER, level)
        }
        org.selkie.kol.campaign.story.KolSeverance.sever("shrouded") // its own flag, and severance (after the scene)
        FireBest.fire(null, dialog, memoryMap, KolTech.TRIGGER_SHROUDED)
    }

    // --- officers -----------------------------------------------------------------------------------------

    private fun previewOfficer(dialog: InteractionDialogAPI, personality: String) {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
        val officer = OfficerManagerEvent.createOfficer(knights, KolTechSettings.officerLevel,
            OfficerManagerEvent.SkillPickPreference.ANY, false, null, true, false, 0, Random())
        officer.setPersonality(personality)
        candidate = officer
        val variance = KolTechSettings.officerPriceVariance
        candidatePrice = (officerBasePrice() * (1f + (Random().nextFloat() * 2f - 1f) * variance)).roundToInt().coerceAtLeast(1)
        dialog.visualPanel.showPersonInfo(officer, true)
        val text = dialog.textPanel
        text.addPara("${officer.nameString}, level ${officer.stats.level}, ${officer.personalityAPI.displayName.lowercase()}.",
            Misc.getHighlightColor(), Misc.getHighlightColor(), officer.nameString)
        text.addSkillPanel(officer, false)
    }

    private fun hireOfficer(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>): Boolean {
        val officer = candidate ?: return false
        val fleet = Global.getSector().playerFleet
        val data = KolTechData.get()
        val price = officerPrice()
        if (officerAllowance() <= 0) return false
        if (fleet.fleetData.officersCopy.size >= Misc.getMaxOfficers(fleet) || !data.spend(price)) return false
        candidate = null
        data.officersHired++
        fleet.fleetData.addOfficer(officer)
        val text = dialog.textPanel
        text.setFontSmallInsignia()
        text.addPara("Spent %s scrip", Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getWithDGS(price.toFloat()))
        text.addPara("${officer.nameString} has joined your fleet", Misc.getPositiveHighlightColor())
        text.setFontInsignia()
        afterPurchase(dialog, memoryMap, "officer")
        return true
    }

    // --- unique ships -------------------------------------------------------------------------------------

    private fun buyUnique(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>, slot: Int): Boolean {
        val unique = KolTechSettings.uniques.firstOrNull { it.slot == slot } ?: return false
        val data = KolTechData.get()
        if (slot in data.uniquesBought || !Global.getSettings().doesVariantExist(unique.variant)) return false
        if (!data.spend(unique.price)) return false
        data.uniquesBought.add(slot)
        val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, unique.variant)
        member.repairTracker.cr = member.repairTracker.maxCR
        Global.getSector().playerFleet.fleetData.addFleetMember(member)
        val text = dialog.textPanel
        text.setFontSmallInsignia()
        text.addPara("Spent %s scrip", Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getWithDGS(unique.price.toFloat()))
        text.setFontInsignia()
        AddRemoveCommodity.addFleetMemberGainText(member, text)
        FireBest.fire(null, dialog, memoryMap, KolTech.TRIGGER_UNIQUE_BOUGHT + slot)
        afterPurchase(dialog, memoryMap, "unique")
        return true
    }
}
