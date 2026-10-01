package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolPrelude
import java.awt.Color

/**
 * Knights prelude, step 1: Brother Enarms asks the player to beat a pirate band harassing Knight traffic.
 * Beating it fires the rules.csv trigger KolPreludePirateDefeated (the dossier scene), which sets
 * $kolPreludePirate_beaten; reporting to Enarms sets $kolPreludePirate_done and offers him as a contact.
 * Dialogue: rules.csv, kolPreludePirate_*.
 */
class KolPreludePirate : HubMissionWithSearch() {

    enum class Stage { FIND_PIRATES, REPORT, COMPLETED }

    companion object {
        const val FLEET_NAME = "Three Prince Gang"
    }

    private var enarms: PersonAPI? = null
    private var target: StarSystemAPI? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID) ?: return false
        if (!knights.relToPlayer.isAtWorst(RepLevel.NEUTRAL)) return false
        if (!setGlobalReference(KolPrelude.PIRATE_REF, KolPrelude.PIRATE_ACTIVE)) return false

        enarms = getImportantPerson(KolPrelude.ENARMS_ID)
        val home = enarms?.market ?: return false
        val homeLoc = home.primaryEntity.locationInHyperspace

        // a system near Knights space where raiders could plausibly lurk
        requireSystemInterestingAndNotUnsafeOrCore()
        requireSystemNot(home.starSystem)
        requireSystemWithinRangeOf(homeLoc, 12f)
        preferSystemWithinRangeOf(homeLoc, 6f)
        target = pickSystem()
        if (target == null) {
            resetSearch()
            requireSystemInterestingAndNotUnsafeOrCore()
            requireSystemNot(home.starSystem)
            preferSystemWithinRangeOf(homeLoc, 15f)
            target = pickSystem()
        }
        val system = target ?: return false

        setStartingStage(Stage.FIND_PIRATES)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()

        makeImportant(system.center, "\$kolPreludePirate_target", Stage.FIND_PIRATES)
        makeImportant(enarms!!, "\$kolPreludePirate_report", Stage.REPORT)
        setStageOnGlobalFlag(Stage.REPORT, KolPrelude.PIRATE_BEATEN)
        setStageOnGlobalFlag(Stage.COMPLETED, KolPrelude.PIRATE_DONE)

        beginStageTrigger(Stage.FIND_PIRATES)
        triggerCreateFleet(FleetSize.MEDIUM, FleetQuality.DEFAULT, Factions.PIRATES, FleetTypes.PATROL_MEDIUM, system)
        triggerSetFleetOfficers(OfficerNum.DEFAULT, OfficerQuality.DEFAULT)
        triggerSetStandardAggroPirateFlags()
        triggerFleetSetName(FLEET_NAME)
        triggerFleetSetNoFactionInName()
        triggerPickLocationAroundEntity(system.center, 3000f)
        triggerSpawnFleetAtPickedLocation("\$kolPreludePirate_fleet", null)
        triggerOrderFleetPatrol(system)
        triggerFleetNoJump()
        triggerFleetMakeImportant(null, Stage.FIND_PIRATES)
        triggerFleetAddDefeatTrigger(KolPrelude.PIRATE_DEFEAT_TRIGGER)
        endTrigger()

        setCreditReward(CreditReward.LOW)
        setRepFactionChangesLow()
        setRepPersonChangesLow()
        return true
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        // permanent: the mission's own flags are unset when it ends
        Global.getSector().memoryWithoutUpdate.set(KolPrelude.PIRATE_COMPLETE, true)
    }

    override fun updateInteractionDataImpl() {
        set("\$kolPreludePirate_systemName", target?.nameWithLowercaseTypeShort ?: "")
        set("\$kolPreludePirate_fleetName", FLEET_NAME)
        set("\$kolPreludePirate_reward", com.fs.starfarer.api.util.Misc.getDGSCredits(creditsReward.toFloat()))
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.FIND_PIRATES -> info.addPara("${enarms?.nameString} wants the $FLEET_NAME, a pirate band harassing " +
                    "Knight traffic, dealt with. They were last seen in the ${target?.nameWithLowercaseTypeShort}.", 10f)
            Stage.REPORT -> info.addPara("The $FLEET_NAME are beaten, and their dossier on Knight shipping raises questions. " +
                    "Report to ${enarms?.nameString} at ${enarms?.market?.name}.", 10f)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        when (currentStage) {
            Stage.FIND_PIRATES -> info.addPara("Defeat the $FLEET_NAME in the ${target?.nameWithLowercaseTypeShort}", tc, pad)
            Stage.REPORT -> info.addPara("Report to ${enarms?.nameString} at ${enarms?.market?.name}", tc, pad)
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = "Raiders on the Pilgrim Lanes"
}
