package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolDockEvents
import org.selkie.kol.campaign.story.KolPeople
import org.selkie.kol.campaign.story.KolStorySettings
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import org.selkie.kol.helpers.KolStaticStrings.KolPatron as K

/**
 * A great power's price: frontloaded, paid as the condition of signing. Data, so alternate prices plug into the
 * meeting without new structure (rules key on `$global.kolPatron_priceKey`, with generic fallbacks).
 */
class KolPatronPrice(val key: String, val power: KolPatronPower, val confirm: String, val apply: () -> Unit)

object KolPatronPrices {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** [PLACEHOLDER] confirm texts: stated plainly before the player commits. */
    val all = listOf(
        KolPatronPrice("league_antipiracy", KolPatronPower.LEAGUE,
            "The Church's forces join anti-piracy efforts in League systems.") { memory.set(K.PREFIX + "price_league_antipiracy", true) },
        KolPatronPrice("hegemony_ties", KolPatronPower.HEGEMONY,
            "Closer ties with the Hegemony, and resupply of Hegemony expeditions at Church worlds.") { memory.set(K.PREFIX + "price_hegemony_ties", true) },
        KolPatronPrice("diktat_volturn", KolPatronPower.DIKTAT,
            "The faith hands the Diktat its own files on Volturn's Path-affiliated faithful.") { memory.set(K.VOLTURN_FILES_GIVEN, true) },
    )

    fun get(key: String?): KolPatronPrice? = all.firstOrNull { it.key == key }

    /** The price on the table for [power]: chosen once (the default for now), kept in `$global.kolPatron_<power>_price`. */
    fun current(power: KolPatronPower): KolPatronPrice {
        val stored = get(memory.getString(K.PREFIX + power.key + K.PRICE_SUFFIX))
        if (stored != null) return stored
        val default = all.first { it.power == power }
        memory.set(K.PREFIX + power.key + K.PRICE_SUFFIX, default.key)
        return default
    }
}

/**
 * The great powers' parleys: each branch's steps (its stage on KolPatronLeadIntel), reception by standing, the
 * Order's delegates and other passengers, the meeting, and the signing. The League is the reference implementation;
 * the Hegemony and the Diktat add their own steps on the same pieces (design, decision 3).
 */
object KolPatronParley {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    fun stage(power: KolPatronPower): String? = KolPatronLeadIntel.get(power.key)?.stage

    /** Moves a power's branch on; its intel shows in the dialog. */
    fun advance(power: KolPatronPower, stage: String, text: TextPanelAPI?) {
        KolPatronLeadIntel.get(power.key)?.advance(stage, text)
    }

    /**
     * How the power's office receives the player: `commissioned`, `neutral`, `unwelcome` (Suspicious or worse), or
     * `hostile` (the branch stalls until standing recovers). Published as `$kolPatron_reception`, with
     * `$kolPatron_powerName` (the faction with its article).
     */
    fun reception(power: KolPatronPower, map: MutableMap<String, MemoryAPI>): String {
        val faction = power.faction
        val reception = when {
            Misc.getCommissionFactionId() == power.factionId -> "commissioned"
            faction.isHostileTo(Factions.PLAYER) -> "hostile"
            faction.relToPlayer.isAtBest(RepLevel.SUSPICIOUS) -> "unwelcome"
            else -> "neutral"
        }
        val local = map[MemKeys.LOCAL]
        local?.set("\$kolPatron_reception", reception, 0f)
        local?.set("\$kolPatron_powerName", faction.displayNameWithArticle, 0f)
        return reception
    }

    // --- passengers ------------------------------------------------------------------------------------------------

    /**
     * The Order's delegation is aboard: one party (the Lector, Greenflight and Helensis, with unnamed delegates),
     * collected once at Lyra and then present for every power's meeting; each meeting's dialogue centers on the two
     * principals most relevant to it. After a signing it goes home at the player's next stop at Lyra (the gate).
     */
    fun delegationAboard(): Boolean = memory.getBoolean(K.DELEGATION)

