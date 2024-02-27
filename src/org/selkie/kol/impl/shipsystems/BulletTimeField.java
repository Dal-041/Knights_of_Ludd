package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.combat.entities.BallisticProjectile;
import com.fs.starfarer.combat.entities.MovingRay;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.ReflectionUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class BulletTimeField extends BaseShipSystemScript {
    public static class DamagingProjectileInfo {
        float startingTime;
        float startingDistance;
        float adjustedElapsedTime;
        float adjustedElapsedDistance;
        float initialSpeed;

        DamagingProjectileInfo(DamagingProjectileAPI threat) {
            float amount = Global.getCombatEngine().getElapsedInLastFrame();
            startingTime = threat.getElapsed();
            adjustedElapsedTime = startingTime + (SLOWDOWN * amount);
            if (threat instanceof BallisticProjectile) {
                initialSpeed = threat.getVelocity().length();
            } else if (threat instanceof MovingRay) {
                MovingRay nearbyRay = (MovingRay) threat;
                Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                initialSpeed = (float) ReflectionUtils.get("Ö00000", rayExtender); // bullet speed
                startingDistance = (float) ReflectionUtils.get("Ø00000", rayExtender);
                adjustedElapsedDistance = startingDistance + (initialSpeed * SLOWDOWN * amount);
            } else if (threat instanceof MissileAPI) {
                initialSpeed = ((MissileAPI) threat).getMaxSpeed();

            }
        }
    }

    public static final float SLOWDOWN = 0.2f;
    public static final float SLOWDOWN_RADIUS = 400f;
    public static final float DEFLECTION_ANGLE = 40f;
    public boolean init = false;
    public float shieldRadius;
    public float shieldArc;
    Map<DamagingProjectileAPI, DamagingProjectileInfo> slowedProjectiles = new HashMap<>();

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        ShipAPI ship = (ShipAPI) stats.getEntity();

        if (!init) {
            shieldRadius = ship.getShield().getRadius();
            shieldArc = ship.getShield().getArc();
            init = true;
        }


        stats.getMaxSpeed().modifyMult(id, 0.5f);
        stats.getAcceleration().modifyMult(id, 10f);
        stats.getDeceleration().modifyMult(id, 10f);

        ship.getShield().setArc(360f);
        ship.getShield().setRadius(10f);
        ship.getShield().toggleOn();
        ship.getShield().setInnerColor(new Color(100, 255, 100));
        ship.getShield().setRingColor(new Color(100, 255, 100));
        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);


        Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(stats.getEntity().getLocation(), 1000 * 2, 1000 * 2);
        Random rand = new Random();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (!(next instanceof DamagingProjectileAPI)) continue;
            DamagingProjectileAPI threat = (DamagingProjectileAPI) next;
            //if(threat.getSource() == stats.getEntity()) continue;
            // Add threats in range to be slowed
            if (MathUtils.getDistanceSquared(threat.getLocation(), stats.getEntity().getLocation()) < SLOWDOWN_RADIUS * SLOWDOWN_RADIUS) {
                if (!slowedProjectiles.containsKey(threat)) {
                    slowedProjectiles.put(threat, new DamagingProjectileInfo(threat));

                    float angle = (float) (rand.nextGaussian() * DEFLECTION_ANGLE);

                    if (threat instanceof BallisticProjectile || threat instanceof MissileAPI) {
                        VectorUtils.rotate(threat.getVelocity(), angle);
                    } else if (threat instanceof MovingRay) {
                        MovingRay nearbyRay = (MovingRay) threat;
                        Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                        Vector2f velocity = (Vector2f) ReflectionUtils.get("o00000", rayExtender);
                        ReflectionUtils.set("o00000", rayExtender, VectorUtils.rotate(velocity, angle));
                    }
                    threat.setFacing(threat.getFacing() + angle);
                }
            } else { // remove threats that have gone out of range
                if (slowedProjectiles.containsKey(threat)) {
                    VectorUtils.resize(threat.getVelocity(), slowedProjectiles.get(threat).initialSpeed);
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
            if (threat instanceof BallisticProjectile) {
                BallisticProjectile nearbyBallistic = (BallisticProjectile) threat;

                Object trailExtender = ReflectionUtils.get("trailExtender", nearbyBallistic);
                ReflectionUtils.set("if", trailExtender, threatInfo.adjustedElapsedTime); //elapsed time
                ReflectionUtils.set("elapsed", nearbyBallistic, threatInfo.adjustedElapsedTime);

                threatInfo.adjustedElapsedTime += amount * SLOWDOWN;
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * SLOWDOWN);
            } else if (threat instanceof MovingRay) {
                MovingRay nearbyRay = (MovingRay) threat;
                Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                ReflectionUtils.set("elapsed", nearbyRay, threatInfo.adjustedElapsedTime);
                //ReflectionUtils.set("Ø00000", rayExtender, threatInfo.adjustedElapsedDistance);
                ReflectionUtils.set("Ø00000", rayExtender, threatInfo.initialSpeed * SLOWDOWN);

                threatInfo.adjustedElapsedTime += amount * SLOWDOWN;
                threatInfo.adjustedElapsedDistance += amount * SLOWDOWN * threatInfo.initialSpeed;
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * SLOWDOWN);
            } else if (threat instanceof MissileAPI) {
                MissileAPI nearbyMissile = (MissileAPI) threat;
                ReflectionUtils.set("elapsed", nearbyMissile, threatInfo.adjustedElapsedTime);
                nearbyMissile.setFlightTime(threatInfo.adjustedElapsedTime);

                threatInfo.adjustedElapsedTime += amount * SLOWDOWN;
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * SLOWDOWN);
            }
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getEntity().getShield().setArc(shieldArc);
        stats.getEntity().getShield().setRadius(shieldRadius);
        for (DamagingProjectileAPI threat : slowedProjectiles.keySet()) {
            DamagingProjectileInfo threatInfo = slowedProjectiles.get(threat);
            if (threat instanceof BallisticProjectile || threat instanceof MissileAPI) {
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed);
            } else if (threat instanceof MovingRay) {
                MovingRay nearbyRay = (MovingRay) threat;
                Object rayExtender = ReflectionUtils.get("rayExtender", nearbyRay);
                ReflectionUtils.set("return", rayExtender, threatInfo.initialSpeed);
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed);
            }
        }
        slowedProjectiles.clear();
    }
}
