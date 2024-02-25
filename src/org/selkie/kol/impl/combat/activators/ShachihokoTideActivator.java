package org.selkie.kol.impl.combat.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShieldAPI;
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
import org.selkie.kol.Utils;
import org.selkie.kol.combat.StarficzAIUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShachihokoTideActivator extends MagicDroneSubsystem {
    private static final Color BASE_SHIELD_COLOR = new Color(220, 190, 70, 255);
    private static final Color HIGHEST_FLUX_SHIELD_COLOR = Color.red;
    private static final float SHIELD_ALPHA = 0.25f;
    private static final float nearbyRange = 2000;
    private final IntervalUtil intervalCheck = new IntervalUtil(0.25f, 0.5f);
    private final PIDController PID = new PIDController(6f, 4f, 8f, 3f);

    public ShachihokoTideActivator(ShipAPI ship) {
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
    public float getBaseChargeRechargeDuration() {
        return 10f;
    }

    @Override
    public boolean canActivate() {
        return false;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return false;
    }

    @Override
    public @NotNull PIDController getPIDController() {
        return PID.copy();
    }

    @NotNull
    @Override
    public ShipAPI spawnDrone() {
        ShipAPI drone = super.spawnDrone();
        drone.getMutableStats().getHardFluxDissipationFraction().modifyMult("shachihokoCorruptedHardFluxDiss", 0f);
        return drone;
    }

    public ShipAPI findDroneTarget(ShipAPI drone) {
        ShipAPI bestTarget = null;
        float maxDanger = 0f;

        for (ShipAPI ally : AIUtils.getNearbyAllies(ship, nearbyRange)) {
            if (ally.isFighter()) continue;
            if (Utils.anyDronesShieldingShip(ally)) continue;

            if (bestTarget == null) {
                bestTarget = ally;
            } else {
                if (bestTarget.getHullSize().ordinal() < ally.getHullSize().ordinal()) {
                    bestTarget = ally;
                } else if (bestTarget.getShield() == null || bestTarget.getShield().getType() == ShieldAPI.ShieldType.NONE) {
                    bestTarget = ally;
                } else {
                    float danger = scoreShipDanger(ship);
                    if (danger >= maxDanger) {
                        bestTarget = ally;
                        maxDanger = danger;
                    }
                }
            }
        }

        return bestTarget;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        for (ShipAPI drone : getActiveWings().keySet()) {
            ShipAPI droneTarget = Utils.getDroneShieldTarget(drone);
            if (droneTarget != null && !droneTarget.isAlive()) {
                droneTarget = null;
                Utils.setDroneShieldTarget(drone, null);
            }

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

            if (droneTarget == null) {
                if (drone.getFluxLevel() > 0f) {
                    drone.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                }

                intervalCheck.advance(amount);
                if (intervalCheck.intervalElapsed()) {
                    droneTarget = findDroneTarget(drone);
                    if (droneTarget != null) {
                        Utils.setDroneShieldTarget(drone, droneTarget);
                    }
                }
            } else {
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
            }

            //vent if we absolutely need to
            if (drone.getFluxLevel() > 0.90f) {
                drone.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
    }

    @Override
    public String getDisplayText() {
        return "Shachihoko Tide";
    }

    @Override
    public int getMaxCharges() {
        return 2;
    }

    @Override
    public int getMaxDeployedDrones() {
        return 8;
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
        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {
            List<Vector2f> untargetedDroneLocations = new ArrayList<>();
            Map<ShipAPI, ShipAPI> droneTargets = new HashMap<>();
            Map<ShipAPI, Vector2f> targetedDroneLocations = new HashMap<>();
            List<Float> shipDirections = new ArrayList<>();
            shipDirections.add(ship.getFacing() - 180f);
            shipDirections.add(ship.getFacing() - 155f);
            shipDirections.add(ship.getFacing() - 205f);

            int i = 0;
            int circles = 0;
            for (ShipAPI drone : drones.keySet()) {
                ShipAPI droneTarget = Utils.getDroneShieldTarget(drone);
                if (droneTarget != null) {
                    float droneDistance = getShieldRadiusForTarget(drone, droneTarget) * 0.33f + 30f * circles;
                    float droneAngle = droneTarget.getFacing() - 180f;
                    targetedDroneLocations.put(drone, MathUtils.getPointOnCircumference(droneTarget.getLocation(), droneDistance, droneAngle));
                } else {
                    droneTarget = ship;
                    float droneDistance = getShieldRadiusForTarget(drone, ship) * 0.33f + 30f * circles;
                    float droneAngle = shipDirections.get(i % shipDirections.size());
                    untargetedDroneLocations.add(MathUtils.getPointOnCircumference(ship.getLocation(), droneDistance, droneAngle));

                    if (i % shipDirections.size() == shipDirections.size() - 1)
                        circles++;
                    i++;
                }
                droneTargets.put(drone, droneTarget);
            }

            // move each drone to its closest location
            List<Vector2f> assignedPoints = new ArrayList<>();
            for (ShipAPI drone : drones.keySet()) {
                ShipAPI droneTarget = droneTargets.get(drone);
                Vector2f desiredLocation = null;
                if (targetedDroneLocations.containsKey(drone)) {
                    desiredLocation = targetedDroneLocations.get(drone);

                    if (StarficzAIUtils.DEBUG_ENABLED | true) {
                        Global.getCombatEngine().addSmoothParticle(drone.getLocation(), drone.getVelocity(), 40f, 50f, 0.1f, Color.red);
                        Global.getCombatEngine().addSmoothParticle(desiredLocation, droneTarget.getVelocity(), 40f, 50f, 0.1f, Color.red);
                    }
                } else {
                    float lowestDistance = Float.POSITIVE_INFINITY;
                    for (Vector2f point : untargetedDroneLocations) {
                        if (assignedPoints.contains(point)) continue;
                        float currentDistance = MathUtils.getDistanceSquared(drone.getLocation(), point);
                        if (currentDistance < lowestDistance) {
                            lowestDistance = currentDistance;
                            desiredLocation = point;
                        }
                    }

                    assignedPoints.add(desiredLocation);

                    if (StarficzAIUtils.DEBUG_ENABLED | true)
                        Global.getCombatEngine().addSmoothParticle(desiredLocation, droneTarget.getVelocity(), 40f, 50f, 0.1f, Color.blue);
                }


                drones.get(drone).move(desiredLocation, drone);
                drones.get(drone).rotate(droneTarget.getFacing(), drone);
            }
        }
    }

    private static float getShieldRadiusForTarget(ShipAPI drone, ShipAPI droneTarget) {
        if (droneTarget == null) {
            return drone.getHullSpec().getShieldSpec().getRadius();
        }

        return Math.max(200f, droneTarget.getCollisionRadius() * 1.4f);
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
        float fluxRatio = ship.getHardFluxLevel() / ship.getMaxFlux();
        //Inverse of max flux, if we're a vulerable craft, times above risk
        float danger = 1 / ship.getMaxFlux() * fluxRatio;

        //The inverse of the enemy's flux usage multiplied by its potential usage for every relevant enemy
        for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 1500)) {
            danger *= 1 / (enemy.getHardFluxLevel() + 1 / enemy.getMaxFlux() + 1) * enemy.getMaxFlux();
        }
        return danger;
    }
}