    /** A power's branch waits for the Order's delegates (and they aren't aboard yet), or null. */
    fun delegationWanted(): KolPatronPower? {
        if (delegationAboard()) return null
        if (stage(KolPatronPower.LEAGUE) == "mazalot") return KolPatronPower.LEAGUE
        if (stage(KolPatronPower.HEGEMONY) == "meetingSet") return KolPatronPower.HEGEMONY
        return null
    }

    /** Collects the Order's delegation at Lyra; every branch waiting on it shows its new step. */
    fun collect(dialog: InteractionDialogAPI) {
        memory.set(K.DELEGATION, true)
        // every branch that was waiting on them shows its new step
        if (stage(KolPatronPower.LEAGUE) == "mazalot") KolPatronLeadIntel.get(KolPatronPower.LEAGUE.key)?.routeAdded(dialog.textPanel)
        if (stage(KolPatronPower.HEGEMONY) == "meetingSet") KolPatronLeadIntel.get(KolPatronPower.HEGEMONY.key)?.routeAdded(dialog.textPanel)
    }

    /** A passenger who travels to the meeting (Mazalot's delegate, Jangala's curate). */
    fun passenger(role: String): Boolean = memory.getBoolean(K.PREFIX + "passenger_" + role)
    fun board(role: String) = memory.set(K.PREFIX + "passenger_" + role, true)
    fun disembark(role: String) = memory.unset(K.PREFIX + "passenger_" + role)

    /** The delegation goes home (its scene in the gate at Lyra, or the announcing assembly as a fallback). */
    fun dropOff() {
        memory.unset(K.DELEGATION)
    }

    // --- the gate at Lyra --------------------------------------------------------------------------------------------

    private const val GATE_KEY = "kolPatron_gate"

    /**
     * The scenes that must play at Lyra before the announcing assembly, in order (each a trigger; empty when none
     * remain): Greenflight's private reaction to a charter (when it didn't play aboard), then the delegation going home.
     * The pirates' steps join this list later.
     */
    private fun gateSteps(): List<String> = listOfNotNull(
        "KolPatronGatePirates".takeIf { pirateArrivalDue() },
        "KolPatronGateCharter".takeIf { charterReactionPending() },
        "KolPatronGateDelegation".takeIf { delegationAboard() },
    )

    /** A pirate deal waits for the assembly that would announce it (the gate stays registered until then). */
    private fun pirateAwaiting(): Boolean =
        memory.getString(KolCh2.PATRON) == KolPatron.PIRATE && KolPirateSwarm.state().swarm == null

    /** The assembly that would announce a pirate deal is sitting: the pirates' "protection fleets" arrive. */
    private fun pirateArrivalDue(): Boolean = pirateAwaiting() &&
            org.selkie.kol.campaign.story.KolAssembly.data().state == org.selkie.kol.campaign.story.KolAssemblyData.State.SITTING

    /** After a signing: one dock event at Lyra plays every pending step before the assembly can open. */
    fun registerGate() {
        if (gateSteps().isNotEmpty() || pirateAwaiting()) KolDockEvents.get().add(KolStaticStrings.KOL_LYRA, GATE_KEY, 45, "KolPatronGate")
    }

    /** Steps remain before the announcing assembly (KolAssembly.isOpen waits on it). */
    fun gatePending(): Boolean = KolDockEvents.get().has(GATE_KEY) && gateSteps().isNotEmpty()

    /** Plays the gate's next step, or closes the gate and hands the dock back (KolDockNext). */
    fun gateNext(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>) {
        val next = gateSteps().firstOrNull()
        if (next != null) {
            FireBest.fire(null, dialog, map, next)
            return
        }
        if (!pirateAwaiting()) KolDockEvents.get().remove(GATE_KEY)
        FireBest.fire(null, dialog, map, "KolDockNext")
    }

    // --- the charter's reaction ------------------------------------------------------------------------------------

    /** Greenflight hasn't yet reacted, privately, to a charter the player signed. */
    fun charterReactionPending(): Boolean =
        memory.getString(KolCh2.PATRON) == KolPatron.CHARTER && !memory.getBoolean(K.PREFIX + "charterReacted")

    fun charterReacted() = memory.set(K.PREFIX + "charterReacted", true)

