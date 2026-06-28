package org.selkie.kol.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.loading.ProjectileSpecAPI
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI
import org.selkie.kol.ReflectionUtilsV2

class LunariaRangeExtender: BaseHullMod() {

    companion object {
        const val HARDPOINT_FLAT_INCR = 300f
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)

        if (ship == null) return

        if (ship.hasListenerOfClass(LunariaRangeExtenderListener::class.java)) return
        ship.addListener(LunariaRangeExtenderListener())
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        if (index == 0) return "${HARDPOINT_FLAT_INCR.toInt()}"
        return null
    }

    class LunariaRangeExtenderListener(): WeaponBaseRangeModifier {

        override fun getWeaponBaseRangePercentMod(
            ship: ShipAPI?,
            weapon: WeaponAPI?
        ): Float {
            return 0f
        }

        override fun getWeaponBaseRangeMultMod(
            ship: ShipAPI?,
            weapon: WeaponAPI?
        ): Float {
            return 1f
        }

        override fun getWeaponBaseRangeFlatMod(
            ship: ShipAPI?,
            weapon: WeaponAPI?
        ): Float {
            if (ship == null || weapon == null) return 0f

            if (weapon.size < WeaponSize.LARGE) return 0f
            if (weapon.type != WeaponType.BALLISTIC) return 0f
            val slot = weapon.slot
            if (slot == null || slot.isTurret) return 0f

            return HARDPOINT_FLAT_INCR
        }
    }

}