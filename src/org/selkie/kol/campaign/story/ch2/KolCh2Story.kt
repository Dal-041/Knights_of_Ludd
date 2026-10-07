package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission
import org.selkie.kol.campaign.story.KolAssembly
import org.selkie.kol.campaign.story.KolChapter
import org.selkie.kol.campaign.story.KolChapterRequirements
import org.selkie.kol.campaign.story.KolChronicle
import org.selkie.kol.campaign.story.KolDockEvents
import org.selkie.kol.campaign.story.KolPeople
import org.selkie.kol.campaign.tech.KolTechSettings
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import org.selkie.kol.helpers.KolStaticStrings.KolTech

/**
 * Knights Chapter 2: registration, the periodic pulse, and shared helpers. Missions are in this package; dialogue
 * is rules.csv kolCh2_*. Planned in openspec/changes/add-knights-chapter-two.
 */
object KolCh2Story {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** Chapter 2's requirements for the chapter assembly (registered at each load). */
    fun register() {
        repairFlags()
        if (memory.getBoolean(KolCh2.PATRON_ACTIVE)) KolPatron.startingLeads()
        KolChapterRequirements.register(2, "patron", { true }, { memory.getBoolean(KolCh2.PATRON_SECURED) })
        KolChapterRequirements.register(2, "ninayaOperation",
            { memory.getBoolean(KolCh2.PATRON_SECURED) }, { memory.getBoolean(KolCh2.NINAYA_OP_DONE) })
        KolChapterRequirements.register(2, "ozymandias", { true }, { memory.getBoolean(KolCh2.OZY_DONE) })
        KolChapterRequirements.register(2, "agentWarning",
            { memory.getBoolean(KolCh2.AGENT_TRIGGERED) }, { memory.getBoolean(KolCh2.AGENT_ANSWERED) })
        KolChapterRequirements.register(2, "inquestPlayer",
            { KolChapterRequirements.inquestValid(trustedThreshold()) }, { memory.getBoolean(KolCh2.INQUEST_PLAYER_DONE) })
    }

    /**
     * Saves from before the stage-flag fix lost permanent flags when their missions ended (HubMissions unset the flags
     * they complete on). Restores them from what else was recorded.
     */
    private fun repairFlags() {
        // a patron is recorded at signing; it's secured once the quest completes (at the announcing assembly)
        if (memory.getString(KolCh2.PATRON) != null && !memory.getBoolean(KolCh2.PATRON_ACTIVE)) memory.set(KolCh2.PATRON_SECURED, true)
        if (memory.getString(KolCh2.CAELI_CHOICE) != null) memory.set(KolCh2.OZY_DONE, true)
        if (memory.getString(KolCh2.INQUEST_PLAYER) != null) memory.set(KolCh2.INQUEST_PLAYER_DONE, true)
        if (memory.getBoolean(KolStaticStrings.KolStory.CHRON_PREFIX + "ninayaDefeated"))
            memory.set(org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys.ZEA_TT_NINAYA_DONE, true)
    }

    /**
     * Plays the best rule on [trigger] as a continuation of the text above it (rule text, options, script), for
     * dossier-style readings. With `readingAppends` on, its text replaces the last paragraph, which is the echo of the
     * option just chosen, so the reading runs on without the "Continue" lines between items.
     */
    fun readOn(trigger: String, ruleId: String?, dialog: InteractionDialogAPI, map: MutableMap<String, com.fs.starfarer.api.campaign.rules.MemoryAPI>): Boolean {
        val rules = Global.getSector().rules
        val rule = rules.getBestMatching(ruleId, trigger, dialog, map) ?: return false
        val text = rule.pickText()?.takeIf { it.isNotBlank() }?.let { rules.performTokenReplacement(rule.id, it, dialog.interactionTarget, map) }
        if (text != null) {
            if (org.selkie.kol.campaign.story.KolStorySettings.readingAppends) dialog.textPanel.replaceLastParagraph(text)
            else dialog.textPanel.addPara(text)
        }
        if (rule.options.isNotEmpty()) {
            dialog.optionPanel.clearOptions()
            for (option in rule.options.sortedBy { it.order }) {
                dialog.optionPanel.addOption(rules.performTokenReplacement(rule.id, option.text, dialog.interactionTarget, map), option.id)
            }
        }
        rule.runScript(dialog, map)
        return true
    }

    /** TRUSTED's threshold in lifetime scrip. */
    fun trustedThreshold(): Int = KolTechSettings.stageThresholds.getOrElse(1) { 5000 }

