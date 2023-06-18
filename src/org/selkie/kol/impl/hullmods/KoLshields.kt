package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

class KoLshields : BaseHullMod() {

    private val INNERLARGE = "graphics/fx/kol_shield_fx.png"
    private val OUTERLARGE = "graphics/fx/kol_shield_fx.png"
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
        }
    }
}