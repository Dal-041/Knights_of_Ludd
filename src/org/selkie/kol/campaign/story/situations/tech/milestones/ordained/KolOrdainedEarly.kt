package org.selkie.kol.campaign.story.situations.tech.milestones.ordained

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import org.selkie.kol.campaign.story.core.KolFleets
import org.selkie.kol.campaign.story.core.KolLog
import org.selkie.kol.campaign.story.systems.cue.KolStoryCue
import org.selkie.kol.campaign.story.systems.spawn.KolFleetSpawnHook
import org.selkie.kol.campaign.story.systems.severance.KolSeverance
import org.selkie.kol.campaign.story.situations.tech.TechKeys
import org.selkie.kol.campaign.story.systems.severance.SeveranceKeys
import org.selkie.kol.campaign.story.systems.spawn.SpawnKeys
import org.selkie.kol.campaign.story.core.KolIds

/**
 * The early Ordained rollout (add-knights-tech-milestones). Some days after ARMORY, Helensis asks the player to come
 * back (a story cue); on their next call she has them up to her office, where Enarms is already confronting her over
 * the unapproved new builds (kolOrdained_* rules). The Ordained squadron then waits at Cygnus for the player's go, or
 * leaves on its own after a few days, against pirates hiding on Canaan's fringe. Whether the player fights those pirates
 * is their support. The mission concludes when the squadron is back at Cygnus.
 */
class KolOrdainedEarly : HubMissionWithSearch(), FleetEventListener {
    companion object {
        const val ID = "kolOrdainedEarly"
        const val REF = "\$kolOrdainedEarly_ref"
        const val ACTIVE = "\$kolOrdainedEarly_active"
        const val STAGE_DONE = "\$kolOrdainedEarly_stageDone"
        /** Global: the clock timestamp when ARMORY was first seen (the delay before the cue). */
        const val ARMORY_DAY = "\$kolOrdained_armoryDay"
        /** Global, after the squadron's return: Helensis' follow-up is open (cleared by its rule). */
        const val FOLLOWUP = "\$kolOrdained_followup"
        /** On the squadron while it waits at Cygnus (the comm topic's gate). */
        const val WAITING = "\$kolOrdained_waiting"
        const val SQUADRON = "\$kolOrdained_squadron"
        /** The squadron's map marker for the shakedown (HubMission importance flag). */
        private const val SQUADRON_MARK = "\$kolOrdained_squadronMarked"
        const val PIRATES = "\$kolOrdained_pirates"
        /** Global, int: Enarms' confrontations with Helensis in her office (this mission's, and the Ozymandias relic's). */
        const val CONFRONTATIONS = "\$kolHelensis_confrontations"

        // dev tuning
        const val CUE_DELAY_DAYS = 7f
        const val WAIT_DAYS = 3f
        const val PIRATE_FP = 160f
        /** How far past the system's outermost entity the pirates lie. */
        const val PIRATE_MARGIN = 300f
        const val SQUADRON_FP = 240f
        /** Share of the filler hulls (beyond one of each Ordained hull) that fly Ordained too. */
        const val FILLER_ORDAINED_SHARE = 0.5f
        /** Vanilla weapons the Knights learn when the early rollout concludes. */
        val ROLLOUT_WEAPONS = listOf("hveldriver", "heavymauler")
        /** One of each Ordained hull. */
        val ORDAINED_VARIANTS = listOf("kol_sundew_Squire", "kol_protea_Knight", "kol_marigold_Attack", "kol_tamarisk_Elite",
            "kol_larkspur_Knight", "kol_lotus_Knightly", "kol_lunaria_Knightmaster")

        private val memory get() = Global.getSector().memoryWithoutUpdate

        /** Records the day ARMORY was first seen (from the story pulse). */
        @JvmStatic
        fun pulse() {
            if (memory.getBoolean(TechKeys.STAGE_FLAGS[3]) && !memory.contains(ARMORY_DAY))
                memory.set(ARMORY_DAY, Global.getSector().clock.timestamp)
        }

        /** The cue and the confrontation are due: ARMORY some days past, the mission neither running nor done. */
        fun due(): Boolean {
            if (!memory.getBoolean(TechKeys.STAGE_FLAGS[3]) || !memory.contains(ARMORY_DAY)) return false
            if (memory.getBoolean(ACTIVE) || memory.getBoolean(OrdainedKeys.ORDAINED_EARLY)) return false
            if (KolSeverance.severed() || memory.getBoolean(SeveranceKeys.EXCOMMUNICATED)) return false
            return Global.getSector().clock.getElapsedDaysSince(memory.getLong(ARMORY_DAY)) >= CUE_DELAY_DAYS
        }

        /** Counts one of Enarms' confrontations with Helensis; the second and later escalate (rules read the count). */
        fun confronted() = memory.set(CONFRONTATIONS, memory.getInt(CONFRONTATIONS) + 1)
    }

