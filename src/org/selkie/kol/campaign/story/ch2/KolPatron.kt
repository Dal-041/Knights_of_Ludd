package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.People
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolPeople
import org.selkie.kol.campaign.story.KolStorySettings
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import org.selkie.kol.helpers.KolStaticStrings.KolPatron as K

/**
 * The great powers the Church can lobby for a patron, each with the seat where its decisions are made.
 * Planned in openspec/changes/add-knights-patron-lobbying.
 */
enum class KolPatronPower(val key: String, val factionId: String, private val seatId: String) {
    LEAGUE("league", Factions.PERSEAN, "kazeron"),
    HEGEMONY("hegemony", Factions.HEGEMONY, "chicomoztoc"),
    DIKTAT("diktat", Factions.DIKTAT, "sindria");

    /**
     * The seat: the named market while this power holds it, else the power's largest market. Null when the power
     * holds no market at all, and then it can't be lobbied.
     */
    fun seat(): MarketAPI? {
        val economy = Global.getSector().economy
        economy.getMarket(seatId)?.takeIf { usable(it) }?.let { return it }
        return economy.marketsCopy.filter { usable(it) }.maxByOrNull { it.size }
    }

    private fun usable(market: MarketAPI) = market.factionId == factionId && !market.isHidden && market.primaryEntity != null

    val faction get() = Global.getSector().getFaction(factionId)

    companion object {
        fun of(key: String?): KolPatronPower? = values().firstOrNull { it.key == key }
        fun ofFaction(factionId: String?): KolPatronPower? = values().firstOrNull { it.factionId == factionId }
    }
}

/** The patron quest's people, discovery and shared helpers. */
object KolPatron {
    private val people get() = Global.getSector().importantPeople
    private val memory get() = Global.getSector().memoryWithoutUpdate

    // --- leads -------------------------------------------------------------------------------------------------

    /** Leads that aren't a power: the routes to a charter, through Tri-Tachyon, to the pirates and to the Path. */
    const val CHARTER = "charter"
    const val TRITACH = "tritach"
    const val PIRATE = "pirate"
    const val PATH = "path"
    /** The player's own forces: a branch opened only by the convocation oath. */
    const val PLAYER = "player"
    /** A step's hint, not a branch: which congregation has influence at Chicomoztoc (the Hegemony's "ask around"). */
    const val HEG_CURATE = "hegemonyCurate"

    fun hasLead(key: String): Boolean = memory.getBoolean(K.LEAD_PREFIX + key)

    /** The branches (one per prospective patron): the three powers, then the wildcards. */
    val BRANCHES = listOf("league", "hegemony", "diktat", CHARTER, PIRATE, PATH, PLAYER)

    /** The branch a lead belongs to: the Tri-Tachyon route is one of the mercenaries' two routes. */
    fun branchOf(lead: String): String = if (lead == TRITACH) CHARTER else lead

    /** The branch of a recorded patron (a faction id, `charter` or `player`); null for the player's own forces. */
    fun branchOfPatron(patron: String): String? = KolPatronPower.ofFaction(patron)?.key ?: patron.takeIf { it == CHARTER || it == PLAYER || it == PIRATE }

    /** Whether a branch has been found (the mercenaries by either route). */
    fun hasBranch(branch: String): Boolean = if (branch == CHARTER) hasLead(CHARTER) || hasLead(TRITACH) else hasLead(branch)

    /** Every wildcard branch has been found: the parent stops tipping that there may be other options. */
    fun allWildcardsFound(): Boolean = hasBranch(CHARTER) && hasBranch(PIRATE) && hasBranch(PATH)

    /**
     * The convocation oath: the player's own forces are pledged. Their branch opens, and the details are settled with
     * Greenflight and the Lector at Lyra (`signOwn`) before the next assembly announces it.
     */
    fun ownPledged(text: TextPanelAPI?) {
        learn(PLAYER, text = text)
    }

    /** Opens the meeting at Lyra: the Lector's tokens (`$global.kolBench_lector_*`). */
    fun ownOpen() {
        KolConvocation.bench("lector")?.let { KolConvocation.publishPerson("\$kolBench_lector_", it) }
    }

    /** The meeting at Lyra settles the player's own forces: signed as `player`, with the commitment chosen. */
    fun signOwn(commitment: String, dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val mission = mission() ?: return false
        memory.set(K.OWN_COMMITMENT, commitment)
        mission.signed(Factions.PLAYER, "oath", null, dialog, map)
        return true
    }

