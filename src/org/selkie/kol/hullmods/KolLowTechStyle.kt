package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import java.awt.Color

/**
 * Built into the low-tech restoration skins (`<hull>_lowtech`). A skin can't change its hull style, so this
 * repaints KOL_TECH's blue shield and vents with vanilla LOW_TECH's colors (starsector-core hull_styles.json),
 * and keeps the Knights shield texture that Primitive Shields applied. Shield sounds and the vent texture stay the
 * hull style's.
 */
class KolLowTechStyle : BaseHullMod() {

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
        ship.shield?.let {
            it.setRadius(ship.shieldRadiusEvenIfNoShield, KnightShields.SHIELD_TEXTURE, KnightShields.SHIELD_TEXTURE)
            it.innerColor = SHIELD_INNER
            it.ringColor = SHIELD_RING
        }
        ship.setVentCoreColor(VENT_CORE)
        ship.setVentFringeColor(VENT_FRINGE)
    }

    companion object {
        private val SHIELD_RING = Color(255, 255, 255, 255)
        private val SHIELD_INNER = Color(255, 125, 125, 75)
        private val VENT_CORE = Color(255, 255, 255, 255)
        private val VENT_FRINGE = Color(125, 0, 155, 255)
    }
}
