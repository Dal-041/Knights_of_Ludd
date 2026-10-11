package org.selkie.kol.campaign.tech

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import org.selkie.kol.campaign.story.KolFleetSpawnHook
import org.selkie.kol.campaign.story.KolStorySettings
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolTech

/** The Ordained rollouts' mark on the world (add-knights-tech-milestones). */
object KolOrdained {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** Share of eligible ships in a new Knights fleet that fly Ordained: none before the early rollout. */
    fun share(): Float = when {
        memory.getBoolean(KolTech.ORDAINED_FULL) -> KolStorySettings.ordainedShareFull
        memory.getBoolean(KolTech.ORDAINED_EARLY) -> KolStorySettings.ordainedShareEarly
        else -> 0f
    }

    /** Fleet-spawn modifier: after a rollout, moves a share of a Knights fleet's eligible ships onto their Ordained skins. */
    class Modifier : KolFleetSpawnHook.Modifier {
        override fun apply(fleet: CampaignFleetAPI): Boolean {
            if (fleet.faction?.id != KolStaticStrings.kolFactionID) return false
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
