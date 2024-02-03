package org.selkie.kol.impl.shipsystems;

import activators.drones.DroneActivator;
import activators.drones.DroneFormation;
import activators.drones.PIDController;
import activators.drones.SpinningCircleFormation;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.combat.StarficzAIUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.selkie.kol.impl.combat.StarficzAIUtils.*;

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
        return new PIDController(15f,3.5f,10f,2f);
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
        return new FacingSpinningCircleFormation();
    }

    private class FacingSpinningCircleFormation extends DroneFormation {
        private final static float DRONE_ARC = 30f;
        private Map<Integer, ShipAPI> droneIndexMap = new HashMap<>();
        private final float rotationSpeed = 35;
        private float currentRotation = 0f;

        private IntervalUtil droneAIInterval = new IntervalUtil(0.3f, 0.5f);
        public float lastUpdatedTime = 0f;
        public List<FutureHit> incomingProjectiles = new ArrayList<>();
        public List<FutureHit> predictedWeaponHits = new ArrayList<>();
        public List<FutureHit> combinedHits = new ArrayList<>();
        public List<Float> omniShieldDirections = new ArrayList<>();
        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {

            float angleIncrease = 360f / getMaxDeployedDrones();
            droneAIInterval.advance(amount);
            if (droneAIInterval.intervalElapsed()) {
                lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
                incomingProjectiles = incomingProjectileHits(ship, ship.getLocation());
                predictedWeaponHits = generatePredictedWeaponHits(ship, ship.getLocation(), 3f);
                combinedHits = new ArrayList<>();
                combinedHits.addAll(incomingProjectiles);
                combinedHits.addAll(predictedWeaponHits);

                // prefilter expensive one time functions
                java.util.List<Float> candidateShieldDirections = new ArrayList<>();
                float FUZZY_RANGE = 0.3f;
                for(StarficzAIUtils.FutureHit hit : combinedHits){
                    boolean tooCLose = false;
                    for(float candidateDirection : candidateShieldDirections){
                        if (Math.abs(hit.angle - candidateDirection) < FUZZY_RANGE) { tooCLose = true; break; }
                    }
                    if(!tooCLose) candidateShieldDirections.add(hit.angle);
                }
                if (candidateShieldDirections.isEmpty()) candidateShieldDirections.add(ship.getFacing());
                omniShieldDirections = candidateShieldDirections;

            }


            // calculate how much damage the ship would take if shields went down
            float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
            float timeElapsed = currentTime - lastUpdatedTime;
            float armor = getWeakestTotalArmor(ship);


            List<Float> droneAngles = new ArrayList<>();
            droneAngles.addAll(omniShieldDirections);

            float bestLowestTime = Float.POSITIVE_INFINITY;
            List<Float> blockedDirections = new ArrayList<>();

            // get as many drone angles as we have drones
            for(int i=0; i < drones.size(); i++){
                List<Triple<Float, Float, Float>> potentialAngles = new ArrayList<>();
                // for each angle where there is incoming damage
                for(float droneAngle : droneAngles){
                    float damageBlocked = 0f;
                    float lowestTime = Float.POSITIVE_INFINITY;
                    // for each future hit check if its blocked, if so at what time and how much
                    for(FutureHit hit : combinedHits){
                        // skip damage already blocked by other drones
                        boolean alreadyBlocked = false;
                        for(float block : blockedDirections){
                            if(Misc.isInArc(block, DRONE_ARC, hit.angle)) {
                                alreadyBlocked = true;
                                break;
                            }
                        }
                        if(alreadyBlocked) continue;

                        // if the drone can block it, note down that stats
                        if(Misc.isInArc(droneAngle, DRONE_ARC, hit.angle) && hit.timeToHit > timeElapsed + 0.15f) {
                            Pair<Float, Float> trueDamage = damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armor, ship);
                            damageBlocked += trueDamage.one + trueDamage.two;
                            lowestTime = Math.min(lowestTime, hit.timeToHit - timeElapsed);
                        }
                    }
                    // add the potential angles to a list
                    if (damageBlocked > 0) potentialAngles.add(new Triple<Float, Float, Float>(droneAngle, lowestTime, damageBlocked));
                }

                Collections.sort(potentialAngles, new Comparator<Triple<Float, Float, Float>>() {
                    @Override
                    public int compare(Triple<Float, Float, Float> angleA, Triple<Float, Float, Float> angleB) {
                        // sort by closest damage, with every 1000 damage = 1 second closer
                        float damageTimeRatio = 1000f;
                        if((angleA.getSecond() - angleA.getThird()/damageTimeRatio) < (angleB.getSecond() - angleB.getThird()/damageTimeRatio)) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });

                if(!potentialAngles.isEmpty()){
                    blockedDirections.add(potentialAngles.get(0).getFirst());
                }
            }

            List<Vector2f> droneLocations = new ArrayList<>();

            // in case of nothing incoming, set idle angles
            currentRotation += rotationSpeed*amount;
            List<Float> idleAngles = new ArrayList<>();
            for (int i = 0; i < drones.size(); i++) {
                idleAngles.add( currentRotation + angleIncrease * i);
            }
            // move closest drone to its point
            List<ShipAPI> usedDrones = new ArrayList<>();
            for(float angle : blockedDirections.isEmpty() ? idleAngles : blockedDirections){
                Vector2f desiredLocation = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getShieldRadiusEvenIfNoShield() * 1.1f, angle);
                Global.getCombatEngine().addSmoothParticle(desiredLocation, ship.getVelocity(), 40f, 50f, 0.1f, Color.red);
                float closestDistance = Float.POSITIVE_INFINITY;
                ShipAPI closestDrone = null;
                for (ShipAPI drone : drones.keySet()){
                    if (usedDrones.contains(drone)) continue;
                    float droneDistance = MathUtils.getDistanceSquared(desiredLocation, drone.getLocation());
                    if(droneDistance < closestDistance){
                        closestDistance = droneDistance;
                        closestDrone = drone;
                    }
                }
                usedDrones.add(closestDrone);
                drones.get(closestDrone).move(desiredLocation, closestDrone);
                drones.get(closestDrone).rotate(Misc.getAngleInDegrees(ship.getLocation(), closestDrone.getLocation()), closestDrone);
            }

            if(!usedDrones.isEmpty()){
                for (ShipAPI drone : drones.keySet()){
                    if (usedDrones.contains(drone)) continue;

                    Vector2f desiredLocation = MathUtils.getPointOnCircumference(ship.getLocation(), MathUtils.getDistance(usedDrones.get(usedDrones.size()-1).getLocation(), ship.getLocation()) * 0.8f,
                            Misc.getAngleInDegrees(ship.getLocation(), usedDrones.get(0).getLocation()));
                    Global.getCombatEngine().addSmoothParticle(desiredLocation, ship.getVelocity(), 40f, 50f, 0.1f, Color.blue);
                    drones.get(drone).move(desiredLocation, drone);
                    drones.get(drone).rotate(Misc.getAngleInDegrees(ship.getLocation(), drone.getLocation()), drone);
                    usedDrones.add(drone);
                }
            }
        }
    }
}
