package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.BallisticProjectile;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import com.fs.starfarer.combat.entities.MovingRay;
import com.fs.starfarer.combat.entities.PlasmaShot;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.magiclib.plugins.MagicRenderPlugin;
import org.selkie.kol.ReflectionUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class BulletTimeField extends BaseShipSystemScript {

    public static class DamagingProjectileInfo {
        float adjustedElapsedTime;
        float initialSpeed;

        @SuppressWarnings("DataFlowIssue")
        DamagingProjectileInfo(DamagingProjectileAPI threat) {
            float amount = Global.getCombatEngine().getElapsedInLastFrame();
            adjustedElapsedTime = threat.getElapsed() + (MAX_SLOWDOWN * amount);
            if (threat instanceof BallisticProjectile || threat instanceof PlasmaShot) {
                initialSpeed = threat.getVelocity().length();
            } else if (threat instanceof MovingRay) {
                MovingRay nearbyRay = (MovingRay) threat;
                Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                initialSpeed = (float) ReflectionUtils.get(RayExtender.BULLET_SPEED, rayExtender); // bullet speed
            } else if (threat instanceof MissileAPI) {
                initialSpeed = ((MissileAPI) threat).getMaxSpeed();
            }
        }
    }

    static public class RayExtender{
        public static final String BULLET_SPEED = "Ö00000";
        public static final String ELAPSED_DISTANCE = "void";
        public static final String RAY_VELOCITY = "String";
    }
    static public class TrailExtender{
        public static final String ELAPSED_TIME = "if";
    }

    public static final float MAX_SLOWDOWN = 0.15f;
    public static final float SLOWDOWN_RADIUS = 400f;
    public static final float CORE_RADIUS = 100f;
    public static final float MAX_DEFLECTION_ANGLE = 70f;
    public static final float ACTIVE_COLLISION_RADIUS = 20f;

    public boolean init = false;
    public float collisionRadius;
    public float shieldRadius;
    public float shieldArc;
    final Map<DamagingProjectileAPI, DamagingProjectileInfo> slowedProjectiles = new HashMap<>();

    public void init(ShipAPI ship){
        if (!init) {
            shieldRadius = ship.getShield().getRadius();
            shieldArc = ship.getShield().getArc();
            collisionRadius = ship.getCollisionRadius();
            init = true;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        ShipAPI ship = (ShipAPI) stats.getEntity();
        init(ship);


        stats.getMaxSpeed().modifyMult(id, 0.5f);
        stats.getAcceleration().modifyMult(id, 10f);
        stats.getDeceleration().modifyMult(id, 10f);
        stats.getBeamDamageTakenMult().modifyMult(id ,0f);

        ship.getShield().setArc(360f);
        //ship.getShield().setRadius(10f);
        ship.getShield().toggleOff();
        ship.getShield().setInnerColor(new Color(100, 255, 100));
        ship.getShield().setRingColor(new Color(100, 255, 100));
        ship.setAlphaMult(0.2f);
        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

        ship.setCollisionRadius(ACTIVE_COLLISION_RADIUS);
        SpriteAPI shipCollisionCircle = Global.getSettings().getSprite("graphics/fx/circle64.png");
        shipCollisionCircle.setColor(Color.GREEN);
        shipCollisionCircle.setAlphaMult(0.8f);
        shipCollisionCircle.setSize(ACTIVE_COLLISION_RADIUS, ACTIVE_COLLISION_RADIUS);
        MagicRenderPlugin.addSingleframe(shipCollisionCircle, ship.getLocation(), CombatEngineLayers.BELOW_INDICATORS_LAYER);

        Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(stats.getEntity().getLocation(), 1000 * 2, 1000 * 2);
        Random rand = new Random();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (!(next instanceof DamagingProjectileAPI)) continue;
            DamagingProjectileAPI threat = (DamagingProjectileAPI) next;
            if(threat.getSource() == stats.getEntity()) continue;
            // Add threats in range to be slowed
            float threatDistance = MathUtils.getDistance(threat.getLocation(), stats.getEntity().getLocation());
            if (threatDistance < SLOWDOWN_RADIUS) {
                if (!slowedProjectiles.containsKey(threat) && !(threat instanceof DamagingExplosion)) {
                    DamagingProjectileInfo threatInfo = new DamagingProjectileInfo(threat);
                    slowedProjectiles.put(threat, threatInfo);

                    // spread bullets around
                    float angle = (float) (rand.nextGaussian() * MAX_DEFLECTION_ANGLE/3); // not technically max, but eh good enough
                    float slowdownMult = Misc.interpolate(MAX_SLOWDOWN, 1f, Math.max(0, (threatDistance-CORE_RADIUS))/(SLOWDOWN_RADIUS-CORE_RADIUS));
                    // anything that's an entity that follows velocity can be directly changed
                    if (threat instanceof BallisticProjectile || threat instanceof MissileAPI || threat instanceof PlasmaShot) {
                        VectorUtils.rotate(threat.getVelocity(), angle);
                        VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult);
                    }
                    // MovingRay is hitscan, and as such the angle is changed with facing, and speed is set with reflection
                    else if (threat instanceof MovingRay) {
                        MovingRay nearbyRay = (MovingRay) threat;
                        Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                        ReflectionUtils.set(RayExtender.BULLET_SPEED, rayExtender, threatInfo.initialSpeed * slowdownMult);
                    }
                    threat.setFacing(threat.getFacing() + angle);

                }
            } else { // remove threats that have gone out of range
                if (slowedProjectiles.containsKey(threat)) {
                    resetDamagingProjectile(threat);
                    slowedProjectiles.remove(threat);
                }
            }
        }

        // stop tracking expired threats
        for (Iterator<Map.Entry<DamagingProjectileAPI, DamagingProjectileInfo>> i = slowedProjectiles.entrySet().iterator(); i.hasNext(); ) {
            if (i.next().getKey().isExpired()) {
                i.remove();
            }
        }


        for (DamagingProjectileAPI threat : slowedProjectiles.keySet()) {
            DamagingProjectileInfo threatInfo = slowedProjectiles.get(threat);
            threatInfo.adjustedElapsedTime += amount * MAX_SLOWDOWN;
            float threatDistance = MathUtils.getDistance(threat.getLocation(), stats.getEntity().getLocation());
            float slowdownMult = Misc.interpolate(MAX_SLOWDOWN, 1f, Math.max(0, (threatDistance-CORE_RADIUS))/(SLOWDOWN_RADIUS-CORE_RADIUS));

            // draw a red collision circle
            if (!(threat instanceof MovingRay)){
                SpriteAPI threatCollisionCircle = Global.getSettings().getSprite("graphics/fx/circle64.png");
                threatCollisionCircle.setColor(Color.red);
                threatCollisionCircle.setAlphaMult(0.5f);
                threatCollisionCircle.setSize(threat.getCollisionRadius(), threat.getCollisionRadius());
                MagicRenderPlugin.addSingleframe(threatCollisionCircle, threat.getLocation(), CombatEngineLayers.BELOW_INDICATORS_LAYER);
            }

            // all slowed objects need to have their internal timers also slowed down, otherwise they would expire early before their max range
            // also make sure to constantly set their speeds/velocity back to the slower value (for things under constant acceleration)
            if (threat instanceof BallisticProjectile) {
                BallisticProjectile nearbyBallistic = (BallisticProjectile) threat;
                Object trailExtender = ReflectionUtils.get("trailExtender", nearbyBallistic);
                ReflectionUtils.set(TrailExtender.ELAPSED_TIME, trailExtender, threatInfo.adjustedElapsedTime); //elapsed time
                ReflectionUtils.set("elapsed", nearbyBallistic, threatInfo.adjustedElapsedTime);
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult);
            } else if (threat instanceof MovingRay) {
                MovingRay nearbyRay = (MovingRay) threat;
                ReflectionUtils.set("elapsed", nearbyRay, threatInfo.adjustedElapsedTime);
                Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                ReflectionUtils.set(RayExtender.BULLET_SPEED, rayExtender, threatInfo.initialSpeed * slowdownMult);
            } else if (threat instanceof MissileAPI) {
                MissileAPI nearbyMissile = (MissileAPI) threat;
                ReflectionUtils.set("elapsed", nearbyMissile, threatInfo.adjustedElapsedTime);
                nearbyMissile.setFlightTime(threatInfo.adjustedElapsedTime);
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult);
            } else if (threat instanceof PlasmaShot) {
                PlasmaShot nearbyPlasma = (PlasmaShot) threat;
                ReflectionUtils.set("flightTime", nearbyPlasma, threatInfo.adjustedElapsedTime);
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult);
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void resetDamagingProjectile(DamagingProjectileAPI threat){
        // reset velocities if possible
        if (threat instanceof BallisticProjectile || threat instanceof MissileAPI || threat instanceof PlasmaShot) {
            VectorUtils.resize(threat.getVelocity(), slowedProjectiles.get(threat).initialSpeed);

        }
        // hitscan needs its speed reset by reflection
        else if (threat instanceof MovingRay) {
            MovingRay nearbyRay = (MovingRay) threat;
            Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
            ReflectionUtils.set(RayExtender.BULLET_SPEED, rayExtender, slowedProjectiles.get(threat).initialSpeed);
        }

        VectorUtils.resize(threat.getVelocity(), slowedProjectiles.get(threat).initialSpeed);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        init((ShipAPI) stats.getEntity());
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getBeamDamageTakenMult().unmodify(id);

        stats.getEntity().getShield().setArc(shieldArc);
        stats.getEntity().getShield().setRadius(shieldRadius);
        stats.getEntity().setCollisionRadius(collisionRadius);
        for (DamagingProjectileAPI threat : slowedProjectiles.keySet()) {
            resetDamagingProjectile(threat);
        }
        ((ShipAPI) stats.getEntity()).setAlphaMult(1f);
        slowedProjectiles.clear();
    }
}