    enum class Stage { CONFRONT, SHAKEDOWN, COMPLETED }

    private var squadron: CampaignFleetAPI? = null
    private var pirates: CampaignFleetAPI? = null
    /** The squadron has left Cygnus for the pirates. */
    private var departed = false
    private var waitSince = 0L
    private var joined = false

    private val cygnus: MarketAPI? get() = Global.getSector().economy.getMarket(KolIds.CYGNUS)

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(REF, ACTIVE)) return false
        if (cygnus?.primaryEntity == null) return false
        setStartingStage(Stage.CONFRONT)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        KolStoryCue.clear("ordainedEarly")
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        when (action) {
            // the confrontation is over: the player's stance (helensis / enarms / silent); the squadron and the pirates appear
            "confronted" -> {
                val stance = params?.getOrNull(1)?.getString(memoryMap) ?: return false
                memory.set(OrdainedKeys.ORDAINED_EARLY_STANCE, stance)
                confronted()
                // Enarms' ARMORY reaction (kolTech_enarmsReact3) would no longer fit
                for (i in 1..3) memory.set("\$kolTech_enarmsSaw$i", true)
                spawnPirates()
                spawnSquadron()
                setCurrentStage(Stage.SHAKEDOWN, dialog, memoryMap)
                return true
            }
            // the player gives the waiting squadron the go
            "go" -> { depart(); return true }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    // --- fleets ---------------------------------------------------------------------------------------------

    private fun system(): StarSystemAPI? = cygnus?.starSystem

    /**
     * A pirate fleet of fixed strength hiding just past everything in Cygnus' system, held in place until engaged. Local
     * patrols ignore it until the squadron leaves, so the shakedown keeps its target.
     */
    private fun spawnPirates() {
        val sys = system() ?: return
        val fleet = KolFleets.combat(Factions.PIRATES, FleetTypes.PATROL_LARGE, PIRATE_FP, locInHyper = sys.location) {
            ignoreMarketFleetSizeMult = true
        } ?: return
        val spot = KolFleets.edgeSpot(sys, PIRATE_MARGIN)
        KolLog.info("ordained", "shakedown pirates at ${Misc.getDistance(spot, sys.center.location).toInt()} from ${sys.name}'s center")
        KolFleets.place(fleet, sys, spot.x, spot.y)
        val mem = fleet.memoryWithoutUpdate
        mem.set(PIRATES, true)
        mem.set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true)
        // fixed in place: they don't chase or wander, but fight whoever engages them
        mem.set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true)
        mem.set(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS, true) // until the squadron leaves
        fleet.isTransponderOn = true
        fleet.addAssignment(FleetAssignment.HOLD, sys.createToken(spot.x, spot.y), 365f, "lying in wait")
        fleet.isNoAutoDespawn = true
        fleet.addEventListener(this)
        makeImportant(fleet, PIRATES, Stage.SHAKEDOWN)
        pirates = fleet
    }

    /** One of each Ordained hull, filled out to the squadron's strength with Knights hulls, some of them Ordained. */
    private fun spawnSquadron() {
        val home = cygnus?.primaryEntity ?: return
        val ordainedFp = ORDAINED_VARIANTS.sumOf { variantFp(it).toDouble() }.toFloat()
        val fillerFp = (SQUADRON_FP - ordainedFp).coerceAtLeast(0f)
        val fleet = KolFleets.combat(KolIds.FACTION, FleetTypes.TASK_FORCE, fillerFp, source = cygnus, allowEmpty = true) {
            ignoreMarketFleetSizeMult = true
        } ?: return
        for (member in fleet.fleetData.membersListCopy) {
            if (Math.random() < FILLER_ORDAINED_SHARE) KolFleetSpawnHook.convertMember(member)
        }
        for (variantId in ORDAINED_VARIANTS) {
            if (!Global.getSettings().doesVariantExist(variantId)) continue
            val member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId)
            fleet.fleetData.addFleetMember(member)
            KolFleetSpawnHook.convertMember(member)
            member.repairTracker.cr = member.repairTracker.maxCR
        }
        KolFleetSpawnHook.finish(fleet)
        fleet.name = "Ordained Squadron"
        fleet.addTag(SpawnKeys.FLEET_MODIFIED_TAG) // already Ordained: leave it to this mission
        KolFleets.place(fleet, home)
        val mem = fleet.memoryWithoutUpdate
        mem.set(SQUADRON, true)
        mem.set(WAITING, true)
        mem.set(MemFlags.MEMORY_KEY_NO_JUMP, true)
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, home, 3650f, "awaiting orders")
        fleet.addEventListener(this)
        makeImportant(fleet, SQUADRON_MARK, Stage.SHAKEDOWN)
        squadron = fleet
        waitSince = Global.getSector().clock.timestamp
    }

    private fun variantFp(variantId: String): Float =
        if (Global.getSettings().doesVariantExist(variantId)) Global.getSettings().getVariant(variantId).hullSpec.fleetPoints.toFloat() else 0f

    /** The squadron leaves: intercept the pirates, then back to Cygnus (despawning there concludes the mission). */
    private fun depart() {
        if (departed) return
        val fleet = squadron ?: return
        departed = true
        fleet.memoryWithoutUpdate.unset(WAITING)
        fleet.clearAssignments()
        val target = pirates
        if (target != null && target.isAlive) {
            target.memoryWithoutUpdate.unset(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS)
            // go to where they lie first (their transponder is off, so they may not be on sensors from Cygnus), then engage
            val lair = target.containingLocation.createToken(target.location.x, target.location.y)
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, lair, 30f, "hunting pirates")
            fleet.addAssignment(FleetAssignment.INTERCEPT, target, 10f, "engaging pirates")
        }
        cygnus?.primaryEntity?.let {
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, it, 1000f, "returning to Cygnus")
        } ?: fleet.despawn()
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (currentStage == Stage.SHAKEDOWN && !departed && squadron != null &&
            Global.getSector().clock.getElapsedDaysSince(waitSince) >= WAIT_DAYS) depart()
    }

    // --- listeners ------------------------------------------------------------------------------------------

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (fleet != null && fleet === pirates && battle?.isPlayerInvolved == true) joined = true
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {
        if (fleet == null || isEnded || isEnding) return
        if (fleet === pirates) {
            pirates = null
            return
        }
        if (fleet !== squadron) return
        squadron = null
        if (reason != FleetDespawnReason.REACHED_DESTINATION) memory.set(OrdainedKeys.ORDAINED_EARLY_LOST, true)
        conclude()
    }

    /** The last mandatory objective is done: the squadron is back (or gone). Records the rollout and the player's part. */
    private fun conclude() {
        memory.set(OrdainedKeys.ORDAINED_EARLY, true)
        // the rollout's armory: the Knights now field these
        Global.getSector().getFaction(KolIds.FACTION)?.let { knights ->
            for (id in ROLLOUT_WEAPONS) if (!knights.knowsWeapon(id)) knights.addKnownWeapon(id, false)
        }
        memory.set(OrdainedKeys.ORDAINED_EARLY_SHAKEDOWN, if (joined) "joined" else "absent")
        memory.set(FOLLOWUP, true)
        memory.set(STAGE_DONE, true)
        checkStageChangesAndTriggers(null, null)
    }

    override fun notifyEnding() {
        super.notifyEnding()
        // pirates left over (the squadron lost, or the pirates slipped away): they leave the system
        pirates?.takeIf { it.isAlive }?.let { fleet ->
            fleet.removeEventListener(this)
            fleet.memoryWithoutUpdate.unset(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER)
            fleet.memoryWithoutUpdate.unset(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS)
            fleet.memoryWithoutUpdate.unset(MemFlags.FLEET_IGNORES_OTHER_FLEETS)
            fleet.clearAssignments()
            Misc.giveStandardReturnToSourceAssignments(fleet)
        }
        squadron?.removeEventListener(this)
    }

    // --- intel ----------------------------------------------------------------------------------------------

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.CONFRONT -> info.addPara("[PLACEHOLDER] Master Helensis has asked to see you in her office at Battlestation Cygnus.", 10f)
            Stage.SHAKEDOWN -> info.addPara("[PLACEHOLDER] Cygnus' yard has built new ships for the Order, and Master Helensis is " +
                    "sending them on their first sortie: a pirate band hiding on the fringe of the system. She has invited you along.", 10f)
            else -> {}
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        when (currentStage) {
            Stage.CONFRONT -> info.addPara("Speak with Master Helensis at Battlestation Cygnus", tc, pad)
            Stage.SHAKEDOWN -> info.addPara(if (departed) "The Ordained Squadron is hunting the pirates on the system's fringe"
                else "The Ordained Squadron awaits orders at Cygnus", tc, pad)
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = "Shakedown"
}
