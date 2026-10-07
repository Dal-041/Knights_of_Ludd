package org.selkie.kol.campaign.libra

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc

/** A supply line: its key and the commodities it carries. */
enum class KolLibraLine(val key: String, val label: String, val commodities: List<String>) {
    METALS("metals", "refined metals", listOf(Commodities.METALS, Commodities.RARE_METALS)),
    INDUSTRY("industry", "heavy industry", listOf(Commodities.SUPPLIES, Commodities.HEAVY_MACHINERY)),
    ORGANICS("organics", "organics", listOf(Commodities.ORGANICS)),
    FOOD("food", "food", listOf(Commodities.FOOD));

    companion object {
        fun of(key: String?) = values().firstOrNull { it.key == key }
    }
}

/**
 * Libra's agreements (design, decision 3): the supply contract's four lines, negotiated with a market's quartermaster
 * (vanilla's supply officer, present wherever the spaceport runs), and the crew contract, with its administrator. Each costs a standing and a price, and sometimes a raider task at the system's edge. Once
 * penned, an agreement is decided, not monitored. Material suppliers carry `kol_libra_supply` until the drill.
 */
object KolLibraAgreements {
    private const val PENDING_KEY = "\$kolLibra_pending"     // on the raider fleet: "line:<key>:<market>" or "crew:<market>"

    // --- what's offered where ------------------------------------------------------------------------------------

    /** A market qualifies for a line: at least `minSupply` available of each commodity, and no deficit. */
    fun qualifies(market: MarketAPI, line: KolLibraLine): Boolean = line.commodities.all {
        val com = market.getCommodityData(it) ?: return@all false
        com.available >= KolLibraSettings.minSupply && com.deficitQuantity <= 0
    }

    /** The lines this market could supply now: the supply contract is open, and the line isn't penned yet. */
    fun linesHere(market: MarketAPI?): List<KolLibraLine> {
        if (market == null || market == KolLibra.market() || !KolLibraSupplyContract.active()) return emptyList()
        val data = KolLibra.data()
        return KolLibraLine.values().filter { it.key !in data.lines && qualifies(market, it) }
    }

    /** The crew contract can be negotiated here: open, two vouches won, not yet penned, a Luddic-Majority market. */
    fun crewHere(market: MarketAPI?): Boolean = market != null && market != KolLibra.market() &&
            KolLibraCrewContract.active() && KolLibra.data().vouches.size >= 2 && KolLibra.data().crewSupplier == null &&
            market.hasCondition(Conditions.LUDDIC_MAJORITY)

    /** The person is this market's quartermaster (supply lines). */
    private fun quartermaster(person: com.fs.starfarer.api.characters.PersonAPI, market: MarketAPI) =
        person.postId == com.fs.starfarer.api.impl.campaign.ids.Ranks.POST_SUPPLY_OFFICER && person.market == market

    /** What the active person can negotiate: supply lines with the quartermaster, the crew contract with the administrator. */
    private fun offers(dialog: InteractionDialogAPI): Pair<List<KolLibraLine>, Boolean> {
        val market = dialog.interactionTarget?.market ?: return emptyList<KolLibraLine>() to false
        val person = dialog.interactionTarget?.activePerson ?: return emptyList<KolLibraLine>() to false
        if (pending(market)) return emptyList<KolLibraLine>() to false
        val lines = if (quartermaster(person, market)) linesHere(market) else emptyList()
        val crew = market.admin === person && crewHere(market)
        return lines to crew
    }

    /** The active person has something to negotiate. */
    fun negotiable(dialog: InteractionDialogAPI): Boolean = offers(dialog).let { (lines, crew) -> lines.isNotEmpty() || crew }

    private fun pending(market: MarketAPI): Boolean = Global.getSector().allLocations.any { loc ->
        loc.fleets.any { it.memoryWithoutUpdate.getString(PENDING_KEY)?.endsWith(":" + market.id) == true }
    }

    // --- negotiating ----------------------------------------------------------------------------------------------

