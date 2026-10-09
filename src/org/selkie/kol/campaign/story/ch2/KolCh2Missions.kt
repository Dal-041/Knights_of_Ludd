package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.KolAssembly
import org.selkie.kol.campaign.story.KolChapter
import org.selkie.kol.campaign.story.KolChronicle
import org.selkie.kol.campaign.story.KolStorySettings
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh2
import org.selkie.zea.campaign.ZeaCaeli
import org.selkie.zea.helpers.ZeaStaticStrings
import java.awt.Color

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
    companion object { const val STAGE_DONE = "\$kolCh2Patron_stageDone" }

    /**
     * BRIEF: meet Greenflight in her office (the branches are introduced there). SECURE: seeking a patron (the
     * branches). ANNOUNCE: a patron is signed; the next assembly announces it.
     */
    enum class Stage { BRIEF, SECURE, ANNOUNCE, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(KolCh2.PATRON_REF, KolCh2.PATRON_ACTIVE)) return false
        // the oath secures the player's own forces at once: its own meeting at Lyra takes the brief's place
        setStartingStage(if (memory.getBoolean(KolCh2.CONV_OATH_SWORN)) Stage.SECURE else Stage.BRIEF)
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
        memory.set(KolCh2.PATRON, patron)
        memory.set(KolStaticStrings.KolPatron.LEVER, lever)
        if (price != null) memory.set(KolStaticStrings.KolPatron.PRICE, price)
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
        memory.set(KolCh2.PATRON_SECURED, true)
        memory.getString(KolCh2.PATRON)?.let { KolPatronFleets.station(it) }
        KolPatronParley.dropOff() // a fallback: the delegation normally goes home in the gate at Lyra
        memory.set(STAGE_DONE, true)
        checkStageChangesAndTriggers(null, null)
        org.selkie.kol.campaign.story.KolStoryCue.check() // the next mission, if it hasn't been taken up
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

    override fun buttonPressConfirmed(buttonId: Any?, ui: com.fs.starfarer.api.ui.IntelUIAPI) {
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
                if (memory.getBoolean("\$kolCh2Patron_briefRefused") && !KolPatron.churchWelcomes())
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
    const val ANNOUNCE = "\$kolCh2Patron_announce"
    /** The brief at Greenflight's office is done. */
    const val BRIEFED = "\$kolCh2Patron_briefed"
}

/**
 * Base for joint operations: a Knights squadron meets the player when they enter the target system and stays with
 * their fleet (vanilla's battle-joining puts it on the player's side against the Order's enemies). If the target is
 * already beaten when the operation starts, it becomes a recovery visit, done on entering the system.
 */
abstract class KolJointOperation : HubMissionWithSearch() {
    enum class Stage { GO, COMPLETED }

    protected var target: StarSystemAPI? = null
    protected var squadron: CampaignFleetAPI? = null
    var alreadyDone = false
        protected set

