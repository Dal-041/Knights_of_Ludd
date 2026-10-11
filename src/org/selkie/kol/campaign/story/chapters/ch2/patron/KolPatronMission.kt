package org.selkie.kol.campaign.story.chapters.ch2.patron

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.chapters.ch2.convocation.ConvocationKeys
import org.selkie.kol.campaign.story.systems.cue.KolStoryCue

private val memory get() = Global.getSector().memoryWithoutUpdate

/**
 * Securing a patron for the Church (raised at the convocation; coordinated by Greenflight). One parent with a branch
 * intel per prospective patron (KolPatronLeadIntel, the Pilgrim's Path pattern). BRIEF: meet Greenflight in her office
 * at Lyra, where each great power is introduced by its champion (skipped after the convocation oath, whose own meeting
 * at Lyra serves). SECURE: the branches develop; a
 * signing (or the convocation oath) moves it to ANNOUNCE; it completes when the next assembly announces the patron
 * (KolPatron.onAssemblyAttended). Discovery is KolPatron (rules kolPatron_*). Planned in add-knights-patron-lobbying.
 */
class KolCh2Patron : HubMissionWithSearch() {
    companion object { const val STAGE_DONE = "\$kolPatron_stageDone" }

    /**
     * BRIEF: meet Greenflight in her office (the branches are introduced there). SECURE: seeking a patron (the
     * branches). ANNOUNCE: a patron is signed; the next assembly announces it.
     */
    enum class Stage { BRIEF, SECURE, ANNOUNCE, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(PatronKeys.PATRON_REF, PatronKeys.PATRON_ACTIVE)) return false
        // the oath secures the player's own forces at once: its own meeting at Lyra takes the brief's place
        setStartingStage(if (memory.getBoolean(ConvocationKeys.CONV_OATH_SWORN)) Stage.SECURE else Stage.BRIEF)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        // stage flags are unset when a mission ends, so missions complete on their own flag, never a permanent one
        connectWithGlobalFlag(Stage.BRIEF, Stage.SECURE, KolPatronFlags.BRIEFED)
        setStageOnGlobalFlag(Stage.ANNOUNCE, KolPatronFlags.ANNOUNCE)
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        KolAssembly.lyra?.primaryEntity?.let {
            makeImportant(it, "\$kolPatron_briefAt", Stage.BRIEF) // the brief's option at Lyra keys on this flag
            makeImportant(it, "\$kolPatron_announceAt", Stage.ANNOUNCE)
        }
        return true
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "briefed") {
            // the champions have introduced their powers (kolPatron_brief*); anything still missing joins quietly
            KolPatron.startingLeads()
            memory.set(KolPatronFlags.BRIEFED, true)
            checkStageChangesAndTriggers(dialog, memoryMap)
            return true
        }
        if (action == "secureOwn") {
            // the oath opens the player's own branch; the meeting at Lyra settles and signs it (KolPatron.signOwn)
            KolPatron.ownPledged(dialog?.textPanel)
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    /**
     * A patron is signed: recorded with its lever and price, its branch marked signed and the others closed by a
     * status update, and the quest moves to ANNOUNCE (the next assembly). [patron] is a faction id, `charter` or
     * `player`.
     */
    fun signed(patron: String, lever: String, price: String?, dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        memory.set(PatronKeys.PATRON, patron)
        memory.set(PatronKeys.LEVER, lever)
        if (price != null) memory.set(PatronKeys.PRICE, price)
        val signedBranch = KolPatron.branchOfPatron(patron)
        for (branch in KolPatronLeadIntel.all()) {
            if (branch.key == signedBranch) branch.advance("signed", dialog?.textPanel)
            else if (branch.stage != "closed") branch.advance("closed", null)
        }
        memory.set(KolPatronFlags.ANNOUNCE, true)
        KolPatronParley.registerGate() // Lyra's pre-assembly scenes: the pirates' arrival, a charter's reaction, the delegation home
        checkStageChangesAndTriggers(dialog, memoryMap)
    }

    /**
     * The announcing assembly sat (KolPatron.onAssemblyAttended): the patron is secured, its fleets take station at
     * Lyra and Cygnus, and the quest completes. The assembly's own scene announces it (kolPatron_announce*).
     */
    fun announced() {
        if (currentStage != Stage.ANNOUNCE) return
        memory.set(PatronKeys.PATRON_SECURED, true)
        memory.getString(PatronKeys.PATRON)?.let { KolPatronFleets.station(it) }
        KolPatronParley.dropOff() // a fallback: the delegation normally goes home in the gate at Lyra
        memory.set(STAGE_DONE, true)
        checkStageChangesAndTriggers(null, null)
        KolStoryCue.check() // the next mission, if it hasn't been taken up
    }

    /** A lead was learned (KolPatron): mark where it leads, while the quest seeks. */
    fun leadLearned(key: String) {
        val target = (KolPatronPower.of(key)?.seat() ?: KolPatron.place(key))?.primaryEntity ?: return
        makeImportant(target, "\$kolPatron_leadMarker", Stage.SECURE)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.BRIEF -> info.addPara("[PLACEHOLDER] The assembly at Star Keep Lyra has charged you with finding the " +
                    "Church a secular patron. Sister Greenflight's office coordinates the Order's part, and expects you.", 10f)
            Stage.SECURE -> {
                info.addPara("[PLACEHOLDER] The assembly at Star Keep Lyra has charged you with finding the Church a secular " +
                        "patron while the Order's fleets are committed. Sister Greenflight's office coordinates the Order's part. " +
                        "The great powers decide such things at their seats, and the faithful under their banners may help you " +
                        "be heard.", 10f)
                if (!KolPatron.allWildcardsFound()) info.addPara("[PLACEHOLDER] There may be less conventional options, " +
                        "for those who ask the right people.", 10f)
            }
            Stage.ANNOUNCE -> info.addPara("[PLACEHOLDER] The arrangement is made. It will be announced at the next " +
                    "assembly at Star Keep Lyra.", 10f)
            else -> {}
        }
        KolPatronLeadIntel.addShowLeadsButton(this, width, height, info)
    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI) {
        if (buttonId == KolPatronLeadIntel.BUTTON_SHOW_LEADS) {
            KolPatronLeadIntel.toggleLeadList(this, ui)
            return
        }
        super.buttonPressConfirmed(buttonId, ui)
    }

    override fun notifyEnding() {
        super.notifyEnding()
        KolPatronLeadIntel.endAll()
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        when (currentStage) {
            Stage.BRIEF -> {
                info.addPara("Meet Sister Greenflight in her office at Star Keep Lyra", tc, pad)
                if (memory.getBoolean(PatronKeys.BRIEF_REFUSED) && !KolPatron.churchWelcomes())
                    info.addPara("Be welcomed by the Luddic Church", tc, 0f)
            }
            Stage.SECURE -> info.addPara("Seek a patron for the Church", tc, pad)
            Stage.ANNOUNCE -> info.addPara("Attend the next assembly at Star Keep Lyra", tc, pad)
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = "A Patron for the Church"
}

/** The patron quest's private flags (the mission completes on its own flags, never a permanent one). */
object KolPatronFlags {
    /** A patron is signed; the quest waits for the next assembly. */
    const val ANNOUNCE = "\$kolPatron_announce"
    /** The brief at Greenflight's office is done. */
    const val BRIEFED = "\$kolPatron_briefed"
}
