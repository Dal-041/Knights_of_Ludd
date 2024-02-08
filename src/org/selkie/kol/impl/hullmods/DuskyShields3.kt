package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

class DuskyShields3 : BaseHullMod() {

    private val INNERLARGE = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/fx/zea_shield_elysia.png"
    private val OUTERLARGE = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/fx/zea_shield_elysia.png"
    private val INNERLARGE2 = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/fx/zea_shield_elysia_2.png"
    private val OUTERLARGE2 = "data/strings/com/fs/starfarer/api/impl/campaign/you can hear it cant you/our whispers through the void/our song/graphics/fx/zea_shield_elysia_2.png"

    private val conformalShieldsID = "kol_conformal_shield"

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            if (ship.variant.hasHullMod(conformalShieldsID)) {
                ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE2, OUTERLARGE2)
            } else {
                ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
            }
        }
    }
}