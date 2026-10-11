package org.selkie.kol.campaign.story.situations.libra

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.selkie.kol.campaign.story.situations.KolSituationIntel
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly

/**
 * Battlestar Libra's restoration (add-knights-libra-situation). The bar's own progress is the record: contributions
 * are one-time factors, the supply lines a monthly factor (KolLibraSettings). Four thresholds gate the milestone
 * events, each also needing the previous one done (KolLibra.open). Upgraded in place from Chapter 1's placeholder.
 */
class KolLibraSituationIntel : KolSituationIntel(LibraKeys.SITUATION_KEY) {

    /** Stage index = ordinal; SUPPLY..RESTORATION are the milestones' thresholds. */
    enum class LibraStage { START, SUPPLY, CREW, DRILL, RESTORATION, MAX }

    override val title = "Knights: Battlestar Libra"
    override val description = "The council judged Battlestar Libra too costly to recommission. It has been " +
            "written off, but not disowned, and nothing in the Order's writ forbids accepting charity on its behalf. " +
            "Knight-Master Martins takes what you can bring."
    override val whomToSee = "Knight-Master Martins at Battlestar Libra."
    override val contactId: String get() = KolIds.MARTINS

    override fun setup() {
        factors.clear()
        stages.clear()
        val t = KolLibraSettings.stageThresholds
        setMaxProgress(KolLibraSettings.maxProgress)
        addStage(LibraStage.START, 0)
        addStage(LibraStage.SUPPLY, t[0], StageIconSize.SMALL)
        addStage(LibraStage.CREW, t[1], StageIconSize.MEDIUM)
        addStage(LibraStage.DRILL, t[2], StageIconSize.MEDIUM)
        addStage(LibraStage.RESTORATION, t[3], StageIconSize.LARGE)
        addStage(LibraStage.MAX, maxProgress, StageIconSize.LARGE)
        addFactor(KolLibraLinesFactor())
    }

    /** Upgrades Chapter 1's placeholder (START/END stages). Touches no sector state: see syncAfterLoad(). */
    @Suppress("unused")
    private fun readResolve(): Any {
        if (getDataFor(LibraStage.SUPPLY) == null) setup()
        return this
    }

    /** Keeps Libra's market effects current. */
    override fun syncAfterLoad() {
        super.syncAfterLoad()
        KolLibraEffects.apply()
    }

    @Transient private var stationCheck: IntervalUtil? = null

