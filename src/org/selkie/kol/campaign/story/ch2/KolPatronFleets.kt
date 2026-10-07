package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolStorySettings
import org.selkie.kol.helpers.KolStaticStrings

/**
 * The charter's story hook. Scuttling (later story only, never otherwise) records the reason in
 * `$kol_patronCharterScuttled` and removes the fleets' management (no replacements, no minder); the fleets themselves
 * are left where they are for the story to deal with.
 */
object KolPatronCharter {
    fun scuttle(reason: String) {
        Global.getSector().memoryWithoutUpdate.set(KolStaticStrings.KolPatron.CHARTER_SCUTTLED, reason)
        KolPatronFleets.data().unmanaged = true
    }

    fun scuttled(): Boolean = Global.getSector().memoryWithoutUpdate.contains(KolStaticStrings.KolPatron.CHARTER_SCUTTLED)
}

/** Saved state of the patron's stationed fleets (in persistent data). */
class KolPatronFleetData {
    var patron: String? = null          // faction id, `charter` or `player`
    var dismissed = false
    var unmanaged = false               // the charter was scuttled: no replacements, no minder
    val lastSpawn = HashMap<String, Long>() // per system id
}

/**
 * The patron's persistent fleets: stationed in the systems containing Lyra and Cygnus once the patron is announced,
 * replenished slowly, until dismissed later in the story ([dismiss]). The charter's `mercenary` fleets copy the Order's
 * hostilities and neutralities (the minder); every patron fleet is kept non-hostile to the Order, the Church and the
 * player. The player's own forces stand as the defenders, at a fleet-size cost to the player's colonies.
 */
object KolPatronFleets {
    const val FLEET_FLAG = "\$kolPatron_guard"
    private const val DATA_KEY = "kol_patronFleets"
    private const val OWN_PENALTY_ID = "kol_patronOwnForces"

    fun data(): KolPatronFleetData {
        val data = Global.getSector().persistentData
        return data[DATA_KEY] as? KolPatronFleetData ?: KolPatronFleetData().also { data[DATA_KEY] = it }
    }

    fun active(): Boolean = data().let { it.patron != null && !it.dismissed }

    /** The patron is announced: its fleets arrive at Lyra and Cygnus. */
    fun station(patron: String) {
        val data = data()
        data.patron = patron
        data.dismissed = false
        for (system in systems()) {
            repeat(KolStorySettings.patronFleetCount) { spawn(system) }
            data.lastSpawn[system.id] = Global.getSector().clock.timestamp
        }
        if (patron == Factions.PLAYER) applyOwnPenalty()
    }

