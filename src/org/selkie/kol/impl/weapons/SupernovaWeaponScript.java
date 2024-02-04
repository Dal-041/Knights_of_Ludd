package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SupernovaWeaponScript implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    public static final String PROJ_PLUGIN_CUSTOM_DATA_KEY =  "infernoboreCanisterPlugin";

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Vector2f projectileLocation = projectile.getLocation();
        Vector2f muzzleLocation = MathUtils.getPointOnCircumference(projectileLocation, 140f, projectile.getFacing());

        SupernovaProjectileScript projScript = new SupernovaProjectileScript(projectile);
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
        if (!canFire) {
            //weapon.setForceNoFireOneFrame(true);
        }
    }

    private void superMuzzle(WeaponAPI weapon, Vector2f loc) {
        float angle = weapon.getCurrAngle();

        CombatEngineAPI engine = Global.getCombatEngine();

        engine.spawnExplosion(
                loc,
                MathUtils.getPoint(
                        new Vector2f(),
                        66,
                        angle
                ),
                Color.DARK_GRAY,
                320f,
                4.5f
        );

        for (int i = 0; i < 5; i++) {
            engine.spawnExplosion(
                    loc,
                    MathUtils.getPoint(
                            new Vector2f(),
                            20 + 6f * i,
                            angle
                    ),
                    Color.DARK_GRAY,
                    180f + 5f * i,
                    5f + 0.5f * i
            );
        }


        engine.addSmoothParticle(loc, new Vector2f(), 400, 1, 0.1f, Color.WHITE);
        engine.addSmoothParticle(loc, new Vector2f(), 400, 1, 0.2f, Color.ORANGE);
        engine.addSmoothParticle(loc, new Vector2f(), 400, 1, 0.3f, Color.RED);

        for (int i = 0; i < MathUtils.getRandomNumberInRange(15, 30); i++) {
            Color sparkColor = Color.RED;
            if (MathUtils.getRandomNumberInRange(0f, 1f) > 0.5f) {
                sparkColor = Color.YELLOW;
            }
            engine.addHitParticle(
                    loc,
                    MathUtils.getPoint(
                            new Vector2f(),
                            MathUtils.getRandomNumberInRange(60, 100) * 4.5f,
                            angle + MathUtils.getRandomNumberInRange(-20f, 20f)
                    ),
                    MathUtils.getRandomNumberInRange(7, 15),
                    1,
                    MathUtils.getRandomNumberInRange(0.25f, 0.4f),
                    sparkColor
            );
        }

        //boom
        DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                1.25f,
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
    }
}