    /** Runs with the assembly schedule (twice a day): starts what has become due. */
    fun pulse() {
        if (KolChapter.get() < 2) return
        // the agent becomes due once the player has felled an Abyss power or finished the Ninaya operation
        if (!memory.getBoolean(KolCh2.AGENT_TRIGGERED) &&
            (memory.getInt(KolStaticStrings.KolStory.CHRON_POWERS_DEFEATED) >= 1 || memory.getBoolean(KolCh2.NINAYA_OP_DONE))) {
            memory.set(KolCh2.AGENT_TRIGGERED, true)
        }
        // the inquest into the player, from TRUSTED
        if (memory.getBoolean(KolTech.STAGE_FLAGS[2]) && !memory.getBoolean(KolCh2.INQUEST_PLAYER_DONE) &&
            !memory.getBoolean(KolCh2.INQUEST_ACTIVE)) {
            start(KolCh2.INQUEST_ID, KolStaticStrings.KolCh1.GREENFLIGHT_ID)
        }
    }

    /** Starts a mission from code (no dialog), with [giverId] as its contact. Returns it, or null if it couldn't start. */
    fun start(missionId: String, giverId: String, dialog: InteractionDialogAPI? = null): HubMission? {
        val giver = Global.getSector().importantPeople.getPerson(giverId) ?: return null
        val mission = Global.getSettings().getMissionSpec(missionId)?.createMission() ?: return null
        mission.setPersonOverride(giver)
        mission.createAndAbortIfFailed(giver.market, false)
        if (mission.isMissionCreationAborted) return null
        mission.accept(dialog, null)
        return mission
    }

    // --- people -----------------------------------------------------------------------------------------------

    /** The Inquisition's people: generated once per slot and remembered (`lead`, `second`). */
    fun inquisitor(slot: String): PersonAPI {
        val cygnus = Global.getSector().economy.getMarket(KolStaticStrings.KOL_CYGNUS)
        return KolPeople.remembered(KolCh2.INQUISITOR_PREFIX + slot, cygnus) {
            val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
            knights.createRandomPerson().apply {
                rankId = if (isMale) Ranks.BROTHER else Ranks.SISTER
                postId = Ranks.POST_INVESTIGATOR
            }
        }
    }

    // --- the outside power ----------------------------------------------------------------------------------

    /**
     * The outside power, chosen once from the player's lore: Dawn and Dusk lore (Project Dawn/Dusk) point to
     * Tri-Tachyon; Elysian lore and the Hegemony witness to the Hegemony; the larger count wins; Tri-Tachyon with no
     * lore. Never the patron.
     */
    fun outsidePower(): String {
        memory.getString(KolCh2.OUTSIDE_POWER)?.let { if (it.isNotEmpty()) return it }
        val tt = KolChronicle.loreCount(KolChronicle.DAWN) + KolChronicle.loreCount(KolChronicle.DUSK) +
                KolChronicle.loreCount(KolChronicle.TRITACH)
        val heg = KolChronicle.loreCount(KolChronicle.ELYSIAN) + KolChronicle.loreCount("hegemony")
        var pick = if (heg > tt) Factions.HEGEMONY else Factions.TRITACHYON
        if (pick == memory.getString(KolCh2.PATRON)) pick = if (pick == Factions.HEGEMONY) Factions.TRITACHYON else Factions.HEGEMONY
        memory.set(KolCh2.OUTSIDE_POWER, pick)
        return pick
    }

    /** The outside power's agent, remembered so they can return in Chapter 3. */
    fun agent(): PersonAPI = KolPeople.remembered(KolCh2.AGENT_PERSON_ID) {
        Global.getSector().getFaction(outsidePower()).createRandomPerson().apply {
            postId = Ranks.POST_AGENT
        }
    }

    // --- an operation's boss already beaten --------------------------------------------------------------------

    /** Share of an operation's strength carried to the next operation (a float in sector memory). */
    const val JOINT_OP_CARRY = "\$kol_jointOpCarry"

    /**
     * Caeli's Zhi Nu was recovered (vanilla's PostShipRecoverySpecial rule hook): a Knights reputation hit if any of
     * the Ozymandias mission's Knights fleets is alive in the system.
     */
    fun zhiNuRecovered(dialog: InteractionDialogAPI) {
        val system = Global.getSector().getStarSystem(org.selkie.zea.helpers.ZeaStaticStrings.ozymandiasSysName) ?: return
        if (system.fleets.none { it.isAlive && it.memoryWithoutUpdate.getBoolean(KolCh2.OZY_KNIGHTS) }) return
        val impact = com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact()
        impact.delta = -org.selkie.kol.campaign.story.KolStorySettings.ozZhiNuRecoveryRep
        KolPatron.knightsRepAction(impact, dialog.textPanel)
    }

    /**
     * The player beat the operation's boss before the Order could sail: half of what was set aside is paid to them
     * as a bounty, half strengthens the next operation; the operation counts as done.
     */
    fun alreadyDoneBounty(doneFlag: String, dialog: InteractionDialogAPI) {
        if (memory.getBoolean(doneFlag)) return
        val credits = org.selkie.kol.campaign.story.KolStorySettings.alreadyDoneBountyCredits
        Global.getSector().playerFleet.cargo.credits.add(credits.toFloat())
        com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity.addCreditsGainText(credits, dialog.textPanel)
        memory.set(JOINT_OP_CARRY, memory.getFloat(JOINT_OP_CARRY) + 0.5f)
        memory.set(doneFlag, true)
        KolAssembly.report("jointOperation")
    }

