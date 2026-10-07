package org.selkie.kol.campaign.libra

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.helpers.KolStaticStrings
import java.awt.Color

/**
 * Libra's milestone events as missions (design, decision 8): each accepted when the player asks Martins, with the
 * premise in its description, the next step in its bullet, and an update at each step. Markers on definite
 * destinations only; ambiguous ones are hinted in the description. The contracts' state stays in KolLibraData, so
 * the missions read it and move on from it.
 */
abstract class KolLibraMission : HubMissionWithSearch() {
    /** A step's update: shown above the next step. */
    fun update(text: String, panel: TextPanelAPI?) = sendUpdateIfPlayerHasIntel(UPDATE_PREFIX + text, panel)

    /** A quartermaster's (or administrator's) favor: raiders at the edge of the supplier's system hold up an agreement. */
    class RaiderTask(val tag: String, val fleet: CampaignFleetAPI, val marketId: String)

    protected val raiderTasks = ArrayList<RaiderTask>()

    private fun RaiderTask.marketName() = Global.getSector().economy.getMarket(marketId)?.name ?: "the supplier"
    private fun RaiderTask.systemName() = Global.getSector().economy.getMarket(marketId)?.starSystem?.baseName ?: "the system"

    /** Tracks a raider task: an update, a line in the description and the next step, and the raiders marked on the map. */
    fun addRaiderTask(tag: String, fleet: CampaignFleetAPI, market: MarketAPI, panel: TextPanelAPI?) {
        val task = RaiderTask(tag, fleet, market.id)
        raiderTasks.add(task)
        (currentStage as? Enum<*>)?.let { stage -> makeImportant(fleet, "\$kolLibra_raiderTask", stage) }
        update("[PLACEHOLDER] Raiders at the edge of the ${task.systemName()} system hold up the agreement with ${task.marketName()}", panel)
    }

    /** The raiders are beaten or gone: the task is done (the agreement is penned by the caller). */
    fun raiderTaskDone(tag: String) {
        val task = raiderTasks.firstOrNull { it.tag == tag } ?: return
        raiderTasks.remove(task)
        Misc.makeUnimportant(task.fleet, getReason())
        task.fleet.memoryWithoutUpdate.unset("\$kolLibra_raiderTask")
    }

    /** The open raider tasks, for the description. */
    protected fun describeRaiderTasks(info: TooltipMakerAPI) {
        for (task in raiderTasks) info.addPara("[PLACEHOLDER] Raiders lurk at the edge of the %s system, holding up the agreement with %s.",
            10f, Misc.getHighlightColor(), task.systemName(), task.marketName())
    }

    /** The open raider tasks, as next steps. */
    protected fun raiderTaskSteps(info: TooltipMakerAPI, tc: Color, pad: Float) {
        for (task in raiderTasks) info.addPara("Clear the raiders at the edge of the %s system", pad, tc, Misc.getHighlightColor(), task.systemName())
    }

    override fun addBulletPointsPre(info: TooltipMakerAPI, tc: Color, initPad: Float, mode: ListInfoMode) {
        (listInfoParam as? String)?.takeIf { it.startsWith(UPDATE_PREFIX) }
            ?.let { info.addPara(it.removePrefix(UPDATE_PREFIX), tc, initPad) }
    }

    protected fun martins(): PersonAPI? = Global.getSector().importantPeople.getPerson(KolStaticStrings.KolPrelude.MARTINS_ID)

    protected fun libraEntity(): SectorEntityToken? = KolLibra.market()?.primaryEntity

    protected fun basics(starting: Any, completed: Any) {
        setStartingStage(starting)
        addSuccessStages(completed)
        setStoryMission()
        setNoRepChanges()
    }

