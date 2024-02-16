package org.selkie.kol.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;

public class ShipExplosionListener implements DamageTakenModifier {

    public final float REDUCED_EXPLOSION_MULT = 0.2f;

    //Inherited from Iron Shell, HMI, Rubi, and many who've come before.
    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        // checking for ship explosions
        if (param instanceof DamagingProjectileAPI) {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            // checks if the damage fits the details of a ship explosion
            if (proj.getDamageType().equals(DamageType.HIGH_EXPLOSIVE)
                    && proj.getProjectileSpecId() == null
                    && !proj.getSource().isAlive()
                    && proj.getSpawnType().equals(ProjectileSpawnType.OTHER)
                    && MathUtils.getDistance(proj.getSpawnLocation(), proj.getSource().getLocation()) < 0.5f) {
                damage.getModifier().modifyMult(this.getClass().getName(), REDUCED_EXPLOSION_MULT);
                return this.getClass().getName();
            }
        }
        return null;
    }
}