    // --- the inquest -----------------------------------------------------------------------------------------

    /**
     * The inquest is one score: the interview (how the player answers), each lie told to Enarms in Chapter 1, and the
     * inspection of the fleet. Nothing decides the outcome on its own; the total does, against the settings'
     * thresholds: cleared, watched, censured, cast out. Surrendering what the inspection counted removes its share.
     */
    private fun inquestTotal(withInspection: Boolean): Float {
        val s = org.selkie.kol.campaign.story.KolStorySettings
        val answer = when (memory.getString("\$kolInquest_answer")) {
            "guarded" -> s.inquestAnswerWeight(1)
            "defiant" -> s.inquestAnswerWeight(2)
            else -> s.inquestAnswerWeight(0)
        }
        val lies = listOf(KolStaticStrings.KolCh1.DECLINE_LIE, KolStaticStrings.KolCh1.TECH_LIE, KolStaticStrings.KolCh1.OATH_LIE,
            KolCh2.CONV_OATH_LIE)
            .count { memory.getBoolean(it) } * s.inquestLieWeight
        val inspection = if (withInspection) memory.getFloat("\$kolInquest_inspectionScore") else 0f
        return answer + lies + inspection
    }

    private fun tierOf(total: Float): Int {
        val s = org.selkie.kol.campaign.story.KolStorySettings
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
            text.addPara("The Inquisition takes possession of everything it counted.", com.fs.starfarer.api.util.Misc.getNegativeHighlightColor())
            if (shuttle) text.addPara("A Mercury shuttle is provided for your passage.", com.fs.starfarer.api.util.Misc.getHighlightColor())
        }
        val tier = tierOf(inquestTotal(!surrendered))
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
        fun repLoss(amount: Float) {
            knights.adjustRelationship(Factions.PLAYER, -amount)
            text.addPara("Relations with ${knights.displayNameWithArticle} worsened", com.fs.starfarer.api.util.Misc.getNegativeHighlightColor())
        }
        val outcome = when (tier) {
            0 -> "cleared"
            1 -> { repLoss(0.05f); "watched" }
            2 -> { repLoss(0.15f); "censured" }
            else -> {
                KolInquestInspection.excommunicate()
                text.addPara("You are cast out of the Church", com.fs.starfarer.api.util.Misc.getNegativeHighlightColor())
                "castOut"
            }
        }
        memory.set("\$kolInquest_surrendered", surrendered)
        recordInquest(outcome)
    }

    fun addInquestEvent() {
        KolDockEvents.get().add(KolStaticStrings.KOL_CYGNUS, KolCh2.INQUEST_EVENT_KEY, 50, KolCh2.TRIGGER_INQUEST)
    }

    fun recordInquest(outcome: String) {
        memory.set(KolCh2.INQUEST_PLAYER, outcome)
        memory.set(KolCh2.INQUEST_PLAYER_DONE, true)
        memory.set(KolCh2Inquest.STAGE_DONE, true)
        KolDockEvents.get().remove(KolCh2.INQUEST_EVENT_KEY)
        KolAssembly.report("inquestPlayer")
    }

    // --- the convocation's worlds of the faithful ---------------------------------------------------------------

    /** The worlds whose Luddic congregations the convocation names, with the power that must hold them. */
    val PATRON_DOORS = linkedMapOf("mazalot" to Factions.PERSEAN, "jangala" to Factions.HEGEMONY, "volturn" to Factions.DIKTAT)

    /**
     * The player's own forces can stand as the Church's patron (offered only at the convocation): a colony and enough
     * fleet, and the player can commit them unilaterally: not a League member, and not holding the commission of a
     * faction hostile to the Knights.
     */
    fun canOfferOwn(): Boolean {
        if (com.fs.starfarer.api.util.Misc.getPlayerMarkets(true).isEmpty()) return false
        if (Global.getSector().playerFleet.fleetPoints < org.selkie.kol.campaign.story.KolStorySettings.patronOwnFleetPoints) return false
        if (com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership.isLeagueMember()) return false
        val commission = com.fs.starfarer.api.util.Misc.getCommissionFactionId() ?: return true
        return !Global.getSector().getFaction(commission).isHostileTo(KolStaticStrings.kolFactionID)
    }

    fun openDoors(): List<MarketAPI> = PATRON_DOORS.mapNotNull { (marketId, faction) ->
        Global.getSector().economy.getMarket(marketId)?.takeIf { it.factionId == faction && !it.isHidden && it.primaryEntity != null }
    }
}
