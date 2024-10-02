package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import org.selkie.kol.impl.helpers.ZeaStaticStrings.GfxCat

class DuskyShields1 : BaseHullMod() {

    private val INNERLARGE = Global.getSettings().getSpriteName(GfxCat.KOL_FX, "zea_shield_dawn")
    private val OUTERLARGE = Global.getSettings().getSpriteName(GfxCat.KOL_FX, "zea_shield_dawn")

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        if (ship.shield != null) {
            ship.shield.setRadius(ship.shieldRadiusEvenIfNoShield, INNERLARGE, OUTERLARGE)
        }
    }
}