package org.selkie.kol.impl.combat.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.magiclib.subsystems.drones.PIDController;
import org.selkie.kol.combat.StarficzAIUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

public class ShachihokoDroneActivator extends MagicDroneSubsystem {
    private static final Color BASE_SHIELD_COLOR = new Color(220, 190, 70, 255);
    private static final Color HIGHEST_FLUX_SHIELD_COLOR = Color.red;
    private static final float SHIELD_ALPHA = 0.25f;
    private static final float nearbyRange = 2000;
    private final IntervalUtil intervalCheck = new IntervalUtil(10f, 10f);
    private final PIDController PID = new PIDController(6f, 4f, 8f, 3f);

    private ShipAPI target = null;
    private ShipAPI droneTarget = null;

    public ShachihokoDroneActivator(ShipAPI ship) {
        super(ship);
    }

    @Override
    public float getBaseActiveDuration() {
        return 0;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 2f;
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    @Override
    public float getBaseChargeRechargeDuration() {
        return 25f;
    }

    @Override
    public boolean canActivate() {
        return super.canActivate();
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        intervalCheck.advance(amount);
        // Weird crash avoidance that I'm not going to diagnose in detail
        if (!canActivate()) return false;
        if (droneTarget == null) return true;
        if (!droneTarget.isAlive()) return true;
        if (droneTarget.isPhased()) return true;
        if (AIUtils.getNearbyEnemies(droneTarget, nearbyRange).isEmpty()) return true;
        if (ship.getShipTarget()!= null && ship.getShipTarget().getOwner() == ship.getOwner() && ship.getShipTarget() != droneTarget) return true;
        if (intervalCheck.intervalElapsed()) return true;

        return false;
    }

    @Override
    public void onActivate() {
        ShipAPI nextTarget = null;
        if (ship.getShipTarget() == null || ship.getShipTarget().getOwner() != ship.getOwner()) {
            if (ship.getCaptain() != Global.getSector().getPlayerPerson()) {
                nextTarget = getAppropriateAlly(ship, true);
            } else if (target != ship) {
                nextTarget = ship;
            }
        } else if (ship.getShipTarget() != null && ship.getShipTarget().getOwner() == ship.getOwner()) {
            if (target != ship.getShipTarget()) {
                nextTarget = ship.getShipTarget();
            }
        }


        if (getActiveWings().containsKey(nextTarget)) {
            nextTarget = null;
        }

        target = nextTarget;
    }

    @Override
    public PIDController getPIDController() {
        return PID.copy();
    }

    @NotNull
    @Override
    public ShipAPI spawnDrone() {
        ShipAPI drone = super.spawnDrone();
        drone.getMutableStats().getHardFluxDissipationFraction().modifyFlat("shachihokoHardFluxDiss", 10f / 100f);
        return drone;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        if (target != null && !target.isAlive()) {
            target = null;
        }

        droneTarget = ship;
        if (target != null) {
            droneTarget = target;
        }

        for (ShipAPI drone : getActiveWings().keySet()) {
            float desiredShieldRadius = getShieldRadiusForTarget(drone, droneTarget);

            if (drone.getFluxTracker().isOverloadedOrVenting()) {
                desiredShieldRadius = drone.getHullSpec().getShieldSpec().getRadius();
            }

            if (desiredShieldRadius > drone.getShield().getRadius()) {
                drone.getShield().setRadius(Math.min(desiredShieldRadius, drone.getShield().getRadius() + amount * 250f));
            } else if (desiredShieldRadius < drone.getShield().getRadius()) {
                drone.getShield().setRadius(Math.max(desiredShieldRadius, drone.getShield().getRadius() - amount * 250f));
            }
            drone.setCollisionRadius(drone.getShieldRadiusEvenIfNoShield());

            float distanceToTarget = MathUtils.getDistance(drone.getLocation(), droneTarget.getLocation());
            if (drone.getShield().isOn()) {
                if (distanceToTarget > desiredShieldRadius * 1.25f) {
                    //turn shield off if too far from target
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                } else {
                    //keep shield on otherwise
                    drone.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

                    float highestFluxLevelRatio = 0.75f;
                    float ratio = drone.getFluxLevel();
                    float red = BASE_SHIELD_COLOR.getRed() * MathUtils.clamp((1f - ratio) / highestFluxLevelRatio, 0f, 1f) + HIGHEST_FLUX_SHIELD_COLOR.getRed() * MathUtils.clamp(ratio / highestFluxLevelRatio, 0f, 1f);
                    float green = BASE_SHIELD_COLOR.getGreen() * MathUtils.clamp((1f - ratio) / highestFluxLevelRatio, 0f, 1f) + HIGHEST_FLUX_SHIELD_COLOR.getGreen() * MathUtils.clamp(ratio / highestFluxLevelRatio, 0f, 1f);
                    float blue = BASE_SHIELD_COLOR.getBlue() * MathUtils.clamp((1f - ratio) / highestFluxLevelRatio, 0f, 1f) + HIGHEST_FLUX_SHIELD_COLOR.getBlue() * MathUtils.clamp(ratio / highestFluxLevelRatio, 0f, 1f);

                    drone.getShield().setInnerColor(new Color(MathUtils.clamp(red / 255f, 0f, 1f),
                            MathUtils.clamp(green / 255f, 0f, 1f),
                            MathUtils.clamp(blue / 255f, 0f, 1f),
                            SHIELD_ALPHA));
                }
            } else if (!drone.getFluxTracker().isOverloadedOrVenting()) {
                if (distanceToTarget <= desiredShieldRadius * 1.25f) {
                    //turn shield on if close to target
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                } else {
                    //keep shield off otherwise
                    drone.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

                    //and vent if we can
                    if (drone.getFluxLevel() > 0.05f) {
                        drone.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    }
                }
            }

            //vent if we absolutely need to
            if (drone.getFluxLevel() > 0.90f) {
                drone.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }

    }

    @Override
    public String getDisplayText() {
        return "Shachihoko Drone";
    }

    @Override
    public int getMaxCharges() {
        return 2;
    }

    @Override
    public int getMaxDeployedDrones() {
        return 1;
    }

    @Override
    public String getDroneVariant() {
        return "zea_edf_shachihoko_wing";
    }

    @NotNull
    @Override
    public DroneFormation getDroneFormation() {
        return new TargetedSupportDroneFormation();
    }

    private class TargetedSupportDroneFormation extends DroneFormation {
        private final static float ROTATION_SPEED = 20;
        private float currentRotation = 0f;

        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {
            List<Float> blockedDirections = new ArrayList<>();
            blockedDirections.add(ship.getFacing() - 180f);

            // generate all locations by doubling up on locations if too many drones
            ShipAPI droneTarget = ship;
            if (target != null) {
                droneTarget = target;
            }

            List<Vector2f> droneLocations = new ArrayList<>();
            int i = 0;
            int circles = 0;
            for (ShipAPI drone : drones.keySet()) {
                float droneDistance = getShieldRadiusForTarget(drone, droneTarget) * 0.33f + 30f * circles;
                float droneAngle = blockedDirections.get(i % blockedDirections.size());
                droneLocations.add(MathUtils.getPointOnCircumference(droneTarget.getLocation(), droneDistance, droneAngle));

                if (i % blockedDirections.size() == blockedDirections.size() - 1)
                    circles++;
                i++;
            }

            // move each drone to its closest location
            List<Vector2f> assignedPoints = new ArrayList<>();
            for (ShipAPI drone : drones.keySet()) {
                Vector2f desiredLocation = null;
                float lowestDistance = Float.POSITIVE_INFINITY;
                for (Vector2f point : droneLocations) {
                    if (assignedPoints.contains(point)) continue;
                    float currentDistance = MathUtils.getDistanceSquared(drone.getLocation(), point);
                    if (currentDistance < lowestDistance) {
                        lowestDistance = currentDistance;
                        desiredLocation = point;
                    }
                }

                assignedPoints.add(desiredLocation);
                if (StarficzAIUtils.DEBUG_ENABLED)
                    Global.getCombatEngine().addSmoothParticle(desiredLocation, droneTarget.getVelocity(), 40f, 50f, 0.1f, Color.blue);
                drones.get(drone).move(desiredLocation, drone);
                drones.get(drone).rotate(droneTarget.getFacing(), drone);
            }
        }
    }

    private static float getShieldRadiusForTarget(ShipAPI drone, ShipAPI droneTarget) {
        if (droneTarget == null) {
            return drone.getHullSpec().getShieldSpec().getRadius();
        }

        return Math.max(200f, droneTarget.getCollisionRadius()*1.4f);
    }

    private static ShipAPI getAppropriateAlly(ShipAPI ship, boolean includeSelf) {
        WeightedRandomPicker<ShipAPI> picker = new WeightedRandomPicker<>();
        if (includeSelf) picker.add(ship, scoreShipDanger(ship));
        for (ShipAPI ally : AIUtils.getNearbyAllies(ship, nearbyRange)) {
            if (ally.isFighter()) continue;
            picker.add(ally, scoreShipDanger(ally));
        }
        return picker.pick();
    }

    protected static float scoreShipDanger(ShipAPI ship) {
        //How close we are to overloading
        float fluxRatio = ship.getHardFluxLevel()/ship.getMaxFlux();
        //Inverse of max flux, if we're a vulerable craft, times above risk
        float danger = 1/ship.getMaxFlux() * fluxRatio;

        //The inverse of the enemy's flux usage multiplied by its potential usage for every relevant enemy
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 1500)) {
            danger *= 1/(enemy.getHardFluxLevel()+1/enemy.getMaxFlux()+1) * enemy.getMaxFlux();
        }
        return danger;
    }
}
