package org.selkie.kol.campaign.situations

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.StageIconSize
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.tech.KolTechData
import org.selkie.kol.campaign.tech.KolTechSettings
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import org.selkie.kol.helpers.KolStaticStrings.KolTech
import java.awt.Color

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
        val person = contactId?.let { Global.getSector().importantPeople.getPerson(it) } ?: return
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
        contactId?.let { Global.getSector().importantPeople.getPerson(it) }?.let { Misc.makeUnimportant(it, memKey) }
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

/**
 * Helensis' "bounty" on AI technology. The bar is the cover story (the Order's understanding of its enemy); under
 * it, the zealots grow stronger. Progress = lifetime scrip / scale (KolTechData); stages unlock the requisitions,
 * set flags for other content, and announce unique ships. Upgraded in place from Chapter 1's placeholder.
 */
class KolTechSituationIntel : KolSituationIntel(KolCh1.TECH_SITUATION_KEY) {

    /** Stage index = ordinal = $kolTech_stage. */
    enum class TechStage { START, REQUISITIONS, TRUSTED, ARMORY, CONSECRATED, MAX }

    override val title = "Knights: Samples of the Enemy"
    override val description = "Master Helensis has asked you to bring back technology taken from the AI fleets: " +
            "equipment, ships, cores. She calls it a bounty, paid for the Order's understanding of its enemy."
    override val whomToSee = "Master Helensis at Battlestation Cygnus."

    override fun setup() {
        factors.clear()
        stages.clear()
        val scale = KolTechSettings.scripPerProgress
        val thresholds = KolTechSettings.stageThresholds
        setMaxProgress(KolTechSettings.maxScrip / scale)
        addStage(TechStage.START, 0)
        addStage(TechStage.REQUISITIONS, thresholds[0] / scale, StageIconSize.SMALL)
        addStage(TechStage.TRUSTED, thresholds[1] / scale, StageIconSize.MEDIUM)
        addStage(TechStage.ARMORY, thresholds[2] / scale, StageIconSize.MEDIUM)
        addStage(TechStage.CONSECRATED, thresholds[3] / scale, StageIconSize.LARGE)
        addStage(TechStage.MAX, maxProgress, StageIconSize.LARGE)
    }

    override val contactId: String get() = KolCh1.HELENSIS_ID

    /** Upgrades Chapter 1's placeholder (START/END stages). Touches no sector state: see syncAfterLoad(). */
    @Suppress("unused")
    private fun readResolve(): Any {
        if (getDataFor(TechStage.REQUISITIONS) == null) setup()
        return this
    }

    /** Keeps the bar equal to lifetime scrip / scale (also repairs saves where a load reset it). */
    override fun syncAfterLoad() {
        super.syncAfterLoad()
        val bar = barFor(KolTechData.get().lifetimeScrip)
        if (progress != bar) setProgress(bar)
    }

    private fun barFor(lifetime: Int) = (lifetime / KolTechSettings.scripPerProgress).coerceAtMost(maxProgress)

    /**
     * Credits a handover: scrip to both totals, then the bar moves to lifetime / scale. Measured against the bar's
     * actual position, so scrip earned while the situation didn't exist yet (or any drift) is caught up here.
     */
    fun credit(scrip: Int, dialog: InteractionDialogAPI?) {
        if (scrip <= 0) return
        ensureSynced()
        KolTechData.get().earn(scrip)
        val delta = barFor(KolTechData.get().lifetimeScrip) - progress
        if (delta > 0) addFactor(KolTechHandoverFactor(delta, scrip), dialog)
    }

    /** Current stage index (0 START .. 5 MAX), from the bar. */
    fun stageIndex(): Int = TechStage.values().lastOrNull { isStageActive(it) }?.ordinal ?: 0

