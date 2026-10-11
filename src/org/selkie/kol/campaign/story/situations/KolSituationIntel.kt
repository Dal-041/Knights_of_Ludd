package org.selkie.kol.campaign.story.situations

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.situations.tech.KolTechSituationIntel
import org.selkie.kol.campaign.story.situations.libra.KolLibraSituationIntel
import org.selkie.kol.campaign.story.situations.libra.LibraKeys
import org.selkie.kol.campaign.story.situations.tech.TechKeys
import org.selkie.kol.campaign.story.core.KolIds

/**
 * Situations (progress-bar event intels) opened at the end of Knights Chapter 1. The base is a placeholder with a
 * start and an end stage; a branch replaces setup() with its own ladder (see KolTechSituationIntel).
 */
abstract class KolSituationIntel(private val memKey: String) : BaseEventIntel() {

    enum class Stage { START, END }

    init {
        Global.getSector().memoryWithoutUpdate.set(memKey, this)
        setup()
    }

    protected open fun setup() {
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
        markContact()
    }

    /** The person to see about this situation, marked important while it runs (null: nobody). */
    protected abstract val contactId: String?

    protected fun markContact() {
        val person = contactId?.let { KolPeople.get(it) } ?: return
        Misc.makeImportant(person, memKey)
    }

    /**
     * Once per session, on the first tick after load: the sector is fully loaded here, unlike in readResolve()
     * (which runs mid-deserialization, when Global.getSector() isn't the loaded game yet).
     */
    @Transient private var syncedThisSession = false

    protected open fun syncAfterLoad() {
        markContact()
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        ensureSynced()
    }

    protected fun ensureSynced() {
        if (syncedThisSession) return
        syncedThisSession = true
        syncAfterLoad()
    }

    /** Listed with accepted missions as well as major events: these are optional story threads. */
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_ACCEPTED)
        tags.add(KolIds.FACTION)
        return tags
    }

    protected abstract val title: String
    protected abstract val description: String
    protected abstract val whomToSee: String

    override fun getName(): String = title

    override fun getIcon(): String = Global.getSector().getFaction(KolIds.FACTION).crest

    override fun getBarColor(): Color = Global.getSector().getFaction(KolIds.FACTION).baseUIColor

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
        contactId?.let { KolPeople.get(it) }?.let { Misc.makeUnimportant(it, memKey) }
    }

    companion object {
        const val MAX_PROGRESS = 100

        /** Opens both Chapter 1 situations if they don't exist yet. */
        fun openChapterOneSituations(text: TextPanelAPI?) {
            val memory = Global.getSector().memoryWithoutUpdate
            if (memory.get(TechKeys.SITUATION_KEY) == null) KolTechSituationIntel().announce(text)
            if (memory.get(LibraKeys.SITUATION_KEY) == null) KolLibraSituationIntel().announce(text)
        }
    }
}
