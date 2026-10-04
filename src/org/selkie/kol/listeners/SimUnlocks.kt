package org.selkie.kol.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.SimulatorPluginImpl
import com.fs.starfarer.api.impl.campaign.intel.misc.SimUpdateIntel
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.helpers.KolStaticStrings
import org.selkie.zea.helpers.ZeaStaticStrings

/**
 * Adds to the simulator's unlocks after battles the player takes part in, alongside vanilla's own learning
 * (CoreScript -> SimulatorPluginImpl.reportPlayerBattleOccurred):
 * - Vanilla credits each enemy fleet to one best-matching faction, so ships of a second faction mixed into a fleet
 *   (Abyss fleets) are learned without their faction and land in "Other". Here every one of our factions that
 *   fields a destroyed ship is unlocked too.
 * - Vanilla learns only from enemies destroyed. Knights ships are also learned from allied Knights fleets that
 *   fight on the player's side.
 *
 * Unlocks live in saves/common (shared across saves), not in the save. Transient; registered on each load,
 * which also runs [unlockFactionsOfLearnedShips] to repair unlocks learned before this listener existed.
 */
class SimUnlocks : BaseCampaignEventListener(false) {

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        if (battle == null || !battle.isPlayerInvolved) return
        // another mod may replace the simulator plugin; only extend vanilla's
        val sim = Misc.getSimulatorPlugin() as? SimulatorPluginImpl ?: return

        val enemyLost = battle.nonPlayerSideSnapshot.flatMap { Misc.getSnapshotMembersLost(it) }
        val alliedKnights = battle.playerSideSnapshot
            .filter { it != Global.getSector().playerFleet && it.faction?.id == KolStaticStrings.kolFactionID }
            .flatMap { it.fleetData.snapshot }

        // reload first: vanilla may not have loaded the file yet, or may already have saved this battle's unlocks
        sim.loadUnlocksData()
        val data = sim.unlocksData
        val addedFactions = LinkedHashSet<String>()
        val addedVariants = LinkedHashSet<String>()

        for (member in enemyLost + alliedKnights) {
            val vid = sim.getStockVariantId(member) ?: continue
            for (factionId in factionsFielding(vid)) {
                if (data.factions.add(factionId)) addedFactions.add(factionId)
            }
        }
        for (member in alliedKnights) {
            val vid = sim.getStockVariantId(member) ?: continue
            if (data.variants.add(vid)) addedVariants.add(vid)
        }

        if (addedFactions.isEmpty() && addedVariants.isEmpty()) return
        sim.saveUnlocksData()
        SimUpdateIntel(addedFactions, addedVariants)
    }

    companion object {
        /** Unlocks any of our factions with ships already learned (e.g. Elysian ships sitting in "Other"). */
        @JvmStatic
        fun unlockFactionsOfLearnedShips() {
            val sim = Misc.getSimulatorPlugin() as? SimulatorPluginImpl ?: return
            sim.loadUnlocksData()
            val data = sim.unlocksData
            val added = LinkedHashSet<String>()
            for (vid in data.variants.toList()) {
                for (factionId in factionsFielding(vid)) if (data.factions.add(factionId)) added.add(factionId)
            }
            if (added.isNotEmpty()) sim.saveUnlocksData()
        }

        /** Which of our simulator factions use this variant in a ship role. */
        private fun factionsFielding(variantId: String): List<String> = FACTIONS.filter { id ->
            val faction = Global.getSector().getFaction(id) ?: return@filter false
            SimulatorPluginImpl.getAllRoles().any { role -> faction.getVariantsForRole(role)?.contains(variantId) == true }
        }

        private val FACTIONS = listOf(
            KolStaticStrings.kolFactionID,
            ZeaStaticStrings.dawnID,
            ZeaStaticStrings.duskID,
            ZeaStaticStrings.elysianID,
        )
    }
}
