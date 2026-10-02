package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.selkie.kol.campaign.situations.KolSituationIntel
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import java.awt.Color

/**
 * Knights Chapter 1, the hand-off: the council refers the player to Sister Greenflight, the Master of Agents at
 * Star Keep Lyra. Accepting it also opens the two placeholder situations (Technology, Libra). The first meeting
 * with Greenflight sets $kolCh1Lyra_met (-> COMPLETED) and concludes the chapter ($kol_ch1_done).
 * Dialogue: rules.csv, kolCh1_council* / kolCh1_greenflight*.
 */
class KolCh1Lyra : HubMissionWithSearch() {

    enum class Stage { MEET, COMPLETED }

    companion object {
        /** Vanilla's Alpha Site system (TTBlackSite); the mod's PrepareDarkDeeds looks it up the same way. */
        const val ALPHA_SITE_NAME = "Unknown Location"
    }

    private var greenflight: PersonAPI? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(KolCh1.LYRA_REF, KolCh1.LYRA_ACTIVE)) return false
        greenflight = Global.getSector().importantPeople.getPerson(KolCh1.GREENFLIGHT_ID) ?: return false
        if (greenflight!!.market == null) return false

        setStartingStage(Stage.MEET)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()

        makeImportant(greenflight!!, "\$kolCh1Lyra_meet", Stage.MEET)
        setStageOnGlobalFlag(Stage.COMPLETED, KolCh1.GREENFLIGHT_MET)
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        KolSituationIntel.openChapterOneSituations(dialog?.textPanel)
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<com.fs.starfarer.api.util.Misc.Token>?,
                            memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        when (action) {
            "met" -> {
                Global.getSector().memoryWithoutUpdate.set(KolCh1.GREENFLIGHT_MET, true)
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    /**
     * For Greenflight's lead: has the player been to Alpha Site (vanilla's "Unknown Location", the Academy's
     * Project Ziggurat target)? Entering the system or beating the Ziggurat both count; vanilla's
     * $gaPZ_scannedZiggurat is a stage flag and is gone once the Academy mission ends.
     */
    override fun updateInteractionDataImpl() {
        val memory = Global.getSector().memoryWithoutUpdate
        val alphaSite = Global.getSector().getStarSystem(ALPHA_SITE_NAME)
        val visited = alphaSite?.isEnteredByPlayer == true || memory.getBoolean(TTBlackSite.DEFEATED_ZIGGURAT_KEY)
        set("\$kolCh1Lyra_visitedAlphaSite", visited)
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        Global.getSector().memoryWithoutUpdate.set(KolCh1.CH1_DONE, true)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage == Stage.MEET) {
            info.addPara("The Knights' council has sent you to report what you have found to the Master of Agents, " +
                    "${greenflight?.nameString}, at ${greenflight?.market?.name}.", 10f)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage != Stage.MEET) return false
        info.addPara("Report to ${greenflight?.nameString} at ${greenflight?.market?.name}", tc, pad)
        return true
    }

    override fun getBaseName(): String = "The Inquisitor"
}
