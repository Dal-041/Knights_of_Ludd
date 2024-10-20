package org.selkie.zea.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize

class ReverseEngineeredPhaseCoils : BaseHullMod()  {
    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.phaseCloakCooldownBonus.modifyMult(id, 1f - PHASE_COOLDOWN_REDUCTION / 100f)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) return "" + Math.round(PHASE_COOLDOWN_REDUCTION) + "%"
        return null
    }

    companion object {
        var PHASE_COOLDOWN_REDUCTION: Float = 50f
    }
}
