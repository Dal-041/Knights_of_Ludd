package org.selkie.kol.campaign.libra

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.situations.KolLibraSituationIntel
import org.selkie.kol.helpers.KolStaticStrings
import kotlin.random.Random

/**
 * Gunnery training and the drill event (design, decision 5). A false distress beacon lit at Libra draws, after a short
 * delay, a pirate wave through the jump point closest to Libra; or, one time in six (never for the drill), a genuine
 * independent responder. The wave's strength combines the player's fleet and Libra's stage (±variance); Libra's stage
 * adds less to the wave than to Libra, so Libra keeps an edge. Every enemy FP destroyed counts as progress (× the
 * multiplier); modules Libra's station loses count against it (detached module statuses, compared after each battle).
 * Defeated wave fleets withdraw. The drill completes when its whole wave is beaten.
 */
class KolLibraGunnery(private val drill: Boolean) : EveryFrameScript, FleetEventListener {
    private val wave = ArrayList<CampaignFleetAPI>()
    private val fpLeft = HashMap<CampaignFleetAPI, Float>()
    private val beaten = HashSet<CampaignFleetAPI>()
    private var arrived = false
    private var delayDays = 1f + Random.nextFloat()
    private var stationDetached = 0
    private var done = false
    private val interval = IntervalUtil(0.1f, 0.2f)

