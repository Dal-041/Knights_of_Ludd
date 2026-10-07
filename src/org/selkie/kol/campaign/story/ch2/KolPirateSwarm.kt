package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolSeverance
import org.selkie.kol.campaign.story.KolStorySettings
import org.selkie.kol.helpers.KolStaticStrings
import kotlin.random.Random

/**
 * The pirates' "protection": the failure state of the patron quest (design, decision 8). At the assembly that would
 * announce the deal, the pirates' fleets arrive at Lyra's jump points (the At the Gates finale's spawn profile:
 * armadas, mercenary-style armadas and fast scouts), approach Lyra and raid the system as vanilla's system raiders do.
 * When one comes close, the Knights launch twice the pirates' strength from Lyra. Severance has already run when the
 * swarm begins; the outcome is decided here:
 * - the player stays out of it, or helps the Knights: when the pirates are gone, the Knights' relationship drops to
 *   `severanceRepLevels.pirates` in Greenflight's rebuff at Lyra's next docking;
 * - the player turns hostile to the Knights while it runs: when either side is gone, it's the pirates' victory, and
 *   the Station King at Kanta's Den pays the turncoat's cut ([KolPirateCut]).
 * Defeated fleets leave and despawn, so "gone" is every fleet on a side dead, routed or despawned. The player's
 * relationship with the pirates is never touched here.
 */
class KolPirateSwarm : EveryFrameScript, FleetEventListener {
    private val pirates = ArrayList<CampaignFleetAPI>()
    private val knights = ArrayList<CampaignFleetAPI>()
    private val routed = HashSet<CampaignFleetAPI>()
    private var launched = false
    var turned = false
        private set
    var decided = false
        private set
    private var done = false
    private val interval = IntervalUtil(0.05f, 0.1f)

    override fun isDone() = done
    override fun runWhilePaused() = false

    private fun lyra(): SectorEntityToken? = Global.getSector().economy.getMarket(KolStaticStrings.KOL_LYRA)?.primaryEntity

    /** The pirates arrive (KolPatronCMD pirateSwarm, from the gate's scene). */
    fun begin() {
        val lyra = lyra() ?: run { done = true; return }
        val system = lyra.starSystem ?: run { done = true; return }
        val points = system.jumpPoints.ifEmpty { listOf(lyra) }
        val total = KolStorySettings.patronSwarmFleetPoints
        // the At the Gates profile: armadas, mercenary-style armadas, two scouts for each
        val profile = listOf(
            FleetTypes.PATROL_LARGE to 0.225f, FleetTypes.PATROL_LARGE to 0.225f,
            FleetTypes.MERC_ARMADA to 0.175f, FleetTypes.MERC_ARMADA to 0.175f,
            FleetTypes.PATROL_SMALL to 0.05f, FleetTypes.PATROL_SMALL to 0.05f,
            FleetTypes.PATROL_SMALL to 0.05f, FleetTypes.PATROL_SMALL to 0.05f)
        profile.forEachIndexed { i, (type, share) ->
            val point = points[i % points.size]
            spawnPirate(system, point, lyra, type, total * share)
        }
        Global.getSector().addScript(this)
    }

