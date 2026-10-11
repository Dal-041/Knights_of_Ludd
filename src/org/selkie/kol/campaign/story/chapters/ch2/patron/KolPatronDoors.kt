package org.selkie.kol.campaign.story.chapters.ch2.patron

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.util.Misc
import org.selkie.kol.campaign.story.core.KolIds
import org.selkie.kol.campaign.story.core.KolStorySettings

/** The worlds of the faithful the convocation names, and the player's own forces. */
object KolPatronDoors {
    private val memory get() = Global.getSector().memoryWithoutUpdate

    /** The worlds whose Luddic congregations the convocation names, with the power that must hold them. */
    val PATRON_DOORS = linkedMapOf("mazalot" to Factions.PERSEAN, "jangala" to Factions.HEGEMONY, "volturn" to Factions.DIKTAT)

    /**
     * The player's own forces can stand as the Church's patron (offered only at the convocation): a colony and enough
     * fleet, and the player can commit them unilaterally: not a League member, and not holding the commission of a
     * faction hostile to the Knights.
     */
    fun canOfferOwn(): Boolean {
        if (Misc.getPlayerMarkets(true).isEmpty()) return false
        if (Global.getSector().playerFleet.fleetPoints < KolStorySettings.patronOwnFleetPoints) return false
        if (PerseanLeagueMembership.isLeagueMember()) return false
        val commission = Misc.getCommissionFactionId() ?: return true
        return !Global.getSector().getFaction(commission).isHostileTo(KolIds.FACTION)
    }

    fun openDoors(): List<MarketAPI> = PATRON_DOORS.mapNotNull { (marketId, faction) ->
        Global.getSector().economy.getMarket(marketId)?.takeIf { it.factionId == faction && !it.isHidden && it.primaryEntity != null }
    }
}
