package org.selkie.kol.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoPickerListener
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.libra.KolLibra
import org.selkie.kol.campaign.libra.KolLibraAgreements
import org.selkie.kol.campaign.libra.KolLibraCrewContract
import org.selkie.kol.campaign.libra.KolLibraDrill
import org.selkie.kol.campaign.libra.KolLibraMission
import org.selkie.kol.campaign.libra.KolLibraRestoration
import org.selkie.kol.campaign.libra.KolLibraSupplyContract
import org.selkie.kol.campaign.libra.KolLibraGunnery
import org.selkie.kol.campaign.libra.KolLibraMilestone
import org.selkie.kol.helpers.KolStaticStrings
import kotlin.math.roundToInt

/**
 * Rule commands for the Libra situation (add-knights-libra-situation):
 * - `KolLibraCMD active` (condition): the situation is running.
 * - `KolLibraCMD open <supply|crew|drill|restoration>` (condition): that milestone is offered.
 * - `KolLibraCMD handIn` / `donate`: the commodity and ship pickers (Martins at Libra); `donationsOpen` (condition).
 * - `KolLibraCMD negotiable` (condition) / `negotiate` / `isTermsOption` (condition) / `terms` / `accept`: agreements
 *   with a market's administrator.
 * - `KolLibraCMD canVouch [id]` (condition) / `vouch [id]`: the crew contract's vouches (the active person, or `id`).
 * - `KolLibraCMD startSupply` / `startCrew` / `startDrill`: Martins gives the milestone's mission (the drill lights its beacon).
 * - `KolLibraCMD escortRestartable` (condition) / `escortRestart`: another crew convoy after one was lost.
 * - `KolLibraCMD drillReady` / `drillRestageable` / `beaconReady` (conditions) / `restageDrill` / `beacon`: the drill
 *   event and gunnery training.
 * - `KolLibraCMD reward <credits|supplies|fuel>`: rewarding a genuine responder.
 * - `KolLibraCMD restore`: the final restoration (the battlestar), queuing Libra as an assembly topic;
 *   `legitimize`: the Libra assembly makes it a charted Knights market.
 */
class KolLibraCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?,
                         memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        dialog ?: return false
        val map = memoryMap ?: return false
        val arg = { i: Int -> params?.getOrNull(i)?.getString(map) }
        return when (arg(0)) {
            "active" -> KolLibra.active()
            "open" -> KolLibraMilestone.values().firstOrNull { it.key == arg(1) }?.let { KolLibra.open(it) } ?: false
            "handIn" -> { handIn(dialog, map); true }
            "donationsOpen" -> KolLibra.donationsOpen()
            "donate" -> { donate(dialog, map); true }
            "negotiable" -> KolLibraAgreements.negotiable(dialog)
            "negotiate" -> { KolLibraAgreements.negotiateOptions(dialog); true }
            "isTermsOption" -> map[MemKeys.LOCAL]?.getString("\$option")?.let { it.startsWith("kolLibra_line_") || it == "kolLibra_crew" } ?: false
            "terms" -> KolLibraAgreements.terms(dialog, map)
            "accept" -> KolLibraAgreements.accept(dialog, map)
            "canVouch" -> KolLibraAgreements.canVouch(arg(1) ?: dialog.interactionTarget?.activePerson?.id)
            "vouch" -> { (arg(1) ?: dialog.interactionTarget?.activePerson?.id)?.let { KolLibraAgreements.vouch(it, dialog) }; true }
            "addKillaOption" -> { dialog.optionPanel.addOption("[PLACEHOLDER] Ask the curate sacraria to vouch for Libra's crew", "kolLibra_killaVouchAsk"); true }
            "startSupply" -> KolLibraMission.start(KolLibraSupplyContract.ID, dialog)
            "startCrew" -> KolLibraMission.start(KolLibraCrewContract.ID, dialog)
            "escortRestartable" -> KolLibraCrewContract.restartable()
            "escortRestart" -> { KolLibraCrewContract.get()?.beginEscort(dialog); true }
            "drillReady" -> KolLibraGunnery.drillReady() && !KolLibraDrill.active()
            "drillRestageable" -> KolLibraDrill.restageable()
            "restageDrill" -> { KolLibraDrill.get()?.restage(dialog); true }
            "beaconReady" -> KolLibraGunnery.beaconReady()
            "startDrill" -> KolLibraMission.start(KolLibraDrill.ID, dialog) // accepting it lights the beacon
            "beacon" -> { KolLibraGunnery.light(false); true }
            "reward" -> KolLibraGunnery.rewardResponder(arg(1) ?: return false, dialog)
            "restore" -> {
                KolLibra.complete(KolLibraMilestone.RESTORATION, dialog)
                com.fs.starfarer.api.Global.getSector().memoryWithoutUpdate.set(org.selkie.kol.campaign.story.KolAssembly.LIBRA_TOPIC_PENDING, true)
                KolLibraMission.start(KolLibraRestoration.ID, dialog)
                true
            }
            "legitimize" -> {
                org.selkie.kol.campaign.libra.KolLibraEffects.legitimize()
                com.fs.starfarer.api.Global.getSector().memoryWithoutUpdate.unset(org.selkie.kol.campaign.story.KolAssembly.LIBRA_TOPIC_PENDING)
                true
            }
            else -> false
        }
    }

    private fun done(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>) {
        FireAll.fire(null, dialog, map, "PopulateOptions")
    }

    /** Accepted commodities, valued by base price × the multiplier (never crew). */
    private fun handIn(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>) {
        val playerCargo = Global.getSector().playerFleet.cargo
        val offer = Global.getFactory().createCargo(true)
        for (stack in playerCargo.stacksCopy) if (stack.isCommodityStack && KolLibra.acceptsCommodity(stack.commodityId)) offer.addFromStack(stack)
        offer.sort()
        val width = 310f
        dialog.showCargoPickerDialog("Goods for Libra", "Confirm", "Cancel", true, width, offer, object : CargoPickerListener {
            override fun pickedCargo(cargo: CargoAPI) {
                if (cargo.isEmpty) { done(dialog, map); return }
                var value = 0f
                for (stack in cargo.stacksCopy) {
                    // the picked cargo can hold empty placeholder stacks: only real, accepted commodities count
                    val id = stack.commodityId ?: continue
                    if (!stack.isCommodityStack || !KolLibra.acceptsCommodity(id) || stack.size <= 0f) continue
                    val qty = stack.size
                    value += KolLibra.commodityValue(id, qty)
                    playerCargo.removeCommodity(id, qty)
                    AddRemoveCommodity.addCommodityLossText(id, qty.roundToInt(), dialog.textPanel)
                }
                KolLibra.contribute(value.roundToInt(), dialog, "Goods for Libra")
                FireAll.fire(null, dialog, map, "KolLibraAfterHandIn")
                done(dialog, map)
            }

            override fun cancelledCargoSelection() = done(dialog, map)

            override fun recreateTextPanel(panel: TooltipMakerAPI, cargo: CargoAPI?, pickedUp: CargoStackAPI?,
                                           pickedUpFromSource: Boolean, combined: CargoAPI) {
                val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
                panel.setParaFontOrbitron()
                panel.addPara("Battlestar Libra", knights.baseUIColor, 1f)
                panel.setParaFontDefault()
                panel.addImage(knights.crest, width, 0f)
                panel.addPara("[PLACEHOLDER] Martins takes what Libra can use: supplies, machinery, metals, rare metals, organics and food.", 10f)
                val value = combined.stacksCopy.filter { it.isCommodityStack && it.commodityId != null }
                    .sumOf { KolLibra.commodityValue(it.commodityId, it.size).toDouble() }.roundToInt()
                panel.addPara("These goods would add %s to Libra's restoration.", 10f, Misc.getHighlightColor(),
                    Misc.getWithDGS(value.toFloat()))
            }
        })
    }

    /** Donated ships: accepted manufacturers, valued at current value × the multiplier; their hull types are kept. */
    private fun donate(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>) {
        val fleet = Global.getSector().playerFleet
        val pool = fleet.fleetData.membersListCopy.filter { KolLibra.acceptsShip(it) }
        dialog.showFleetMemberPickerDialog("Ships for Libra", "Confirm", "Cancel", 3, 7, 58f, true, true, pool,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: MutableList<FleetMemberAPI>?) {
                    if (members.isNullOrEmpty()) { done(dialog, map); return }
                    var value = 0f
                    for (m in members) {
                        value += KolLibra.shipValue(m)
                        KolLibra.addToRoster(m.hullId)
                        fleet.fleetData.removeFleetMember(m)
                        AddRemoveCommodity.addFleetMemberLossText(m, dialog.textPanel)
                    }
                    KolLibra.contribute(value.roundToInt(), dialog, "Ships for Libra")
                    FireAll.fire(null, dialog, map, "KolLibraAfterDonation")
                    done(dialog, map)
                }

                override fun cancelledFleetMemberPicking() = done(dialog, map)
            })
    }
}