    protected abstract val ref: String
    protected abstract val activeFlag: String
    protected abstract val doneFlag: String
    protected abstract val bossFlag: String
    private val stageDone get() = ref.removeSuffix("_ref") + "_stageDone"
    protected abstract fun targetSystem(): StarSystemAPI?

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(ref, activeFlag)) return false
        target = targetSystem() ?: return false
        alreadyDone = memory.getBoolean(bossFlag)
        setStartingStage(Stage.GO)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        makeImportant(target!!.hyperspaceAnchor, "\$kolJointOp_target", Stage.GO)
        if (alreadyDone) setStageOnEnteredLocation(Stage.COMPLETED, target)
        else setStageOnGlobalFlag(Stage.COMPLETED, stageDone) // set from advanceImpl: the boss flag belongs to Zea
        return true
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (currentStage == Stage.GO && !alreadyDone && memory.getBoolean(bossFlag)) memory.set(stageDone, true)
        if (currentStage != Stage.GO || alreadyDone || squadron != null) return
        val player = Global.getSector().playerFleet ?: return
        if (player.containingLocation == target) spawnSquadron(player)
    }

    private fun spawnSquadron(player: CampaignFleetAPI) {
        // resources set aside for an operation that wasn't needed strengthen the next one
        val carry = memory.getFloat(KolCh2Story.JOINT_OP_CARRY)
        memory.unset(KolCh2Story.JOINT_OP_CARRY)
        val params = FleetParamsV3(null, KolStaticStrings.kolFactionID, null, FleetTypes.TASK_FORCE,
            KolStorySettings.jointOpFleetPoints * (1f + carry), 0f, 0f, 0f, 0f, 0f, 0f)
        val fleet = FleetFactoryV3.createFleet(params) ?: return
        fleet.name = "Knights Squadron"
        target!!.addEntity(fleet)
        fleet.setLocation(player.location.x + 200f, player.location.y + 200f)
        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_MISSION_IMPORTANT, true)
        fleet.memoryWithoutUpdate.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, player, 3650f, "accompanying your fleet")
        Misc.makeImportant(fleet, "kolJointOperation")
        squadron = fleet
    }

    override fun notifyEnding() {
        super.notifyEnding()
        val fleet = squadron ?: return
        if (!fleet.isAlive) return
        fleet.clearAssignments()
        Misc.makeUnimportant(fleet, "kolJointOperation")
        val home = Global.getSector().economy.getMarket(KolStaticStrings.KOL_CYGNUS)?.primaryEntity
        if (home != null) fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f, "returning to Cygnus")
        else fleet.despawn()
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        memory.set(doneFlag, true)
        KolAssembly.report("jointOperation")
    }
}

/** The first joint operation, from Greenflight: the Tri-Tachyon vessel at Alpha Site (Ninaya). */
class KolCh2Ninaya : KolJointOperation() {
    override val ref = KolCh2.NINAYA_REF
    override val activeFlag = KolCh2.NINAYA_ACTIVE
    override val doneFlag = KolCh2.NINAYA_OP_DONE
    override val bossFlag = ZeaStaticStrings.ZeaMemKeys.ZEA_TT_NINAYA_DONE
    override fun targetSystem(): StarSystemAPI? = Global.getSector().getStarSystem("Unknown Location")

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage != Stage.GO) return
        info.addPara(if (alreadyDone)
            "[PLACEHOLDER] Sister Greenflight wants the wreck at Alpha Site examined by the Order. A Knights team will accompany your visit."
        else
            "[PLACEHOLDER] Sister Greenflight has traced the vessel that haunts Alpha Site. A Knights squadron will meet you there.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage != Stage.GO) return false
        info.addPara(if (alreadyDone) "Visit Alpha Site with the Order" else "Destroy the vessel at Alpha Site", tc, pad)
        return true
    }

    override fun getBaseName(): String = "Operation: Alpha Site"
}

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
 * Stops are flagged on their entities and handled by high-score OpenInteractionDialog rules (kolCh2_oz*).
 */
