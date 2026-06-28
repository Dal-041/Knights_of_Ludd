package org.selkie.zea.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BeamAPI
import com.fs.starfarer.api.combat.BeamEffectPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.WeaponAPI
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class HellfireOnFireEffect: EveryFrameWeaponEffectPlugin {

    override fun advance(
        amount: Float,
        engine: CombatEngineAPI?,
        weapon: WeaponAPI?
    ) {
        if (engine == null || weapon == null) return

        if (weapon.custom == "KOL_DRAGONFIRECD") {
            if (!weapon.isFiring) {
                weapon.custom = null
            }
        } else {
            if (weapon.isFiring) {
                val beam = weapon.beams[0] ?: return

                weapon.custom = "KOL_DRAGONFIRECD"

                val vec = VectorUtils.getDirectionalVector(weapon.location, beam.to)
                vec.scale(100f)

                val ship = weapon.ship ?: return
                val oldVelocity = Vector2f(ship.velocity)
                ship.velocity.translate(vec.x, vec.y)

                engine.spawnMuzzleFlashOrSmoke(
                    weapon.ship,
                    weapon.location,
                    Global.getSettings().getWeaponSpec("dragon"),
                    weapon.arcFacing
                )

                ship.velocity.set(oldVelocity)
            }
        }    }
}