    companion object {
        const val UPDATE_PREFIX = "kolLibra:"

        /** Starts a Libra mission, given by Martins (when the player asks him). */
        fun start(id: String, dialog: InteractionDialogAPI?): Boolean {
            val martins = Global.getSector().importantPeople.getPerson(KolStaticStrings.KolPrelude.MARTINS_ID) ?: return false
            val mission = Global.getSettings().getMissionSpec(id)?.createMission() ?: return false
            mission.setPersonOverride(martins)
            mission.createAndAbortIfFailed(martins.market, false)
            if (mission.isMissionCreationAborted) return false
            mission.accept(dialog, null)
            return true
        }
    }
}

/** The supply contract: four lines to pen, in any order, with any qualifying supplier. No markers: suppliers are ambiguous. */
class KolLibraSupplyContract : KolLibraMission() {
    enum class Stage { LINES, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(REF, ACTIVE)) return false
        basics(Stage.LINES, Stage.COMPLETED)
        return true
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (currentStage == Stage.LINES && KolLibra.done(KolLibraMilestone.SUPPLY)) setCurrentStage(Stage.COMPLETED, null, null)
    }

    override fun getBaseName(): String = "Libra: A Supply Line"

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        info.addPara("[PLACEHOLDER] Charity keeps Libra breathing; it doesn't mend a hull. Knightmaster Martins wants four " +
                "standing supply lines for the repairs, penned with the quartermasters of markets that have the goods to spare.", 10f)
        val lines = KolLibra.data().lines
        for (line in KolLibraLine.values()) {
            val supplier = lines[line.key]?.let { Global.getSector().economy.getMarket(it)?.name }
            if (supplier != null) info.addPara("%s: penned with %s", 3f, Misc.getGrayColor(), h, Misc.ucFirst(line.label), supplier)
            else info.addPara("%s: open", 3f, Misc.getTextColor(), h, Misc.ucFirst(line.label))
        }
        info.addPara("[PLACEHOLDER] A supplier must have at least %s of each of the line's goods available, and no shortfall " +
                "in them, and its faction must be on at least neutral terms with the Luddic Church. Its quartermaster will want " +
                "standing, credits, and perhaps a favor.", 10f, h,
            "" + KolLibraSettings.minSupply)
        describeRaiderTasks(info)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        info.addPara("Pen supply lines for Libra: %s of 4", pad, tc, Misc.getHighlightColor(), "" + KolLibra.data().lines.size)
        raiderTaskSteps(info, tc, 0f)
        return true
    }

    companion object {
        const val ID = "kolLibraSupplyContract"
        const val REF = "\$kolLibraSupply_ref"
        const val ACTIVE = "\$kolLibraSupply_active"
        fun get(): KolLibraSupplyContract? = Global.getSector().memoryWithoutUpdate.get(REF) as? KolLibraSupplyContract
        fun active() = Global.getSector().memoryWithoutUpdate.getBoolean(ACTIVE)
    }
}

/**
 * The crew contract: two vouches (the reachable vouchers marked), the agreement with a Luddic-Majority world (a hint;
 * any will do), then the escort of the first crew convoy (the supplier marked). The raiders strike about 30% along
 * the route. A lost convoy sends the mission back to Martins for another, with the vouches and the agreement kept.
 */
class KolLibraCrewContract : KolLibraMission(), FleetEventListener {
    enum class Stage { VOUCHES, NEGOTIATE, ESCORT, RESTART, COMPLETED }

