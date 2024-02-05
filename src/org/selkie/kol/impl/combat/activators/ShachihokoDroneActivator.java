package org.selkie.kol.impl.combat.activators;

import activators.drones.DroneActivator;
import activators.drones.DroneFormation;
import activators.drones.PIDController;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.combat.StarficzAIUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

public class ShachihokoDroneActivator extends DroneActivator {
    private static final Color BASE_SHIELD_COLOR = Color.cyan;
    private static final Color HIGHEST_FLUX_SHIELD_COLOR = Color.red;
    private static final float SHIELD_ALPHA = 0.25f;

    private ShipAPI target = null;

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
    public boolean shouldActivateAI(float amount) {
        return canActivate();
    }

    @Override
    public void onActivate() {
        if (target != null) {
            target = null;
            return;
        }

        if (ship.getShipTarget() != null && ship.getShipTarget().getOwner() == ship.getOwner()) {
            target = ship.getShipTarget();
        }
    }

    @Override
    public PIDController getPIDController() {
        return new PIDController(15f, 3.5f, 10f, 2f);
    }

    @Override
    public void advance(float amount) {
        if (target != null && !target.isAlive()) {
            target = null;
        }

        ShipAPI droneTarget = ship;
        if (target != null) {
            droneTarget = target;
        }

        for (ShipAPI drone : getActiveWings().keySet()) {
            if (drone.getShield().isOn()) {
                if (MathUtils.getDistance(drone.getLocation(), droneTarget.getLocation()) > droneTarget.getCollisionRadius()) {
                    //turn shield off if too far from target
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    drone.getShield().setRadius(drone.getHullSpec().getShieldSpec().getRadius());
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
            } else {
                if (MathUtils.getDistance(drone.getLocation(), droneTarget.getLocation()) <= droneTarget.getCollisionRadius()) {
                    //turn shield on if close to target
                    drone.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    drone.getShield().setRadius(droneTarget.getShieldRadiusEvenIfNoShield() * 1.75f);
                } else {
                    //keep shield off otherwise
                    drone.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                }
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
        private final static float DRONE_ARC = 30f;
        private final static float ROTATION_SPEED = 20;
        private float currentRotation = 0f;
        private final IntervalUtil droneAIInterval = new IntervalUtil(0.3f, 0.5f);
        private float lastUpdatedTime = 0f;
        private List<StarficzAIUtils.FutureHit> combinedHits = new ArrayList<>();
        private List<Float> omniShieldDirections = new ArrayList<>();

        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {
            // rotate idle
            currentRotation += ROTATION_SPEED * amount;
            List<Float> blockedDirections = new ArrayList<>();

            if (target != null) {
                droneAIInterval.advance(amount);
                if (droneAIInterval.intervalElapsed()) {
                    lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
                    combinedHits = new ArrayList<>();
                    combinedHits.addAll(StarficzAIUtils.incomingProjectileHits(target, target.getLocation()));
                    combinedHits.addAll(StarficzAIUtils.generatePredictedWeaponHits(target, target.getLocation(), 3f));

                    // prefilter expensive one time functions
                    List<Float> candidateShieldDirections = new ArrayList<>();
                    float FUZZY_RANGE = 0.3f;
                    for (StarficzAIUtils.FutureHit hit : combinedHits) {
                        boolean tooCLose = false;
                        for (float candidateDirection : candidateShieldDirections) {
                            if (Math.abs(hit.angle - candidateDirection) < FUZZY_RANGE) {
                                tooCLose = true;
                                break;
                            }
                        }
                        if (!tooCLose) candidateShieldDirections.add(hit.angle);
                    }
                    if (candidateShieldDirections.isEmpty()) candidateShieldDirections.add(target.getFacing());
                    omniShieldDirections = candidateShieldDirections;
                }

                // calculate how much damage the ship would take if shields went down
                float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
                float timeElapsed = currentTime - lastUpdatedTime;
                float armor = StarficzAIUtils.getWeakestTotalArmor(target);

                List<Float> droneAngles = new ArrayList<>();
                droneAngles.addAll(omniShieldDirections);

                // get as many drone angles as we have drones
                for (int i = 0; i < drones.size(); i++) {
                    List<Triple<Float, Float, Float>> potentialAngles = new ArrayList<>();
                    // for each angle where there is incoming damage
                    for (float droneAngle : droneAngles) {
                        float damageBlocked = 0f;
                        float lowestTime = Float.POSITIVE_INFINITY;
                        // for each future hit check if its blocked, if so at what time and how much
                        for (StarficzAIUtils.FutureHit hit : combinedHits) {
                            // skip damage already blocked by other drones
                            boolean alreadyBlocked = false;
                            for (float block : blockedDirections) {
                                if (Misc.isInArc(block, DRONE_ARC, hit.angle)) {
                                    alreadyBlocked = true;
                                    break;
                                }
                            }
                            if (alreadyBlocked) continue;

                            // if the drone can block it, note down that stats
                            if (Misc.isInArc(droneAngle, DRONE_ARC, hit.angle) && hit.timeToHit > timeElapsed + 0.15f) {
                                Pair<Float, Float> trueDamage = StarficzAIUtils.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armor, target);
                                damageBlocked += trueDamage.one + trueDamage.two;
                                lowestTime = Math.min(lowestTime, hit.timeToHit - timeElapsed);
                            }
                        }
                        // add the potential angles to a list
                        if (damageBlocked > 0)
                            potentialAngles.add(new Triple<>(droneAngle, lowestTime, damageBlocked));
                    }

                    // find the best direction and add it to blocked directions
                    Collections.sort(potentialAngles, new Comparator<Triple<Float, Float, Float>>() {
                        @Override
                        public int compare(Triple<Float, Float, Float> angleA, Triple<Float, Float, Float> angleB) {
                            // sort by closest damage, with every 1000 damage = 1 second closer
                            float damageTimeRatio = 1000f;
                            if ((angleA.getSecond() - angleA.getThird() / damageTimeRatio) < (angleB.getSecond() - angleB.getThird() / damageTimeRatio)) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    });

                    if (!potentialAngles.isEmpty()) {
                        blockedDirections.add(potentialAngles.get(0).getFirst());
                    }
                }
            } else {
                //stay behind ship

                if (drones.size() == 1) {
                    blockedDirections.add(ship.getFacing() - 180f);
                } else if (drones.size() % 2 == 0) {
                    blockedDirections.add(ship.getFacing() - 150f);
                    blockedDirections.add(ship.getFacing() - 210f);
                } else {
                    blockedDirections.add(ship.getFacing() - 135f);
                    blockedDirections.add(ship.getFacing() - 180f);
                    blockedDirections.add(ship.getFacing() - 225f);
                }
            }

            // generate all locations by doubling up on locations if too many drones
            ShipAPI droneTarget = ship;
            float droneDistance = ship.getShieldRadiusEvenIfNoShield() * 0.95f;
            if (target != null) {
                droneTarget = target;
                droneDistance = target.getShieldRadiusEvenIfNoShield() * 0.95f;
            }

            List<Vector2f> droneLocations = new ArrayList<>();
            for (int i = 0; i < drones.size(); i++) {
                float droneAngle;
                if (blockedDirections.isEmpty()) {
                    droneAngle = currentRotation + 360f / drones.size() * i;
                } else {
                    droneAngle = blockedDirections.get(i % blockedDirections.size());
                }
                droneLocations.add(MathUtils.getPointOnCircumference(droneTarget.getLocation(), droneDistance, droneAngle));
                if (!blockedDirections.isEmpty() && i % blockedDirections.size() == blockedDirections.size() - 1)
                    droneDistance += 30f;
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
                drones.get(drone).rotate(Misc.getAngleInDegrees(droneTarget.getLocation(), drone.getLocation()), drone);
            }
        }
    }
}
