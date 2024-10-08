package org.selkie.zea.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize

class HardcodedOffsetShields : BaseHullMod() {
    // actual max arcs to cover front of ship with offset shields, stats lie and say 180 degrees
    val maxArcMap = mapOf(
        "zea_edf_ryujin" to 160f,
        "zea_edf_kiyohime" to 170f,
        "zea_dusk_ayakashi" to 140f)

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {

        ship?.let { s ->
            if (ship.originalOwner == -1) return
            val baseHullId = s.hullSpec?.baseHullId
            val maxArc = maxArcMap[baseHullId] ?: return
            val currentArc = s.shield?.arc ?: return

            if (currentArc > maxArc) {
                s.shield.arc = maxArc
            }

            if(s.shield.type == ShieldType.OMNI) s.shield.forceFacing(s.facing)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String {
        return "180"
    }
}