    override fun notifyStageReached(stage: EventStageData) {
        super.notifyStageReached(stage)
        val reached = stage.id as? TechStage ?: return
        val memory = Global.getSector().memoryWithoutUpdate
        memory.set(KolTech.STAGE, reached.ordinal)
        memory.set(KolTech.STAGE_FLAGS[reached.ordinal], true)
        if (reached != TechStage.START) org.selkie.kol.campaign.story.KolAssembly.report("techStage")
        // unique ships announced at this stage (hidden while their variant doesn't exist yet)
        val data = KolTechData.get()
        for (slot in KolTechSettings.uniques) {
            if (slot.stage == reached.ordinal && Global.getSettings().doesVariantExist(slot.variant)) {
                data.uniquesUnlocked.add(slot.slot)
                memory.set("\$kolTech_uniqueUnlocked${slot.slot}", true)
            }
        }
    }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode, isUpdate: Boolean,
                                 tc: Color, initPad: Float) {
        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) return
        val esd = listInfoParam as? EventStageData ?: return
        if (isUpdate) stageUnlockText(esd.id as? TechStage)?.let { info.addPara(it, tc, initPad) }
    }

    override fun getStageIconImpl(stageId: Any?): String =
        if (stageId == TechStage.START) icon else Global.getSettings().getSpriteName("events", "stage_unknown_good")

    override fun addStageDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        if (!isStageActive(stageId) || stageId != TechStage.values().last { isStageActive(it) }) return
        if (stageId == TechStage.START) {
            info.addPara(description, 0f)
        } else {
            info.addPara(stageDescription(stageId as TechStage), 0f)
        }
        info.addPara("See: %s", 10f, Misc.getHighlightColor(), whomToSee)
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        val stage = stageId as? TechStage ?: return null
        if (stage == TechStage.START) return null
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addTitle(stageTitle(stage))
                tooltip.addPara(stageDescription(stage), 10f)
                stageUnlockText(stage)?.let { tooltip.addPara(it, Misc.getHighlightColor(), 10f) }
            }
        }
    }

    // Cover-story texts (placeholder wording)
    private fun stageTitle(stage: TechStage) = when (stage) {
        TechStage.START -> "Bounty"
        TechStage.REQUISITIONS -> "Requisitions"
        TechStage.TRUSTED -> "Trusted Supplier"
        TechStage.ARMORY -> "The Armory"
        TechStage.CONSECRATED -> "Consecrated"
        TechStage.MAX -> "Full Understanding"
    }

    private fun stageDescription(stage: TechStage) = when (stage) {
        TechStage.START -> description
        TechStage.REQUISITIONS -> "Master Helensis' scholars have begun their study. She will requisition ordained " +
                "equipment from the Order's stores in exchange for scrip."
        TechStage.TRUSTED -> "The Order counts you a trusted supplier. Helensis will accept whole hulls, and offers " +
                "officers and larger hulls from the Order's stores."
        TechStage.ARMORY -> "The study has grown. The Order's armory opens further to you."
        TechStage.CONSECRATED -> "What you bring is put to use, and the Order's whole catalog is open to you."
        TechStage.MAX -> "The Order understands its enemy as well as it ever will."
    }

    private fun stageUnlockText(stage: TechStage?): String? = when (stage) {
        TechStage.REQUISITIONS -> "Requisitions open: small and medium weapons, fighters"
        TechStage.TRUSTED -> "Ship handover; officers; frigates and destroyers"
        TechStage.ARMORY -> "Large weapons and cruisers"
        TechStage.CONSECRATED -> "Capital ships"
        else -> null
    }

    companion object {
        @JvmStatic
        fun get(): KolTechSituationIntel? =
            Global.getSector().memoryWithoutUpdate.get(KolCh1.TECH_SITUATION_KEY) as? KolTechSituationIntel
    }
}

/** One handover: the bar moves by [points]; the tooltip names the scrip earned. */
class KolTechHandoverFactor(points: Int, private val scrip: Int) : BaseOneTimeFactor(points) {
    override fun getDesc(intel: BaseEventIntel?): String = "Technology handed over"

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator =
        object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara("You handed over technology taken from the AI fleets, earning %s scrip.", 0f,
                    Misc.getHighlightColor(), Misc.getWithDGS(scrip.toFloat()))
            }
        }
}

/** Enarms' invitation to do what you can for Libra, as charity the Order does not forbid. */
class KolLibraSituationIntel : KolSituationIntel(KolCh1.LIBRA_SITUATION_KEY) {
    override val title = "Knights: Charity for Libra"
    override val description = "The council judged Battlestar Libra too costly to recommission. It has been " +
            "written off, but not disowned, and nothing in the Order's writ forbids accepting charity on its behalf. " +
            "Brother Enarms asks you to do what you can."
    override val whomToSee = "Knightmaster Martins at Battlestar Libra, or Brother Enarms at Battlestation Cygnus."
    override val contactId: String get() = KolStaticStrings.KolPrelude.MARTINS_ID
}
