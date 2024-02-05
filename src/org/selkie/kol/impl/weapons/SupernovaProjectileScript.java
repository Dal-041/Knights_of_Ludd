package org.selkie.kol.impl.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.awt.*;
import java.util.List;

public class SupernovaProjectileScript extends BaseEveryFrameCombatPlugin {
    private static final float TIER_1_ANGLE = 60f;
    private static final float TIER_2_ANGLE = 45f;
    private static final float TIER_3_ANGLE = 45f;
    private static final Color TIER_1_EXPLOSION_COLOR = new Color(230, 200, 70, 200);
    private static final Color TIER_2_EXPLOSION_COLOR = new Color(255, 140, 70, 200);
    private static final Color TIER_3_EXPLOSION_COLOR = new Color(255, 110, 60, 200);
    private static final float INACTIVE_DELAY = 2f; //time in State.PRIMING until the canister releases submunitions.
    private static final float PRIMING_DELAY = 1.25f; //time in State.PRIMING until the canister releases submunitions.
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
    private float targetingAngle = MathUtils.getRandomNumberInRange(-180f, 180f);

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

        elapsedStageTime += amount;
        if (state.length - elapsedStageTime <= 0f) {
            state = State.values()[state.ordinal() + 1];
            elapsedStageTime = 0f;
        }

        //spin the canister
        if (state == State.INACTIVE || state == State.PRIMING) {
            infernoShot.setAngularVelocity(desiredAngularVelocity);
            desiredAngularVelocity *= 1f - amount;
        }

        float velDecrease = 0.4f;
        targetingAngle += amount * 5f;
        MagicRender.singleframe(
                Global.getSettings().getSprite("fx", "zea_nian_targetingRing"),
                MathUtils.getPoint(infernoShot.getLocation(), calculateTotalDistance(infernoShot.getVelocity().length(), velDecrease, state.timeToExplode(elapsedStageTime)), VectorUtils.getFacing(infernoShot.getVelocity())), //location
                new Vector2f(1200, 1200), //size
                targetingAngle, //angle
                new Color(255, 100, 0, 128),
                true, //additive
                CombatEngineLayers.UNDER_SHIPS_LAYER
        );
        infernoShot.getVelocity().scale(1f - velDecrease * amount);

        if (state == State.RELEASING) {
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

            if (KOL_ModPlugin.hasGraphicsLib) {
                RippleDistortion ripple = new RippleDistortion(infernoShot.getLocation(), new Vector2f());
                ripple.setSize(400f);
                ripple.setIntensity(50f);
                ripple.setFrameRate(60f);
                ripple.fadeInSize(0.2f);
                ripple.fadeOutIntensity(1.5f);
                DistortionShader.addDistortion(ripple);

                StandardLight light = new StandardLight(infernoShot.getLocation(), new Vector2f(), new Vector2f(), null);
                light.setSize(500f);
                light.setIntensity(10f);
                light.setLifetime(0.45f);
                light.setAutoFadeOutTime(0.3f);
                light.setColor(new Color(255, 125, 25, 255));
                LightShader.addLight(light);
            }

            Global.getCombatEngine().removePlugin(this);
            Global.getCombatEngine().removeEntity(infernoShot);
        }
    }

    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (state == State.INACTIVE) {
            //immediately prime
            elapsedStageTime = 0f;
            state = State.RELEASING;
        }
    }

    // Function to calculate the total distance traveled
    public static float calculateTotalDistance(double initialSpeed, double decreasePercentage, float totalTime) {
        // Calculate total distance using the formula for the sum of a geometric sequence
        double totalDistance = initialSpeed * (1 - Math.pow(1 - decreasePercentage, totalTime)) / decreasePercentage;

        return (float) totalDistance;
    }

    private enum State {
        INITIALIZE(0f),
        INACTIVE(2f),
        PRIMING(1.25f),
        RELEASING(4.5f),
        EXPLOSION(0f);

        private final float length;
        State(float length) {
            this.length = length;
        }

        float timeToExplode(float elapsedStageTime) {
            float totalTime = this.length - elapsedStageTime;
            for (int i = this.ordinal() + 1; i < State.values().length; i++) {
                totalTime += State.values()[i].length;
            }
            return totalTime;
        }
    }
}