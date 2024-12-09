package org.selkie.zea.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.GateCMD
import com.fs.starfarer.api.util.Misc
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import org.selkie.zea.campaign.NullspaceGateTransferVFXRenderer

class NullGateCMD : BaseCommandPlugin() {
    override fun execute(ruleId: String?, dialog: InteractionDialogAPI?, params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {

        val command = params!![0].getString(memoryMap)

        if (command == "CanBeAdded") {
            var data = GateEntityPlugin.getGateData()
            var gate = data.scanned.find { it.id.contains("zea_nullgate_dusk") }
            return gate != null && dialog!!.interactionTarget != gate
        }

        if (command == "Highlight") {
            var data = GateEntityPlugin.getGateData()
            var gate = data.scanned.find { it.id.contains("zea_nullgate_dusk") }
            if (gate != null && dialog!!.interactionTarget != gate) {
                var requiredFuel = GateCMD.computeFuelCost(gate)

                var enoughFuel = requiredFuel <= Global.getSector().playerFleet.cargo.fuel

                if (!enoughFuel) {
                    dialog.optionPanel.setEnabled("NULL_GATE", false)
                }

                dialog.optionPanel.addOptionTooltipAppender("NULL_GATE") { tooltip, hadOtherText ->
                    tooltip!!.addPara("Travel towards the Abyssal Gate. Requires ${requiredFuel.toInt()} Fuel.",
                        0f, Misc.getTextColor(), Misc.getHighlightColor(), "$requiredFuel")

                    if (!enoughFuel) {
                        tooltip.addSpacer(5f)
                        tooltip.addPara("The fleet does not have enough fuel.", 0f, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor())
                    }

                    tooltip.addSpacer(2f)
                }
            }
        }

        if (command == "Traverse") {

            var data = GateEntityPlugin.getGateData()
            var gate = data.scanned.find { it.id.contains("zea_nullgate_dusk") }

            var requiredFuel = GateCMD.computeFuelCost(gate)
            Global.getSector().playerFleet.cargo.removeFuel(requiredFuel.toFloat())


            Global.getSector().doHyperspaceTransition(Global.getSector().playerFleet, dialog!!.interactionTarget, JumpPointAPI.JumpDestination(gate, ""), 7.5f)

            //Add the VFX Renderer, will be removed after the jump.
            LunaCampaignRenderer.addRenderer(NullspaceGateTransferVFXRenderer(dialog!!.interactionTarget, false))


            dialog.dismiss()
        }


        return false
    }

}