package org.selkie.kol.campaign.story

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RuleBasedDialog
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub
import org.selkie.kol.campaign.missions.cb.KolCustomBounty
import org.selkie.kol.campaign.missions.cb.KolDutyPatrolCreator
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolStory

/**
 * The Order's duties bulletin, at every Knights market from Chapter 2. Vanilla's mission hub works through an active
 * person, so each Knights market has a hidden board person (not in the comm directory) carrying the hub. The market
 * option "Consult the duties board" makes it active and opens vanilla's mission list; closing returns to the market.
 * Duties are person_missions.csv rows tagged `kol_duties`.
 */
object KolDutiesBoard {

    fun personId(market: MarketAPI) = KolStory.DUTIES_PERSON_PREFIX + market.id

    /** Gives every Knights market a board person with a mission hub (on load). */
    fun sync() {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID) ?: return
        for (market in Global.getSector().economy.marketsCopy) {
            if (market.factionId != KolStaticStrings.kolFactionID || market.isHidden) continue
            val person = KolPeople.remembered(personId(market), market) {
                knights.createRandomPerson().apply {
                    name = FullName("Duties", "Board", FullName.Gender.ANY)
                    portraitSprite = knights.crest
                }
            }
            person.addTag(KolStory.DUTIES_TAG)
            person.memoryWithoutUpdate.set(KolStory.DUTIES_BOARD_FLAG, true)
            if (BaseMissionHub.get(person) == null) BaseMissionHub.set(person, BaseMissionHub(person))
        }
    }

    /** Makes the market's board person active, so vanilla's mission-hub rules apply. */
    fun open(dialog: InteractionDialogAPI): Boolean {
        val market = dialog.interactionTarget?.market ?: return false
        val person = Global.getSector().importantPeople.getPerson(personId(market)) ?: return false
        dialog.interactionTarget.setActivePerson(person)
        (dialog.plugin as? RuleBasedDialog)?.notifyActivePersonChanged()
        return true
    }

    /** Back to the market: no active person (the rule then shows the default visual). */
    fun close(dialog: InteractionDialogAPI) {
        dialog.interactionTarget.setActivePerson(null)
        (dialog.plugin as? RuleBasedDialog)?.notifyActivePersonChanged()
    }
}

/**
 * Placeholder duty: clear raiders (pirates, or sometimes Remnant) harassing a Luddic world. Pays credits and Knights standing,
 * counts toward `$global.kolDuties_done` and the assembly's reckoning; never touches situation progress.
 */
class KolDutyPatrol : KolCustomBounty() {
    override fun getCreators(): MutableList<CustomBountyCreator> = mutableListOf(KolDutyPatrolCreator())

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        val memory = Global.getSector().memoryWithoutUpdate
        memory.set(KolStory.DUTIES_DONE, memory.getInt(KolStory.DUTIES_DONE) + 1)
        KolAssembly.report("duties")
    }
}
