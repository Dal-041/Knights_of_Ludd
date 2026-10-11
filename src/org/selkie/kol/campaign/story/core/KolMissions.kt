package org.selkie.kol.campaign.story.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission

/** Starting a story HubMission from code: the one path, so creation and acceptance behave the same everywhere. */
object KolMissions {
    /** Starts [missionId] with [giverId] as its contact, created at the giver's market. Null if it couldn't start. */
    fun start(missionId: String, giverId: String, dialog: InteractionDialogAPI? = null): HubMission? {
        val giver = KolPeople.get(giverId) ?: return null
        return start(missionId, giver, giver.market, dialog)
    }

    /** Starts [missionId] created at [market], with [giver] (if any) as its contact. Null if it couldn't start. */
    fun start(missionId: String, giver: PersonAPI?, market: MarketAPI?, dialog: InteractionDialogAPI? = null): HubMission? {
        val mission = Global.getSettings().getMissionSpec(missionId)?.createMission() ?: return null
        if (giver != null) mission.setPersonOverride(giver)
        mission.createAndAbortIfFailed(market, false)
        if (mission.isMissionCreationAborted) return null
        mission.accept(dialog, null)
        return mission
    }
}
