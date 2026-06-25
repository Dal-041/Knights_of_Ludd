package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI

class TamariskHmod: BaseHullMod() {
    companion object {
        const val PROJECTILE_SPEED_MULT = 1.25f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)

        stats?.projectileSpeedMult?.modifyMult(id, PROJECTILE_SPEED_MULT)
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        when (index) {
            0 -> return "${PROJECTILE_SPEED_MULT}x"
        }
        return null
    }
}