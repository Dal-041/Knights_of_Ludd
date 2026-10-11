package org.selkie.kol.campaign.story.chapters.ch2.jointops

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.helpers.ZeaStaticStrings
import java.awt.Color
import org.selkie.kol.campaign.story.core.KolFleets
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.chapters.ch2.patron.PatronKeys


private val memory get() = Global.getSector().memoryWithoutUpdate

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
        val carry = memory.getFloat(KolJointOps.JOINT_OP_CARRY)
        memory.unset(KolJointOps.JOINT_OP_CARRY)
        val fleet = KolFleets.combat(KolIds.FACTION, FleetTypes.TASK_FORCE, KolStorySettings.jointOpFleetPoints * (1f + carry)) ?: return
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
        Misc.makeUnimportant(fleet, "kolJointOperation")
        KolFleets.sendHome(fleet, KolIds.cygnus?.primaryEntity, "returning to Cygnus")
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        memory.set(doneFlag, true)
        KolAssembly.report("jointOperation")
    }
}

/** The first joint operation, from Greenflight: the Tri-Tachyon vessel at Alpha Site (Ninaya). */
class KolCh2Ninaya : KolJointOperation() {
    override val ref = JointOpsKeys.NINAYA_REF
    override val activeFlag = JointOpsKeys.NINAYA_ACTIVE
    override val doneFlag = JointOpsKeys.NINAYA_OP_DONE
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

/** Joint operations with the Order's squadrons (Ninaya first). */
object KolJointOpsModule : KolStoryModule {
    override val id = "jointops"
    private val memory get() = Global.getSector().memoryWithoutUpdate
    override fun requirements() = listOf(KolStoryModule.Requirement(2, "ninayaOperation",
        { memory.getBoolean(PatronKeys.PATRON_SECURED) }, { memory.getBoolean(JointOpsKeys.NINAYA_OP_DONE) }))
    override fun severedKeys() = listOf("\$kolJointOp", "\$kolNinaya")
}
