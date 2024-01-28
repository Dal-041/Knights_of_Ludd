package org.selkie.kol.impl.shipsystems;

import activators.drones.DroneActivator;
import activators.drones.DroneFormation;
import activators.drones.PIDController;
import activators.drones.SpinningCircleFormation;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Spawns a drone with an Ion Beam. Has no usable key and doesn't take a key index. Blocks wing system, activating it if the ship is venting.
 */
public class CircularShieldDroneActivator extends DroneActivator {
    private static Color BASE_SHIELD_COLOR = Color.cyan;
    private static Color HIGHEST_FLUX_SHIELD_COLOR = Color.red;
    private static float SHIELD_ALPHA = 0.25f;

    public CircularShieldDroneActivator(ShipAPI ship) {
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

            float shieldCenter = ship.getCollisionRadius();
            drone.getShield().setActiveArc(Math.min(180, 360 / getMaxDeployedDrones()));
            drone.getShield().setCenter(-shieldCenter, 0);
            drone.getShield().setRadius(shieldCenter + 80f);
        }
    }

    @Override
    public String getDisplayText() {
        return "Shield Drones";
    }

    @Override
    public int getMaxCharges() {
        return 0;
    }

    @Override
    public int getMaxDeployedDrones() {
        return 6;
    }

    @Override
    public String getDroneVariant() {
        return "shield_drone_wing";
    }

    @NotNull
    @Override
    public DroneFormation getDroneFormation() {
        return new FacingSpinningCircleFormation();
    }

    private class FacingSpinningCircleFormation extends DroneFormation {
        private Map<Integer, ShipAPI> droneIndexMap = new HashMap<>();
        private final float rotationSpeed = 0.2f;
        private float currentRotation = 0f;

        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {
            float angleIncrease = 360f / getMaxDeployedDrones();
            currentRotation += rotationSpeed;

            for (int i = 0; i < CircularShieldDroneActivator.this.getMaxDeployedDrones(); i++) {
                if (droneIndexMap.containsKey(i)) {
                    ShipAPI drone = droneIndexMap.get(i);
                    if (!drone.isAlive() || !drones.containsKey(drone)) {
                        droneIndexMap.remove(i);
                    }
                }

                if (!droneIndexMap.containsKey(i)) {
                    for (Map.Entry<ShipAPI, ? extends PIDController> entry : drones.entrySet()) {
                        ShipAPI drone = entry.getKey();
                        if (!droneIndexMap.containsValue(drone)) {
                            droneIndexMap.put(i, drone);
                        }
                    }
                }

                if (droneIndexMap.containsKey(i)) {
                    ShipAPI drone = droneIndexMap.get(i);
                    PIDController controller = drones.get(drone);

                    Vector2f shipLocation = ship.getLocation();

                    Vector2f desiredLocation = MathUtils.getPointOnCircumference(shipLocation, ship.getCollisionRadius() * 1.16f, currentRotation + angleIncrease * i);
                    controller.move(desiredLocation, drone);
                    controller.rotate(Misc.getAngleInDegrees(ship.getLocation(), drone.getLocation()), drone);
                }
            }
        }
    }
}
