package org.selkie.kol.campaign.story.situations.tech.milestones.inquest

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.selkie.kol.campaign.story.core.KolIds

/** The Inquisition's summons at TRUSTED: the inquest into the player at Cygnus (dock event; kolInquest_* rules). */
class KolPlayerInquest : HubMissionWithSearch() {
    companion object { const val STAGE_DONE = "\$kolInquest_stageDone" }

    enum class Stage { SUMMONED, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(InquestKeys.INQUEST_REF, InquestKeys.INQUEST_ACTIVE)) return false
        val cygnus = Global.getSector().economy.getMarket(KolIds.CYGNUS) ?: return false
        setStartingStage(Stage.SUMMONED)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        makeImportant(cygnus.primaryEntity, "\$kolInquest_summons", Stage.SUMMONED)
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        KolInquest.addInquestEvent()
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "outcome") {
            val outcome = params?.getOrNull(1)?.getString(memoryMap) ?: return false
            KolInquest.recordInquest(outcome)
            checkStageChangesAndTriggers(dialog, memoryMap)
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage == Stage.SUMMONED) info.addPara("[PLACEHOLDER] The Inquisition has summoned you to Battlestation " +
                "Cygnus to answer questions about your service to the Order.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage != Stage.SUMMONED) return false
        info.addPara("Answer the summons at Battlestation Cygnus", tc, pad)
        return true
    }

    override fun getBaseName(): String = "Summoned"
}
