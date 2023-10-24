package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

class DuskyShields2 : BaseHullMod() {

    private val INNERLARGE = "graphics/fx/kol_shield_fx3.png"
    private val OUTERLARGE = "graphics/fx/kol_shield_fx3.png"

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
        }
    }
}