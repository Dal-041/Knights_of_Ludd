package org.selkie.kol.impl.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import org.selkie.kol.impl.helpers.ZeaStaticStrings.GfxCat

class DuskyShields3 : BaseHullMod() {

    private val INNERLARGE = Global.getSettings().getSpriteName(GfxCat.KOL_FX, "zea_shield_elysia")
    private val OUTERLARGE = Global.getSettings().getSpriteName(GfxCat.KOL_FX, "zea_shield_elysia")
    private val INNERLARGE2 = Global.getSettings().getSpriteName(GfxCat.KOL_FX, "zea_shield_elysia_2")
    private val OUTERLARGE2 = Global.getSettings().getSpriteName(GfxCat.KOL_FX, "zea_shield_elysia_2")

    private val conformalShieldsID = "zea_conformal_shield"

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