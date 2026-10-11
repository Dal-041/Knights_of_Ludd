package org.selkie.kol.campaign.story.situations.tech.milestones.inquest

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.core.KolRep
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.systems.dock.KolDockEvents
import org.selkie.kol.campaign.story.situations.tech.KolTechSettings
import org.selkie.kol.campaign.story.chapters.ch1.Ch1Keys
import org.selkie.kol.campaign.story.chapters.ch2.convocation.ConvocationKeys
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.core.KolStorySettings

/** The inquest into the player: the Inquisition's people, the reading and the outcome. */
object KolInquest {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** TRUSTED's threshold in lifetime scrip. */
    fun trustedThreshold(): Int = KolTechSettings.stageThresholds.getOrElse(1) { 5000 }

    /** The Inquisition's people: generated once per slot and remembered (`lead`, `second`). */
    fun inquisitor(slot: String): PersonAPI {
        val cygnus = Global.getSector().economy.getMarket(KolIds.CYGNUS)
        return KolPeople.remembered(KolIds.INQUISITOR_PREFIX + slot, cygnus) {
            val knights = Global.getSector().getFaction(KolIds.FACTION)
            knights.createRandomPerson().apply {
                rankId = if (isMale) Ranks.BROTHER else Ranks.SISTER
                postId = Ranks.POST_INVESTIGATOR
            }
        }
    }

    /**
     * The inquest is one score: the interview (how the player answers), each lie told to Enarms in Chapter 1, and the
     * inspection of the fleet. Nothing decides the outcome on its own; the total does, against the settings'
     * thresholds: cleared, watched, censured, cast out. Surrendering what the inspection counted removes its share.
     */
    private fun inquestTotal(withInspection: Boolean): Float {
        val s = KolStorySettings
        val answer = when (memory.getString("\$kolInquest_answer")) {
            "guarded" -> s.inquestAnswerWeight(1)
            "defiant" -> s.inquestAnswerWeight(2)
            else -> s.inquestAnswerWeight(0)
        }
        val lies = listOf(Ch1Keys.DECLINE_LIE, Ch1Keys.TECH_LIE, Ch1Keys.OATH_LIE,
            ConvocationKeys.CONV_OATH_LIE)
            .count { memory.getBoolean(it) } * s.inquestLieWeight
        val inspection = if (withInspection) memory.getFloat("\$kolInquest_inspectionScore") else 0f
        return answer + lies + inspection
    }

    private fun tierOf(total: Float): Int {
        val s = KolStorySettings
        return when {
            total >= s.inquestHeavyScore -> 3
            total >= s.inquestModerateScore -> 2
            total >= s.inquestWatchedScore -> 1
            else -> 0
        }
    }

    /** Inspects the player's fleet and publishes the findings for the scene (`$global.kolInquest_*`). */
    fun inspect() {
        val report = KolInquestInspection.inspect()
        memory.set("\$kolInquest_inspectionScore", report.score)
        memory.set("\$kolInquest_hasItems", report.score > 0f)
        memory.set("\$kolInquest_ships", report.ships)
        memory.set("\$kolInquest_weapons", report.weapons)
        memory.set("\$kolInquest_hullmods", report.hullmods)
        memory.set("\$kolInquest_aiOfficers", report.aiOfficers)
        memory.set("\$kolInquest_cores", report.cores)
    }

    /** Records the interview's answer (candid / guarded / defiant) and projects the tier if the player keeps everything. */
    fun answerInquest(answer: String) {
        memory.set("\$kolInquest_answer", answer)
        memory.set("\$kolInquest_projected", tierOf(inquestTotal(true)))
    }

    /** Settles the inquest. [choice]: `surrender`, `refuse`, or `none` (nothing was found). */
    fun resolveInquest(choice: String, dialog: InteractionDialogAPI) {
        val text = dialog.textPanel
        val surrendered = choice == "surrender"
        if (surrendered) {
            val shuttle = KolInquestInspection.surrender()
            text.addPara("The Inquisition takes possession of everything it counted.", Misc.getNegativeHighlightColor())
            if (shuttle) text.addPara("A Mercury shuttle is provided for your passage.", Misc.getHighlightColor())
        }
        val tier = tierOf(inquestTotal(!surrendered))
        // through the mirrored Knights path, with vanilla's message (the Church follows)
        fun repLoss(amount: Float) = KolRep.knights(-amount, text)
        val outcome = when (tier) {
            0 -> "cleared"
            1 -> { repLoss(0.05f); "watched" }
            2 -> { repLoss(0.15f); "censured" }
            else -> {
                KolInquestInspection.excommunicate()
                text.addPara("You are cast out of the Church", Misc.getNegativeHighlightColor())
                "castOut"
            }
        }
        memory.set("\$kolInquest_surrendered", surrendered)
        recordInquest(outcome)
    }

    fun addInquestEvent() {
        KolDockEvents.get().add(KolIds.CYGNUS, InquestKeys.INQUEST_EVENT_KEY, 50, InquestKeys.TRIGGER_INQUEST)
    }

    fun recordInquest(outcome: String) {
        memory.set(InquestKeys.INQUEST_PLAYER, outcome)
        memory.set(InquestKeys.INQUEST_PLAYER_DONE, true)
        memory.set(KolPlayerInquest.Companion.STAGE_DONE, true)
        KolDockEvents.get().remove(InquestKeys.INQUEST_EVENT_KEY)
        KolAssembly.report("inquestPlayer")
    }
}