    // --- scenes aboard ----------------------------------------------------------------------------------------------

    /**
     * A scene the delegation holds aboard, or null: state-driven, so nothing is queued or saved. The scene's rules
     * move the state on, so it plays once. Balashi's terms are put to the delegation when they arrive.
     */
    fun aboardScene(): String? = when {
        !delegationAboard() -> null
        stage(KolPatronPower.DIKTAT) == "terms" -> "KolPatronAboardDiktat"
        charterReactionPending() -> "KolPatronAboardCharter"
        else -> null
    }

    /** Opens an aboard scene in space, at the next moment no dialog or menu is up (KolPatronFleetScript). */
    fun playAboard(trigger: String) {
        val sector = Global.getSector()
        val ui = sector.campaignUI
        if (sector.isInNewGameAdvance || ui.isShowingDialog || ui.isShowingMenu || Global.getCurrentState() != GameState.CAMPAIGN) return
        val fleet = sector.playerFleet ?: return
        if (fleet.isInHyperspaceTransition) return
        Misc.showRuleDialog(fleet, trigger)
    }

    /** Campaign tick (KolPatronFleetScript): the Diktat's reply arrives after its delay, as a branch update. */
    fun tick() {
        if (stage(KolPatronPower.DIKTAT) != "proposed") return
        val at = memory.get(K.PREFIX + "diktat_proposedAt") as? Long ?: return
        if (Global.getSector().clock.getElapsedDaysSince(at) < KolStorySettings.patronDiktatReplyDays) return
        KolPatronPrices.current(KolPatronPower.DIKTAT) // the terms on the table
        advance(KolPatronPower.DIKTAT, "terms", null)
    }

    // --- the people ------------------------------------------------------------------------------------------------