    /**
     * Opens the announcing assembly's scene: the patron's name for the text (`$global.kolPatron_name`: the faction with
     * its article, the company, or "the captain's own forces").
     */
    fun announceOpen() {
        val patron = memory.getString(KolCh2.PATRON) ?: return
        val name = when (patron) {
            CHARTER -> if (memory.getString(K.CHARTER_SOURCE) == TRITACH) "Tactistar" else KolStorySettings.patronCharterCompanyName
            Factions.PLAYER -> "the captain's own forces"
            else -> Global.getSector().getFaction(patron).displayNameWithArticle
        }
        memory.set(K.PREFIX + "name", name)
    }

    /** The announcing assembly sat (KolAssembly.attended): the quest at ANNOUNCE completes. */
    fun onAssemblyAttended() {
        mission()?.announced()
    }

    private fun mission(): KolCh2Patron? = memory.get(KolCh2.PATRON_REF) as? KolCh2Patron

    /**
     * The quest starts knowing the seats of the powers that hold one. On load it also catches up a running quest:
     * the starting leads, and a branch for every lead already known.
     */
    fun startingLeads() {
        for (power in KolPatronPower.values()) if (power.seat() != null) learn(power.key, quiet = true)
        KolPatronLeadIntel.get(TRITACH)?.endImmediately() // development saves: the Tri-Tachyon route was its own branch
        for (branch in BRANCHES) if (hasBranch(branch)) KolPatronLeadIntel.add(branch, null, true)
    }

