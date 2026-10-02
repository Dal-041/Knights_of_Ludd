package org.selkie.kol.campaign.missions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.kol.helpers.KolStaticStrings.KolPrelude
import org.selkie.kol.world.GenerateKnights
import org.selkie.zea.helpers.ZeaStaticStrings
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Knights prelude, steps 2 and 3: find Battlestar Libra from an approximate location, deliver relief cargo
 * (the Knights pay 80% up front), deal with a Dawn scouting party, then report to Brother Enarms.
 * Rules call: checkCargo, deliver, martinsTrade, complete. Dialogue: rules.csv, kolPreludeLibra_*.
 */
class KolPreludeLibra : HubMissionWithSearch() {

    enum class Stage { DELIVER, REPORT, COMPLETED }

    companion object {
        val CARGO = linkedMapOf(
            Commodities.SUPPLIES to 100,
            Commodities.FOOD to 300,
            Commodities.HEAVY_MACHINERY to 20,
        )
        const val STIPEND_FRACTION = 0.8f
        const val HYPER_OFFSET_LY = 4f
        const val SCOUT_FLEET_POINTS = 30f

        /** What Martins pushes on the player in trade at Libra ("Trouble and scrap."). */
        val MARTINS_TRADE = linkedMapOf(
            Commodities.HAND_WEAPONS to 40,
            Commodities.METALS to 100,
        )

        fun cargoBaseCost(): Int = CARGO.entries.sumOf { (id, qty) ->
            (Global.getSettings().getCommoditySpec(id).basePrice * qty).toInt()
        }
    }

    private var enarms: PersonAPI? = null
    private var libra: SectorEntityToken? = null
    private var libraSystem: StarSystemAPI? = null
    private var knownAtStart = false
    private var stipend = 0
    private var tradeGiven = false

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        val knights = Global.getSector().getFaction(KolStaticStrings.kolFactionID) ?: return false
        if (!knights.relToPlayer.isAtWorst(RepLevel.NEUTRAL)) return false
        if (!Global.getSector().memoryWithoutUpdate.getBoolean(KolPrelude.PIRATE_COMPLETE)) return false
        if (!setGlobalReference(KolPrelude.LIBRA_REF, KolPrelude.LIBRA_ACTIVE)) return false

        GenerateKnights.ensureStoryState() // Libra and its people, if this save predates them
        enarms = Global.getSector().importantPeople.getPerson(KolPrelude.ENARMS_ID) ?: return false
        val libraMarket = GenerateKnights.getLibraMarket() ?: return false
        libra = libraMarket.primaryEntity
        libraSystem = libra?.starSystem ?: return false
        val entity = libra!!
        val system = libraSystem!!

        knownAtStart = !entity.isDiscoverable
        stipend = (cargoBaseCost() * STIPEND_FRACTION).toInt()

        setStartingStage(Stage.DELIVER)
        addSuccessStages(Stage.COMPLETED)
        setStoryMission()

        // Map marker: exact once Libra is known, otherwise only the general area
        val flag = "\$kolPreludeLibra_deliver"
        when {
            knownAtStart -> makeImportant(entity, flag, Stage.DELIVER)
            system.constellation != null ->
                makeImportant(entity.memoryWithoutUpdate, flag, MapLocationType.CONSTELLATION, entity, Stage.DELIVER)
            else -> {
                // No constellation: BaseHubMission would fall back to Libra itself, so pin a point in
                // hyperspace a few light-years off the system instead, and flag Libra without a pin.
                val angle = genRandom.nextFloat() * Math.PI * 2
                val dist = HYPER_OFFSET_LY * Misc.getUnitsPerLightYear()
                val anchor = system.location
                val token = Global.getSector().hyperspace.createToken(
                    anchor.x + (cos(angle) * dist).toFloat(), anchor.y + (sin(angle) * dist).toFloat())
                makeImportant(token, "\$kolPreludeLibra_area", Stage.DELIVER)
                makeImportantDoNotShowAsIntelMapLocation(entity, flag, Stage.DELIVER)
            }
        }
        // Martins: highlighted in Libra's comm directory, without a map pin that would give Libra away
        val martins = Global.getSector().importantPeople.getPerson(KolPrelude.MARTINS_ID)
        if (martins != null) makeImportantDoNotShowAsIntelMapLocation(martins, "\$kolPreludeLibra_martins", Stage.DELIVER)
        makeImportant(enarms!!, "\$kolPreludeLibra_report", Stage.REPORT)
        setStageOnGlobalFlag(Stage.REPORT, KolPrelude.LIBRA_DELIVERED)
        setStageOnGlobalFlag(Stage.COMPLETED, KolPrelude.LIBRA_DONE)

        // Dawn scouts: spawn once when the player enters Libra's system during the delivery,
        // on a point far from Libra; leashed, never jumping out, not picking fights with anyone but the player.
        val post = pickScoutPost(system, entity)
        beginEnteredLocationTrigger(system, Stage.DELIVER)
        // a token presence, not a real threat: ~30 FP is about two Dawn frigates (Glare/Aglow are 12-14 FP),
        // fixed rather than a size fraction so it doesn't scale up with the player
        triggerCreateFleet(FleetSize.TINY, FleetQuality.DEFAULT, ZeaStaticStrings.dawnID, FleetTypes.PATROL_SMALL, system)
        triggerSetFleetCombatFleetPoints(SCOUT_FLEET_POINTS)
        triggerSetFleetOfficers(OfficerNum.FC_ONLY, OfficerQuality.AI_GAMMA)
        triggerMakeHostileAndAggressive()
        triggerMakeFleetNotIgnorePlayer()
        triggerMakeFleetIgnoreOtherFleets()
        triggerMakeFleetIgnoredByOtherFleets()
        triggerFleetSetName("Dawn Scouts")
        triggerFleetSetNoFactionInName()
        triggerPickLocationAroundEntity(post, 400f)
        triggerSpawnFleetAtPickedLocation("\$kolPreludeLibra_scouts", null)
        triggerOrderFleetPatrol(post)
        triggerFleetSetPatrolLeashRange(800f)
        triggerFleetNoJump()
        endTrigger()

