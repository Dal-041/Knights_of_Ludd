package org.selkie.kol.impl.combat.activators;

import activators.CombatActivator;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NianFlaresActivator extends CombatActivator {
    private static final int FLARES_PER_WAVE_PER_SIDE = 6;
    private static final int FLARE_WAVES_NORMAL = 1;
    private static final int FLARE_WAVES_ENRAGED = 3;
    private static final float FLARE_WAVE_DELAY = 0.75f;
    private static final float MISSILE_SEARCH_RANGE = 800f;

    IntervalUtil flaresInterval = new IntervalUtil(0.07f, 0.15f);
    IntervalUtil aiInterval = new IntervalUtil(0.5f, 1f);
    IntervalUtil nextWaveInterval = new IntervalUtil(FLARE_WAVE_DELAY, FLARE_WAVE_DELAY);
    int flaresReleasedInWave = 0;
    int wavesToLaunch = 0;

    public NianFlaresActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    public String getDisplayText() {
        return "Flare Wave";
    }

    @Override
    public float getBaseInDuration() {
        return 0f;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0.1f;
    }

    @Override
    public float getBaseOutDuration() {
        return 0f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 10f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        aiInterval.advance(amount);
        if (aiInterval.intervalElapsed()) {
            List<MissileAPI> missiles = AIUtils.getNearbyEnemyMissiles(ship, MISSILE_SEARCH_RANGE);
            return canActivate() && !missiles.isEmpty();
        } else {
            return false;
        }
    }

    @Override
    public void advance(float amount) {
        if (wavesToLaunch > 0) {
            if (flaresReleasedInWave < FLARES_PER_WAVE_PER_SIDE * 2) {
                flaresInterval.advance(amount);
                if (flaresInterval.intervalElapsed()) {
                    Vector2f launchVelocity = (Vector2f) new Vector2f(ship.getVelocity()).scale(0.5f);

                    float leftLaunchAngle = ship.getFacing() + 90f + MathUtils.getRandomNumberInRange(-30f, 30f);
                    Vector2f leftLaunchPos = CollisionUtils.getNearestPointOnBounds(MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), leftLaunchAngle), ship);
                    Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", leftLaunchPos, leftLaunchAngle, launchVelocity);

                    float rightLaunchAngle = ship.getFacing() - 90f + MathUtils.getRandomNumberInRange(-30f, 30f);
                    Vector2f rightLaunchPos = CollisionUtils.getNearestPointOnBounds(MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), rightLaunchAngle), ship);
                    Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", rightLaunchPos, rightLaunchAngle, launchVelocity);

                    Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, leftLaunchPos, launchVelocity);
                    Global.getCombatEngine().spawnExplosion(
                            leftLaunchPos,
                            MathUtils.getPoint(
                                    new Vector2f(),
                                    66,
                                    leftLaunchAngle
                            ),
                            Color.DARK_GRAY,
                            30f,
                            0.45f
                    );

                    Global.getSoundPlayer().playSound("launch_flare_1", 1f, 1f, rightLaunchPos, launchVelocity);
                    Global.getCombatEngine().spawnExplosion(
                            rightLaunchPos,
                            MathUtils.getPoint(
                                    new Vector2f(),
                                    66,
                                    rightLaunchAngle
                            ),
                            Color.DARK_GRAY,
                            30f,
                            0.45f
                    );

                    flaresReleasedInWave += 2;
                }
            } else {
                nextWaveInterval.advance(amount);
                if (nextWaveInterval.intervalElapsed()) {
                    wavesToLaunch--;
                    flaresReleasedInWave = 0;
                }
            }
        }
    }

    public void onActivate() {
        int flareWaves = FLARE_WAVES_NORMAL;
        if (ship.getHullLevel() <= 0.5f) {
            flareWaves = FLARE_WAVES_ENRAGED;
        }
        wavesToLaunch = flareWaves;

        flaresReleasedInWave = 0;
        nextWaveInterval.setInterval(FLARE_WAVE_DELAY, FLARE_WAVE_DELAY);
    }
}