    private var convoy: CampaignFleetAPI? = null
    private var underway = false
    private var raided = false
    private var hyperDays = 0f

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(REF, ACTIVE)) return false
        val data = KolLibra.data()
        basics(when {
            data.crewSupplier != null -> Stage.RESTART
            data.vouches.size >= 2 -> Stage.NEGOTIATE
            else -> Stage.VOUCHES
        }, Stage.COMPLETED)
        martins()?.let { makeImportant(it, "\$kolLibra_crewRestart", Stage.RESTART) }
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        markVouchers()
    }

    /** The vouchers the player can reach now: their comm entries open (or the Order's, while friendly). */
    private fun markVouchers() {
        for (id in KolLibraAgreements.VOUCHERS) {
            if (!KolLibraAgreements.canVouch(id)) continue
            val person = Global.getSector().importantPeople.getPerson(id) ?: continue
            val entry = person.market?.commDirectory?.getEntryForPerson(person) ?: continue
            if (entry.isHidden) continue
            makeImportant(person, "\$kolLibra_voucher", Stage.VOUCHES)
        }
    }

    private fun supplier(): MarketAPI? = KolLibra.data().crewSupplier?.let { Global.getSector().economy.getMarket(it) }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        val data = KolLibra.data()
        when (currentStage) {
            Stage.VOUCHES -> if (data.vouches.size >= 2) setCurrentStage(Stage.NEGOTIATE, null, null)
            Stage.NEGOTIATE -> if (data.crewSupplier != null) beginEscort(null)
            Stage.ESCORT -> escort(amount)
        }
        if (currentStage != Stage.COMPLETED && KolLibra.done(KolLibraMilestone.CREW)) setCurrentStage(Stage.COMPLETED, null, null)
    }

    /** The convoy waits at the supplier until the player comes for it. */
    fun beginEscort(dialog: InteractionDialogAPI?) {
        val supplier = supplier() ?: return
        val entity = supplier.primaryEntity ?: return
        makeImportant(entity, "\$kolLibra_crewPickup", Stage.ESCORT)
        val params = FleetParamsV3(null, KolStaticStrings.kolFactionID, null, FleetTypes.TRADE_LINER,
            0f, 0f, 0f, KolLibraSettings.convoyFleetPoints, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        fleet.name = "[PLACEHOLDER] Libra Crew Convoy"
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MISSION_IMPORTANT, true)
        fleet.memoryWithoutUpdate.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
        entity.containingLocation.addEntity(fleet)
        fleet.setLocation(entity.location.x, entity.location.y)
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, entity, 1_000_000f, "waiting for an escort")
        fleet.addEventListener(this)
        Misc.makeImportant(fleet, "kolLibra_convoy")
        convoy = fleet
        underway = false
        raided = false
        hyperDays = 0f
        setCurrentStage(Stage.ESCORT, dialog, null)
    }

    private fun escort(amount: Float) {
        val fleet = convoy ?: return
        val libra = libraEntity() ?: return
        val player = Global.getSector().playerFleet ?: return
        if (!underway && player.containingLocation == fleet.containingLocation && Misc.getDistance(player.location, fleet.location) < 1000f) {
            underway = true
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, libra, 1_000_000f, "bound for Battlestar Libra")
            update("[PLACEHOLDER] The crew convoy is underway", null)
        }
        if (underway && !raided && raidDue(fleet, libra, Misc.getDays(amount))) {
            raided = true
            spawnRaiders(fleet, libra)
        }
        if (underway && fleet.containingLocation == libra.containingLocation && Misc.getDistance(fleet.location, libra.location) < 400f) {
            Misc.makeUnimportant(fleet, "kolLibra_convoy")
            fleet.despawn(FleetDespawnReason.REACHED_DESTINATION, null)
            convoy = null
            KolLibra.complete(KolLibraMilestone.CREW, null)
        }
    }

    /** About 30% along the route, in hyperspace between the supplier and Libra; two days in, on a route too short to measure. */
    private fun raidDue(fleet: CampaignFleetAPI, libra: SectorEntityToken, days: Float): Boolean {
        if (!fleet.containingLocation.isHyperspace) return false
        hyperDays += days
        val start = supplier()?.primaryEntity?.locationInHyperspace ?: return hyperDays >= 2f
        val end = libra.locationInHyperspace
        val total = Misc.getDistance(start, end)
        if (total < 2000f) return hyperDays >= 2f
        return 1f - Misc.getDistance(fleet.locationInHyperspace, end) / total >= 0.3f
    }

    /** Raiders ahead of the convoy, toward Libra, so they meet it on the road. */
    private fun spawnRaiders(target: CampaignFleetAPI, libra: SectorEntityToken) {
        val params = FleetParamsV3(null, Factions.PIRATES, null, FleetTypes.PATROL_MEDIUM,
            KolLibraSettings.convoyRaidFleetPoints, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        fleet.name = "[PLACEHOLDER] Raiders"
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE + "_" + KolStaticStrings.kolFactionID, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
        target.containingLocation.addEntity(fleet)
        val dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(target.location, libra.locationInHyperspace))
        fleet.setLocation(target.location.x + dir.x * 800f, target.location.y + dir.y * 800f)
        fleet.addAssignment(FleetAssignment.INTERCEPT, target, 30f, "hunting")
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, target, 1000f)
        update("[PLACEHOLDER] Raiders are closing on the convoy", null)
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {
        if (fleet != null && fleet == convoy && reason == FleetDespawnReason.DESTROYED_BY_BATTLE) lost()
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (fleet != null && fleet == convoy && !fleet.isAlive) lost()
    }

    /** The convoy is lost: back to Martins for another, with the vouches and the agreement kept. */
    private fun lost() {
        if (currentStage != Stage.ESCORT) return
        convoy = null
        update("[PLACEHOLDER] The crew convoy was lost", null)
        setCurrentStage(Stage.RESTART, null, null)
    }

    override fun notifyEnding() {
        super.notifyEnding()
        convoy?.let { if (it.isAlive) { Misc.makeUnimportant(it, "kolLibra_convoy"); it.despawn() } }
        convoy = null
    }

    override fun getBaseName(): String = "Libra: A Crew of Its Own"

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        info.addPara("[PLACEHOLDER] Libra has guns enough and too few hands for them. Martins wants a crew of its own, " +
                "from a congregation that will send its people, if two of the faithful will vouch for Libra.", 10f)
        val vouches = KolLibra.data().vouches
        when (currentStage) {
            Stage.VOUCHES -> {
                info.addPara("Vouches: %s of 2.", 10f, h, "" + vouches.size)
                info.addPara("[PLACEHOLDER] Those who might vouch: Archcurate Jaspis, Jethro Bornanew, the Excubitor Orbis, " +
                        "Mother Standfast, the curate sacraria of Killa's ossuary, and, if they think well of you, Brother " +
                        "Enarms or Sister Greenflight.", 3f)
            }
            Stage.NEGOTIATE -> info.addPara("[PLACEHOLDER] With two vouches, the administrator of any world with a " +
                    "Luddic majority can be asked for volunteers.", 10f)
            Stage.ESCORT -> info.addPara("[PLACEHOLDER] The first crew waits at %s. Escort the convoy to Battlestar Libra; " +
                    "someone has to prove the route safe enough.", 10f, h, supplier()?.name ?: "the supplier")
            Stage.RESTART -> info.addPara("[PLACEHOLDER] The convoy was lost. Martins can ask %s for another.", 10f, h,
                supplier()?.name ?: "the supplier")
        }
        describeRaiderTasks(info)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        when (currentStage) {
            Stage.VOUCHES -> info.addPara("Win two vouches for Libra's crew: %s of 2", pad, tc, Misc.getHighlightColor(), "" + KolLibra.data().vouches.size)
            Stage.NEGOTIATE -> {
                if (raiderTasks.isEmpty()) info.addPara("Negotiate with a Luddic-majority world for volunteers", tc, pad)
                else raiderTaskSteps(info, tc, pad)
            }
            Stage.ESCORT -> info.addPara(if (underway) "Escort the crew convoy to Battlestar Libra"
                                         else "Meet the crew convoy at %s", pad, tc, Misc.getHighlightColor(), supplier()?.name ?: "the supplier")
            Stage.RESTART -> info.addPara("Ask Knightmaster Martins to send for another convoy", tc, pad)
            else -> return false
        }
        return true
    }

    companion object {
        const val ID = "kolLibraCrewContract"
        const val REF = "\$kolLibraCrew_ref"
        const val ACTIVE = "\$kolLibraCrew_active"
        fun get(): KolLibraCrewContract? = Global.getSector().memoryWithoutUpdate.get(REF) as? KolLibraCrewContract
        fun active() = Global.getSector().memoryWithoutUpdate.getBoolean(ACTIVE)
        fun restartable() = get()?.currentStage == Stage.RESTART
    }
}