        setCreditReward(CreditReward.AVERAGE)
        setRepFactionChangesMedium()
        setRepPersonChangesMedium()
        return true
    }

    /** The planet (or jump point) in the system farthest from Libra, so the scouts sit well outside its reach. */
    private fun pickScoutPost(system: StarSystemAPI, libra: SectorEntityToken): SectorEntityToken {
        val candidates = system.planets.filter { !it.isStar } + system.jumpPoints
        return candidates.maxByOrNull { Misc.getDistance(it.location, libra.location) } ?: system.center
    }

    override fun callAction(action: String?, ruleId: String?, dialog: InteractionDialogAPI?,
                            params: MutableList<com.fs.starfarer.api.util.Misc.Token>?,
                            memoryMap: MutableMap<String, MemoryAPI>?): Boolean {
        when (action) {
            "checkCargo" -> {
                val local = memoryMap?.get(MemKeys.LOCAL) ?: return true
                local.set("\$kolPreludeLibra_hasCargo", missingCargo().isEmpty(), 0f)
                local.set("\$kolPreludeLibra_missing", missingCargoText(), 0f)
                return true
            }
            "deliver" -> {
                if (missingCargo().isNotEmpty()) return false
                val cargo = Global.getSector().playerFleet.cargo
                for ((id, qty) in CARGO) {
                    cargo.removeCommodity(id, qty.toFloat())
                    dialog?.textPanel?.let { AddRemoveCommodity.addCommodityLossText(id, qty, it) }
                }
                Global.getSector().memoryWithoutUpdate.set(KolPrelude.LIBRA_DELIVERED, true)
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
            "martinsTrade" -> {
                // Martins' goods in exchange for the relief cargo; once only
                if (tradeGiven) return true
                tradeGiven = true
                val cargo = Global.getSector().playerFleet.cargo
                for ((id, qty) in MARTINS_TRADE) {
                    cargo.addCommodity(id, qty.toFloat())
                    dialog?.textPanel?.let { AddRemoveCommodity.addCommodityGainText(id, qty, it) }
                }
                return true
            }
            "complete" -> {
                Global.getSector().memoryWithoutUpdate.set(KolPrelude.LIBRA_DONE, true)
                checkStageChangesAndTriggers(dialog, memoryMap)
                return true
            }
        }
        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        // permanent: the mission's own flags are unset when it ends
        val memory = Global.getSector().memoryWithoutUpdate
        memory.set(KolPrelude.LIBRA_COMPLETE, true)
        memory.set(KolPrelude.PRELUDE_DONE, true)
    }

    private fun missingCargo(): Map<String, Int> {
        val cargo = Global.getSector().playerFleet.cargo
        return CARGO.mapValues { (id, qty) -> qty - cargo.getCommodityQuantity(id).toInt() }.filterValues { it > 0 }
    }

    private fun missingCargoText(): String = missingCargo().entries.joinToString(", ") { (id, qty) ->
        "$qty ${Global.getSettings().getCommoditySpec(id).lowerCaseName}"
    }

    override fun updateInteractionDataImpl() {
        set("\$kolPreludeLibra_stipend", Misc.getDGSCredits(stipend.toFloat()))
        // The Knights fund most of the relief cargo; paid by AddCredits in kolPrelude_libraAccept
        set("\$kolPreludeLibra_stipendAmount", stipend)
        set("\$kolPreludeLibra_cargoCost", Misc.getDGSCredits(cargoBaseCost().toFloat()))
        set("\$kolPreludeLibra_known", knownAtStart)
        set("\$kolPreludeLibra_systemName", libraSystem?.nameWithLowercaseTypeShort ?: "")
        set("\$kolPreludeLibra_areaName", libraSystem?.constellation?.nameWithType ?: "the region")
    }

    override fun addDescriptionForNonEndStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val h = Misc.getHighlightColor()
        when (currentStage) {
            Stage.DELIVER -> {
                val where = if (knownAtStart) "Battlestar Libra in the ${libraSystem?.nameWithLowercaseTypeShort}"
                            else "Battlestar Libra, somewhere in ${libraSystem?.constellation?.nameWithType ?: "the marked region"}"
                info.addPara("Deliver relief supplies to $where. The Knights paid part of the cost; the rest is on you.", 10f)
                info.addPara("Required: %s supplies, %s food, %s heavy machinery.", 10f, h,
                    "${CARGO[Commodities.SUPPLIES]}", "${CARGO[Commodities.FOOD]}", "${CARGO[Commodities.HEAVY_MACHINERY]}")
            }
            Stage.REPORT -> info.addPara("Libra has its supplies. Report to ${enarms?.nameString} at ${enarms?.market?.name}.", 10f)
        }
    }

    override fun addNextStepText(info: TooltipMakerAPI, tc: Color?, pad: Float): Boolean {
        when (currentStage) {
            Stage.DELIVER -> info.addPara(if (knownAtStart) "Deliver the relief cargo to Battlestar Libra"
                                          else "Find Battlestar Libra and deliver the relief cargo", tc, pad)
            Stage.REPORT -> info.addPara("Report to ${enarms?.nameString} at ${enarms?.market?.name}", tc, pad)
            else -> return false
        }
        return true
    }

    override fun getBaseName(): String = "Relief for Libra"
}
