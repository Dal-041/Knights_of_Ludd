package org.selkie.zea.combat.subsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.magiclib.subsystems.drones.PIDController;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.zea.helpers.ZeaStaticStrings;

import java.util.Iterator;
import java.util.Map;

/**
 * Spawns a drone with an Ion Beam. Has no usable key and doesn't take a key index. Blocks wing system, activating it if the ship is venting.
 */
public class PDDroneSubsystem extends MagicDroneSubsystem {
    public PDDroneSubsystem(ShipAPI ship) {
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
        return 0;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        return canActivate();
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
        if (ship.getFluxTracker().isOverloaded()) {
            for (ShipAPI wing : getActiveWings().keySet()) {
                if (!wing.getFluxTracker().isOverloaded()) {
                    wing.getFluxTracker().beginOverloadWithTotalBaseDuration(ship.getFluxTracker().getOverloadTimeRemaining());
                }
            }
            return;
        }

        if (ship.getFluxTracker().isVenting()) {
            for (ShipAPI wing : getActiveWings().keySet()) {
                wing.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
            }
        } else {
            for (ShipAPI wing : getActiveWings().keySet()) {
                wing.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            }
        }
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
    public String getDisplayText() {
        return "Shachi PD Drones";
    }

    @Override
    public String getStateText() {
        return "";
    }

    @Override
    public float getBarFill() {
        float fill = 0f;
        if (charges < getMaxCharges()) {
            fill = chargeInterval.getElapsed() / chargeInterval.getIntervalDuration();
        }

        return fill;
    }

    @Override
    public int getMaxCharges() {
        return 0;
    }

    @Override
    public int getMaxDeployedDrones() {
        return 2;
    }

    @Override
    public boolean usesChargesOnActivate() {
        return false;
    }

    @Override
    public @NotNull String getDroneVariant() {
        return ZeaStaticStrings.ZEA_EDF_SHACHI_WING;
    }

    @NotNull
    @Override
    public DroneFormation getDroneFormation() {
        return new PDDroneFormation();
    }

    private static class PDDroneFormation extends DroneFormation {
        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {
            int index = 0;
            for (Map.Entry<ShipAPI, ? extends PIDController> entry : drones.entrySet()) {
                ShipAPI drone = entry.getKey();
                PIDController controller = entry.getValue();
                Vector2f shipLocation = ship.getLocation();
                float angle = ship.getFacing() + 180;
                float aheadAngle = angle;
                if (isOnLeft(drone, index)){
                    angle += 30;
                    aheadAngle += 33.13f;
                }
                else{
                    angle -= 30;
                    aheadAngle -= 33.13f;
                }
                Vector2f desiredLocation = MathUtils.getPointOnCircumference(shipLocation, 1000f, angle);
                float radius = Misc.getTargetingRadius(desiredLocation, ship, false) + 50f;
                desiredLocation = MathUtils.getPointOnCircumference(shipLocation, radius, angle);

                Vector2f facingLocation = Vector2f.add(new Vector2f(desiredLocation), VectorUtils.resize(Misc.getUnitVectorAtDegreeAngle(ship.getFacing()), 50),null);
                StarficzAIUtils.turnToPoint(drone, facingLocation);

                controller.move(desiredLocation, drone);

                Iterator<Object> iter = Global.getCombatEngine().getShipGrid().getCheckIterator(drone.getLocation(), 100f, 100f);
                ShipAPI target = null;
                float minDistance = 10000000f;
                while (iter.hasNext()) {
                    Object obj = iter.next();
                    if (obj instanceof ShipAPI) {
                        ShipAPI potentialTarget = (ShipAPI) obj;
                        if (potentialTarget.isFighter()) continue;
                        if (potentialTarget.getOwner() == ship.getOwner()) continue;
                        if (potentialTarget.isHulk()) continue;

                        float distance = MathUtils.getDistanceSquared(potentialTarget, ship);
                        if (minDistance > distance) {
                            minDistance = distance;
                            target = potentialTarget;
                        }
                    }
                }

                if (target != null) {
                    controller.rotate(Misc.getAngleInDegrees(drone.getLocation(), target.getLocation()), drone);
                } else {
                    controller.rotate(ship.getFacing(), drone);
                }

                index++;
            }
        }

        private boolean isOnLeft(ShipAPI drone, int index) {
            return index % 2 == 0;
        }
    }
}
