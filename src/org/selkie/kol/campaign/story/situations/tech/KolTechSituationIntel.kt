package org.selkie.kol.campaign.story.situations.tech

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.selkie.kol.campaign.story.situations.KolSituationIntel
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly

/**
 * Helensis' "bounty" on AI technology. The bar is the cover story (the Order's understanding of its enemy); under
 * it, the zealots grow stronger. Progress = lifetime scrip / scale (KolTechData); stages unlock the requisitions,
 * set flags for other content, and announce unique ships. Upgraded in place from Chapter 1's placeholder.
 */
class KolTechSituationIntel : KolSituationIntel(TechKeys.SITUATION_KEY) {

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

    override val contactId: String get() = KolIds.HELENSIS

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
        memory.set(TechKeys.STAGE, reached.ordinal)
        memory.set(TechKeys.STAGE_FLAGS[reached.ordinal], true)
        if (reached != TechStage.START) KolAssembly.report("techStage")
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
            Global.getSector().memoryWithoutUpdate.get(TechKeys.SITUATION_KEY) as? KolTechSituationIntel
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
