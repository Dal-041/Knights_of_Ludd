package org.selkie.zea.campaign.coffinlink

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.FleetDataAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.Misc
import org.selkie.zea.helpers.ZeaStaticStrings.CoffinLink

/**
 * While the player has the COFFIN Link skill, keeps one hidden COFFIN Link commodity in cargo on the
 * fleet and refit tabs so it appears in the automated-ship captain picker, and removes it everywhere
 * else so it never shows up in trade or cargo screens. Transient: registered in onGameLoad.
 * Adapted from Tahlan Shipworks' DigitalSoulScript (by Nia).
 */
class CoffinLinkScript : EveryFrameScript {

    companion object {
        // SotF's Sierra is a special officer on her own ship; never touch her
        const val SOTF_SIERRA = "sotf_sierra"
    }

    // AI cores in command of ships as of last frame. Moving the player onto one of those ships deletes
    // the core instead of returning it to cargo, so it is put back here.
    private val coredShips = HashMap<FleetMemberAPI, String>()
    private var wasInTab = false

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        val sector = Global.getSector()
        val fleet = sector.playerFleet ?: return
        val cargo = fleet.cargo

        removeLink(cargo)
        if (!sector.playerStats.hasSkill(CoffinLink.SKILL_ID)) return

        // Being picked through an AI core plugin marks the player as that core; undo it
        sector.playerPerson.aiCoreId = null

        // Captains only change through the fleet/refit screens, so the fleet scan runs there
        // (plus one frame after leaving, to catch a change made on the last frame)
        val tab = sector.campaignUI.currentCoreTab
        val inTab = tab == CoreUITabId.FLEET || tab == CoreUITabId.REFIT
        if (inTab) cargo.addCommodity(CoffinLink.COMMODITY_ID, 1f)
        if (inTab || wasInTab) {
            fixCaptains(fleet.fleetData, cargo)
        } else {
            coredShips.clear()
        }
        wasInTab = inTab
    }

    private fun fixCaptains(fleetData: FleetDataAPI, cargo: CargoAPI) {
        for ((member, coreId) in coredShips) {
            if (member.captain.isPlayer) cargo.addCommodity(coreId, 1f)
        }
        coredShips.clear()

        for (member in fleetData.membersListCopy) {
            if (member.isFighterWing || !Misc.isAutomated(member)) continue
            val captain = member.captain ?: continue
            if (captain.id == SOTF_SIERRA) continue
            if (captain.isAICore) {
                val coreId = captain.aiCoreId
                if (coreId != null) coredShips[member] = coreId
            } else if (!captain.isPlayer && !captain.isDefault && !Misc.isUnremovable(captain)
                && fleetData.getOfficerData(captain) != null) {
                // The captain picker's "assign the player" path hands the player's old ship to the displaced
                // officer without an automated check. Unassign them; they stay in the fleet's officer list.
                member.captain = null
            }
        }
    }

    private fun removeLink(cargo: CargoAPI) {
        val quantity = cargo.getCommodityQuantity(CoffinLink.COMMODITY_ID)
        if (quantity > 0f) cargo.removeCommodity(CoffinLink.COMMODITY_ID, quantity)
    }
}