/** The drill: Martins' first beacon. Restaged through Martins if the raiders leave unbeaten. */
class KolLibraDrill : KolLibraMission() {
    enum class Stage { WAVE, RESTAGE, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(REF, ACTIVE)) return false
        basics(Stage.WAVE, Stage.COMPLETED)
        libraEntity()?.let { makeImportant(it, "\$kolLibra_drill", Stage.WAVE) }
        martins()?.let { makeImportant(it, "\$kolLibra_drillRestage", Stage.RESTAGE) }
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        KolLibraGunnery.light(true)
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (currentStage != Stage.COMPLETED && KolLibra.done(KolLibraMilestone.DRILL)) setCurrentStage(Stage.COMPLETED, null, null)
    }

    /** The raiders left unbeaten: back to Martins. */
    fun unbeaten() {
        update("[PLACEHOLDER] The raiders left unbeaten", null)
        setCurrentStage(Stage.RESTAGE, null, null)
    }

    fun restage(dialog: InteractionDialogAPI?) {
        KolLibraGunnery.light(true)
        setCurrentStage(Stage.WAVE, dialog, null)
    }

    override fun getBaseName(): String = "Libra: The Drill"

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        info.addPara("[PLACEHOLDER] Libra's new crews can man its guns. Martins wants to see whether they can fire them: a " +
                "false distress beacon, and whoever comes for easy pickings. Fight beside Libra.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        if (currentStage == Stage.RESTAGE) info.addPara("Ask Knightmaster Martins to stage the drill again", tc, pad)
        else info.addPara("Beat the raiders the beacon draws to Battlestar Libra", tc, pad)
        return true
    }

    companion object {
        const val ID = "kolLibraDrill"
        const val REF = "\$kolLibraDrill_ref"
        const val ACTIVE = "\$kolLibraDrill_active"
        fun get(): KolLibraDrill? = Global.getSector().memoryWithoutUpdate.get(REF) as? KolLibraDrill
        fun active() = Global.getSector().memoryWithoutUpdate.getBoolean(ACTIVE)
        fun restageable() = get()?.currentStage == Stage.RESTAGE && !KolLibraGunnery.running()
    }
}

/** The restoration's last step: Libra on the agenda of an important assembly at Lyra, until it's legitimized. */
class KolLibraRestoration : KolLibraMission() {
    enum class Stage { ASSEMBLY, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(REF, ACTIVE)) return false
        basics(Stage.ASSEMBLY, Stage.COMPLETED)
        Global.getSector().economy.getMarket(KolStaticStrings.KOL_LYRA)?.primaryEntity?.let { makeImportant(it, "\$kolLibra_assembly", Stage.ASSEMBLY) }
        return true
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (currentStage != Stage.COMPLETED && KolLibra.data().legitimized) setCurrentStage(Stage.COMPLETED, null, null)
    }

    override fun getBaseName(): String = "Libra: Restored"

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        info.addPara("[PLACEHOLDER] Battlestar Libra is restored. Martins means to have it heard at Lyra, in the hall, " +
                "where the Order wrote it off. Libra is on the agenda of the next important assembly.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        info.addPara("Attend the assembly at Star Keep Lyra that takes up Libra", tc, pad)
        return true
    }

    companion object {
        const val ID = "kolLibraRestoration"
        const val REF = "\$kolLibraRestoration_ref"
        const val ACTIVE = "\$kolLibraRestoration_active"
    }
}