    /** Lists what can be negotiated here as options (`kolLibra_line_<key>`, `kolLibra_crew`), then a way back. */
    fun negotiateOptions(dialog: InteractionDialogAPI) {
        val (lines, crew) = offers(dialog)
        val options = dialog.optionPanel
        options.clearOptions()
        for (line in lines) options.addOption("[PLACEHOLDER] A supply line for ${line.label}", "kolLibra_line_${line.key}")
        if (crew) options.addOption("[PLACEHOLDER] A crew contract: volunteers for Libra", "kolLibra_crew")
        options.addOption("[PLACEHOLDER] Never mind", "kolLibra_negotiateBack")
    }

    /**
     * The terms for the chosen agreement (`$option` = `kolLibra_line_<key>` or `kolLibra_crew`): publishes
     * `$kolLibra_terms*` and fires `KolLibraTerms`; accepting is disabled below the standing or without the credits.
     */
    fun terms(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val market = dialog.interactionTarget?.market ?: return false
        val local = map[MemKeys.LOCAL] ?: return false
        val option = local.getString("\$option") ?: return false
        val crew = option == "kolLibra_crew"
        val line = KolLibraLine.of(option.removePrefix("kolLibra_line_"))
        if (!crew && line == null) return false
        val price = if (crew) KolLibraSettings.crewPrice else KolLibraSettings.supplyPrice
        val standing = runCatching { RepLevel.valueOf(if (crew) KolLibraSettings.crewStanding else KolLibraSettings.supplyStanding) }
            .getOrDefault(RepLevel.FAVORABLE)
        val own = market.isPlayerOwned
        val standingOk = own || market.faction.relToPlayer.isAtWorst(standing)
        // the supplying faction can't be worse than Neutral with the Luddic Church while the agreement is negotiated
        val churchOk = churchOk(market)
        local.set("\$kolLibra_termsWhat", if (crew) "crew" else line!!.key, 0f)
        local.set("\$kolLibra_termsLabel", if (crew) "volunteers for Libra's crew" else line!!.label, 0f)
        local.set("\$kolLibra_termsPrice", Misc.getDGSCredits(price.toFloat()), 0f)
        local.set("\$kolLibra_termsStanding", standing.displayName.lowercase(), 0f)
        local.set("\$kolLibra_termsStandingOk", standingOk, 0f)
        local.set("\$kolLibra_termsCrew", crew, 0f)
        local.set("\$kolLibra_termsChurchOk", churchOk, 0f)
        FireBest.fire(null, dialog, map, "KolLibraTerms")
        val options = dialog.optionPanel
        if (!churchOk) {
            options.setEnabled("kolLibra_termsAccept", false)
            options.setTooltip("kolLibra_termsAccept", "Relations between ${market.faction.displayNameWithArticle} and the Luddic Church are too poor.")
        } else if (!standingOk) {
            options.setEnabled("kolLibra_termsAccept", false)
            options.setTooltip("kolLibra_termsAccept", "Your standing with ${market.faction.displayNameWithArticle} is too low.")
        } else if (Global.getSector().playerFleet.cargo.credits.get() < price) {
            options.setEnabled("kolLibra_termsAccept", false)
            options.setTooltip("kolLibra_termsAccept", "You don't have enough credits.")
        }
        return true
    }

    /** The supplier's faction is at least Neutral with the Luddic Church (the Church itself always is). */
    fun churchOk(market: MarketAPI): Boolean = market.faction.id == Factions.LUDDIC_CHURCH ||
            market.faction.getRelationshipLevel(Factions.LUDDIC_CHURCH).isAtWorst(RepLevel.NEUTRAL)

