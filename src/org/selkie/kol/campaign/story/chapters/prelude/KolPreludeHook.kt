package org.selkie.kol.campaign.story.chapters.prelude

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Voices
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.core.KolIds

/**
 * Knights prelude, step 0: a one-time bar scene at Knights markets saying the Knights are hiring
 * freelance captains. Accepting only points the player at Brother Enarms; the mission completes the
 * first time the player talks to him about work ($kolPreludeHook_talked, a stage flag; $kolPrelude_metEnarms
 * stays permanent). Dialogue: rules.csv, kolPreludeHook_*.
 */
class KolPreludeHook : HubMissionWithBarEvent() {

    enum class Stage { TALK_TO_ENARMS, COMPLETED }

    private var enarms: PersonAPI? = null

    override fun shouldShowAtMarket(market: MarketAPI): Boolean {
        val memory = Global.getSector().memoryWithoutUpdate
        if (market.factionId != KolIds.FACTION) return false
        if (memory.getBoolean(PreludeKeys.HOOK_SEEN) || memory.getBoolean(PreludeKeys.ENARMS_MET)) return false
        if (!knightsAtWorstNeutral()) return false
        return KolPeople.get(KolIds.ENARMS) != null
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!knightsAtWorstNeutral()) return false
        if (!setGlobalReference(PreludeKeys.HOOK_REF)) return false

        enarms = KolPeople.get(KolIds.ENARMS)
        if (enarms == null) return false

        if (barEvent) {
            // the recruiter at the bar: a Novice pinning up notices (vanilla's Bornanew: rank Brother, post Novice)
            setGiverFaction(KolIds.FACTION)
            setGiverPost(Ranks.POST_NOVICE)
            setGiverVoice(Voices.FAITHFUL)
            setGiverImportance(pickLowImportance())
            findOrCreateGiver(createdAt, false, false)
            val recruiter = person ?: return false
            recruiter.rankId = if (recruiter.isMale) Ranks.BROTHER else Ranks.SISTER
        }

        setStartingStage(Stage.TALK_TO_ENARMS)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()

        makeImportant(enarms!!, "\$kolPreludeHook_talk", Stage.TALK_TO_ENARMS)
        setStageOnGlobalFlag(Stage.COMPLETED, PreludeKeys.HOOK_TALKED)

        setRepFactionChangesNone()
        setRepPersonChangesNone()
        return true
    }

    override fun updateInteractionDataImpl() {
        set("\$kolPreludeHook_enarmsName", enarms?.nameString ?: "Brother Enarms")
        set("\$kolPreludeHook_marketName", enarms?.market?.name ?: "Battlestation Cygnus")
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage == Stage.TALK_TO_ENARMS) {
            info.addPara("The Knights of Ludd are looking for freelance captains. Their recruiter pointed you to " +
                    "${enarms?.nameString} at ${enarms?.market?.name}.", 10f)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage == Stage.TALK_TO_ENARMS) {
            info.addPara("Talk to ${enarms?.nameString} at ${enarms?.market?.name}", tc, pad)
            return true
        }
        return false
    }

    override fun getBaseName(): String = "Freelancers Wanted"

    private fun knightsAtWorstNeutral(): Boolean =
        Global.getSector().getFaction(KolIds.FACTION)?.relToPlayer?.isAtWorst(RepLevel.NEUTRAL) == true
}
