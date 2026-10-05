package org.selkie.kol.campaign.story.ch2

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
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
import org.selkie.zea.helpers.ZeaStaticStrings
import java.awt.Color

private val memory get() = Global.getSector().memoryWithoutUpdate

/**
 * Securing a patron for the Church (raised at the convocation). Doors: Mazalot (Persean League), Jangala (Hegemony),
 * Volturn (Sindrian Diktat) while their powers hold them, else independent Luddic Majority worlds; or the player's
 * own faction, offered through Enarms. Rules: kolCh2_patron*.
 */
class KolCh2Patron : HubMissionWithSearch() {
    companion object { const val STAGE_DONE = "\$kolCh2Patron_stageDone" }

    enum class Stage { SECURE, COMPLETED }

    private val doors = ArrayList<MarketAPI>()

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(KolCh2.PATRON_REF, KolCh2.PATRON_ACTIVE)) return false
        setStartingStage(Stage.SECURE)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        doors.addAll(KolCh2Story.openDoors().ifEmpty { KolCh2Story.fallbackDoors() })
        for (door in doors) makeImportant(door.primaryEntity, KolCh2.PATRON_DOOR, Stage.SECURE)
        // stage flags are unset when a mission ends, so missions complete on their own flag, never a permanent one
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        return true
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        when (action) {
            "secure" -> {
                val factionId = dialog?.interactionTarget?.market?.factionId ?: return false
                secure(factionId, dialog, memoryMap)
                return true
            }
            "secureOwn" -> {
                secure(Factions.PLAYER, dialog, memoryMap)
                return true
            }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    /** The player's own faction may stand as patron with a colony and a strong enough fleet. */
    private fun canOfferOwn(): Boolean = KolCh2Story.canOfferOwn()
    // Conditions don't go through Call: BaseHubMission.callEvent treats a false return as an unhandled action and
    // throws. Rules use `KolCh2CMD canOfferOwn` instead.

    private fun secure(factionId: String, dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        memory.set(KolCh2.PATRON, factionId)
        memory.set(KolCh2.PATRON_SECURED, true)
        memory.set(STAGE_DONE, true)
        if (factionId != Factions.PLAYER) {
            val faction = Global.getSector().getFaction(factionId)
            faction?.adjustRelationship(Factions.PLAYER, 0.1f)
            dialog?.textPanel?.addPara("Relations with ${faction?.displayNameWithArticle} improved",
                Misc.getPositiveHighlightColor())
        }
        KolAssembly.report("patron")
        checkStageChangesAndTriggers(dialog, memoryMap)
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        if (currentStage != Stage.SECURE) return
        info.addPara("[PLACEHOLDER] The assembly at Star Keep Lyra has charged you with finding the Church a secular " +
                "patron while the Order's fleets are committed. Luddic communities under other powers may broker it.", 10f)
        for (door in doors) info.addPara("${door.name} (${door.faction.displayName})", 3f)
        if (canOfferOwn()) info.addPara("Your own forces may be strong enough to offer instead; speak to Brother Enarms.", 10f)
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        if (currentStage != Stage.SECURE) return false
        info.addPara("Seek a patron for the Church", tc, pad)
        return true
    }

    override fun getBaseName(): String = "A Patron for the Church"
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
 * The Order investigates and memorializes Ozymandias (from Enarms after the convocation): Mourn, Abandon, Forget and
 * Rest, then the Caeli cryosleeper, where the player decides the sleepers' fate; ends with a memorial beacon at Mourn.
 * Stops are flagged on their entities and handled by high-score OpenInteractionDialog rules (kolCh2_oz*).
 */
class KolCh2Ozymandias : HubMissionWithSearch() {
    companion object { const val STAGE_DONE = "\$kolCh2Oz_stageDone" }

    enum class Stage { SURVEY, CAELI, COMPLETED }

    private var system: StarSystemAPI? = null
    private val stops = ArrayList<SectorEntityToken>()
    private var caeli: SectorEntityToken? = null
    private var visited = 0

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        if (!setGlobalReference(KolCh2.OZY_REF, KolCh2.OZY_ACTIVE)) return false
        system = Global.getSector().getStarSystem(ZeaStaticStrings.ozymandiasSysName) ?: return false
        val ids = listOf(ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_ONE, ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_TWO,
            ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_THREE, ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_FOUR)
        ids.mapNotNullTo(stops) { system!!.getEntityById(it) }
        if (stops.isEmpty()) return false
        caeli = system!!.customEntities.firstOrNull { it.customEntityType == Entities.DERELICT_CRYOSLEEPER }

        setStartingStage(Stage.SURVEY)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()
        setNoRepChanges()
        setStageOnGlobalFlag(Stage.CAELI, "\$kolCh2Oz_surveyed")
        setStageOnGlobalFlag(Stage.COMPLETED, STAGE_DONE)
        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        memory.set("\$kolCh2Oz_wasExplored", KolChronicle.has("ozymandiasVisited"))
        for (stop in stops) {
            stop.memoryWithoutUpdate.set(KolCh2.OZY_STOP, true)
            Misc.makeImportant(stop, "kolCh2Oz")
        }
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
                if (visited >= stops.size) surveyed(dialog, memoryMap)
                return true
            }
            "caeli" -> {
                val choice = params?.getOrNull(1)?.getString(memoryMap) ?: return false
                memory.set(KolCh2.CAELI_CHOICE, choice)
                caeli?.memoryWithoutUpdate?.unset(KolCh2.OZY_CAELI)
                caeli?.let { Misc.makeUnimportant(it, "kolCh2Oz") }
                finish(dialog, memoryMap)
                return true
            }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    private fun surveyed(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        val sleeper = caeli
        if (sleeper == null) {
            finish(dialog, memoryMap)
            return
        }
        sleeper.memoryWithoutUpdate.set(KolCh2.OZY_CAELI, true)
        Misc.makeImportant(sleeper, "kolCh2Oz")
        memory.set("\$kolCh2Oz_surveyed", true)
        checkStageChangesAndTriggers(dialog, memoryMap)
    }

    private fun finish(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        layMemorial()
        memory.set(KolCh2.OZY_DONE, true)
        memory.set(STAGE_DONE, true)
        KolAssembly.report("ozymandias")
        checkStageChangesAndTriggers(dialog, memoryMap)
    }

    private fun layMemorial() {
        val sys = system ?: return
        val mourn = sys.getEntityById(ZeaStaticStrings.ZeaEntities.ZEA_OZYMANDIAS_PLANET_ONE) ?: return
        val beacon = sys.addCustomEntity(null, "Memorial of the Order", Entities.WARNING_BEACON, KolStaticStrings.kolFactionID)
        beacon.setCircularOrbitPointingDown(mourn, 90f, mourn.radius + 250f, 60f)
    }

    override fun notifyEnding() {
        super.notifyEnding()
        for (stop in stops) {
            stop.memoryWithoutUpdate.unset(KolCh2.OZY_STOP)
            Misc.makeUnimportant(stop, "kolCh2Oz")
        }
        caeli?.memoryWithoutUpdate?.unset(KolCh2.OZY_CAELI)
        caeli?.let { Misc.makeUnimportant(it, "kolCh2Oz") }
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.SURVEY -> info.addPara("[PLACEHOLDER] Brother Enarms asks you to carry the Order's eyes to Ozymandias: " +
                    "visit its four worlds, Mourn, Abandon, Forget and Rest ($visited of ${stops.size} visited).", 10f)
            Stage.CAELI -> info.addPara("[PLACEHOLDER] The worlds are seen. What remains is the cryosleeper, and the people still in it.", 10f)
            else -> {}
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        when (currentStage) {
            Stage.SURVEY -> info.addPara("Visit the worlds of Ozymandias ($visited/${stops.size})", tc, pad)
            Stage.CAELI -> info.addPara("Visit the Caeli cryosleeper", tc, pad)
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