    override fun isDone() = done
    override fun runWhilePaused() = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        if (!arrived) {
            delayDays -= days
            if (delayDays <= 0f) arrive()
            return
        }
        interval.advance(days)
        if (!interval.intervalElapsed()) return
        if (wave.all { gone(it) }) finish()
    }

    private fun gone(fleet: CampaignFleetAPI) = !fleet.isAlive || fleet.containingLocation == null || fleet in beaten

    private fun libra(): SectorEntityToken? = KolLibra.market()?.primaryEntity

    private fun entry(libra: SectorEntityToken): SectorEntityToken =
        libra.starSystem?.jumpPoints?.minByOrNull { Misc.getDistance(it.location, libra.location) } ?: libra

    private fun arrive() {
        arrived = true
        val libra = libra() ?: run { done = true; return }
        if (!drill && Random.nextFloat() < KolLibraSettings.responderChance) {
            spawnResponder(libra)
            done = true
            return
        }
        val stage = KolLibraSituationIntel.get()?.stageIndex() ?: 0
        val player = Global.getSector().playerFleet?.fleetPoints?.toFloat() ?: 0f
        val variance = KolLibraSettings.waveVariance
        var total = (player * KolLibraSettings.wavePlayerFactor + stage * KolLibraSettings.waveStageFP) *
                (1f + (Random.nextFloat() * 2f - 1f) * variance)
        if (drill) total *= KolLibraSettings.drillWaveMult
        val count = KolLibraSettings.waveFleets.coerceAtLeast(1)
        val system = libra.starSystem ?: run { done = true; return }
        val entry = entry(libra)
        repeat(count) {
            val params = FleetParamsV3(null, Factions.PIRATES, null, FleetTypes.PATROL_MEDIUM,
                (total / count).coerceAtLeast(10f), 0f, 0f, 0f, 0f, 0f, 0f)
            val fleet = FleetFactoryV3.createFleet(params) ?: return@repeat
            fleet.name = "[PLACEHOLDER] Raiders"
            fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE + "_" + KolStaticStrings.kolFactionID, true)
            fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
            system.addEntity(fleet)
            val at = Misc.getPointAtRadius(entry.location, 150f)
            fleet.setLocation(at.x, at.y)
            fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, libra, 20f, "answering a distress call")
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, entry, 1000f, "leaving")
            fleet.addEventListener(this)
            wave.add(fleet)
            fpLeft[fleet] = fleet.fleetPoints.toFloat()
        }
        stationDetached = detachedModules()
        if (drill) KolLibraDrill.get()?.update("[PLACEHOLDER] Raiders answer the drill's beacon", null)
        else Global.getSector().campaignUI.addMessage("[PLACEHOLDER] Raiders answer Libra's false distress call.", Misc.getHighlightColor())
    }

    /** Modules Libra's station has lost (detached statuses beyond the core hull). */
    private fun detachedModules(): Int {
        val market = KolLibra.market() ?: return 0
        val station = market.industries.firstOrNull { it is OrbitalStation } as? OrbitalStation ?: return 0
        val member = station.stationFleet?.fleetData?.membersListCopy?.firstOrNull() ?: return 0
        val status = member.status
        return (1 until status.numStatuses).count { status.isDetached(it) }
    }

    /** After a battle: the wave's FP destroyed, less Libra's newly lost modules. */
    private fun account(dialog: InteractionDialogAPI?) {
        var destroyed = 0f
        for (fleet in wave) {
            val before = fpLeft[fleet] ?: continue
            val now = if (fleet.isAlive) fleet.fleetPoints.toFloat() else 0f
            if (now < before) { destroyed += before - now; fpLeft[fleet] = now }
        }
        val detached = detachedModules()
        val lost = (detached - stationDetached).coerceAtLeast(0)
        stationDetached = detached
        val progress = Math.round(destroyed * KolLibraSettings.gunneryMult) - lost * KolLibraSettings.moduleLossProgress
        KolLibra.contribute(progress, dialog, if (drill) "The drill" else "Gunnery training")
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (fleet == null || battle == null || fleet !in wave) return
        account(null)
        if (battle.wasFleetDefeated(fleet, primaryWinner)) {
            beaten.add(fleet)
            if (fleet.isAlive) {
                fleet.clearAssignments()
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, libra()?.let { entry(it) } ?: fleet, 1000f, "withdrawing")
            }
        }
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {
        if (fleet == null || fleet !in wave) return
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE) { account(null); beaten.add(fleet) }
    }

    private fun finish() {
        done = true
        if (drill) {
            if (wave.isNotEmpty() && wave.all { it in beaten || !it.isAlive }) KolLibra.complete(KolLibraMilestone.DRILL, null)
            else KolLibraDrill.get()?.unbeaten()
        }
        state().active = null
    }

    /** One time in six: someone answers the beacon in earnest. */
    private fun spawnResponder(libra: SectorEntityToken) {
        val system = libra.starSystem ?: return
        val params = FleetParamsV3(null, Factions.INDEPENDENT, null, FleetTypes.PATROL_SMALL, 30f, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        fleet.name = "[PLACEHOLDER] Would-be Rescuers"
        val entry = entry(libra)
        system.addEntity(fleet)
        fleet.setLocation(entry.location.x, entry.location.y)
        fleet.memoryWithoutUpdate.set(RESPONDER_FLAG, true)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true)
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, libra, 30f, "answering a distress call")
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, libra, 10f, "looking for survivors")
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, entry, 1000f, "leaving")
        Misc.makeImportant(fleet, "kolLibra_responder")
        Global.getSector().campaignUI.addMessage("[PLACEHOLDER] A ship answers Libra's false distress call, in earnest.", Misc.getHighlightColor())
        state().active = null
    }

    companion object {
        const val RESPONDER_FLAG = "\$kolLibra_responder"
        private const val DATA_KEY = "kol_libraGunnery"

        fun state(): KolLibraGunneryState {
            val data = Global.getSector().persistentData
            return data[DATA_KEY] as? KolLibraGunneryState ?: KolLibraGunneryState().also { data[DATA_KEY] = it }
        }

        fun running(): Boolean = state().active != null

        /** Gunnery training can be lit: after the drill, off cooldown, nothing running. */
        fun beaconReady(): Boolean = KolLibra.active() && KolLibra.done(KolLibraMilestone.DRILL) && !running() &&
                Global.getSector().clock.getElapsedDaysSince(KolLibra.data().lastBeacon) >= KolLibraSettings.beaconCooldownDays

        /** The drill can be staged: its milestone open, nothing running. */
        fun drillReady(): Boolean = KolLibra.open(KolLibraMilestone.DRILL) && !running()

        /** Lights a beacon (gunnery training) or stages the drill. */
        fun light(drill: Boolean) {
            if (running()) return
            if (!drill) KolLibra.data().lastBeacon = Global.getSector().clock.timestamp
            val script = KolLibraGunnery(drill)
            state().active = script
            Global.getSector().addScript(script)
        }

        /** The player rewards a responder: credits, supplies or fuel; a reputation gain with the independents. */
        fun rewardResponder(kind: String, dialog: InteractionDialogAPI): Boolean {
            val cargo = Global.getSector().playerFleet.cargo
            val text = dialog.textPanel
            when (kind) {
                "credits" -> { if (cargo.credits.get() < RESPONDER_CREDITS) return false
                    cargo.credits.subtract(RESPONDER_CREDITS.toFloat()); AddRemoveCommodity.addCreditsLossText(RESPONDER_CREDITS, text) }
                "supplies" -> { if (cargo.supplies < RESPONDER_SUPPLIES) return false
                    cargo.removeSupplies(RESPONDER_SUPPLIES.toFloat()); AddRemoveCommodity.addCommodityLossText(Commodities.SUPPLIES, RESPONDER_SUPPLIES, text) }
                "fuel" -> { if (cargo.fuel < RESPONDER_FUEL) return false
                    cargo.removeFuel(RESPONDER_FUEL.toFloat()); AddRemoveCommodity.addCommodityLossText(Commodities.FUEL, RESPONDER_FUEL, text) }
                else -> return false
            }
            val impact = CoreReputationPlugin.CustomRepImpact().apply { delta = KolLibraSettings.responderRep }
            Global.getSector().adjustPlayerReputation(CoreReputationPlugin.RepActionEnvelope(
                CoreReputationPlugin.RepActions.CUSTOM, impact, null, text, true), Factions.INDEPENDENT)
            return true
        }

        // [PLACEHOLDER] amounts
        const val RESPONDER_CREDITS = 10000
        const val RESPONDER_SUPPLIES = 20
        const val RESPONDER_FUEL = 40
    }
}

/** Gunnery's saved state: the beacon or drill in progress (persistent data). */
class KolLibraGunneryState {
    var active: KolLibraGunnery? = null
}
