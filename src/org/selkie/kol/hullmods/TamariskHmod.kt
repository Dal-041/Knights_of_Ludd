package org.selkie.kol.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import org.selkie.kol.helpers.WeaponModificationHelpers
import org.selkie.kol.helpers.WeaponModificationHelpers.getClonedProjectileSpec
import org.selkie.kol.helpers.WeaponModificationHelpers.setProjectileSpec

class TamariskHmod: BaseHullMod() {
    companion object {
        const val PROJECTILE_SPEED_MULT = 1.25f
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)

        if (ship == null) return

        for (weapon in ship.allWeapons) {
            val slot = weapon.slot
            if (slot.weaponType != WeaponAPI.WeaponType.BALLISTIC) continue
            if (slot.slotSize != WeaponAPI.WeaponSize.LARGE) continue

            val spec = weapon.getClonedProjectileSpec() ?: continue
            spec.setMoveSpeed(spec.getMoveSpeed(null, weapon) * PROJECTILE_SPEED_MULT)
            weapon.setProjectileSpec(spec)
        }
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        when (index) {
            0 -> return "${PROJECTILE_SPEED_MULT}x"
        }
        return null
    }
}