    /** Remembered people of the parleys, created on first need (design, "The cast"). */
    fun person(role: String): PersonAPI? = when (role) {
        "mazalotElder" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("mazalot")) {
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson().apply {
                rankId = Ranks.ELDER
                postId = Ranks.POST_CURATE
            }
        }
        "mazalotDelegate" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("mazalot")) {
            Global.getSector().getFaction(Factions.PERSEAN).createRandomPerson().apply {
                rankId = Ranks.CITIZEN
                postId = Ranks.POST_ARISTOCRAT
            }
        }
        "jangalaCurate" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("jangala")) {
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson().apply {
                rankId = if (isMale) Ranks.FATHER else Ranks.MOTHER
                postId = Ranks.POST_CURATE
            }
        }
        "hegemonyLiaison" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("chicomoztoc")) {
            Global.getSector().getFaction(Factions.HEGEMONY).createRandomPerson().apply {
                rankId = Ranks.SPACE_COMMANDER
                postId = Ranks.POST_INVESTIGATOR
            }
        }
        "diktatFunctionary" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("sindria")) {
            Global.getSector().getFaction(Factions.DIKTAT).createRandomPerson().apply {
                rankId = Ranks.GROUND_COLONEL // the Diktat is a junta: a rank a few steps from the top
                postId = Ranks.POST_OFFICER
            }
        }
        // the charter's representatives: the independent company's admiral, and Tactistar's
        "charterAdmiral" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("new_maxios")) {
            Global.getSector().getFaction(Factions.MERCENARY).createRandomPerson().apply {
                rankId = Ranks.SPACE_ADMIRAL
                postId = Ranks.POST_MERCENARY
            }
        }
        "tactistarRep" -> KolPeople.remembered(K.PERSON_PREFIX + role, Global.getSector().economy.getMarket("cethlenn")) {
            Global.getSector().getFaction(Factions.MERCENARY).createRandomPerson().apply {
                rankId = Ranks.SPACE_ADMIRAL
                postId = Ranks.POST_MERCENARY
            }
        }
        else -> null
    }

    /** Shows a parley's person, and publishes their tokens as `$global.kolPerson_<role>_*`. */
    fun meet(role: String, dialog: InteractionDialogAPI): Boolean {
        val person = person(role) ?: return false
        KolConvocation.publishPerson("\$kolPerson_${role}_", person)
        dialog.visualPanel.showPersonInfo(person, true)
        return true
    }

    // --- the meeting and the signing -----------------------------------------------------------------------------

    /**
     * Opens a power's meeting: the price on the table (`$global.kolPatron_priceKey`, `_priceConfirm`), the Lector's
     * and the principal's tokens. The meeting's rows key on the power (`$kolPatron_power`) and the price.
     */
    fun openMeeting(power: KolPatronPower, map: MutableMap<String, MemoryAPI>) {
        val price = KolPatronPrices.current(power)
        memory.set(K.PREFIX + "priceKey", price.key)
        memory.set(K.PREFIX + "priceConfirm", price.confirm)
        map[MemKeys.LOCAL]?.set("\$kolPatron_power", power.key, 0f)
        KolPatron.ownOpen() // the Lector's tokens
    }

    /**
     * Signs with [power] at its price: the price applied, rival tension shown, the patron recorded, and the quest
     * moved to the announcing assembly. Passengers other than the Order's delegates go home.
     */
    fun sign(power: KolPatronPower, lever: String, dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val mission = Global.getSector().memoryWithoutUpdate.get(KolCh2.PATRON_REF) as? KolCh2Patron ?: return false
        val price = KolPatronPrices.current(power)
        price.apply()
        for ((rivalId, amount) in KolStorySettings.patronRivalRep(power.key)) {
            if (amount <= 0f) continue
            val rival = Global.getSector().getFaction(rivalId) ?: continue
            rival.adjustRelationship(Factions.PLAYER, -amount)
            dialog.textPanel.addPara("Relations with ${rival.displayNameWithArticle} worsened", Misc.getNegativeHighlightColor())
        }
        disembark("mazalotDelegate")
        disembark("jangalaCurate")
        mission.signed(power.factionId, lever, price.key, dialog, map)
        return true
    }

    /** The lever a power's branch was won by, for the record: its intermediary or channel. */
    fun leverOf(power: KolPatronPower): String = when (power) {
        KolPatronPower.LEAGUE -> "mazalot"
        KolPatronPower.HEGEMONY -> "jangala"
        KolPatronPower.DIKTAT -> "balashi"
    }

    // --- the Hegemony ----------------------------------------------------------------------------------------------

    /**
     * The Hegemony's steps: `lead` → `sentAway` (petitioned through Daud's office and sent away: ask around) →
     * `jangala` (Jangala's curate named; having met Daud or a Hegemony commission skips straight here) → `curate` (the
     * curate aboard) → `meetingSet` (Daud's office set a meeting for all the delegates) → the meeting in person at
     * Chicomoztoc → `signed`.
     */
    fun hegemonyLabel(stage: String): String? = when (stage) {
        "sentAway" -> "Ask around: which congregation in Hegemony space has influence at Chicomoztoc?"
        "jangala" -> "Convince Jangala's curate to speak for the Church"
        "curate" -> "Bring Jangala's curate to Chicomoztoc"
        "meetingSet" -> if (delegationAboard()) "Attend the meeting at Chicomoztoc" else "Collect the Order's delegation at Star Keep Lyra"
        else -> null
    }

    /** Daud's office has Hegemony business for the player: a petition, or the curate's introduction. */
    fun hegemonyOffice(): String? = when (stage(KolPatronPower.HEGEMONY)) {
        "lead" -> "petition"
        "curate" -> if (passenger("jangalaCurate")) "curate" else null
        else -> null
    }

    /** Petitioned and sent away; having met Daud, or holding a Hegemony commission, points straight at the curate. */
    fun hegemonySentAway(text: TextPanelAPI) {
        val direct = Global.getSector().characterData.memoryWithoutUpdate.getBoolean("\$metDaud") ||
                Misc.getCommissionFactionId() == Factions.HEGEMONY
        advance(KolPatronPower.HEGEMONY, if (direct) "jangala" else "sentAway", text)
    }

    /** Jangala's curate is convinced and boards. */
    fun jangalaDone(text: TextPanelAPI) {
        board("jangalaCurate")
        advance(KolPatronPower.HEGEMONY, "curate", text)
    }

    /** Everyone's aboard for the meeting at Chicomoztoc. */
    fun hegemonyMeetingReady(): Boolean =
        stage(KolPatronPower.HEGEMONY) == "meetingSet" && delegationAboard() && passenger("jangalaCurate")

    // --- the Diktat ------------------------------------------------------------------------------------------------

    /**
     * The Diktat's steps: `lead` → `proposed` (a proposal left with Balashi at Andrada's office) → `terms` (after
     * `patronDiktatReplyDays`, Balashi's terms arrive as a branch update: the price) → `dispatched` (the Order's
     * delegation has discussed the terms, aboard or at Greenflight's office, and goes to the formal discussions) → the
     * meeting in person at Sindria → `signed`.
     */
    fun diktatLabel(stage: String): String? = when (stage) {
        "proposed" -> "Await the Diktat's reply to the Church's proposal"
        "terms" -> if (delegationAboard()) "Put Balashi's terms to the Order's delegation" else "Present Balashi's terms at Star Keep Lyra"
        "dispatched" -> "Attend the meeting at Sindria"
        else -> null
    }

    /** Balashi has Diktat business for the player: the Church's proposal. */
    fun diktatOffice(): String? = if (stage(KolPatronPower.DIKTAT) == "lead") "proposal" else null

    /** The proposal is left with Balashi; the reply comes after `patronDiktatReplyDays`. */
    fun diktatProposed(text: TextPanelAPI) {
        memory.set(K.PREFIX + "diktat_proposedAt", Global.getSector().clock.timestamp)
        advance(KolPatronPower.DIKTAT, "proposed", text)
    }

    /** Balashi's terms wait for a delegation that isn't aboard: they're presented at Greenflight's office. */
    fun diktatAtLyra(): Boolean = stage(KolPatronPower.DIKTAT) == "terms" && !delegationAboard()

    /** The delegation has discussed the terms and goes to Sindria; collected in the same scene if it wasn't aboard. */
    fun diktatDispatched(dialog: InteractionDialogAPI) {
        if (!delegationAboard()) collect(dialog)
        advance(KolPatronPower.DIKTAT, "dispatched", dialog.textPanel)
    }

    /** The delegation is aboard for the meeting at Sindria. */
    fun diktatMeetingReady(): Boolean = stage(KolPatronPower.DIKTAT) == "dispatched" && delegationAboard()

    /** Balashi's terms, for the branch's description once they've arrived. */
    fun diktatTerms(): String? =
        if (stage(KolPatronPower.DIKTAT) in setOf("terms", "dispatched")) KolPatronPrices.current(KolPatronPower.DIKTAT).confirm else null

    // --- the League ------------------------------------------------------------------------------------------------

    /**
     * The League's steps: `lead` → `kazeron` (Kazeron asked for Mazalot's congregation) → `mazalot` (the congregation
     * involved; Mazalot's delegate aboard) → the meeting at Kazeron once the Order's delegates are aboard → `signed`.
     */
    fun leagueLabel(stage: String): String? = when (stage) {
        "kazeron" -> "Kazeron will hear the Church when Mazalot's faithful speak for it"
        "mazalot" -> if (delegationAboard()) "Bring Mazalot's delegate and the Order's delegates to Kazeron"
                     else "Collect the Order's delegates at Star Keep Lyra"
        else -> null
    }

    /** Gens Hannan's office has League business for the player: a petition, or the meeting once everyone's aboard. */
    fun leagueOffice(): String? = when (stage(KolPatronPower.LEAGUE)) {
        "lead", "kazeron" -> "petition"
        "mazalot" -> if (delegationAboard() && passenger("mazalotDelegate")) "meeting" else null
        else -> null
    }

    /** Mazalot's congregation can be sought out for the League (before it's involved). */
    fun mazalotOpen(): Boolean = stage(KolPatronPower.LEAGUE).let { it == "lead" || it == "kazeron" }

    /** Mazalot's congregation is involved: the delegate boards, and the branch moves on. */
    fun mazalotDone(text: TextPanelAPI) {
        board("mazalotDelegate")
        advance(KolPatronPower.LEAGUE, "mazalot", text)
    }
}