    /** The station's damage penalty is reapplied now and then: its fleet is re-created by the industry. */
    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        val check = stationCheck ?: IntervalUtil(1f, 2f).also { stationCheck = it }
        check.advance(Misc.getDays(amount))
        if (check.intervalElapsed()) KolLibraEffects.stationPenalty()
    }

    /** The milestone's threshold is reached (its stage is active). */
    fun reached(m: KolLibraMilestone): Boolean = isStageActive(LibraStage.values()[m.ordinal + 1])

    /** Current stage index (0 START .. 5 MAX), from the bar. */
    fun stageIndex(): Int = LibraStage.values().lastOrNull { isStageActive(it) }?.ordinal ?: 0

    override fun notifyStageReached(stage: EventStageData) {
        super.notifyStageReached(stage)
        val reached = stage.id as? LibraStage ?: return
        Global.getSector().memoryWithoutUpdate.set("\$kolLibra_stage", reached.ordinal)
        Global.getSector().memoryWithoutUpdate.set("\$kolLibra_${reached.name.lowercase()}", true)
        if (reached != LibraStage.START) KolAssembly.report("libraStage")
    }

    /** A milestone event completed: an update for the player. */
    fun milestoneCompleted(m: KolLibraMilestone, text: TextPanelAPI?) {
        sendUpdateIfPlayerHasIntel(m, text)
    }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode, isUpdate: Boolean,
                                 tc: Color, initPad: Float) {
        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) return
        when (val param = listInfoParam) {
            is KolLibraMilestone -> info.addPara(milestoneDoneText(param), tc, initPad)
            is EventStageData -> if (isUpdate) (param.id as? LibraStage)?.let { stageUnlockText(it) }?.let { info.addPara(it, tc, initPad) }
        }
    }

    override fun getStageIconImpl(stageId: Any?): String =
        if (stageId == LibraStage.START) icon else Global.getSettings().getSpriteName("events", "stage_unknown_good")

    override fun addStageDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        if (!isStageActive(stageId) || stageId != LibraStage.values().last { isStageActive(it) }) return
        info.addPara(if (stageId == LibraStage.START) description else stageDescription(stageId as LibraStage), 0f)
        info.addPara("See: %s", 10f, Misc.getHighlightColor(), whomToSee)
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        val stage = stageId as? LibraStage ?: return null
        if (stage == LibraStage.START) return null
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addTitle(stageTitle(stage))
                tooltip.addPara(stageDescription(stage), 10f)
                stageUnlockText(stage)?.let { tooltip.addPara(it, Misc.getHighlightColor(), 10f) }
            }
        }
    }

    // [PLACEHOLDER] texts
    private fun stageTitle(stage: LibraStage) = when (stage) {
        LibraStage.START -> "Charity"
        LibraStage.SUPPLY -> "A Supply Line"
        LibraStage.CREW -> "A Crew of Its Own"
        LibraStage.DRILL -> "The Drill"
        LibraStage.RESTORATION -> "Restoration"
        LibraStage.MAX -> "Restored"
    }

    private fun stageDescription(stage: LibraStage) = when (stage) {
        LibraStage.START -> description
        LibraStage.SUPPLY -> "[PLACEHOLDER] Libra can be kept on charity a while longer, but not repaired. Martins wants a standing supply line."
        LibraStage.CREW -> "[PLACEHOLDER] The station is mending. Martins wants a crew of Libra's own, from a congregation that will send its people."
        LibraStage.DRILL -> "[PLACEHOLDER] Libra has hands enough to man its guns. Martins wants to see whether they can shoot."
        LibraStage.RESTORATION -> "[PLACEHOLDER] Everything Libra needs is aboard. What remains is to bring it back."
        LibraStage.MAX -> "[PLACEHOLDER] Libra has everything it can be given."
    }

    private fun stageUnlockText(stage: LibraStage): String? = when (stage) {
        LibraStage.SUPPLY -> "[PLACEHOLDER] Martins can arrange the supply contract"
        LibraStage.CREW -> "[PLACEHOLDER] Martins can arrange the crew contract, once the supply contract is done"
        LibraStage.DRILL -> "[PLACEHOLDER] Martins can stage the drill, once the crew contract is done"
        LibraStage.RESTORATION -> "[PLACEHOLDER] Libra can be restored, once the drill is done"
        else -> null
    }

    private fun milestoneDoneText(m: KolLibraMilestone) = when (m) {
        KolLibraMilestone.SUPPLY -> "[PLACEHOLDER] Supply contract complete: Libra joins the economy"
        KolLibraMilestone.CREW -> "[PLACEHOLDER] Crew contract complete: Libra has a crew of its own"
        KolLibraMilestone.DRILL -> "[PLACEHOLDER] The drill is done: gunnery training is open"
        KolLibraMilestone.RESTORATION -> "[PLACEHOLDER] Battlestar Libra is restored"
    }

    companion object {
        @JvmStatic
        fun get(): KolLibraSituationIntel? =
            Global.getSector().memoryWithoutUpdate.get(LibraKeys.SITUATION_KEY) as? KolLibraSituationIntel
    }
}

/** A one-time contribution to Libra (negative for a loss), listed under its source. */
class KolLibraFactor(progress: Int, private val reason: String) : BaseOneTimeFactor(progress) {
    override fun getDesc(intel: BaseEventIntel?): String = reason
}

/** The penned supply lines, monthly. */
class KolLibraLinesFactor : BaseEventFactor() {
    private fun lines() = if (KolLibra.active()) KolLibra.data().lines.size else 0

    override fun getProgress(intel: BaseEventIntel?): Int = lines() * KolLibraSettings.lineMonthlyProgress

    override fun shouldShow(intel: BaseEventIntel?): Boolean = lines() > 0

    override fun getDesc(intel: BaseEventIntel?): String = "Supply lines"

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator =
        object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara("[PLACEHOLDER] %s of 4 supply lines penned, each adding %s a month.", 0f,
                    Misc.getHighlightColor(), "" + lines(), "" + KolLibraSettings.lineMonthlyProgress)
            }
        }
}
