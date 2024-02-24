package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.ShockwaveVisual;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.selkie.kol.plugins.KOL_ModPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GraciousLightStats extends BaseShipSystemScript {
    private static final String GRACIOUS_LIGHT_KEY = "GraciousLight";
    public static final float HEALING_LIGHT_RANGE = 1600f;
    private static final float BURNING_LIGHT_RANGE = 1200f;
    private static final float HEALING_FLUX_MULT = 0.75f; // current flux reduced by this amount of current flux
    private static final float HEALING_HULL_MULT = 0.75f; // current hull healed by this amount of missing hull

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.setJitter(id, new Color(1f, 1f, 0f, MathUtils.clamp(effectLevel * 0.2f, 0f, 1f)), effectLevel * 0.66f, 3, 5f + 10f * effectLevel);
        ship.setJitterUnder(id, new Color(1f, 0f, 0f, MathUtils.clamp(effectLevel * 0.2f, 0f, 1f)), effectLevel * 0.66f, 8, 6f + 20f * effectLevel);

        if (state == State.ACTIVE || effectLevel == 1f) {
            burningAura(ship);
            healingAura(ship);
        }
        stats.getHardFluxDissipationFraction().modifyMult(id, 0f);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getHardFluxDissipationFraction().unmodify(id);
    }

    private void burningAura(ShipAPI ship) {
        DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                0.5f,
                BURNING_LIGHT_RANGE,
                BURNING_LIGHT_RANGE / 2f,
                1000f,
                150f,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                1f,
                2f,
                0.33f,
                0,
                new Color(255, 200, 30, 0),
                null
        );
        explosionSpec.setDamageType(DamageType.FRAGMENTATION);
        explosionSpec.setShowGraphic(false);
        Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, ship.getLocation(), false);

        ShockwaveVisual.ShockwaveParams params = new ShockwaveVisual.ShockwaveParams();
        params.loc = ship.getLocation();
        params.color = new Color(MathUtils.getRandomNumberInRange(225, 255), 120, 50, 20);
        params.radius = BURNING_LIGHT_RANGE;
        ShockwaveVisual.spawnShockwave(params);

        if (KOL_ModPlugin.hasGraphicsLib) {
            RippleDistortion ripple = new RippleDistortion(ship.getLocation(), new Vector2f());
            ripple.setSize(BURNING_LIGHT_RANGE);
            ripple.setIntensity(66f);
            ripple.setFrameRate(60f);
            ripple.fadeInSize(0.2f);
            ripple.fadeOutIntensity(1.5f);
            DistortionShader.addDistortion(ripple);

            StandardLight light = new StandardLight(ship.getLocation(), new Vector2f(), new Vector2f(), null);
            light.setSize(BURNING_LIGHT_RANGE);
            light.setIntensity(2f);
            light.setLifetime(0.66f);
            light.setAutoFadeOutTime(0.5f);
            light.setColor(new Color(255, 125, 25, 255));
            LightShader.addLight(light);
        }
    }

    private void healingAura(ShipAPI ship) {
        List<ShipAPI> fighters = new ArrayList<>();
        for (ShipAPI wing : Global.getCombatEngine().getShips()) {
            if (!wing.isFighter()) continue;
            if (wing.getWing() == null) continue;
            if (wing.getWing().getSourceShip() == ship) {
                fighters.add(wing);
            }
        }

        List<MagicSubsystem> activators = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (activators != null) {
            for (MagicSubsystem activator : activators) {
                if (activator instanceof MagicDroneSubsystem) {
                    MagicDroneSubsystem droneActivator = (MagicDroneSubsystem) activator;
                    fighters.addAll(droneActivator.getActiveWings().keySet());
                }
            }
        }

        for (ShipAPI fighter : fighters) {
            if (MathUtils.getDistance(ship.getLocation(), fighter.getLocation()) > HEALING_LIGHT_RANGE) continue;

            fighter.getFluxTracker().decreaseFlux(Math.max(0, fighter.getFluxTracker().getCurrFlux() * HEALING_FLUX_MULT));
            fighter.setHitpoints(Math.min(fighter.getMaxHitpoints(), fighter.getHitpoints() + ((fighter.getMaxHitpoints() - fighter.getHitpoints()) * HEALING_HULL_MULT)));
            fighter.clearDamageDecals();

            if (fighter.getFluxTracker().isOverloaded()) {
                float overloadTime = fighter.getFluxTracker().getOverloadTimeRemaining();
                fighter.getFluxTracker().setOverloadDuration(overloadTime - overloadTime * HEALING_FLUX_MULT);
            }
        }
    }

    public static GraciousLightData getGraciousLightData(ShipAPI ship) {
        if (ship.getCustomData().containsKey(GRACIOUS_LIGHT_KEY)) {
            return ((GraciousLightData) ship.getCustomData().get(GRACIOUS_LIGHT_KEY));
        }
        return null;
    }

    public class GraciousLightData {
        public IntervalUtil auraInterval = new IntervalUtil(1f, 1f);

        private GraciousLightData() {
        }
    }

    private class GraciousLightExplosionOnHitEffect implements OnHitEffectPlugin {
        @Override
        public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
            if (target != null && (target instanceof MissileAPI || (target instanceof ShipAPI && ((ShipAPI) target).isFighter()))) {
                damageResult.setDamageToHull(damageResult.getDamageToHull() * 2f);
                damageResult.setDamageToShields(damageResult.getDamageToShields() * 2f);
                damageResult.setDamageToPrimaryArmorCell(damageResult.getDamageToPrimaryArmorCell() * 2f);
                damageResult.setTotalDamageToArmor(damageResult.getTotalDamageToArmor() * 2f);
            }
        }
    }
}