    private fun spawnPirate(system: com.fs.starfarer.api.campaign.StarSystemAPI, point: SectorEntityToken, lyra: SectorEntityToken,
                            type: String, points: Float) {
        val params = FleetParamsV3(null, Factions.PIRATES, null, type, points, 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        fleet.name = "[PLACEHOLDER] Protection Fleet"
        val memory = fleet.memoryWithoutUpdate
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE + "_" + KolStaticStrings.kolFactionID, true)
        memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
        system.addEntity(fleet)
        fleet.setLocation(point.location.x, point.location.y)
        // as vanilla's system raiders: come in, raid, leave
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, lyra, 1000f, "approaching ${lyra.name}")
        fleet.addAssignment(FleetAssignment.RAID_SYSTEM, lyra, 20f, "raiding the ${system.baseName} star system")
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, point, 1000f, "leaving")
        fleet.addEventListener(this)
        pirates.add(fleet)
    }

    /** The Knights launch from Lyra: twice the pirates' strength, across as many fleets. */
    private fun launch(lyra: SectorEntityToken) {
        launched = true
        val system = lyra.starSystem ?: return
        val total = KolStorySettings.patronSwarmFleetPoints * KolStorySettings.patronSwarmKnightsMult
        val count = 8
        repeat(count) {
            val params = FleetParamsV3(null, KolStaticStrings.kolFactionID, null, FleetTypes.PATROL_LARGE,
                total / count, 0f, 0f, 0f, 0f, 0f, 0f)
            val fleet = FleetFactoryV3.createFleet(params) ?: return@repeat
            fleet.name = "[PLACEHOLDER] Lyra Defense Squadron"
            fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true)
            system.addEntity(fleet)
            fleet.setLocation(lyra.location.x, lyra.location.y)
            fleet.addAssignment(FleetAssignment.DEFEND_LOCATION, lyra, 1000f, "defending ${lyra.name}")
            fleet.addEventListener(this)
            knights.add(fleet)
        }
    }

    private fun gone(fleet: CampaignFleetAPI) = !fleet.isAlive || fleet.containingLocation == null || fleet in routed

    override fun advance(amount: Float) {
        interval.advance(Misc.getDays(amount))
        if (!interval.intervalElapsed()) return
        val lyra = lyra()
        if (!turned && Global.getSector().getFaction(KolStaticStrings.kolFactionID).isHostileTo(Factions.PLAYER)) turned = true
        if (!launched && lyra != null &&
            pirates.any { !gone(it) && it.containingLocation == lyra.containingLocation &&
                    Misc.getDistance(it.location, lyra.location) < KolStorySettings.patronSwarmLaunchRange }) launch(lyra)

        if (!decided) {
            val piratesGone = pirates.all { gone(it) }
            val knightsGone = launched && knights.all { gone(it) }
            if (turned && (piratesGone || knightsGone)) turncoat()
            else if (!turned && piratesGone) knightsWin()
        }
        if (decided) {
            // the Knights go home; the pirates finish their raid on their own
            for (fleet in knights) if (!gone(fleet)) {
                fleet.clearAssignments()
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, lyra ?: fleet, 1000f, "returning to Lyra")
            }
            done = true
        }
    }

    /** The Knights beat the pirates back: Lyra's next docking brings Greenflight's rebuff, and the relationship drops. */
    private fun knightsWin() {
        decided = true
        state().rebuffPending = true
    }

    /** The player turned on the Order: the pirates' victory, and the cut waiting at Kanta's Den. */
    private fun turncoat() {
        decided = true
        KolPirateCut.start()
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {}

    /** A defeated fleet leaves (as At the Gates' fleets do after defeat), so it counts as routed. */
    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (fleet == null || battle == null || !battle.wasFleetDefeated(fleet, primaryWinner)) return
        if (fleet !in pirates && fleet !in knights) return
        routed.add(fleet)
        if (!fleet.isAlive) return
        fleet.clearAssignments()
        val exit = fleet.starSystem?.jumpPoints?.minByOrNull { Misc.getDistance(it.location, fleet.location) } ?: lyra() ?: return
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, exit, 1000f, "withdrawing")
    }

    companion object {
        private const val DATA_KEY = "kol_pirateSwarm"

        fun state(): KolPirateSwarmState {
            val data = Global.getSector().persistentData
            return data[DATA_KEY] as? KolPirateSwarmState ?: KolPirateSwarmState().also { data[DATA_KEY] = it }
        }

        /** The pirates arrive at Lyra: the swarm begins, and the Order severs ties. */
        fun begin() {
            val state = state()
            if (state.swarm != null) return
            state.swarm = KolPirateSwarm().also { it.begin() }
            KolSeverance.sever("pirates")
        }

        /** Lyra refuses docking while the swarm is undecided. */
        fun active(): Boolean = state().swarm?.let { !it.decided } ?: false

        fun rebuffPending(): Boolean = state().rebuffPending

        /** Greenflight's rebuff: the Knights' (and so the Church's) relationship drops to `severanceRepLevels.pirates`, at best. */
        fun rebuffed(text: com.fs.starfarer.api.campaign.TextPanelAPI?) {
            state().rebuffPending = false
            val level = KolStorySettings.severanceRepLevel("pirates")?.let { runCatching { RepLevel.valueOf(it) }.getOrNull() }
                ?: RepLevel.INHOSPITABLE
            KolPatron.knightsRepAction(com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact().apply { ensureAtBest = level }, text)
        }
    }
}