    /**
     * Accepts the terms on the table: pays, then either pens the agreement at once or (with `raiderTaskChance`) asks
     * the player to clear raiders at the system's edge first. Sets `$kolLibra_raiderTask` for the reply.
     */
    fun accept(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>): Boolean {
        val market = dialog.interactionTarget?.market ?: return false
        val local = map[MemKeys.LOCAL] ?: return false
        val what = local.getString("\$kolLibra_termsWhat") ?: return false
        val crew = what == "crew"
        if (!churchOk(market)) return false
        val price = if (crew) KolLibraSettings.crewPrice else KolLibraSettings.supplyPrice
        val credits = Global.getSector().playerFleet.cargo.credits
        if (credits.get() < price) return false
        credits.subtract(price.toFloat())
        AddRemoveCommodity.addCreditsLossText(price, dialog.textPanel)
        val tag = if (crew) "crew:${market.id}" else "line:$what:${market.id}"
        val raiders = if (market.starSystem != null && Math.random() < KolLibraSettings.raiderTaskChance) spawnRaider(market, tag) else null
        local.set("\$kolLibra_raiderTask", raiders != null, 0f)
        if (raiders != null) {
            // the mission that asked tracks it: listed, in the next step, and the raiders marked on the map
            val mission: KolLibraMission? = if (crew) KolLibraCrewContract.get() else KolLibraSupplyContract.get()
            mission?.addRaiderTask(tag, raiders, market, dialog.textPanel)
        } else pen(tag, dialog)
        return true
    }

    /** Raiders waiting at the edge of the supplier's system: the agreement is penned once they're beaten or gone. */
    private fun spawnRaider(market: MarketAPI, tag: String): CampaignFleetAPI? {
        val system = market.starSystem ?: return null
        val params = FleetParamsV3(null, Factions.PIRATES, null, FleetTypes.PATROL_MEDIUM,
            KolLibraSettings.raiderFleetPoints, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return null
        fleet.name = "[PLACEHOLDER] Raiders"
        val edge = system.jumpPoints.maxByOrNull { Misc.getDistance(it.location, market.primaryEntity.location) }
            ?: market.primaryEntity
        system.addEntity(fleet)
        fleet.setLocation(edge.location.x, edge.location.y)
        fleet.memoryWithoutUpdate.set(PENDING_KEY, tag)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
        fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, edge, 1_000_000f, "lurking")
        fleet.addEventListener(KolLibraRaiderListener())
        return fleet
    }

    /** Pens an agreement (`line:<key>:<market>` or `crew:<market>`). */
    fun pen(tag: String, dialog: InteractionDialogAPI?) {
        val parts = tag.split(":")
        val data = KolLibra.data()
        if (parts[0] == "crew") {
            data.crewSupplier = parts.getOrNull(1)
            val name = parts.getOrNull(1)?.let { Global.getSector().economy.getMarket(it)?.name } ?: "the supplier"
            KolLibraCrewContract.get()?.update("[PLACEHOLDER] Volunteers agreed at $name", dialog?.textPanel)
            return
        }
        val line = KolLibraLine.of(parts.getOrNull(1)) ?: return
        val marketId = parts.getOrNull(2) ?: return
        data.lines[line.key] = marketId
        Global.getSector().economy.getMarket(marketId)?.let { m ->
            if (!m.hasCondition(KolLibraEffects.SUPPLY_CONDITION)) m.addCondition(KolLibraEffects.SUPPLY_CONDITION)
            m.reapplyConditions()
        }
        val name = Global.getSector().economy.getMarket(marketId)?.name ?: "the supplier"
        KolLibraSupplyContract.get()?.update("Supply line penned: ${line.label}, with $name", dialog?.textPanel)
        if (KolLibraLine.values().all { it.key in data.lines }) KolLibra.complete(KolLibraMilestone.SUPPLY, dialog)
    }

    /** The drill: every supplier's availability condition is lifted. */
    fun liftSupplierConditions() {
        for (marketId in KolLibra.data().lines.values.toSet()) {
            val market = Global.getSector().economy.getMarket(marketId) ?: continue
            if (market.hasCondition(KolLibraEffects.SUPPLY_CONDITION)) {
                market.removeCondition(KolLibraEffects.SUPPLY_CONDITION)
                market.reapplyConditions()
            }
        }
    }

    /** The lines this supplier carries (for its condition). */
    fun linesOf(marketId: String): List<KolLibraLine> =
        KolLibra.data().lines.filterValues { it == marketId }.keys.mapNotNull { KolLibraLine.of(it) }

