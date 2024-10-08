package org.selkie.zea.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat

class DawnShieldStyle : BaseHullMod() {

    private val INNERLARGE = Global.getSettings().getSpriteName(GfxCat.FX, "zea_shield_dawn")
    private val OUTERLARGE = Global.getSettings().getSpriteName(GfxCat.FX, "zea_shield_dawn")

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
        }
    }
}