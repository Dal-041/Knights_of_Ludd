package org.selkie.kol.impl.combat.activators;

import activators.drones.DroneActivator;
import activators.drones.DroneFormation;
import activators.drones.PIDController;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Spawns a drone with an Ion Beam. Has no usable key and doesn't take a key index. Blocks wing system, activating it if the ship is venting.
 */
public class SimpleShieldDronesActivator extends DroneActivator {
    private static Color BASE_SHIELD_COLOR = Color.cyan;
    private static Color HIGHEST_FLUX_SHIELD_COLOR = Color.red;
    private static float SHIELD_ALPHA = 0.25f;

    public SimpleShieldDronesActivator(ShipAPI ship) {
        super(ship);
    }
    @Override
    public boolean canAssignKey() {
        return false;
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
        return 10f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return canActivate();
    }

    @Override
    public void onActivate() {
        for (ShipAPI drone : getActiveWings().keySet()) {
            if (drone.getFluxLevel() > 0f) {
                drone.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
    }

    @Override
    public PIDController getPIDController() {
        return new PIDController(15f,4f,10f,2f);
    }

    @Override
    public void advance(float amount) {
        for (ShipAPI drone : getActiveWings().keySet()) {
            if (drone.getShield().isOff() && !drone.getFluxTracker().isOverloadedOrVenting()) {
                drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            } else {
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
        }
    }

    @Override
    public String getDisplayText() {
        return "Shield Drones";
    }

    @Override
    public int getMaxCharges() { return 3; }

    @Override
    public int getMaxDeployedDrones() {
        return 3;
    }

    @Override
    public String getDroneVariant() {
        return "zea_dawn_chiwen_wing";
    }

    @NotNull
    @Override
    public DroneFormation getDroneFormation() {
        return new SpinningCircleFormation();
    }

    private class SpinningCircleFormation extends DroneFormation {
        private final static float ROTATION_SPEED = 20;
        private float currentRotation = 0f;
        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {

            // rotate idle
            currentRotation += ROTATION_SPEED * amount;

            // generate all locations by doubling up on locations if too many drones
            List<Vector2f> droneLocations = new ArrayList<>();
            float droneDistance = ship.getShieldRadiusEvenIfNoShield()*1.15f;
            for (int i = 0; i < drones.size(); i++) {
                float droneAngle = currentRotation + 360f/drones.size() * i;
                droneLocations.add(MathUtils.getPointOnCircumference(ship.getLocation(), droneDistance, droneAngle));
            }

            // move each drone to its closest location
            List<Vector2f> assignedPoints = new ArrayList<>();
            for (ShipAPI drone : drones.keySet()){
                Vector2f desiredLocation = null;
                float lowestDistance = Float.POSITIVE_INFINITY;
                for(Vector2f point : droneLocations){
                    if(assignedPoints.contains(point)) continue;
                    float currentDistance = MathUtils.getDistanceSquared(drone.getLocation(), point);
                    if(currentDistance < lowestDistance){
                        lowestDistance = currentDistance;
                        desiredLocation = point;
                    }
                }
                assignedPoints.add(desiredLocation);
                drones.get(drone).move(desiredLocation, drone);
                drones.get(drone).rotate(Misc.getAngleInDegrees(ship.getLocation(), drone.getLocation()), drone);
            }
        }
    }
}