    /** Greenflight briefs only a player the faith welcomes: Welcoming or better with the Luddic Church. */
    fun churchWelcomes(): Boolean =
        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).relToPlayer.isAtWorst(com.fs.starfarer.api.campaign.RepLevel.WELCOMING)

    /** The brief: a great power introduced by its champion; its branch shows in the dialog. */
    fun introduce(power: KolPatronPower, text: TextPanelAPI?) {
        if (power.seat() != null) learn(power.key, text = text)
    }

    /**
     * Records a lead: marks where it leads (through the mission) and adds its branch intel, shown in the dialog when
     * [text] is given. [asked] is the market where it was learned, a fallback place for the pirate and Path leads.
     * Returns whether it was new.
     */
    private fun learn(key: String, asked: MarketAPI? = null, text: TextPanelAPI? = null, quiet: Boolean = false): Boolean {
        if (hasLead(key)) return false
        val branch = branchOf(key)
        val existing = KolPatronLeadIntel.get(branch)
        memory.set(K.LEAD_PREFIX + key, true)
        placeFor(key, asked)?.let { memory.set(K.PREFIX + "place_" + key, it.id) }
        mission()?.leadLearned(key)
        if (existing != null) existing.routeAdded(text) // the mercenaries' second route
        else KolPatronLeadIntel.add(branch, text, quiet)
        return true
    }

    /**
     * Where a wildcard lead is followed: the mercenaries at New Maxios, or at Cethlenn through Tri-Tachyon; the
     * pirates at Kanta's Den while they hold it, else the pirate market where the lead was learned; the Path at
     * Chalcedon while it holds it, else the Path market where the lead was learned.
     */
    private fun placeFor(key: String, asked: MarketAPI?): MarketAPI? {
        val economy = Global.getSector().economy
        val (marketId, factionId) = when (key) {
            PLAYER -> return economy.getMarket(KolStaticStrings.KOL_LYRA)
            CHARTER -> return economy.getMarket("new_maxios")?.takeIf { !it.isHidden }
            TRITACH -> return economy.getMarket("cethlenn")?.takeIf { !it.isHidden }
            PIRATE -> "kantas_den" to Factions.PIRATES
            PATH -> "chalcedon" to Factions.LUDDIC_PATH
            else -> return null
        }
        return economy.getMarket(marketId)?.takeIf { it.factionId == factionId }
            ?: asked?.takeIf { it.factionId == factionId }
    }

    /** The market recorded for a lead's place, while it still exists. */
    fun place(key: String): MarketAPI? = memory.getString(K.PREFIX + "place_" + key)?.let { Global.getSector().economy.getMarket(it) }

    /** The place's name for reply text, or a general phrase when there's none. */
    private fun placeText(key: String, asked: MarketAPI?): String = (place(key) ?: placeFor(key, asked))?.name
        ?: if (key == PATH) "one of the Path's worlds" else "one of their ports"

    /** One line naming a lead, for the dialog and the mission intel. */
    fun leadLine(key: String): String? {
        KolPatronPower.of(key)?.let { power ->
            val seat = power.seat() ?: return null
            return "${Misc.ucFirst(power.faction.displayNameWithArticle)}: decisions are made at ${seat.name}."
        }
        return when (key) {
            CHARTER -> place(CHARTER)?.let { "A mercenary company can be chartered at ${it.name}." }
                ?: "A mercenary company could be hired to guard the congregations."
            TRITACH -> place(TRITACH)?.let { "Tri-Tachyon can arrange Tactistar's services, at ${it.name}." }
                ?: "Tri-Tachyon can introduce you to contractors who offer protection."
            PIRATE -> place(PIRATE)?.let { "A pirate broker at ${it.name} deals in protection for the faithful." }
                ?: "A pirate broker deals in protection for the faithful."
            PLAYER -> "Your own forces, pledged under oath at the convocation."
            PATH -> place(PATH)?.let { "The Luddic Path might hear the Church out at ${it.name}." }
                ?: "The Luddic Path might be asked to shield the congregations."
            else -> null
        }
    }

    // --- asking by post ----------------------------------------------------------------------------------------

    /** Vanilla figures with storylines of their own: mentioned, never met, so they're never asked. */
    private fun offStage(person: PersonAPI): Boolean =
        person.id in KolStorySettings.patronOffStagePeople || person.postId == Ranks.POST_FACTION_LEADER

    /**
     * Who can be asked, and as what: any of the Path's people (`path`), the market's administrator (`admin`), or one
     * of the player's contacts (`contact`). Null for anyone else, the Order's and the player's own people, and the
     * vanilla figures kept off stage.
     */
    private fun roleOf(person: PersonAPI?, market: MarketAPI?): String? {
        if (person == null || offStage(person)) return null
        val factionId = person.faction?.id ?: return null
        if (factionId == KolStaticStrings.kolFactionID || factionId == Factions.PLAYER) return null
        if (factionId == Factions.LUDDIC_PATH) return "path"
        if (market == null) return null
        return when {
            market.admin === person -> "admin"
            ContactIntel.playerHasContact(person, false) -> "contact"
            else -> null
        }
    }

    /**
     * Every hint the person has, known or not. An administrator's come from their port: the charter at independent
     * markets and free ports, the pirate broker at pirate markets, the Tri-Tachyon route at Tri-Tachyon markets, the
     * Path at Path markets. A contact's come from their faction (the charter for independents).
     */
    private fun hintsOf(role: String, person: PersonAPI, market: MarketAPI?): List<String> = when (role) {
        "path" -> listOf(PATH)
        "admin" -> if (market == null) emptyList() else listOfNotNull(
            HEG_CURATE.takeIf { market.factionId == Factions.HEGEMONY && KolPatronParley.stage(KolPatronPower.HEGEMONY) == "sentAway" },
            PIRATE.takeIf { market.factionId == Factions.PIRATES },
            PATH.takeIf { market.factionId == Factions.LUDDIC_PATH },
            TRITACH.takeIf { market.factionId == Factions.TRITACHYON },
            CHARTER.takeIf { market.factionId == Factions.INDEPENDENT || market.isFreePort })
        "contact" -> listOfNotNull(when (person.faction.id) {
            Factions.PIRATES -> PIRATE
            Factions.LUDDIC_PATH -> PATH
            Factions.TRITACHYON -> TRITACH
            Factions.INDEPENDENT -> CHARTER
            else -> null
        })
        else -> emptyList()
    }

    /** The hints the person has that the player hasn't learned yet. */
    private fun newHints(person: PersonAPI?, market: MarketAPI?): List<String> {
        val role = roleOf(person, market) ?: return emptyList()
        if (pathAdmin(person, market)) return emptyList() // Chalcedon's administrator answers in person (pathAsk)
        return hintsOf(role, person!!, market).filterNot { hasLead(it) }
    }

    /** Condition: the active person can be asked (the mission runs, and they have a hint the player doesn't know). */
    fun askable(person: PersonAPI?, market: MarketAPI?): Boolean = newHints(person, market).isNotEmpty()

    /**
     * Asks the active person. A hostile official stonewalls (and can be asked again later); a hostile pirate names a
     * personal price first (`payBribe`). Otherwise every new hint is given in turn: `FireBest KolPatronAskReply` with
     * `$kolPatron_role` (admin / contact), `$kolPatron_lead` (the hint) and `$kolPatron_placeText`, then the lead.
     */
    fun ask(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>, returnTrigger: String = "PopulateOptions"): Boolean {
        val person = dialog.interactionTarget?.activePerson ?: return false
        val market = dialog.interactionTarget?.market
        val role = roleOf(person, market) ?: return false
        val hints = newHints(person, market)
        if (hints.isEmpty()) return false
        val local = map[MemKeys.LOCAL]
        // where the conversation goes back to: PopulateOptions, or a vanilla list (the Station King's KDStationKingOptions)
        local?.set("\$kolPatron_return", returnTrigger, 0f) // lasts the dialog (expiry stops while paused)
        local?.set("\$kolPatron_side", sideOf(person.faction.id), 0f)
        local?.set("\$kolPatron_commissioned", Misc.getCommissionFactionId() == person.faction.id, 0f)
        if (role != "path" && person.faction.isHostileTo(Factions.PLAYER)) {
            if (person.faction.id != Factions.PIRATES) {
                local?.set("\$kolPatron_role", "stonewall", 0f)
                FireBest.fire(null, dialog, map, "KolPatronAskReply")
                FireAll.fire(null, dialog, map, returnTrigger)
                return true
            }
            val price = KolStorySettings.patronPirateBribe
            local?.set("\$kolPatron_role", "bribe", 0f)
            local?.set("\$kolPatron_bribe", Misc.getDGSCredits(price.toFloat()), 0f)
            FireBest.fire(null, dialog, map, "KolPatronAskReply")
            if (Global.getSector().playerFleet.cargo.credits.get() < price) {
                dialog.optionPanel.setEnabled("kolPatron_bribePay", false)
                dialog.optionPanel.setTooltip("kolPatron_bribePay", "You don't have enough credits.")
            }
            return true
        }
        give(role, hints, market, dialog, map)
        FireAll.fire(null, dialog, map, returnTrigger)
        return true
    }

    /** Pays a hostile pirate's price: credits, then the broker's whereabouts and any other hints they have. */
    fun payBribe(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val person = dialog.interactionTarget?.activePerson ?: return false
        val market = dialog.interactionTarget?.market
        val role = roleOf(person, market) ?: return false
        val price = KolStorySettings.patronPirateBribe
        val credits = Global.getSector().playerFleet.cargo.credits
        if (credits.get() < price) return false
        credits.subtract(price.toFloat())
        com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity.addCreditsLossText(price, dialog.textPanel)
        val hints = newHints(person, market)
        if (PIRATE in hints) {
            map[MemKeys.LOCAL]?.set("\$kolPatron_placeText", placeText(PIRATE, market), 0f)
            FireBest.fire(null, dialog, map, "KolPatronBribePaid")
            learn(PIRATE, market, dialog.textPanel)
        }
        give(role, hints - PIRATE, market, dialog, map)
        FireAll.fire(null, dialog, map, map[MemKeys.LOCAL]?.getString("\$kolPatron_return") ?: "PopulateOptions")
        return true
    }

    /**
     * A Path fleet's captain, through Open Comm Link (from the fleet's options, or its tithe hail while hostile):
     * directs the player to Chalcedon, then returns to [returnTrigger]'s options.
     */
    fun askFleet(returnTrigger: String, dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        if (hasLead(PATH)) return false
        give("fleet", listOf(PATH), null, dialog, map)
        FireAll.fire(null, dialog, map, returnTrigger)
        return true
    }

    /** Plays each hint's reply and records its lead. */
    private fun give(role: String, hints: List<String>, market: MarketAPI?, dialog: InteractionDialogAPI,
                     map: MutableMap<String, MemoryAPI>) {
        val local = map[MemKeys.LOCAL]
        local?.set("\$kolPatron_role", role, 0f)
        for (hint in hints) {
            local?.set("\$kolPatron_lead", hint, 0f)
            local?.set("\$kolPatron_placeText", placeText(hint, market), 0f)
            FireBest.fire(null, dialog, map, "KolPatronAskReply")
            if (hint == HEG_CURATE) { // a step's hint: the Hegemony branch moves on, no branch of its own
                memory.set(K.LEAD_PREFIX + hint, true)
                KolPatronParley.advance(KolPatronPower.HEGEMONY, "jangala", dialog.textPanel)
            } else learn(hint, market, dialog.textPanel)
        }
    }

    /** Whose side the person speaks from, kept for the deferred power-specific replies. */
    private fun sideOf(factionId: String?): String = KolPatronPower.ofFaction(factionId)?.key ?: when (factionId) {
        Factions.TRITACHYON -> TRITACH
        Factions.PIRATES -> PIRATE
        Factions.LUDDIC_PATH -> PATH
        Factions.LUDDIC_CHURCH -> "church"
        Factions.INDEPENDENT -> "independent"
        else -> "other"
    }

    // --- the local congregation --------------------------------------------------------------------------------

    /** A market whose Luddic faithful can be sought out: Luddic Majority or held by the Church, not the Order's own. */
    fun hasCongregation(market: MarketAPI?): Boolean {
        if (market == null || market.isHidden || market.isPlayerOwned) return false
        if (market.factionId == KolStaticStrings.kolFactionID) return false
        if (market.memoryWithoutUpdate.getBoolean("\$kolPatron_congregationSought")) return false
        if (market.id == "mazalot" && KolPatronParley.mazalotOpen()) return false // the League's own scene there
        if (market.id == "jangala" && KolPatronParley.jangalaOpen()) return false // the Hegemony's
        return market.factionId == Factions.LUDDIC_CHURCH || market.hasCondition(Conditions.LUDDIC_MAJORITY)
    }

    /** Seeks out the congregation: marks the market, records the lead for the power that holds it, plays the reply. */
    fun seekCongregation(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val market = dialog.interactionTarget?.market ?: return false
        if (!hasCongregation(market)) return false
        market.memoryWithoutUpdate.set("\$kolPatron_congregationSought", true)
        map[MemKeys.LOCAL]?.set("\$kolPatron_side", sideOf(market.factionId), 0f)
        publishSeats()
        val leads = listOfNotNull(KolPatronPower.ofFaction(market.factionId)?.key)
        map[MemKeys.LOCAL]?.set("\$kolPatron_lead", leads.firstOrNull() ?: "none", 0f)
        FireBest.fire(null, dialog, map, "KolPatronCongregationReply")
        for (lead in leads) learn(lead, market, dialog.textPanel)
        return true
    }

    // --- the charter ------------------------------------------------------------------------------------------------

    private fun seeking(): Boolean = mission() != null && !memory.getBoolean(KolPatronFlags.ANNOUNCE)

    /**
     * The charter's route at this market, or null: New Maxios for the independent company (`charter`), Cethlenn for
     * Tactistar (`tritach`), once its lead is known and while the quest seeks.
     */
    fun charterRoute(market: MarketAPI?): String? {
        if (market == null || !seeking()) return null
        return listOf(CHARTER, TRITACH).firstOrNull { hasLead(it) && place(it)?.id == market.id }
    }

    /**
     * Opens the broker's scene: `$kolPatron_route` (`independent` / `tritach`), `$kolPatron_company`, `$kolPatron_cost`,
     * and `$kolPatron_indieHostile` (skepticism when the independents are hostile; no gate).
     */
    fun charterOpen(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val route = charterRoute(dialog.interactionTarget?.market) ?: return false
        val local = map[MemKeys.LOCAL] ?: return false
        local.set("\$kolPatron_route", if (route == TRITACH) "tritach" else "independent", 0f)
        local.set("\$kolPatron_company", if (route == TRITACH) "Tactistar" else KolStorySettings.patronCharterCompanyName, 0f)
        local.set("\$kolPatron_cost", Misc.getDGSCredits(KolStorySettings.patronCharterCost.toFloat()), 0f)
        local.set("\$kolPatron_indieHostile", Global.getSector().getFaction(Factions.INDEPENDENT).isHostileTo(Factions.PLAYER), 0f)
        return true
    }

    /** The representative's pitch (`FireBest KolPatronCharterPitch`); signing is disabled without the funds. */
    fun charterPitch(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val route = charterRoute(dialog.interactionTarget?.market) ?: return false
        KolPatronParley.meet(if (route == TRITACH) "tactistarRep" else "charterAdmiral", dialog)
        FireBest.fire(null, dialog, map, "KolPatronCharterPitch")
        if (Global.getSector().playerFleet.cargo.credits.get() < KolStorySettings.patronCharterCost) {
            dialog.optionPanel.setEnabled("kolPatron_charterSign", false)
            dialog.optionPanel.setTooltip("kolPatron_charterSign", "You don't have enough credits.")
        }
        return true
    }

    /**
     * The player signs the charter and pays its whole first leg up front: recorded as `charter`, with its source
     * (`$kol_patronCharterSource`: `independent` or `tritach`); the quest moves to ANNOUNCE.
     */
    fun signCharter(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val route = charterRoute(dialog.interactionTarget?.market) ?: return false
        val mission = mission() ?: return false
        val cost = KolStorySettings.patronCharterCost
        val credits = Global.getSector().playerFleet.cargo.credits
        if (credits.get() < cost) return false
        credits.subtract(cost.toFloat())
        com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity.addCreditsLossText(cost, dialog.textPanel)
        memory.set(K.CHARTER_SOURCE, if (route == TRITACH) TRITACH else "independent")
        mission.signed(CHARTER, if (route == TRITACH) "cethlenn" else "new_maxios", null, dialog, map)
        return true
    }

    /** The announcing assembly reads the charter in: Knights reputation lost (`patronCharterOffenseRep`). */
    fun charterOffense(text: TextPanelAPI) = knightsRep(KolStorySettings.patronCharterOffenseRep, text)

    private fun knightsRep(amount: Float, text: TextPanelAPI) {
        if (amount <= 0f) return
        knightsRepAction(com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact().apply { delta = -amount }, text)
    }

    /**
     * A Knights reputation change, as a vanilla reputation action (shown and reported), with the Church set to match.
     * The Knights and the Church share one standing (UpdateRelationships mirrors them on reputation events), so a
     * direct change to the Knights alone would be undone by the next Church event. Unless the Order has schismed,
     * when the two are deliberately apart.
     */
    fun knightsRepAction(impact: com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact, text: TextPanelAPI?) {
        val sector = Global.getSector()
        sector.adjustPlayerReputation(
            com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope(
                com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions.CUSTOM, impact, null, text, true),
            KolStaticStrings.kolFactionID)
        if (sector.memoryWithoutUpdate.getBoolean(KolStaticStrings.MEMKEY_KOL_SCHISMED)) return
        val knights = sector.getFaction(KolStaticStrings.kolFactionID).getRelationship(Factions.PLAYER)
        sector.getFaction(Factions.LUDDIC_CHURCH)?.setRelationship(Factions.PLAYER, knights)
    }

    // --- the pirates -------------------------------------------------------------------------------------------------

    /** The pirates favor the player (Favorable or better): bribes halved, a nominal fee, a cut promised, the plan tipped. */
    private fun pirateFavored(): Boolean =
        Global.getSector().getFaction(Factions.PIRATES).relToPlayer.isAtWorst(com.fs.starfarer.api.campaign.RepLevel.FAVORABLE)

    private fun pirateBribe(base: Int) = if (pirateFavored()) base / 2 else base
    private fun piratePrice() = if (pirateFavored()) KolStorySettings.patronPirateNominalFee else KolStorySettings.patronPirateDeal

    private fun pay(amount: Int, dialog: InteractionDialogAPI): Boolean {
        val credits = Global.getSector().playerFleet.cargo.credits
        if (credits.get() < amount) return false
        credits.subtract(amount.toFloat())
        com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity.addCreditsLossText(amount, dialog.textPanel)
        return true
    }

    private fun disableUnaffordable(dialog: InteractionDialogAPI, option: String, amount: Int) {
        if (Global.getSector().playerFleet.cargo.credits.get() >= amount) return
        dialog.optionPanel.setEnabled(option, false)
        dialog.optionPanel.setTooltip(option, "You don't have enough credits.")
    }

    private fun piratePublish(map: MutableMap<String, MemoryAPI>) {
        val local = map[MemKeys.LOCAL] ?: return
        local.set("\$kolPatron_favored", pirateFavored(), 0f)
        local.set("\$kolPatron_audienceBribe", Misc.getDGSCredits(pirateBribe(KolStorySettings.patronPirateAudienceBribe).toFloat()), 0f)
        local.set("\$kolPatron_agreementBribe", Misc.getDGSCredits(pirateBribe(KolStorySettings.patronPirateAgreementBribe).toFloat()), 0f)
        local.set("\$kolPatron_dealPrice", Misc.getDGSCredits(piratePrice().toFloat()), 0f)
        local.set("\$kolPatron_cut", Misc.getDGSCredits(KolStorySettings.patronPirateCut.toFloat()), 0f)
    }

    /** The Station King can arrange an audience with Kanta's people: the lead known, while the quest seeks. */
    fun pirateArrangeOpen(market: MarketAPI?): Boolean =
        market?.id == "kantas_den" && hasLead(PIRATE) && seeking() && !memory.getBoolean(K.PREFIX + "pirateAudience")

    /** The Station King names his price for arranging the audience (`FireBest KolPatronSKArrange`). */
    fun pirateArrangeOffer(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        piratePublish(map)
        FireBest.fire(null, dialog, map, "KolPatronSKArrange")
        disableUnaffordable(dialog, "kolPatron_skArrangePay", pirateBribe(KolStorySettings.patronPirateAudienceBribe))
        return true
    }

    /** The audience bribe, paid to the Station King: a shuttle down is arranged. */
    fun pirateAudiencePay(dialog: InteractionDialogAPI): Boolean {
        if (!pay(pirateBribe(KolStorySettings.patronPirateAudienceBribe), dialog)) return false
        memory.set(K.PREFIX + "pirateAudience", true)
        return true
    }

    /** The shuttle down to Kanta's delegate: the audience arranged, while the quest seeks. */
    fun pirateShuttle(market: MarketAPI?): Boolean =
        market?.id == "kantas_den" && seeking() && memory.getBoolean(K.PREFIX + "pirateAudience")

    /**
     * Meets Kanta's delegate: generated for the scene, not remembered (tokens `$global.kolPerson_kantaDelegate_*`),
     * then `FireBest KolPatronPirateOpen`.
     */
    fun pirateMeet(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val delegate = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson()
        KolConvocation.publishPerson("\$kolPerson_kantaDelegate_", delegate)
        dialog.visualPanel.showPersonInfo(delegate, true)
        piratePublish(map)
        FireBest.fire(null, dialog, map, "KolPatronPirateOpen")
        return true
    }

    /** The delegate asks a token of sincerity (`FireBest KolPatronPirateAgreement`). */
    fun pirateAgreementOffer(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        FireBest.fire(null, dialog, map, "KolPatronPirateAgreement")
        disableUnaffordable(dialog, "kolPatron_piratePayAgreement", pirateBribe(KolStorySettings.patronPirateAgreementBribe))
        return true
    }

    /** The agreement bribe, paid: the terms follow. Paid once; coming back later goes straight to the terms. */
    fun piratePayAgreement(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        if (!pay(pirateBribe(KolStorySettings.patronPirateAgreementBribe), dialog)) return false
        memory.set(K.PREFIX + "pirateAgreed", true)
        return pirateTerms(dialog, map)
    }

    /** The arrangement's price (`FireBest KolPatronPirateTerms`): the full price, or a nominal fee with a cut promised. */
    fun pirateTerms(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        FireBest.fire(null, dialog, map, "KolPatronPirateTerms")
        disableUnaffordable(dialog, "kolPatron_pirateSign", piratePrice())
        return true
    }

    /** The final payment: the pirates are the Church's patron, and the quest moves to ANNOUNCE (and to the swarm). */
    fun signPirate(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val mission = mission() ?: return false
        if (!pay(piratePrice(), dialog)) return false
        memory.set(K.TRIED_PREFIX + PIRATE, true)
        mission.signed(PIRATE, "kanta", null, dialog, map)
        return true
    }

    // --- the Path ----------------------------------------------------------------------------------------------------

    /** Chalcedon's administrator, while the quest seeks and before the Path has been asked there. */
    fun pathAdmin(person: PersonAPI?, market: MarketAPI?): Boolean =
        person != null && market?.id == "chalcedon" && market.admin === person && seeking() && !memory.getBoolean(K.TRIED_PREFIX + PATH)

    /**
     * The player asks Chalcedon's administrator (with or without the lead): the Path branch is found, then closed by
     * the speech (`FireBest KolPatronPathSpeech`, warmer with `$kolPatron_pathFavored`).
     */
    fun pathAsk(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val market = dialog.interactionTarget?.market
        if (!pathAdmin(dialog.interactionTarget?.activePerson, market)) return false
        map[MemKeys.LOCAL]?.set("\$kolPatron_pathFavored",
            Global.getSector().getFaction(Factions.LUDDIC_PATH).relToPlayer.isAtWorst(com.fs.starfarer.api.campaign.RepLevel.FAVORABLE), 0f)
        learn(PATH, market, dialog.textPanel)
        FireBest.fire(null, dialog, map, "KolPatronPathSpeech")
        return true
    }

    /** The speech is over: the attempt is recorded (`$kolPatron_tried_path`) and the branch closes. */
    fun pathTried(text: TextPanelAPI) {
        memory.set(K.TRIED_PREFIX + PATH, true)
        KolPatronLeadIntel.get(PATH)?.advance("tried", text)
    }

    /** The next assembly murmurs about the attempt, once: Knights reputation lost (`patronPathAttemptRep`). */
    fun pathMurmur(text: TextPanelAPI) {
        memory.set(K.PREFIX + "pathMurmured", true)
        knightsRep(KolStorySettings.patronPathAttemptRep, text)
    }

    // --- history -----------------------------------------------------------------------------------------------

    /**
     * Vanilla records no flag for meeting the Tri-Tachyon fixer. Its visit flag (`$gaKA_triTachyonVisit`) is set some
     * time after the Kallichore hack and unset when the player visits a Tri-Tachyon bar, where the fixer waits: seeing
     * it set and then unset means they met. Runs with the story scripts from the start of the game.
     */
    fun watchFixerVisit() {
        if (memory.getBoolean(K.MET_FIXER)) return
        if (memory.getBoolean("\$gaKA_triTachyonVisit")) memory.set(K.FIXER_VISIT_SEEN, true)
        else if (memory.getBoolean(K.FIXER_VISIT_SEEN)) {
            memory.set(K.MET_FIXER, true)
            memory.unset(K.FIXER_VISIT_SEEN)
        }
    }

    // --- shared ------------------------------------------------------------------------------------------------

    /**
     * Publishes the seats for reply text: `$global.kolPatron_seats` ("the Persean League at Kazeron, ...") and
     * `$global.kolPatron_seat_<power>` for each power that holds a market.
     */
    private fun publishSeats() {
        val parts = KolPatronPower.values().mapNotNull { power ->
            val seat = power.seat()
            if (seat == null) memory.unset(K.PREFIX + "seat_" + power.key) else memory.set(K.PREFIX + "seat_" + power.key, seat.name, 0f)
            seat?.let { "${power.faction.displayNameWithArticle} at ${it.name}" }
        }
        memory.set(K.PREFIX + "seats", Misc.getAndJoined(parts), 0f)
    }

    // --- people ------------------------------------------------------------------------------------------------

    /**
     * Mother Moyra Standfast, the Diktat parley's intermediary. If she's missing (Volturn wasn't in the sector at the
     * start), a curate of the Church's stands in, remembered as the intermediary is.
     */
    fun standfast(): PersonAPI = people.getPerson(People.STANDFAST)
        ?: KolPeople.remembered(K.STANDIN_PREFIX + People.STANDFAST) {
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson().apply {
                rankId = if (isMale) Ranks.FATHER else Ranks.MOTHER
                postId = Ranks.POST_SHRINE_PRIEST
            }
        }

    /**
     * Spender Balashi, who screens calls to Andrada: the Diktat parley's gatekeeper. If he's missing, an officer of
     * the Diktat stands in for the scene, and isn't remembered.
     */
    fun balashi(): PersonAPI = people.getPerson(People.SEC_OFFICER)
        ?: Global.getSector().getFaction(Factions.DIKTAT).createRandomPerson().apply {
            rankId = Ranks.SPACE_LIEUTENANT
            postId = Ranks.POST_OFFICER
        }

    /**
     * The Tri-Tachyon fixer, for the charter through Tri-Tachyon. Null when missing: that route then goes through a
     * Tri-Tachyon administrator instead.
     */
    fun fixer(): PersonAPI? = people.getPerson(People.TRITACH_FIXER)
}
