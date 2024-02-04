package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

/**
 * Dumb workaround for not being able to add an OnHitEffect through scripts.
 * Used in zea_inferno_shot.proj.
 * Custom data ({@link SupernovaProjectileScript}) used by this effect is set in {@link SupernovaWeaponScript}.
 */
public class SupernovaProjectileOnHitEffect implements OnHitEffectPlugin {
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (projectile.getCustomData().containsKey(SupernovaWeaponScript.PROJ_PLUGIN_CUSTOM_DATA_KEY)) {
            SupernovaProjectileScript canisterScript = (SupernovaProjectileScript) projectile.getCustomData().get(SupernovaWeaponScript.PROJ_PLUGIN_CUSTOM_DATA_KEY);
            canisterScript.onHit(projectile, target, point, shieldHit, damageResult, engine);
        }
    }
}