/** The swarm's lasting state, in persistent data (not story state: severance leaves it). */
class KolPirateSwarmState {
    var swarm: KolPirateSwarm? = null
    var rebuffPending = false
}

/**
 * The turncoat's cut: a brief mission to collect it from the Station King at Kanta's Den, who ships it over to the
 * player's fleet. The loot is a generated spread of Knights equipment and commodities (`patronPirateCut` in value).
 */
class KolPirateCut : HubMissionWithSearch() {
    enum class Stage { COLLECT, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(REF, ACTIVE)) return false
        val den = kantasDen() ?: return false
        setStartingStage(Stage.COLLECT)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        den.primaryEntity?.let { makeImportant(it, "\$kolPirateCut_collect", Stage.COLLECT) }
        return true
    }

    override fun getBaseName(): String = "[PLACEHOLDER] The Pirates' Cut"

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        info.addPara("[PLACEHOLDER] Kanta's people promised you a cut of the Knights' loot. The Station King at " +
                "Kanta's Den will ship it over when you contact him.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: java.awt.Color, pad: Float): Boolean {
        info.addPara("[PLACEHOLDER] Contact the Station King at Kanta's Den", tc, pad)
        return true
    }

    companion object {
        const val ID = "kolPirateCut"
        const val REF = "\$kolPirateCut_ref"
        const val ACTIVE = "\$kolPirateCut_active"
        const val STAGE_DONE = "\$kolPirateCut_stageDone"

        fun kantasDen(): MarketAPI? = Global.getSector().economy.getMarket("kantas_den")?.takeIf { it.factionId == Factions.PIRATES }

        /** Starts the mission, given by Kanta's Den's station commander (its administrator: the Station King). */
        fun start() {
            val den = kantasDen() ?: return
            val king = den.admin ?: return
            val mission = Global.getSettings().getMissionSpec(ID)?.createMission() ?: return
            mission.setPersonOverride(king)
            mission.createAndAbortIfFailed(den, false)
            if (mission.isMissionCreationAborted) return
            mission.accept(null, null)
        }

        fun active(): Boolean = Global.getSector().memoryWithoutUpdate.getBoolean(ACTIVE)

        /** The Station King ships the cut over: Knights weapons and commodities, about `patronPirateCut` in value. */
        fun collect(dialog: InteractionDialogAPI, map: MutableMap<String, MemoryAPI>?): Boolean {
            if (!active()) return false
            val cargo = Global.getSector().playerFleet.cargo
            val budget = KolStorySettings.patronPirateCut.toFloat()
            val text = dialog.textPanel
            // half in the Order's weapons
            val weapons = Global.getSector().getFaction(KolStaticStrings.kolFactionID).knownWeapons
                .mapNotNull { runCatching { Global.getSettings().getWeaponSpec(it) }.getOrNull() }
                .filter { it.baseValue > 0f }
            var spent = 0f
            if (weapons.isNotEmpty()) {
                var guard = 0
                while (spent < budget / 2f && guard++ < 200) {
                    val spec = weapons[Random.nextInt(weapons.size)]
                    cargo.addWeapons(spec.weaponId, 1)
                    AddRemoveCommodity.addWeaponGainText(spec.weaponId, 1, text)
                    spent += spec.baseValue
                }
            }
            // the rest in the commodities the Order stocks
            val goods = listOf(Commodities.SUPPLIES, Commodities.HEAVY_MACHINERY, Commodities.HAND_WEAPONS, Commodities.FUEL)
            val share = (budget - spent).coerceAtLeast(0f) / goods.size
            for (id in goods) {
                val price = Global.getSettings().getCommoditySpec(id).basePrice.coerceAtLeast(1f)
                val qty = (share / price).toInt()
                if (qty <= 0) continue
                cargo.addCommodity(id, qty.toFloat())
                AddRemoveCommodity.addCommodityGainText(id, qty, text)
            }
            Global.getSector().memoryWithoutUpdate.set("\$kolPatron_pirateCut", true)
            Global.getSector().memoryWithoutUpdate.set(STAGE_DONE, true)
            (Global.getSector().memoryWithoutUpdate.get(REF) as? KolPirateCut)?.checkStageChangesAndTriggers(dialog, map)
            return true
        }
    }
}
