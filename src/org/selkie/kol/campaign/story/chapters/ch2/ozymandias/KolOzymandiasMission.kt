package org.selkie.kol.campaign.story.chapters.ch2.ozymandias

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.helpers.ZeaStaticStrings
import java.awt.Color
import org.selkie.kol.campaign.story.core.KolPeople
import org.selkie.kol.campaign.story.core.KolFleets
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.systems.chronicle.KolChronicle
import org.selkie.kol.campaign.story.systems.cue.KolStoryCue
import org.selkie.kol.campaign.story.systems.severance.KolSeverance
import org.selkie.kol.campaign.story.chapters.ch2.patron.PatronKeys
import org.selkie.kol.campaign.story.situations.tech.KolTechSituationIntel
import org.selkie.kol.campaign.story.situations.tech.milestones.ordained.KolOrdainedEarly
import org.selkie.zea.campaign.ZeaCaeli
import java.util.Random

private val memory get() = Global.getSector().memoryWithoutUpdate

/**
 * The Order secures and memorializes Ozymandias (from Enarms after the convocation). The Knights come only if a patron
 * is secured when the mission is accepted; without one the player goes alone, and the rendezvous is skipped.
 * - Rendezvous: four Knights fleets from Cygnus wait at Ozymandias's jump point in hyperspace. Told the player is
 *   ready, they jump in and hold at the inner jump point. With none left to talk to, the stage passes silently; if
 *   Caeli's guardian is beaten while they wait, they go home.
 * - Secure: visit the four worlds and the two stations, and beat Caeli's guardian (ZeaCaeli). In Ozymandias the
 *   Knights follow the player (an aggressive orbit, so they engage Dawn fleets); `$cfai_noJump` keeps them there.
 * - Memorial: one memorial beacon at Caeli, with or without the Knights present; then they return to Cygnus. A
 *   cache surfaces beside it with the memorial: the relic.
 * - Recover and deliver: the relic is brought aboard (a status line, not cargo) and taken to Helensis at Cygnus,
 *   where Enarms objects; it counts as a Technology handover. The mission ends there.
 * If the guardian is already beaten when the mission starts, securing is the visits alone.
 * Stops are flagged on their entities and handled by high-score OpenInteractionDialog rules (kolOzy_*).
 */
class KolCh2Ozymandias : HubMissionWithSearch() {
    companion object {
        const val STAGE_DONE = "\$kolOzy_stageDone"
        private const val MET = "\$kolOzy_metKnights"
        private const val SECURED = "\$kolOzy_secured"
        /** On the Knights fleets while they wait at the rendezvous (the comm topic's gate). */
        const val WAITING = "\$kolOzy_waiting"
        private const val FOLLOWING = "\$kolOzy_following"
        /** On the memorial beacon. */
        const val MEMORIAL = "\$kolOzy_memorial"
        /** Global, kept current: a Knights fleet of the mission is alive in Ozymandias (memorial text variants). */
        const val KNIGHTS_HERE = "\$kolOzy_knightsHere"
        private const val MEMORIALIZED = "\$kolOzy_memorialized"
        private const val RECOVERED = "\$kolOzy_relicAboard"
        /** On the cache that holds the relic, and on Helensis while it's to be delivered. */
        const val CACHE = "\$kolOzy_cache"
        const val DELIVER = "\$kolOzy_deliver"
        /** Global, permanent: the relic was delivered to Helensis. */
        const val RELIC_DELIVERED = "\$kolOzy_relicDelivered"
        /** [PLACEHOLDER] What the Order calls the object; narrative only (a status line, never cargo). */
        const val RELIC_NAME = "the Caeli relic"

        // four fleets the size of the system's regular Dawn spawns (ZeaFleetSoloManager in PrepareShadows: 40-80 FP)
        private const val FLEETS = 4
        private const val MIN_FP = 80f
        private const val MAX_FP = 120f
    }

    enum class Stage { RENDEZVOUS, SECURE, MEMORIAL, RECOVER, DELIVER, COMPLETED }

