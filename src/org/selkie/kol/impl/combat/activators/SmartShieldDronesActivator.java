package org.selkie.kol.impl.combat.activators;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.drones.DroneFormation;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;
import org.magiclib.subsystems.drones.PIDController;
import org.selkie.kol.combat.StarficzAIUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Spawns a drone with an Ion Beam. Has no usable key and doesn't take a key index. Blocks wing system, activating it if the ship is venting.
 */
public class SmartShieldDronesActivator extends MagicDroneSubsystem {
    private static Color BASE_SHIELD_COLOR = Color.cyan;
    private static Color HIGHEST_FLUX_SHIELD_COLOR = Color.red;
    private static float SHIELD_ALPHA = 0.25f;

    public SmartShieldDronesActivator(ShipAPI ship) {
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
    public void advance(float amount, boolean isPaused) {
        if (isPaused) return;
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
    public int getMaxCharges() { return 0; }

    @Override
    public int getMaxDeployedDrones() {
        return 5;
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
        private final static float ROTATION_SPEED = 20;
        private float currentRotation = 0f;
        private final IntervalUtil droneAIInterval = new IntervalUtil(0.3f, 0.5f);
        private float lastUpdatedTime = 0f;
        private List<StarficzAIUtils.FutureHit> combinedHits = new ArrayList<>();
        private List<Float> omniShieldDirections = new ArrayList<>();
        @Override
        public void advance(@NotNull ShipAPI ship, @NotNull Map<ShipAPI, ? extends PIDController> drones, float amount) {

            List<ShipAPI> activeDrones = new ArrayList<>();
            List<ShipAPI> ventingDrones = new ArrayList<>();

            for(ShipAPI drone : drones.keySet()){
                if (drone.getFluxLevel() < 0.8f && !drone.getFluxTracker().isOverloadedOrVenting()) activeDrones.add(drone);
                else {
                    ventingDrones.add(drone);
                    drone.getFluxTracker().ventFlux();
                }
            }

            droneAIInterval.advance(amount);
            if (droneAIInterval.intervalElapsed()) {
                lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
                combinedHits = new ArrayList<>();
                combinedHits.addAll(StarficzAIUtils.incomingProjectileHits(ship, ship.getLocation()));
                combinedHits.addAll(StarficzAIUtils.generatePredictedWeaponHits(ship, ship.getLocation(), 3f));

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
            float armor = StarficzAIUtils.getCurrentArmorRating(ship);

            List<Float> droneAngles = new ArrayList<>();
            droneAngles.addAll(omniShieldDirections);

            float bestLowestTime = Float.POSITIVE_INFINITY;
            List<Float> blockedDirections = new ArrayList<>();

            // get as many drone angles as we have drones
            for(int i=0; i < activeDrones.size(); i++){
                List<Triple<Float, Float, Float>> potentialAngles = new ArrayList<>();
                // for each angle where there is incoming damage
                for(float droneAngle : droneAngles){
                    float damageBlocked = 0f;
                    float lowestTime = Float.POSITIVE_INFINITY;
                    // for each future hit check if its blocked, if so at what time and how much
                    for(StarficzAIUtils.FutureHit hit : combinedHits){
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
                            Pair<Float, Float> trueDamage = StarficzAIUtils.damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, armor, ship);
                            damageBlocked += trueDamage.one + trueDamage.two;
                            lowestTime = Math.min(lowestTime, hit.timeToHit - timeElapsed);
                        }
                    }
                    // add the potential angles to a list
                    if (damageBlocked > 0) potentialAngles.add(new Triple<Float, Float, Float>(droneAngle, lowestTime, damageBlocked));
                }

                // find the best direction and add it to blocked directions
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

            // rotate idle
            currentRotation += ROTATION_SPEED * amount;

            if(!activeDrones.isEmpty()) {
                // generate all locations by doubling up on locations if too many drones
                List<Vector2f> droneLocations = new ArrayList<>();
                float droneDistance = ship.getShieldRadiusEvenIfNoShield() * 0.95f;
                for (int i = 0; i < activeDrones.size(); i++) {
                    float droneAngle;
                    if (blockedDirections.isEmpty()) {
                        droneAngle = currentRotation + 360f / activeDrones.size() * i;
                    } else {
                        droneAngle = blockedDirections.get(i % blockedDirections.size());
                    }
                    droneLocations.add(MathUtils.getPointOnCircumference(ship.getLocation(), droneDistance, droneAngle));
                    if (!blockedDirections.isEmpty() && i % blockedDirections.size() == blockedDirections.size() - 1) droneDistance += 30f;
                }

                // fill weights for Hungarian Algorithm
                float[][] weights = new float[activeDrones.size()][droneLocations.size()];
                for (int i = 0; i < activeDrones.size(); i++) {
                    float[] weightRow = new float[droneLocations.size()];
                    for (int j = 0; j < droneLocations.size(); j++) {
                        weightRow[j] = Misc.getDistance(droneLocations.get(j), activeDrones.get(i).getLocation());
                    }
                    weights[i] = weightRow;
                }

                // execute and move drones
                StarficzAIUtils.HungarianAlgorithm algo = new StarficzAIUtils.HungarianAlgorithm(weights);
                int[] results = algo.execute();
                for (int i = 0; i < activeDrones.size(); i++) {
                    ShipAPI drone = activeDrones.get(i);
                    drones.get(drone).move(droneLocations.get(results[i]), drone);
                    drones.get(drone).rotate(Misc.getAngleInDegrees(ship.getLocation(), drone.getLocation()), drone);
                }
            }

            if(!ventingDrones.isEmpty()){
                // deal with venting drones
                List<Vector2f> droneVentingLocations = new ArrayList<>();
                for (int i = 0; i < ventingDrones.size(); i++) {
                    droneVentingLocations.add(MathUtils.getPointOnCircumference(ship.getLocation(),  ship.getShieldRadiusEvenIfNoShield()*0.3f, currentRotation + 360f/ventingDrones.size() * i));
                }

                // move each drone to its closest location
                // fill weights for Hungarian Algorithm
                float[][] ventingWeights = new float[ventingDrones.size()][droneVentingLocations.size()];
                for (int i = 0; i < ventingDrones.size(); i++){
                    float[] weightRow = new float[droneVentingLocations.size()];
                    for(int j = 0; j < droneVentingLocations.size(); j++){
                        weightRow[j] = Misc.getDistance(droneVentingLocations.get(j), ventingDrones.get(i).getLocation());
                    }
                    ventingWeights[i] = weightRow;
                }

                // execute and move drones
                StarficzAIUtils.HungarianAlgorithm ventAlgo = new StarficzAIUtils.HungarianAlgorithm(ventingWeights);
                int[] ventResults = ventAlgo.execute();
                for (int i = 0; i < ventingDrones.size(); i++){
                    ShipAPI drone = ventingDrones.get(i);
                    drones.get(drone).move(droneVentingLocations.get(ventResults[i]), drone);
                    drones.get(drone).rotate(Misc.getAngleInDegrees(ship.getLocation(), drone.getLocation()), drone);
                }
            }
        }
    }
}
