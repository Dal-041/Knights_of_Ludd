package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import org.selkie.kol.helpers.KolStaticStrings.KolPrelude
import java.awt.Color

/**
 * Knights Chapter 1: the return leg after the make-work bounty. Started from KolCh1Bounty when the target is
 * beaten; points the player back to Enarms at Cygnus without revealing the council. Completes when the
 * player docks at Cygnus (kolCh1_dockNotice, or kolCh1_council as a fallback: Call ... complete).
 */
class KolCh1Return : HubMissionWithSearch() {

    enum class Stage { RETURN, COMPLETED }

    private var enarms: PersonAPI? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(KolCh1.RETURN_REF, KolCh1.RETURN_ACTIVE)) return false
        enarms = Global.getSector().importantPeople.getPerson(KolPrelude.ENARMS_ID) ?: return false
        if (enarms!!.market == null) return false

        setStartingStage(Stage.RETURN)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()

        makeImportant(enarms!!, "\$kolCh1Return_report", Stage.RETURN)
        setStageOnGlobalFlag(Stage.COMPLETED, KolCh1.RETURN_DONE)
        return true
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "complete") {
            // complete now, inside the docking dialog (the periodic stage check doesn't run while the game is paused)
            Global.getSector().memoryWithoutUpdate.set(KolCh1.RETURN_DONE, true)
            checkStageChangesAndTriggers(dialog, memoryMap)
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage == Stage.RETURN) {
            info.addPara("The raiders preying on the Luddic merchants are dealt with. ${enarms?.nameString} will want " +
                    "to hear it from you. Return to him at ${enarms?.market?.name}.", 10f)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage != Stage.RETURN) return false
        info.addPara("Return to ${enarms?.nameString} at ${enarms?.market?.name}", tc, pad)
        return true
    }

    override fun getBaseName(): String = "Back to Cygnus"

    companion object {
        /** Starts the return leg from code (no dialog): created from its spec with Enarms as the giver. */
        fun start() {
            val enarms = Global.getSector().importantPeople.getPerson(KolPrelude.ENARMS_ID) ?: return
            val mission = Global.getSettings().getMissionSpec(KolCh1.RETURN_ID)?.createMission() ?: return
            mission.setPersonOverride(enarms)
            mission.createAndAbortIfFailed(enarms.market, false)
            if (mission.isMissionCreationAborted) return
            mission.accept(null, null)
        }
    }
}
