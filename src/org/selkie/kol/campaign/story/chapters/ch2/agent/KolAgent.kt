package org.selkie.kol.campaign.story.chapters.ch2.agent

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.systems.chronicle.KolChronicle
import org.selkie.kol.campaign.story.chapters.ch2.patron.PatronKeys
import org.selkie.kol.campaign.story.core.KolIds

/** The outside power and its agent. */
object KolAgent {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /**
     * The outside power, chosen once from the player's lore: Dawn and Dusk lore (Project Dawn/Dusk) point to
     * Tri-Tachyon; Elysian lore and the Hegemony witness to the Hegemony; the larger count wins; Tri-Tachyon with no
     * lore. Never the patron.
     */
    fun outsidePower(): String {
        memory.getString(AgentKeys.OUTSIDE_POWER)?.let { if (it.isNotEmpty()) return it }
        val tt = KolChronicle.loreCount(KolChronicle.DAWN) + KolChronicle.loreCount(KolChronicle.DUSK) +
                KolChronicle.loreCount(KolChronicle.TRITACH)
        val heg = KolChronicle.loreCount(KolChronicle.ELYSIAN) + KolChronicle.loreCount("hegemony")
        var pick = if (heg > tt) Factions.HEGEMONY else Factions.TRITACHYON
        if (pick == memory.getString(PatronKeys.PATRON)) pick = if (pick == Factions.HEGEMONY) Factions.TRITACHYON else Factions.HEGEMONY
        memory.set(AgentKeys.OUTSIDE_POWER, pick)
        return pick
    }

    /** The outside power's agent, remembered so they can return in Chapter 3. */
    fun agent(): PersonAPI = KolPeople.remembered(KolIds.AGENT) {
        Global.getSector().getFaction(outsidePower()).createRandomPerson().apply {
            postId = Ranks.POST_AGENT
        }
    }
}