class KolCh2Ozymandias : HubMissionWithSearch() {
    companion object {
        const val STAGE_DONE = "\$kolCh2Oz_stageDone"
        private const val MET = "\$kolCh2Oz_metKnights"
        private const val SECURED = "\$kolCh2Oz_secured"
        /** On the Knights fleets while they wait at the rendezvous (the comm topic's gate). */
        const val WAITING = "\$kolCh2Oz_waiting"
        private const val FOLLOWING = "\$kolCh2Oz_following"
        /** On the memorial beacon. */
        const val MEMORIAL = "\$kolCh2Oz_memorial"
        /** Global, kept current: a Knights fleet of the mission is alive in Ozymandias (memorial text variants). */
        const val KNIGHTS_HERE = "\$kolCh2Oz_knightsHere"
        private const val MEMORIALIZED = "\$kolCh2Oz_memorialized"
        private const val RECOVERED = "\$kolCh2Oz_relicAboard"
        /** On the cache that holds the relic, and on Helensis while it's to be delivered. */
        const val CACHE = "\$kolCh2Oz_cache"
        const val DELIVER = "\$kolCh2Oz_deliver"
        /** Global, permanent: the relic was delivered to Helensis. */
        const val RELIC_DELIVERED = "\$kolCh2_ozRelicDelivered"
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
        if (!setGlobalReference(KolCh2.OZY_REF, KolCh2.OZY_ACTIVE)) return false
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
        withKnights = Global.getSector().memoryWithoutUpdate.getBoolean(KolCh2.PATRON_SECURED)

        setStartingStage(if (withKnights) Stage.RENDEZVOUS else Stage.SECURE)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        connectWithGlobalFlag(Stage.RENDEZVOUS, Stage.SECURE, MET)
        connectWithGlobalFlag(Stage.SECURE, Stage.MEMORIAL, SECURED)
        connectWithGlobalFlag(Stage.MEMORIAL, Stage.RECOVER, MEMORIALIZED)
        connectWithGlobalFlag(Stage.RECOVER, Stage.DELIVER, RECOVERED)
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        makeImportant(sleeper, KolCh2.OZY_CAELI, Stage.MEMORIAL)
        Global.getSector().importantPeople.getPerson(KolStaticStrings.KolCh1.HELENSIS_ID)?.let { makeImportant(it, DELIVER, Stage.DELIVER) }
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        memory.set("\$kolCh2Oz_wasExplored", KolChronicle.has("ozymandiasVisited"))
        for (stop in stops) {
            stop.memoryWithoutUpdate.set(KolCh2.OZY_STOP, true)
            Misc.makeImportant(stop, "kolCh2Oz")
        }
        // a hint toward the cryosleeper while its guardian stands
        if (!beatenAtStart) caeli?.let { makeImportant(it, "\$kolCh2Oz_guardian", Stage.SECURE) }
        if (withKnights) spawnKnights()
    }

    private fun spawnKnights() {
        val target = outer ?: return
        val home = Global.getSector().economy.getMarket(KolStaticStrings.KOL_CYGNUS)?.primaryEntity ?: return
        val random = java.util.Random()
        repeat(FLEETS) {
            val fp = MIN_FP + random.nextFloat() * (MAX_FP - MIN_FP)
            val params = FleetParamsV3(null, KolStaticStrings.kolFactionID, null, FleetTypes.PATROL_MEDIUM, fp, 0f, 0f, 0f, 0f, 0f, 0f)
            val fleet = FleetFactoryV3.createFleet(params) ?: return@repeat
            home.containingLocation.addEntity(fleet)
            fleet.setLocation(home.location.x, home.location.y)
            val mem = fleet.memoryWithoutUpdate
            mem.set(KolCh2.OZY_KNIGHTS, true)
            mem.set(WAITING, true)
            // undistracted on the way and while waiting
            mem.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
            mem.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true)
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, target, 1000f, "travelling to Ozymandias")
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, target, 3650f, "waiting at the Ozymandias jump point")
            makeImportant(fleet, "\$kolCh2Oz_rendezvous", Stage.RENDEZVOUS)
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
        val mem = fleet.memoryWithoutUpdate
        for (key in listOf(WAITING, FOLLOWING, MemFlags.MEMORY_KEY_NO_JUMP, MemFlags.FLEET_IGNORES_OTHER_FLEETS,
                MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS)) mem.unset(key)
        fleet.clearAssignments()
        val home = Global.getSector().economy.getMarket(KolStaticStrings.KOL_CYGNUS)?.primaryEntity
        if (home != null) fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, home, 1000f, "returning to Cygnus")
        else fleet.despawn()
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
                if (!entity.memoryWithoutUpdate.getBoolean(KolCh2.OZY_STOP)) return false
                entity.memoryWithoutUpdate.unset(KolCh2.OZY_STOP)
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
                org.selkie.kol.campaign.situations.KolTechSituationIntel.get()?.credit(KolStorySettings.ozRelicScrip, dialog)
                memory.set(RELIC_DELIVERED, true)
                // the last objective: only now is Ozymandias done (the chapter requirement, the assembly's reckoning)
                memory.set(KolCh2.OZY_DONE, true)
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
            KolStaticStrings.kolFactionID)
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
            stop.memoryWithoutUpdate.unset(KolCh2.OZY_STOP)
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