    /** Later in the story: the patron's fleets go home, and stop being replaced. */
    fun dismiss() {
        val data = data()
        data.dismissed = true
        for (fleet in fleets()) {
            fleet.clearAssignments()
            val exit = fleet.starSystem?.jumpPoints?.firstOrNull() ?: fleet
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, exit, 1000f, "returning home")
        }
        removeOwnPenalty()
    }

    /** The systems containing Lyra and Cygnus. */
    fun systems(): List<StarSystemAPI> = listOf(KolStaticStrings.KOL_LYRA, KolStaticStrings.KOL_CYGNUS)
        .mapNotNull { Global.getSector().economy.getMarket(it)?.starSystem }.distinct()

    /** The station a system's fleets guard (Lyra's or Cygnus'). */
    private fun anchor(system: StarSystemAPI): SectorEntityToken? = listOf(KolStaticStrings.KOL_LYRA, KolStaticStrings.KOL_CYGNUS)
        .mapNotNull { Global.getSector().economy.getMarket(it)?.primaryEntity }.firstOrNull { it.starSystem == system }

    fun fleets(system: StarSystemAPI? = null): List<CampaignFleetAPI> = (if (system != null) listOf(system) else systems())
        .flatMap { it.fleets }.filter { it.isAlive && it.memoryWithoutUpdate.getBoolean(FLEET_FLAG) }

    private fun factionFor(patron: String): String = when (patron) {
        KolPatron.CHARTER -> Factions.MERCENARY
        else -> patron
    }

    private fun tactistar(): Boolean = Global.getSector().memoryWithoutUpdate.getString(KolStaticStrings.KolPatron.CHARTER_SOURCE) == KolPatron.TRITACH

    /** [PLACEHOLDER] fleet names. */
    private fun nameFor(patron: String): String = when (patron) {
        KolPatron.CHARTER -> if (tactistar()) "Tactistar Guard Detachment" else KolStorySettings.patronCharterCompanyName + " Detachment"
        Factions.PLAYER -> "Protectorate of the Faithful"
        else -> Global.getSector().getFaction(patron).displayName + " Protectorate Squadron"
    }

    /** One fleet arrives at a jump point of [system] and takes station by its Knights holding. */
    private fun spawn(system: StarSystemAPI) {
        val patron = data().patron ?: return
        val anchor = anchor(system) ?: return
        val params = FleetParamsV3(null, factionFor(patron), null, FleetTypes.PATROL_LARGE,
            KolStorySettings.patronFleetPoints, 0f, 0f, 0f, 0f, 0f, 0f)
        if (patron == KolPatron.CHARTER && tactistar()) { // Tactistar's better offerings, as vanilla's Tactistar fleets
            params.qualityOverride = 1f
            params.averageSMods = 2
        }
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        fleet.name = nameFor(patron)
        val memory = fleet.memoryWithoutUpdate
        memory.set(FLEET_FLAG, true)
        memory.set(MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE, true)
        for (friend in listOf(KolStaticStrings.kolFactionID, Factions.LUDDIC_CHURCH, Factions.PLAYER))
            memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE + "_" + friend, true)
        system.addEntity(fleet)
        val arrival = system.jumpPoints.randomOrNull() ?: anchor
        fleet.setLocation(arrival.location.x, arrival.location.y)
        fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, anchor, 1_000_000f, "guarding the faithful")
        if (patron == KolPatron.CHARTER) mind(fleet)
    }

    /** The charter's minder: the `mercenary` fleets take the Order's hostilities and neutralities. */
    private fun mind(fleet: CampaignFleetAPI) {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID)
        val memory = fleet.memoryWithoutUpdate
        for (faction in Global.getSector().allFactions) {
            val id = faction.id
            if (id == KolStaticStrings.kolFactionID || id == Factions.MERCENARY) continue
            val hostile = knights.isHostileTo(id)
            if (hostile) {
                memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE + "_" + id, true)
                memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE + "_" + id)
            } else {
                memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE + "_" + id, true)
                memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE + "_" + id)
            }
        }
    }

    /** The player's own forces stand as patron: a flat fleet-size penalty on the player's colonies. */
    private fun applyOwnPenalty() {
        val mult = 1f - KolStorySettings.patronOwnFleetPenalty
        for (market in Misc.getPlayerMarkets(true))
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(OWN_PENALTY_ID, mult, "The Church's patron (your own forces)")
    }

    private fun removeOwnPenalty() {
        for (market in Misc.getPlayerMarkets(true)) market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(OWN_PENALTY_ID)
    }

    /** Runs with the story scripts: replaces lost fleets slowly, keeps the minder and the own-forces penalty current. */
    fun tick() {
        if (!active() || data().unmanaged) return
        val data = data()
        val patron = data.patron ?: return
        for (system in systems()) {
            val fleets = fleets(system)
            if (patron == KolPatron.CHARTER) fleets.forEach { mind(it) }
            if (fleets.size >= KolStorySettings.patronFleetCount) continue
            val last = data.lastSpawn[system.id] ?: 0L
            if (Global.getSector().clock.getElapsedDaysSince(last) < KolStorySettings.patronFleetReplenishDays) continue
            spawn(system)
            data.lastSpawn[system.id] = Global.getSector().clock.timestamp
        }
        if (patron == Factions.PLAYER) applyOwnPenalty()
    }
}

/** Ticks the patron's fleets every few days (transient; registered at each load). */
class KolPatronFleetScript : EveryFrameScript {
    private val interval = IntervalUtil(2f, 3f)
    private val aboard = IntervalUtil(0.1f, 0.2f)
    override fun isDone() = false
    override fun runWhilePaused() = false
    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            KolPatronParley.tick()
            KolPatronFleets.tick()
        }
        aboard.advance(days)
        if (aboard.intervalElapsed()) KolPatronParley.aboardScene()?.let { KolPatronParley.playAboard(it) }
    }
}
