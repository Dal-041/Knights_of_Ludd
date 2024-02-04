package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class SupernovaProjectileScript extends BaseEveryFrameCombatPlugin {
    private static final float TIER_1_ANGLE = 60f;
    private static final float TIER_2_ANGLE = 45f;
    private static final float TIER_3_ANGLE = 45f;
    private static final Color TIER_1_EXPLOSION_COLOR = new Color(230, 200, 70, 200);
    private static final Color TIER_2_EXPLOSION_COLOR = new Color(255, 140, 70, 200);
    private static final Color TIER_3_EXPLOSION_COLOR = new Color(255, 110, 60, 200);
    private static final float EXPLOSION_DELAY = 4.5f; //time in State.RELEASING until the canister explodes.

    private final DamagingProjectileAPI infernoShot;
    private State state = State.INITIALIZE;
    private float elapsedStageTime = 0f;
    private final IntervalUtil blink = new IntervalUtil(0.4f, 0.4f);
    private IntervalUtil canisterReleaseInterval = new IntervalUtil(0.075f, 0.12f);
    private int canisterTier = 1;
    private int canistersReleased = 0;
    private float canisterExplosionTime = 0f;
    private float desiredAngularVelocity = 0f;

    /**
     * @param proj inferno cannon shot
     */
    public SupernovaProjectileScript(DamagingProjectileAPI proj) {
        this.infernoShot = proj;
        proj.setMass(500f);

        float angularVelocity = MathUtils.getRandomNumberInRange(500f, 600f);
        if (MathUtils.getRandomNumberInRange(0f, 1f) >= 0.5f) {
            angularVelocity *= -1;
        }
        desiredAngularVelocity = angularVelocity;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        if (infernoShot.isExpired() || !Global.getCombatEngine().isEntityInPlay(infernoShot)) {
            Global.getCombatEngine().removePlugin(this);
            return;
        }

        if (state == State.INITIALIZE) {
            state = State.INACTIVE;
        }

        //spin the canister
        if (state == State.INACTIVE || state == State.PRIMING) {
            infernoShot.setAngularVelocity(desiredAngularVelocity);
            desiredAngularVelocity *= 1f - amount;
        }

        elapsedStageTime += amount;
        if (state == State.INACTIVE) {
            if (elapsedStageTime >= 2f) {
                Global.getCombatEngine().addHitParticle(infernoShot.getLocation(), infernoShot.getVelocity(), 200f, Math.min(1, infernoShot.getElapsed() / 2f), 0.1f, Color.red);
                state = State.PRIMING;
                elapsedStageTime = 0f;
            }
        } else if (state == State.PRIMING) {
            infernoShot.getVelocity().scale(0.995f);

            if (elapsedStageTime >= 1.25f) {
                state = State.RELEASING;
                elapsedStageTime = 0f;
            }
        } else if (state == State.RELEASING) {
            infernoShot.getVelocity().scale(0.98f);

            if (canisterTier <= 3) {
                canisterReleaseInterval.advance(amount);
                if (canisterReleaseInterval.intervalElapsed()) {
                    //every time we finish a full circle, go to the next tier.
                    if (canisterExplosionTime == 0f) {
                        canisterExplosionTime = Global.getCombatEngine().getTotalElapsedTime(false) + EXPLOSION_DELAY + canisterTier * 0.33f;
                    }

                    Color explosionColor = TIER_1_EXPLOSION_COLOR;
                    float facing = infernoShot.getFacing();
                    if (canisterTier == 1) {
                        facing = facing + TIER_1_ANGLE * canistersReleased;
                        explosionColor = TIER_1_EXPLOSION_COLOR;
                    } else if (canisterTier == 2) {
                        facing = facing + TIER_2_ANGLE * canistersReleased;
                        explosionColor = TIER_2_EXPLOSION_COLOR;
                    } else if (canisterTier == 3) {
                        facing = facing + TIER_3_ANGLE * canistersReleased;
                        explosionColor = TIER_3_EXPLOSION_COLOR;
                    }

                    //don't spawn canisters if we're on tier 3 and already spawned all canisters for a full circle
                    if (!(canisterTier == 3 && facing > (360f + infernoShot.getFacing()))) {
                        canistersReleased++;
                        Vector2f targetLocation = MathUtils.getPointOnCircumference(infernoShot.getLocation(), 125f * canisterTier, facing);
                        Vector2f canisterVelocity = (Vector2f) VectorUtils.getDirectionalVector(infernoShot.getLocation(), targetLocation).scale(72f * canisterTier);
                        DamagingProjectileAPI proj = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(infernoShot.getSource(), infernoShot.getWeapon(), "zea_nian_canister", infernoShot.getLocation(), facing,
                                canisterVelocity);

                        Global.getCombatEngine().addPlugin(new SupernovaSubmunitionScript(proj, canisterExplosionTime, explosionColor));
                        Global.getSoundPlayer().playSound("system_canister_flak_fire", 1f, 1f, infernoShot.getLocation(), canisterVelocity);

                        Global.getCombatEngine().spawnExplosion(
                                infernoShot.getLocation(),
                                MathUtils.getPoint(
                                        new Vector2f(),
                                        66,
                                        facing
                                ),
                                Color.DARK_GRAY,
                                30f,
                                0.45f
                        );
                    }

                    if (facing >= (360f + infernoShot.getFacing())) {
                        canisterTier++;
                        canisterExplosionTime = 0f;
                        canistersReleased = 0;
                    }
                }
            }

            if (canisterTier == 4) {
                blink.advance(amount);
                if (blink.intervalElapsed()) {
                    float ramp = 0.9f;
                    blink.setInterval(Math.max(0.1f, blink.getMinInterval() * ramp), Math.max(0.1f, blink.getMinInterval() * ramp));
                    Global.getCombatEngine().addHitParticle(infernoShot.getLocation(), infernoShot.getVelocity(), 100f, Math.min(1, infernoShot.getElapsed() / 2f), 0.1f, Color.red);
                }
            }

            if (elapsedStageTime >= EXPLOSION_DELAY) {
                state = State.EXPLOSION;
            }
        } else if (state == State.EXPLOSION) {
            //boom
            Global.getCombatEngine().spawnExplosion(
                    infernoShot.getLocation(),
                    MathUtils.getPoint(
                            new Vector2f(),
                            30,
                            0f
                    ),
                    Color.DARK_GRAY,
                    600f,
                    2.5f
            );

            DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                    1f,
                    400f,
                    200f,
                    7500f,
                    2500f,
                    CollisionClass.PROJECTILE_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    3f,
                    6f,
                    1f,
                    250,
                    new Color(255, 200, 30, 100),
                    new Color(255, 30, 30, 180)
            );
            Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, infernoShot.getSource(), infernoShot.getLocation(), false);
            Global.getSoundPlayer().playSound("system_orion_device_explosion", 1f, 1f, infernoShot.getLocation(), new Vector2f());

            Global.getCombatEngine().removePlugin(this);
            Global.getCombatEngine().removeEntity(infernoShot);
        }
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (state == State.INACTIVE) {
            elapsedStageTime = 999f;
            //immediately prime
        }
    }

    private enum State {
        INITIALIZE,
        INACTIVE,
        PRIMING,
        RELEASING,
        EXPLOSION
    }
}