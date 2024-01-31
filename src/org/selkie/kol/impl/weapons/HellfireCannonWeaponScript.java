package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.ShockwaveVisual;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class HellfireCannonWeaponScript implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    public static final String PROJ_PLUGIN_CUSTOM_DATA_KEY =  "infernoboreCanisterPlugin";
    public static final Color WEAPON_GLOW = new Color(255, 50, 50, 155);
    private float glowScale = 0f;
    private float glowingStartTime = 0f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f projectileLocation = projectile.getLocation();
        Vector2f muzzleLocation = MathUtils.getPointOnCircumference(projectileLocation, 50f, projectile.getFacing());

        HellfireCannonProjectileScript projScript = new HellfireCannonProjectileScript(projectile);
        engine.addPlugin(projScript);
        projectile.setCustomData(PROJ_PLUGIN_CUSTOM_DATA_KEY, projScript);
        superMuzzle(weapon, muzzleLocation);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (!ship.isAlive()) {
            return;
        }

        boolean canFire = ship.getSystem().isStateActive();
        boolean doChargeEffects = canFire || ship.getSystem().isChargeup();
        if (doChargeEffects) {
            weapon.setGlowAmount(0.5f * ship.getSystem().getEffectLevel(), WEAPON_GLOW);

            glowScale = ship.getSystem().getEffectLevel() * 4f;
            if (ship.getSystem().getEffectLevel() > 0.5f) {
                if (glowingStartTime == 0f) {
                    glowingStartTime = engine.getTotalElapsedTime(false);
                }
                glowScale = 2f + (float) Math.abs(Math.sin((engine.getTotalElapsedTime(false) - glowingStartTime) * (1f + glowScale)) * 3f);
            }

            weapon.setWeaponGlowWidthMult(glowScale);
            weapon.setWeaponGlowHeightMult(glowScale);
        } else {
            glowingStartTime = 0f;
            weapon.setGlowAmount(0.5f * ship.getSystem().getEffectLevel(), WEAPON_GLOW);
            weapon.setWeaponGlowWidthMult(glowScale * ship.getSystem().getEffectLevel());
            weapon.setWeaponGlowHeightMult(glowScale * ship.getSystem().getEffectLevel());
        }

        if (!canFire) {
            weapon.setForceNoFireOneFrame(true);
        }
    }

    private void superMuzzle(WeaponAPI weapon, Vector2f loc) {
        float angle = weapon.getCurrAngle();

        CombatEngineAPI engine = Global.getCombatEngine();
        engine.spawnExplosion(
                loc,
                MathUtils.getPoint(
                        new Vector2f(),
                        30,
                        angle
                ),
                Color.DARK_GRAY,
                320f,
                3f
        );
        //boom
        DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                3,
                320f,
                320f,
                2000f,
                2000f,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                0f,
                0f,
                0f,
                0,
                new Color(255, 200, 30, 0),
                new Color(255, 170, 30, 0)
        );
        Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, weapon.getShip(), loc, false);

        engine.addSmoothParticle(loc, new Vector2f(), 400, 1, 0.1f, Color.WHITE);
        engine.addSmoothParticle(loc, new Vector2f(), 400, 1, 0.2f, Color.ORANGE);
        engine.addSmoothParticle(loc, new Vector2f(), 400, 1, 0.3f, Color.RED);

        for (int i = 0; i < 10; i++) {
            engine.addHitParticle(
                    loc,
                    MathUtils.getPoint(
                            new Vector2f(),
                            MathUtils.getRandomNumberInRange(25, 100) * 5f,
                            angle + MathUtils.getRandomNumberInRange(-2.5f, 2.5f)
                    ),
                    MathUtils.getRandomNumberInRange(3, 15),
                    1,
                    MathUtils.getRandomNumberInRange(0.25f, 1f),
                    Color.RED
            );
        }
    }
}