    // --- vouches ----------------------------------------------------------------------------------------------

    /**
     * Who can vouch, by person id. The Order's two only while their relationship with the player is above 0.
     * Bornanew is deliberately absent: he gives his word in a scene (rules.csv), but it doesn't count toward the two.
     */
    val VOUCHERS = listOf("jaspis", "oak", "standfast", "killa_curate", "kol_knightcaptain", "kol_intel_director")
    private val ORDER = setOf("kol_knightcaptain", "kol_intel_director")

    /** This person can vouch for Libra now: the crew contract is open, fewer than two vouches, not theirs yet. */
    fun canVouch(id: String?): Boolean {
        if (id == null || id !in VOUCHERS || !KolLibraCrewContract.active()) return false
        val data = KolLibra.data()
        if (data.vouches.size >= 2 || id in data.vouches || data.crewSupplier != null) return false
        // vanilla's razed Volturn shrine (`$global.lpp_volturnShrineRazed`) leaves Standfast with nothing to send
        if (id == "standfast" && Global.getSector().memoryWithoutUpdate.getBoolean("\$lpp_volturnShrineRazed")) return false
        if (id in ORDER) {
            val person = Global.getSector().importantPeople.getPerson(id) ?: return false
            if (person.relToPlayer.rel <= 0f) return false
        }
        return true
    }

    fun vouch(id: String, dialog: InteractionDialogAPI?) {
        if (!canVouch(id)) return
        KolLibra.data().vouches.add(id)
        val n = KolLibra.data().vouches.size
        val who = Global.getSector().importantPeople.getPerson(id)?.nameString ?: "the curate sacraria of Killa"
        KolLibraCrewContract.get()?.update("Vouch for Libra's crew from $who ($n of 2)", dialog?.textPanel)
    }

}

/** A raider task: when the raiders are beaten or gone, the agreement they held up is penned. */
class KolLibraRaiderListener : FleetEventListener {
    private var resolved = false

    private fun resolve(fleet: CampaignFleetAPI?) {
        if (resolved || fleet == null) return
        val tag = fleet.memoryWithoutUpdate.getString("\$kolLibra_pending") ?: return
        resolved = true
        KolLibraSupplyContract.get()?.raiderTaskDone(tag)
        KolLibraCrewContract.get()?.raiderTaskDone(tag)
        KolLibraAgreements.pen(tag, null)
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) resolve(fleet)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (fleet == null || battle == null || !battle.isPlayerInvolved) return
        if (!battle.wasFleetDefeated(fleet, primaryWinner)) return
        resolve(fleet)
        if (fleet.isAlive) Misc.giveStandardReturnToSourceAssignments(fleet, true)
    }
}

/** A supplier's diverted goods (visible): −1 available of each commodity on its penned lines, until Libra's drill. */
class KolLibraSupplyCondition : BaseMarketConditionPlugin() {
    override fun apply(id: String) {
        for (line in KolLibraAgreements.linesOf(market.id)) for (c in line.commodities)
            market.getCommodityData(c)?.availableStat?.modifyFlat(id, -1f, "Supplying Battlestar Libra")
    }

    override fun unapply(id: String) {
        for (line in KolLibraLine.values()) for (c in line.commodities) market.getCommodityData(c)?.availableStat?.unmodifyFlat(id)
    }

    override fun createTooltipAfterDescription(tooltip: com.fs.starfarer.api.ui.TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        val lines = KolLibraAgreements.linesOf(market.id)
        if (lines.isEmpty()) return
        tooltip.addPara("Supplying Battlestar Libra with %s.", 10f, Misc.getHighlightColor(), Misc.getAndJoined(lines.map { it.label }))
        val goods = lines.flatMap { it.commodities }.mapNotNull { Global.getSettings().getCommoditySpec(it)?.name?.lowercase() }
        tooltip.addPara("%s availability of %s.", 3f, Misc.getNegativeHighlightColor(), "-1", Misc.getAndJoined(goods))
    }
}
