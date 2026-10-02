package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolCh1
import org.selkie.kol.helpers.KolStaticStrings.KolPrelude
import org.selkie.zea.helpers.ZeaStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaStarTypes
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Knights Chapter 1: Enarms sends the player to observe a "gravo-electric anomaly" (a black pulsar).
 * Resolving any Dusk fleet in any black pulsar system to composition level sets $kolCh1Scout_scouted (-> REPORT).
 * Fighting Dusk during the mission and being pulled through a pulsar are recorded for the debrief
 * ($kolCh1_foughtDusk here, $kolCh1_pulledThrough in TrackFleet). The debrief calls "complete".
 * Dialogue: rules.csv, kolCh1_scout* / kolCh1_debrief*.
 */
class KolCh1Scout : HubMissionWithSearch() {

    enum class Stage { SCOUT, REPORT, COMPLETED }

    companion object {
        const val HYPER_OFFSET_LY = 4f
        const val CHECK_SECONDS = 0.5f

        fun isBlackPulsarSystem(loc: LocationAPI?): Boolean =
            (loc as? StarSystemAPI)?.star?.typeId == ZeaStarTypes.ZEA_STAR_BLACK_NEUTRON

        /** Any Dusk fleet in the player's current location seen at composition level or better. */
        fun playerHasResolvedDusk(): Boolean {
            val loc = Global.getSector().playerFleet?.containingLocation ?: return false
            if (!isBlackPulsarSystem(loc)) return false
            return loc.fleets.any {
                it.faction.id == ZeaStaticStrings.duskID &&
                    it.visibilityLevelToPlayerFleet.ordinal >= VisibilityLevel.COMPOSITION_DETAILS.ordinal
            }
        }
    }

    private var enarms: PersonAPI? = null
    private var target: StarSystemAPI? = null
    private var landmark: StarSystemAPI? = null
    private var battleListener: DuskBattleListener? = null
    @Transient private var interval: IntervalUtil? = null

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID) ?: return false
        if (!knights.relToPlayer.isAtWorst(RepLevel.NEUTRAL)) return false
        if (!setGlobalReference(KolCh1.SCOUT_REF, KolCh1.SCOUT_ACTIVE)) return false

        enarms = Global.getSector().importantPeople.getPerson(KolPrelude.ENARMS_ID) ?: return false
        val home = enarms!!.market?.primaryEntity?.locationInHyperspace ?: return false

        // the black pulsar nearest Cygnus: the one Knight expeditions would have tried first
        val system = Global.getSector().starSystems
            .filter { isBlackPulsarSystem(it) }
            .minByOrNull { Misc.getDistance(it.location, home) } ?: return false
        target = system

        setStartingStage(Stage.SCOUT)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()

        // Approximate marker: black pulsars have no constellation, so pin hyperspace a few light-years off
        val angle = genRandom.nextFloat() * Math.PI * 2
        val dist = HYPER_OFFSET_LY * Misc.getUnitsPerLightYear()
        val token = Global.getSector().hyperspace.createToken(
            system.location.x + (cos(angle) * dist).toFloat(), system.location.y + (sin(angle) * dist).toFloat())
        makeImportant(token, "\$kolCh1Scout_area", Stage.SCOUT)
        landmark = nearestCharted(token, system)

        makeImportant(enarms!!, "\$kolCh1Scout_report", Stage.REPORT)
        setStageOnGlobalFlag(Stage.REPORT, KolCh1.SCOUT_SCOUTED)
        setStageOnGlobalFlag(Stage.COMPLETED, KolCh1.SCOUT_DONE)

        setCreditReward(CreditReward.LOW)
        setRepFactionChangesLow()
        setRepPersonChangesLow()
        return true
    }

    /** The nearest ordinary, non-hidden system to the marker, for the dialogue's "near the X". */
    private fun nearestCharted(token: SectorEntityToken, exclude: StarSystemAPI): StarSystemAPI? =
        Global.getSector().starSystems
            .filter { it != exclude && !it.hasTag(Tags.THEME_HIDDEN) && !isBlackPulsarSystem(it) }
            .minByOrNull { Misc.getDistance(it.location, token.location) }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        battleListener = DuskBattleListener().also { Global.getSector().addListener(it) }
    }

    override fun notifyEnding() {
        super.notifyEnding()
        battleListener?.let { Global.getSector().removeListener(it) }
        battleListener = null
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)
        if (currentStage != Stage.SCOUT) return
        val timer = interval ?: IntervalUtil(CHECK_SECONDS, CHECK_SECONDS).also { interval = it }
        timer.advance(amount)
        if (!timer.intervalElapsed()) return
        if (playerHasResolvedDusk()) {
            Global.getSector().memoryWithoutUpdate.set(KolCh1.SCOUT_SCOUTED, true)
            checkStageChangesAndTriggers(null, null)
        }
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<Misc.Token>?, memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        when (action) {
            "complete" -> {
                Global.getSector().memoryWithoutUpdate.set(KolCh1.SCOUT_DONE, true)
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        Global.getSector().memoryWithoutUpdate.set(KolCh1.SCOUT_COMPLETE, true)
    }

    override fun updateInteractionDataImpl() {
        set("\$kolCh1Scout_landmark", landmark?.nameWithLowercaseTypeShort ?: "the outer dark")
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.SCOUT -> {
                info.addPara("${enarms?.nameString} has asked you to observe a \"gravo-electric anomaly\" that Knight " +
                        "expeditions have failed to scout. Its location is only roughly known: somewhere near " +
                        "${landmark?.nameWithLowercaseTypeShort ?: "the marked area"}.", 10f)
                info.addPara("Observe only. If anything there takes notice of you, leave.", 10f)
            }
            Stage.REPORT -> info.addPara("You have seen what waits at the anomaly. Report to ${enarms?.nameString} " +
                    "at ${enarms?.market?.name}.", 10f)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        when (currentStage) {
            Stage.SCOUT -> info.addPara("Find the anomaly and observe whatever is there", tc, pad)
            Stage.REPORT -> info.addPara("Report to ${enarms?.nameString} at ${enarms?.market?.name}", tc, pad)
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = "The Anomaly"

    /** Records a battle between the player and the Duskborne while the mission runs (saved with the sector). */
    class DuskBattleListener : BaseCampaignEventListener(false) {
        override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
            if (battle == null || !battle.isPlayerInvolved) return
            val memory = Global.getSector().memoryWithoutUpdate
            if (!memory.getBoolean(KolCh1.SCOUT_ACTIVE)) return
            if (battle.nonPlayerSideSnapshot.any { it.faction.id == ZeaStaticStrings.duskID }) {
                memory.set(KolCh1.FOUGHT_DUSK, true)
            }
        }
    }
}
