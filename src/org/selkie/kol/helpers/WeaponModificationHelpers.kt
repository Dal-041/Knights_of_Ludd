package org.selkie.kol.helpers

import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.ProjectileSpecAPI
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import org.selkie.kol.ReflectionUtilsV2

object WeaponModificationHelpers {

    // How to modify certain aspects of weapons on a case-by-case basis:
    // Handleable through this object: Damage, velocity, virtually everything tied to the projectile
    // ROF: Create an EFS to call setRefireDelay() on the wpn every frame
    // Turn rate: setTurnRateOverride() on the wpn
    // Range: Use a WeaponBaseRangeModifier
    // Flux: UNKNOWN
    // Weapon health: UNKNOWN

    // Intended use:
    // val spec = wpn.getClonedSpec()
    // [CODE HERE]
    // wpn.setProjectileSpec(spec)

    fun WeaponAPI.getClonedSpec(): WeaponSpecAPI = getClonedSpecExternal(this)
    fun getClonedSpecExternal(weapon: WeaponAPI): WeaponSpecAPI {
        weapon.ensureClonedSpec()
        return weapon.spec
    }
    /**
     * Mutate the spec before you call [setProjectileSpec].
     * */
    fun WeaponAPI.getClonedProjectileSpec(): ProjectileSpecAPI? = getClonedProjectileSpecExternal(this)
    /**
     * Mutate the spec before you call [setProjectileSpec].
     * */
    fun getClonedProjectileSpecExternal(weapon: WeaponAPI): ProjectileSpecAPI? {
        val spec = weapon.getClonedSpec()
        val projectileSpec = (spec.projectileSpec as? ProjectileSpecAPI) ?: return null
        val clonedProjSpec = ReflectionUtilsV2.invoke("clone", projectileSpec) as ProjectileSpecAPI
        return clonedProjSpec
    }

    fun WeaponAPI.setProjectileSpec(newSpec: ProjectileSpecAPI) = setProjectileSpecExternal(this, newSpec)
    fun setProjectileSpecExternal(weapon: WeaponAPI, newSpec: ProjectileSpecAPI) {
        val spec = weapon.getClonedSpec()
        ReflectionUtilsV2.invoke("setProjectileSpec", spec, newSpec, parameterCount = 1, skipParameterCheck = true)
    }
}