package org.selkie.kol.campaign.situations

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.StageIconSize
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import java.awt.Color

/**
 * Placeholder situation (progress-bar event intel) opened at the end of Knights Chapter 1.
 * Only a start stage and an end stage for now; each branch's own change adds factors, stages and missions.
 * Work in progress: later redesigns may leave dangling memory entries in older saves.
 */
abstract class KolSituationIntel(private val memKey: String) : BaseEventIntel() {

    enum class Stage { START, END }

    init {
        Global.getSector().memoryWithoutUpdate.set(memKey, this)
        setup()
    }

    protected fun setup() {
        factors.clear()
        stages.clear()
        setMaxProgress(MAX_PROGRESS)
        addStage(Stage.START, 0)
        addStage(Stage.END, MAX_PROGRESS, StageIconSize.LARGE)
    }

    /** Adds the intel (call once construction is complete, as vanilla events do). Starts marked important. */
    fun announce(text: TextPanelAPI?) {
        isImportant = true
        Global.getSector().intelManager.addIntel(this, false, text)
    }

    /** Listed with accepted missions as well as major events: these are optional story threads. */
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_ACCEPTED)
        tags.add(KolStaticStrings.kolFactionID)
        return tags
    }

    protected abstract val title: String
    protected abstract val description: String
    protected abstract val whomToSee: String

    override fun getName(): String = title

    override fun getIcon(): String = Global.getSector().getFaction(KolStaticStrings.kolFactionID).crest

    override fun getBarColor(): Color = Global.getSector().getFaction(KolStaticStrings.kolFactionID).baseUIColor

    // Must be overridden: vanilla's default points at events/stage_unknown, which 0.98a doesn't define (crashes the intel screen)
    override fun getStageIconImpl(stageId: Any?): String =
        if (stageId == Stage.START) icon else Global.getSettings().getSpriteName("events", "stage_unknown_neutral")

    override fun addStageDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        if (stageId != Stage.START || !isStageActive(stageId)) return
        info.addPara(description, 0f)
        info.addPara("See: %s", 10f, Misc.getHighlightColor(), whomToSee)
    }

    override fun notifyEnded() {
        super.notifyEnded()
        Global.getSector().memoryWithoutUpdate.unset(memKey)
    }

    companion object {
        const val MAX_PROGRESS = 100

        /** Opens both Chapter 1 situations if they don't exist yet. */
        fun openChapterOneSituations(text: TextPanelAPI?) {
            val memory = Global.getSector().memoryWithoutUpdate
            if (memory.get(KolCh1.TECH_SITUATION_KEY) == null) KolTechSituationIntel().announce(text)
            if (memory.get(KolCh1.LIBRA_SITUATION_KEY) == null) KolLibraSituationIntel().announce(text)
        }
    }
}

/** Helensis' "bounty" on AI technology: a quiet strengthening of the zealous camp. */
class KolTechSituationIntel : KolSituationIntel(KolCh1.TECH_SITUATION_KEY) {
    override val title = "Knights: Samples of the Enemy"
    override val description = "Master Helensis has asked you to bring back technology taken from the AI fleets: " +
            "equipment, ships, cores. She calls it a bounty, paid for the Order's understanding of its enemy."
    override val whomToSee = "Master Helensis at Battlestation Cygnus."
}

/** Enarms' invitation to do what you can for Libra, as charity the Order does not forbid. */
class KolLibraSituationIntel : KolSituationIntel(KolCh1.LIBRA_SITUATION_KEY) {
    override val title = "Knights: Charity for Libra"
    override val description = "The council judged Battlestar Libra too costly to recommission. It has been " +
            "written off, but not disowned, and nothing in the Order's writ forbids accepting charity on its behalf. " +
            "Brother Enarms asks you to do what you can."
    override val whomToSee = "Knightmaster Martins at Battlestar Libra, or Brother Enarms at Battlestation Cygnus."
}