/**
 * The outside power's agent: a bar encounter (large markets of that power, or independents) once the player's
 * Abyss progress is noticed. Never accepted as a mission; the answer is recorded (kolCh2_agent* rules).
 */
class KolCh2Agent : HubMissionWithBarEvent() {
    enum class Stage { TALK, DONE }

    override fun shouldShowAtMarket(market: MarketAPI): Boolean {
        if (KolChapter.get() < 2 || !memory.getBoolean(KolCh2.AGENT_TRIGGERED) || memory.getBoolean(KolCh2.AGENT_ANSWERED)) return false
        if (market.size < 5) return false
        val power = KolCh2Story.outsidePower()
        return market.factionId == power || market.factionId == Factions.INDEPENDENT
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!barEvent) return false
        if (!setGlobalReference(KolCh2.AGENT_REF)) return false
        val power = Global.getSector().getFaction(KolCh2Story.outsidePower()) ?: return false
        setPersonOverride(KolCh2Story.agent())
        memory.set("\$kolCh2Agent_powerName", power.displayName)
        memory.set("\$kolCh2Agent_powerArticle", power.displayNameWithArticle)
        setStartingStage(Stage.TALK)
        addSuccessStages(Stage.DONE)
        setRepFactionChangesNone()
        setRepPersonChangesNone()
        return true
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "answer") {
            val answer = params?.getOrNull(1)?.getString(memoryMap) ?: return false
            memory.set(KolCh2.AGENT_ANSWER, answer)
            memory.set(KolCh2.AGENT_ANSWERED, true)
            if (answer == "comply") {
                val power = Global.getSector().getFaction(KolCh2Story.outsidePower())
                power?.adjustRelationship(Factions.PLAYER, 0.05f)
                dialog?.textPanel?.addPara("Relations with ${power?.displayNameWithArticle} improved", Misc.getPositiveHighlightColor())
            }
            KolAssembly.report("agent")
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun getBaseName(): String = "A Quiet Word"
}

/** The Inquisition's summons at TRUSTED: the inquest into the player at Cygnus (dock event; kolCh2_inquest* rules). */
class KolCh2Inquest : HubMissionWithSearch() {
    companion object { const val STAGE_DONE = "\$kolCh2Inquest_stageDone" }

    enum class Stage { SUMMONED, COMPLETED }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(KolCh2.INQUEST_REF, KolCh2.INQUEST_ACTIVE)) return false
        val cygnus = Global.getSector().economy.getMarket(KolStaticStrings.KOL_CYGNUS) ?: return false
        setStartingStage(Stage.SUMMONED)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        makeImportant(cygnus.primaryEntity, "\$kolCh2Inquest_summons", Stage.SUMMONED)
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        KolCh2Story.addInquestEvent()
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        if (action == "outcome") {
            val outcome = params?.getOrNull(1)?.getString(memoryMap) ?: return false
            KolCh2Story.recordInquest(outcome)
            checkStageChangesAndTriggers(dialog, memoryMap)
            return true
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage == Stage.SUMMONED) info.addPara("[PLACEHOLDER] The Inquisition has summoned you to Battlestation " +
                "Cygnus to answer questions about your service to the Order.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage != Stage.SUMMONED) return false
        info.addPara("Answer the summons at Battlestation Cygnus", tc, pad)
        return true
    }

    override fun getBaseName(): String = "Summoned"
}
