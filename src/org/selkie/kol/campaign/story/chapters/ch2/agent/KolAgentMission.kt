package org.selkie.kol.campaign.story.chapters.ch2.agent

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.systems.chapter.KolChapter
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.chapters.ch2.jointops.JointOpsKeys
import org.selkie.kol.campaign.story.systems.chronicle.ChronicleKeys

private val memory get() = Global.getSector().memoryWithoutUpdate

/**
 * The outside power's agent: a bar encounter (large markets of that power, or independents) once the player's
 * Abyss progress is noticed. Never accepted as a mission; the answer is recorded (kolAgent_* rules).
 */
class KolCh2Agent : HubMissionWithBarEvent() {
    enum class Stage { TALK, DONE }

    override fun shouldShowAtMarket(market: MarketAPI): Boolean {
        if (KolChapter.get() < 2 || !memory.getBoolean(AgentKeys.AGENT_TRIGGERED) || memory.getBoolean(AgentKeys.AGENT_ANSWERED)) return false
        if (market.size < 5) return false
        val power = KolAgent.outsidePower()
        return market.factionId == power || market.factionId == Factions.INDEPENDENT
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!barEvent) return false
        if (!setGlobalReference(AgentKeys.AGENT_REF)) return false
        val power = Global.getSector().getFaction(KolAgent.outsidePower()) ?: return false
        setPersonOverride(KolAgent.agent())
        memory.set("\$kolAgent_powerName", power.displayName)
        memory.set("\$kolAgent_powerArticle", power.displayNameWithArticle)
        setStartingStage(Stage.TALK)
        addSuccessStages(Stage.DONE)
        setRepFactionChangesNone()
        setRepPersonChangesNone()
        return true
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "answer") {
            val answer = params?.getOrNull(1)?.getString(memoryMap) ?: return false
            memory.set(AgentKeys.AGENT_ANSWER, answer)
            memory.set(AgentKeys.AGENT_ANSWERED, true)
            if (answer == "comply") {
                val power = Global.getSector().getFaction(KolAgent.outsidePower())
                power?.adjustRelationship(Factions.PLAYER, 0.05f)
                dialog?.textPanel?.addPara("Relations with ${power?.displayNameWithArticle} improved", Misc.getPositiveHighlightColor())
            }
            KolAssembly.report("agent")
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun getBaseName(): String = "A Quiet Word"
}

/** The outside power's agent and its warning. */
object KolAgentModule : KolStoryModule {
    override val id = "agent"
    private val memory get() = Global.getSector().memoryWithoutUpdate

    override fun onLoad() = KolPeople.resolver("agent") { KolAgent.agent() }

    override fun requirements() = listOf(KolStoryModule.Requirement(2, "agentWarning",
        { memory.getBoolean(AgentKeys.AGENT_TRIGGERED) }, { memory.getBoolean(AgentKeys.AGENT_ANSWERED) }))

    /** The agent becomes due once the player has felled an Abyss power or finished the Ninaya operation. */
    override fun pulse() {
        if (KolChapter.get() < 2 || memory.getBoolean(AgentKeys.AGENT_TRIGGERED)) return
        if (memory.getInt(ChronicleKeys.CHRON_POWERS_DEFEATED) >= 1 || memory.getBoolean(JointOpsKeys.NINAYA_OP_DONE))
            memory.set(AgentKeys.AGENT_TRIGGERED, true)
    }

    override fun severedKeys() = listOf("\$kolAgent", "\$kol_outsidePower")
}
