package org.selkie.kol.campaign.story.systems.desertion

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles
import com.fs.starfarer.api.util.WeightedRandomPicker
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import org.selkie.kol.campaign.story.core.KolStoryData
import org.selkie.kol.campaign.story.systems.KolStoryModule
import org.selkie.kol.campaign.story.core.KolStorySettings
import org.selkie.kol.campaign.story.systems.assembly.KolAssembly
import org.selkie.kol.campaign.story.systems.spawn.KolFleetSpawnHook
import org.selkie.kol.campaign.story.situations.KolSituationIntel
import org.selkie.kol.campaign.story.situations.libra.LibraKeys
import org.selkie.kol.campaign.story.situations.tech.TechKeys
import org.selkie.kol.campaign.story.situations.tech.milestones.ordained.OrdainedKeys
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.core.KolLog

/**
 * Knights deserting to the Path. Each month a rate (rising steeply as the Technology bar leads the Libra bar) is added
 * to a budget; large Path fleets spend it as they spawn, swapping vanilla hulls for Knights ships (Ordained ones after
 * the early rollout) under a deserter commander. The Path's faction data is never touched.
 */
class KolDesertionData {
    var budget = 0f
    var total = 0
    var fleetsUpgraded = 0

    companion object {
        fun get(): KolDesertionData {
            return KolStoryData.get(DesertionKeys.DESERTION_DATA_KEY) { KolDesertionData() }
        }
    }
}

object KolDesertion {

    private fun progressOf(key: String): Int? =
        (Global.getSector().memoryWithoutUpdate.get(key) as? KolSituationIntel)?.progress

    /** Deserters per month at the current balance; 0 until both situations exist. */
    fun monthlyRate(): Float {
        val tech = progressOf(TechKeys.SITUATION_KEY) ?: return 0f
        val libra = progressOf(LibraKeys.SITUATION_KEY) ?: return 0f
        val lead = max(0, tech - libra).toFloat()
        val rate = KolStorySettings.desertionBase * exp(KolStorySettings.desertionK * lead / KolStorySettings.desertionScale)
        return min(KolStorySettings.desertionCap, rate)
    }

    /** Monthly accrual (transient listener, registered at each load). */
    class Tick : EconomyTickListener {
        override fun reportEconomyTick(iterIndex: Int) {}
        override fun reportEconomyMonthEnd() {
            KolDesertionData.get().budget += monthlyRate()
        }
    }

    /** Fleet-spawn modifier: upgrades large Path fleets while the budget allows. */
    class Modifier : KolFleetSpawnHook.Modifier {
        override fun apply(fleet: CampaignFleetAPI): Boolean {
            if (fleet.faction?.id != Factions.LUDDIC_PATH) return false
            if (fleet.fleetPoints < KolStorySettings.desertionMinFleetPoints) return false
            val data = KolDesertionData.get()
            val allowed = min(data.budget.toInt(), KolStorySettings.desertionPerFleetCap)
            if (allowed < 1) return false

            val candidates = fleet.fleetData.membersListCopy
                .filter { !it.isFighterWing && !it.isStation && it.hullSpec.sourceMod == null && !it.isFlagship }
                .sortedByDescending { it.fleetPointCost }
            val ordained = Global.getSector().memoryWithoutUpdate.getBoolean(OrdainedKeys.ORDAINED_EARLY)
            var swapped = 0
            var strongest: FleetMemberAPI? = null
            for (old in candidates) {
                if (swapped >= allowed) break
                val variant = pickKnightsVariant(old.hullSpec.hullSize) ?: continue
                val member = KolFleetSpawnHook.swapMember(fleet, old, variant) ?: continue
                if (ordained && Math.random() < KolStorySettings.desertionOrdainedShare) KolFleetSpawnHook.convertMember(member)
                if (strongest == null || member.fleetPointCost > strongest.fleetPointCost) strongest = member
                swapped++
            }
            if (swapped == 0) return false

            strongest?.let { addDeserterCommander(fleet, it) }
            fleet.addTag(DesertionKeys.DESERTER_FLEET_TAG)
            KolFleetSpawnHook.finish(fleet)
            data.budget -= swapped
            data.total += swapped
            data.fleetsUpgraded++
            KolAssembly.report("deserters", swapped)
            KolLog.info("desertion", "${swapped} Knights deserters joined ${fleet.name} (budget left ${data.budget})")
            return true
        }

        private fun pickKnightsVariant(size: HullSize): String? {
            val role = when (size) {
                HullSize.FRIGATE -> ShipRoles.COMBAT_SMALL
                HullSize.DESTROYER -> ShipRoles.COMBAT_MEDIUM
                HullSize.CRUISER -> ShipRoles.COMBAT_LARGE
                HullSize.CAPITAL_SHIP -> ShipRoles.COMBAT_CAPITAL
                else -> return null
            }
            val knights = Global.getSector().getFaction(KolIds.FACTION) ?: return null
            val picker = WeightedRandomPicker<String>()
            knights.getVariantsForRole(role)?.forEach { picker.add(it) }
            return picker.pick()
        }

        /** A Knight who went over: a Knights face, now of the Path, commanding from the strongest swapped ship. */
        private fun addDeserterCommander(fleet: CampaignFleetAPI, member: FleetMemberAPI) {
            val knights = Global.getSector().getFaction(KolIds.FACTION) ?: return
            val person = knights.createRandomPerson()
            person.setFaction(Factions.LUDDIC_PATH)
            person.stats.level = max(person.stats.level, 5)
            member.captain = person
            fleet.fleetData.setFlagship(member)
            fleet.commander = person
        }
    }
}

/** Knights deserting to the Path (kept through severance). */
object KolDesertionModule : KolStoryModule {
    override val id = "desertion"
    override fun onLoad() {
        com.fs.starfarer.api.Global.getSector().listenerManager.addListener(KolDesertion.Tick(), true)
    }
    override fun spawnModifiers(): Map<String, KolFleetSpawnHook.Modifier> = mapOf("desertion" to KolDesertion.Modifier())
}