    private var system: StarSystemAPI? = null
    private val stops = ArrayList<SectorEntityToken>()
    private var caeli: SectorEntityToken? = null
    private var visited = 0
    private val knights = ArrayList<CampaignFleetAPI>()
    /** Ozymandias's jump point in hyperspace (the rendezvous), and where it leads in the system. */
    private var outer: SectorEntityToken? = null
    private var inner: SectorEntityToken? = null
    private var beatenAtStart = false
    /** A patron was secured at acceptance: the Knights come. Fixed for the mission. */
    private var withKnights = false
    private var cache: SectorEntityToken? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(OzymandiasKeys.OZY_REF, OzymandiasKeys.OZY_ACTIVE)) return false
        val sys = Global.getSector().getStarSystem(ZeaStaticStrings.ozymandiasSysName) ?: return false
        system = sys
        val ids = listOf(ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_ONE, ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_TWO,
            ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_THREE, ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_FOUR)
        ids.mapNotNullTo(stops) { sys.getEntityById(it) }
        sys.customEntities.filterTo(stops) { it.customEntityType == Entities.ORBITAL_HABITAT || it.customEntityType == Entities.STATION_MINING }
        if (stops.isEmpty()) return false
        val sleeper = ZeaCaeli.caeli() ?: return false
        caeli = sleeper

        // the outermost jump point leading to hyperspace
        val center = sys.center
        val jump = sys.jumpPoints.filterIsInstance<JumpPointAPI>()
            .filter { jp -> jp.destinations.any { it.destination?.containingLocation?.isHyperspace == true } }
            .maxByOrNull { Misc.getDistance(it, center) } ?: return false
        inner = jump
        outer = jump.destinations.first { it.destination?.containingLocation?.isHyperspace == true }.destination
        beatenAtStart = ZeaCaeli.beaten()
        withKnights = Global.getSector().memoryWithoutUpdate.getBoolean(PatronKeys.PATRON_SECURED)

        setStartingStage(if (withKnights) Stage.RENDEZVOUS else Stage.SECURE)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        connectWithGlobalFlag(Stage.RENDEZVOUS, Stage.SECURE, MET)
        connectWithGlobalFlag(Stage.SECURE, Stage.MEMORIAL, SECURED)
        connectWithGlobalFlag(Stage.MEMORIAL, Stage.RECOVER, MEMORIALIZED)
        connectWithGlobalFlag(Stage.RECOVER, Stage.DELIVER, RECOVERED)
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        makeImportant(sleeper, OzymandiasKeys.OZY_CAELI, Stage.MEMORIAL)
        KolPeople.get(KolIds.HELENSIS)?.let { makeImportant(it, DELIVER, Stage.DELIVER) }
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        memory.set("\$kolOzy_wasExplored", KolChronicle.has("ozymandiasVisited"))
        for (stop in stops) {
            stop.memoryWithoutUpdate.set(OzymandiasKeys.OZY_STOP, true)
            Misc.makeImportant(stop, "kolCh2Oz")
        }
        // a hint toward the cryosleeper while its guardian stands
        if (!beatenAtStart) caeli?.let { makeImportant(it, "\$kolOzy_guardian", Stage.SECURE) }
        if (withKnights) spawnKnights()
    }

    private fun spawnKnights() {
        val target = outer ?: return
        val home = Global.getSector().economy.getMarket(KolIds.CYGNUS)?.primaryEntity ?: return
        val random = Random()
        repeat(FLEETS) {
            val fp = MIN_FP + random.nextFloat() * (MAX_FP - MIN_FP)
            val fleet = KolFleets.combat(KolIds.FACTION, FleetTypes.PATROL_MEDIUM, fp) ?: return@repeat
            home.containingLocation.addEntity(fleet)
            fleet.setLocation(home.location.x, home.location.y)
            val mem = fleet.memoryWithoutUpdate
            mem.set(OzymandiasKeys.OZY_KNIGHTS, true)
            mem.set(WAITING, true)
            // undistracted on the way and while waiting
            mem.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
            mem.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true)
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 1000f, "travelling to Ozymandias")
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 3650f, "waiting at the Ozymandias jump point")
            makeImportant(fleet, "\$kolOzy_rendezvous", Stage.RENDEZVOUS)
            knights.add(fleet)
        }
    }

    private fun alive() = knights.filter { it.isAlive }

    /** The player tells the Knights at the rendezvous they're ready: they jump in and hold at the inner jump point. */
    private fun ready() {
        val target = inner ?: return
        for (fleet in alive()) {
            fleet.memoryWithoutUpdate.unset(WAITING)
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 1000f, "jumping into Ozymandias")
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 3650f, "holding at the jump point")
        }
        memory.set(MET, true)
    }

    private fun sendHome(fleet: CampaignFleetAPI) {
        KolFleets.sendHome(fleet, KolIds.cygnus?.primaryEntity, "returning to Cygnus", WAITING, FOLLOWING)
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        val sys = system ?: return
        val player = Global.getSector().playerFleet ?: return
        if (currentStage == Stage.RENDEZVOUS) {
            when {
                alive().isEmpty() -> memory.set(MET, true)       // none left to talk to: on without a conversation
                !beatenAtStart && ZeaCaeli.beaten() -> {          // beaten while they waited: their work is done
                    alive().forEach { sendHome(it) }
                    knights.clear()
                    memory.set(MET, true)
                }
            }
            return
        }
        // the Knights in Ozymandias stay there, and follow the player while the player is there too
        var here = false
        for (fleet in alive()) {
            if (fleet.containingLocation != sys) continue
            here = true
            val mem = fleet.memoryWithoutUpdate
            if (!mem.getBoolean(MemFlags.MEMORY_KEY_NO_JUMP)) {
                mem.set(MemFlags.MEMORY_KEY_NO_JUMP, true)
                mem.unset(MemFlags.FLEET_IGNORES_OTHER_FLEETS)
                mem.unset(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS)
            }
            if (player.containingLocation == sys && !mem.getBoolean(FOLLOWING)) {
                mem.set(FOLLOWING, true)
                fleet.clearAssignments()
                // aggressive: a passive orbit never engages anything but the player (StrategicModule.isAllowedToEngage)
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, player, 3650f, "accompanying your fleet")
            }
        }
        memory.set(KNIGHTS_HERE, here)
        if (currentStage == Stage.SECURE && visited >= stops.size && ZeaCaeli.beaten()) memory.set(SECURED, true)
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        when (action) {
            "visit" -> {
                val entity = dialog?.interactionTarget ?: return false
                if (!entity.memoryWithoutUpdate.getBoolean(OzymandiasKeys.OZY_STOP)) return false
                entity.memoryWithoutUpdate.unset(OzymandiasKeys.OZY_STOP)
                Misc.makeUnimportant(entity, "kolCh2Oz")
                visited++
                return true
            }
            "ready" -> {
                ready()
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
            "memorial" -> {
                layMemorial()
                spawnCache()
                memory.set(MEMORIALIZED, true)
                // their duties done, the Knights go home; the relic is the player's to carry
                alive().forEach { sendHome(it) }
                knights.clear()
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
            "takeRelic" -> {
                val text = dialog?.textPanel
                text?.setFontSmallInsignia()
                text?.addPara("%s brought aboard", Misc.getPositiveHighlightColor(), Misc.getHighlightColor(), Misc.ucFirst(RELIC_NAME))
                text?.setFontInsignia()
                cache?.let { Misc.fadeAndExpire(it) }
                cache = null
                memory.set(RECOVERED, true)
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
            "deliverRelic" -> {
                val text = dialog?.textPanel
                text?.setFontSmallInsignia()
                text?.addPara("%s handed over", Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.ucFirst(RELIC_NAME))
                text?.setFontInsignia()
                KolTechSituationIntel.get()?.credit(KolStorySettings.ozRelicScrip, dialog)
                memory.set(RELIC_DELIVERED, true)
                KolOrdainedEarly.confronted() // Enarms' objection in her office (kolOzy_deliver*)
                // the last objective: only now is Ozymandias done (the chapter requirement, the assembly's reckoning)
                memory.set(OzymandiasKeys.OZY_DONE, true)
                KolAssembly.report("ozymandias")
                memory.set(STAGE_DONE, true)
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    private fun layMemorial() {
        val at = caeli ?: return
        val beacon = at.containingLocation.addCustomEntity(null, "[PLACEHOLDER] Memorial of the Order", Entities.WARNING_BEACON,
            KolIds.FACTION)
        beacon.setCircularOrbitPointingDown(at, 90f, at.radius + 200f, 60f)
        beacon.memoryWithoutUpdate.set(MEMORIAL, true)
    }

    /** The relic's cache, surfaced beside Caeli as the memorial is laid. */
    private fun spawnCache() {
        val at = caeli ?: return
        val entity = at.containingLocation.addCustomEntity(null, "[PLACEHOLDER] Sealed Cache",
            ZeaStaticStrings.ZeaEntities.ZEA_CACHE_LOW, ZeaStaticStrings.dawnID)
        entity.setCircularOrbitPointingDown(at, 270f, at.radius + 150f, 45f)
        entity.isDiscoverable = false // in plain sight: it surfaced as the player watched
        cache = entity
        makeImportant(entity, CACHE, Stage.RECOVER)
    }

    override fun notifyEnding() {
        super.notifyEnding()
        for (stop in stops) {
            stop.memoryWithoutUpdate.unset(OzymandiasKeys.OZY_STOP)
            Misc.makeUnimportant(stop, "kolCh2Oz")
        }
        alive().forEach { sendHome(it) }
        knights.clear()
        memory.unset(KNIGHTS_HERE)
        cache?.let { Misc.fadeAndExpire(it) } // the mission ended (severance) before the relic was taken
        cache = null
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        when (currentStage) {
            Stage.RENDEZVOUS -> info.addPara("[PLACEHOLDER] Brother Enarms has sent Knights from Cygnus to secure Ozymandias " +
                    "with you. They will wait at the system's jump point in hyperspace until you join them.", 10f)
            Stage.SECURE -> info.addPara("[PLACEHOLDER] " + (if (withKnights) "" else "Without a patron, the Order has no Knights to spare; you go alone. ") +
                    "Scout and secure Ozymandias: its worlds and stations " +
                    "(%s of %s visited)" + (if (ZeaCaeli.beaten()) "." else ", and whatever keeps the Caeli cryosleeper."),
                10f, h, "$visited", "${stops.size}")
            Stage.MEMORIAL -> info.addPara("[PLACEHOLDER] Ozymandias is secured. The Order's memorial is to be laid at Caeli.", 10f)
            Stage.RECOVER -> info.addPara("[PLACEHOLDER] As the memorial was laid, a sealed cache surfaced from the guardian's " +
                    "wreckage beside Caeli. Master Helensis has asked that anything recovered be brought to her.", 10f)
            Stage.DELIVER -> info.addPara("[PLACEHOLDER] You carry %s. Master Helensis wants it at Cygnus; not everyone " +
                    "in the Order will be glad to see it there.", 10f, h, RELIC_NAME)
            else -> {}
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        val h = Misc.getHighlightColor()
        when (currentStage) {
            Stage.RENDEZVOUS -> info.addPara("Meet the Knights at the Ozymandias jump point", tc, pad)
            Stage.SECURE -> {
                if (visited < stops.size) info.addPara("Visit Ozymandias's worlds and stations: %s of %s", pad, tc, h, "$visited", "${stops.size}")
                if (!ZeaCaeli.beaten()) info.addPara("Secure the Caeli cryosleeper", tc, if (visited < stops.size) 0f else pad)
            }
            Stage.MEMORIAL -> info.addPara("Lay the memorial at Caeli", tc, pad)
            Stage.RECOVER -> info.addPara("Recover the cache beside Caeli", tc, pad)
            Stage.DELIVER -> info.addPara("Bring %s to Master Helensis at Cygnus", pad, tc, h, RELIC_NAME)
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = "Ozymandias"
}

/** Ozymandias: the pilgrimage, the memorial and the Caeli encounter. */
object KolOzymandiasModule : KolStoryModule {
    override val id = "ozymandias"
    override fun severedKeys() = listOf("\$kolOzy")
    private val memory get() = Global.getSector().memoryWithoutUpdate

    override fun onLoad() = ZeaCaeli.ensure()

    override fun requirements() = listOf(KolStoryModule.Requirement(2, "ozymandias", { true }, { memory.getBoolean(OzymandiasKeys.OZY_DONE) }))

    override fun cues() = mapOf(
        // after the patron's announcement: Ozymandias, if it hasn't been taken up
        "ozymandias" to KolStoryCue.Cue(KolIds.ENARMS, "Knights: Brother Enarms Asks for You",
            "[PLACEHOLDER] With the patron announced, Brother Enarms has asked to see you at Cygnus. The Order has work for you beyond the assembly hall.",
            "Speak with Brother Enarms at Cygnus") {
            memory.getBoolean(PatronKeys.PATRON_SECURED) && !memory.getBoolean(OzymandiasKeys.OZY_ACTIVE) &&
                    !memory.getBoolean(OzymandiasKeys.OZY_DONE) && !KolSeverance.severed()
        },
    )
}
