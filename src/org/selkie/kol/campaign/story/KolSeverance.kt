package org.selkie.kol.campaign.story

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

/**
 * Severance: failing out of the Knights story. One place for the cleanup, so later hooks can decide here what
 * survives. Severing ends every Knights story mission and intel and wipes the story's state; afterwards the story
 * can't restart because Enarms' introduction checks `$kol_severed` (the only check). Consequences that depend on
 * how the story was severed (reputation, scenes) belong to the callers: the pirate swarm, the inquest's and the
 * Shrouded handover's excommunications.
 *
 * Kept: the notice-giver before the story (`$kolPreludeHook*`), the remembered people (`$kolBench*`, `$kolPerson*`,
 * and every important person), the chronicle (`$kolChron*`), the tech arrangement's results (`$kolTech*`: stage and
 * stage flags, e.g. the Order flying Ordained ships), excommunication (`$kol_excommunicated`, its own flag), world
 * facts outside the story (bosses, Libra's market, takeovers), desertion, and the mod's other content. The situations
 * are ended, their results kept.
 */
object KolSeverance {
    const val SEVERED = "\$kol_severed"
    const val REASON = "\$kol_severedReason"
    private const val PENDING = "\$kol_severPending"

    /** Story keys wiped by severance, by prefix (sector, market, entity and person memory). */
    private val WIPED = listOf(
        "\$kolPrelude", "\$kolCh1", "\$kolCh2", "\$kolPatron", "\$kolConv", "\$kolAssembly", "\$kolInq", "\$kolInquest",
        "\$kolDuties", "\$kolDutyPatrol", "\$kolDock", "\$kolJointOp",
        "\$kol_prelude_done", "\$kol_ch1_done", "\$kol_chapter", "\$kol_agentAnswer", "\$kol_outsidePower", "\$kol_patron",
    )

    /** Exceptions to [WIPED]. */
    private val KEPT = listOf("\$kolPreludeHook")

    /** Saved story data removed by severance (persistent data keys). */
    private val DATA = listOf("kol_assemblyData", "kol_dockEvents", "kol_patronFleets")

    fun severed(): Boolean = Global.getSector().memoryWithoutUpdate.getBoolean(SEVERED)

    /**
     * Severs ties for [reason] (`pirates`, `inquest`, `shrouded`). Runs at once when no dialog is open, otherwise at
     * the next moment none is (the dialog that caused it may still be reading the story's keys).
     */
    fun sever(reason: String) {
        Global.getSector().memoryWithoutUpdate.set(PENDING, reason)
        if (safe()) run()
    }

    private fun safe(): Boolean {
        val sector = Global.getSector()
        return Global.getCurrentState() == GameState.CAMPAIGN && !sector.campaignUI.isShowingDialog && !sector.isInNewGameAdvance
    }

    /** Runs a pending severance once it's safe (KolSeveranceScript). */
    fun tick() {
        if (Global.getSector().memoryWithoutUpdate.contains(PENDING) && safe()) run()
    }

    private fun run() {
        val sector = Global.getSector()
        val memory = sector.memoryWithoutUpdate
        val reason = memory.getString(PENDING) ?: return
        memory.unset(PENDING)

        // every Knights story mission and intel ends (the notice-giver's mission is the notice; it stays)
        for (intel in ArrayList<IntelInfoPlugin>(sector.intelManager.intel)) {
            if (!storyIntel(intel)) continue
            if (intel is BaseHubMission && intel.currentStage != null && !intel.isEnding && !intel.isEnded) {
                intel.setAbandonStage(BaseHubMission.Abandon.ABANDON)
                intel.setCurrentStage(BaseHubMission.Abandon.ABANDON, null, null)
            }
            (intel as? com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin)?.let { if (!it.isEnded) it.endImmediately() }
        }

        // the story's state
        wipe(memory)
        for (market in sector.economy.marketsCopy) {
            wipe(market.memoryWithoutUpdate)
            market.primaryEntity?.let { wipe(it.memoryWithoutUpdate) }
            for (person in market.peopleCopy) wipe(person.memoryWithoutUpdate)
        }
        for (person in sector.importantPeople.peopleCopy) person.person?.let { wipe(it.memoryWithoutUpdate) }
        wipe(sector.characterData.memoryWithoutUpdate)
        for (key in DATA) sector.persistentData.remove(key)

        memory.set(SEVERED, true)
        memory.set(REASON, reason)
    }

    /** Knights story intel: the mod's campaign classes named Kol*, except the notice-giver's mission. */
    private fun storyIntel(intel: IntelInfoPlugin): Boolean {
        val name = intel.javaClass.name
        if (!name.startsWith("org.selkie.kol.campaign.")) return false
        val simple = intel.javaClass.simpleName
        return simple.startsWith("Kol") && simple != "KolPreludeHook" && simple != "KolPirateCut"
    }

    private fun wipe(memory: MemoryAPI) {
        for (key in ArrayList(memory.keys)) {
            val k = if (key.startsWith("$")) key else "$$key"
            if (WIPED.any { k.startsWith(it) } && KEPT.none { k.startsWith(it) }) memory.unset(k)
        }
    }
}

/** Runs a deferred severance at the first moment no dialog is open (transient; registered at each load). */
class KolSeveranceScript : EveryFrameScript {
    private val interval = IntervalUtil(0.05f, 0.1f)
    override fun isDone() = false
    override fun runWhilePaused() = false
    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (interval.intervalElapsed()) KolSeverance.tick()
    }
}
