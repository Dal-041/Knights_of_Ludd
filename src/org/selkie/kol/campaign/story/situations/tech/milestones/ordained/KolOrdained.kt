package org.selkie.kol.campaign.story.situations.tech.milestones.ordained

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.kol.campaign.story.systems.spawn.KolFleetSpawnHook
import org.selkie.kol.campaign.story.situations.tech.KolLowTech
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.systems.chapter.KolChapter
import org.selkie.kol.campaign.story.systems.cue.KolStoryCue

/** The Ordained rollouts' mark on the world (add-knights-tech-milestones). */
object KolOrdained {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** Share of eligible ships in a new Knights fleet that fly Ordained: none before the early rollout. */
    fun share(): Float = when {
        memory.getBoolean(OrdainedKeys.ORDAINED_FULL) -> KolStorySettings.ordainedShareFull
        memory.getBoolean(OrdainedKeys.ORDAINED_EARLY) -> KolStorySettings.ordainedShareEarly
        else -> 0f
    }

    /** Fleet-spawn modifier: after a rollout, moves a share of a Knights fleet's eligible ships onto their Ordained skins. */
    class Modifier : KolFleetSpawnHook.Modifier {
        override fun apply(fleet: CampaignFleetAPI): Boolean {
            if (fleet.faction?.id != KolIds.FACTION) return false
            val share = share()
            if (share <= 0f) return false
            var converted = 0
            for (member in fleet.fleetData.membersListCopy) {
                if (member.isFighterWing || member.isStation || KolLowTech.isConverted(member.variant)) continue
                if (KolLowTech.skinFor(member.hullSpec) == null) continue
                if (Math.random() < share && KolFleetSpawnHook.convertMember(member)) converted++
            }
            if (converted == 0) return false
            KolFleetSpawnHook.finish(fleet)
            return true
        }
    }
}

/** The Ordained rollouts (Technology milestones): the early rollout's cue and pulse, and Ordained ships in Knights fleets. */
object KolOrdainedModule : KolStoryModule {
    override val id = "ordained"

    override fun spawnModifiers(): Map<String, KolFleetSpawnHook.Modifier> = mapOf("ordained" to KolOrdained.Modifier())

    override fun pulse() {
        if (KolChapter.get() >= 2) KolOrdainedEarly.Companion.pulse()
    }

    override fun cues() = mapOf(
        // some days after ARMORY: the early Ordained rollout, until the player answers
        "ordainedEarly" to KolStoryCue.Cue(KolIds.HELENSIS,
            "Knights: Master Helensis Asks for You",
            "[PLACEHOLDER] Master Helensis has sent word to your fleet: she asks that you call on her at Battlestation Cygnus " +
                    "at your earliest convenience. The message does not say why.",
            "Speak with Master Helensis at Cygnus") { KolOrdainedEarly.Companion.due() },
    )
}
