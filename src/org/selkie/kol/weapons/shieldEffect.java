package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;
//import static data.scripts.util.SKR_txt.txt;
import java.awt.Color;
import java.util.*;

//Concept and impl by Tartiflette, AI by Starficz

public class shieldEffect implements EveryFrameWeaponEffectPlugin {
    
    private final float MAX_SHIELD=10;
    private final static boolean DEBUG = true;
    private boolean runOnce = false, disabled=false;
    private float shieldT=0,aiT=0;
    private ShipAPI ship;
    private CombatEngineAPI engine;
    private ShieldAPI shield;
    private boolean wantToShield;
    private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.3f); //Seconds
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        
        if (!runOnce){
            runOnce=true;
            ship=weapon.getShip();
            shield=ship.getShield();
            this.engine=engine;
        }
        
        if (engine.isPaused() || !ship.isAlive()) {return;}
        
        if(shield!=null){
            if(!disabled){
                if(shield.isOn()){
                    shieldT=Math.min(MAX_SHIELD, shieldT+amount);
                } else {
                    shieldT=Math.max(0, shieldT-(amount*2));
                }
                
                //UI
                float color=shieldT/MAX_SHIELD;
//                MagicUI.drawSystemBar(ship,new Color(color,1,(1-color)/2), shieldT/MAX_SHIELD, 0);
                MagicUI.drawInterfaceStatusBar(ship, 1-shieldT/MAX_SHIELD, new Color(color*0.4f+0.6f,1f-0.4f*color,0), null, 0, "SHIELD", (int) (100-100*shieldT/MAX_SHIELD));

                //AI

                //If debug is turned on, weapons that can hit the ship will be blue, and projectiles that can hit the ship will be magenta

                tracker.advance(amount); // use a tracker for the expensive damage calcs
                if (tracker.intervalElapsed()) {
                    //Cap estimate at 80 degrees of unfold (about what is needed to block a full direction)
                    float estimateTime = Math.min(ship.getShield().getUnfoldTime() / (ship.getShield().getArc() / 80), ship.getShield().getUnfoldTime());
                    float[] estimatedBlockableDamage = estimateBlockableDamage(ship, estimateTime);
                    float[] estimatedBlockableUnfiredDamage = estimateBlockableUnfiredDamage(ship, estimateTime);

                    float estimatedHullDamage = estimatedBlockableDamage[0] + estimatedBlockableUnfiredDamage[0];
                    float estimatedShieldHardFlux = estimatedBlockableDamage[1] + estimatedBlockableUnfiredDamage[1];
                    float estimatedEMPDamage = estimatedBlockableDamage[2] + estimatedBlockableUnfiredDamage[2];

                    // consider the max of shield time or flux level to decide when to shield
                    float alpha = Math.max(ship.getFluxLevel(), shieldT / MAX_SHIELD);
                    float lowDamage = 0.001f; // 0.1% hull damage
                    float highDamage = 0.020f; // 2.0% hull damage

                    // shield based on the current flux level and shield timer
                    wantToShield = (estimatedHullDamage + estimatedEMPDamage/3) > ship.getHitpoints() * (alpha * highDamage + (1 - alpha) * lowDamage);

                    // if the damage is high enough to shield, but flux is high/ shield timer is low, armor tank KE
                    if (wantToShield && estimatedShieldHardFlux/(estimatedHullDamage + estimatedEMPDamage/3) > (alpha * 2f + (1 - alpha) * 7f))
                        wantToShield = false;

                    if (shieldT + estimateTime > MAX_SHIELD) // Save enough time for 1 emergency unfold
                        wantToShield = false;

                    if ((estimatedShieldHardFlux + ship.getCurrFlux())/ship.getMaxFlux() > 1) // Prevent overloads...
                        wantToShield = false;

                    if (estimatedHullDamage > 4000f) // Unless a reaper is on the way, in that case try and block it no matter what
                        wantToShield = true;
                }

                if (!disabled && (!engine.isUIAutopilotOn() || engine.getPlayerShip() != ship)) {
                    if (shield.isOn() ^ wantToShield)
                        ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    else
                        ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                }

                // Overloaded Shields
                if(shieldT==MAX_SHIELD){
                    shield.toggleOff();
                    ship.getFluxTracker().showOverloadFloatyIfNeeded("Shield offline.", Color.red, 2, true);
                    ship.getFluxTracker().beginOverloadWithTotalBaseDuration(0.05f);
                    disabled=true;
                }
                
            } else {
                shield.toggleOff();
                shieldT=Math.max(0, shieldT-(amount/2));
                
//                MagicUI.drawSystemBar(ship,new Color(255,0,0), shieldT/MAX_SHIELD,0);
                MagicUI.drawInterfaceStatusBar(ship, 1-shieldT/MAX_SHIELD, Color.RED, null, 0, "SHIELD", (int) (100-100*shieldT/MAX_SHIELD));
                
                if(shieldT==0){
                    disabled=false;
                    ship.getFluxTracker().showOverloadFloatyIfNeeded("Shield online.", Color.green, 2, true);
                }
            }
        }
    }

    public static ArrayList<DamagingProjectileAPI> getAllProjectilesInRange(Vector2f loc, float radius) {
        ArrayList<DamagingProjectileAPI> entities = new ArrayList<>();

        Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(loc, radius * 2, radius * 2);
        while (iterator.hasNext()) {

            Object next = iterator.next();
            if (!(next instanceof DamagingProjectileAPI)) continue;
            if (MathUtils.getDistanceSquared(loc, ((DamagingProjectileAPI) next).getLocation()) > radius*radius) continue;

            entities.add((DamagingProjectileAPI) next);
        }
        return entities;
    }

    public static float[] estimateBlockableDamage(ShipAPI ship, float secondsToEstimate){
        float MAX_SPEED_OF_PROJECTILE = 2000f;

        Set<DamagingProjectileAPI> nearbyUnguided = new HashSet<>();
        Set<MissileAPI> nearbyGuided = new HashSet<>();

        // Sort all projectiles into guided or unguided
        for (DamagingProjectileAPI threat : getAllProjectilesInRange(ship.getLocation(), secondsToEstimate * MAX_SPEED_OF_PROJECTILE)) {

            boolean blockable;
            // assume omni/phase shields can block everything
            if (ship.getShield().getType() == ShieldAPI.ShieldType.FRONT)
                blockable = MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(), threat.getLocation()), ship.getFacing()) < ship.getShield().getArc()/2;
            else
                blockable = true;

            if (!threat.isFading() && threat.getOwner() != ship.getOwner() && blockable) {
                if (threat instanceof MissileAPI){
                    MissileAPI missile = (MissileAPI) threat;
                    if (!missile.isFlare()) {
                        if (!missile.isGuided() || !(missile.getEngineController().isTurningLeft() || missile.getEngineController().isTurningRight()))
                            nearbyUnguided.add(threat);
                        else
                            nearbyGuided.add(missile);
                    }
                }else{
                    nearbyUnguided.add(threat);
                }
            }
        }

        Set<DamagingProjectileAPI> estimatedHits = new HashSet<>();

        // do a line-circle collision check for unguided
        for (DamagingProjectileAPI unguided : nearbyUnguided){
            float radius = Misc.getTargetingRadius(unguided.getLocation(), ship, false);
            float maxSpeed = (unguided instanceof MissileAPI) ? ((MissileAPI) unguided).getMaxSpeed() : unguided.getMoveSpeed();
            Vector2f futureProjectileLocation = Vector2f.add(unguided.getLocation(), VectorUtils.resize(new Vector2f(unguided.getVelocity()), secondsToEstimate*maxSpeed), null);
            float hitDistance = MathUtils.getDistance(ship.getLocation(), unguided.getLocation()) - radius;
            float travelTime = hitDistance/unguided.getMoveSpeed();
            Vector2f futureTestPoint = Vector2f.add(ship.getLocation(), (Vector2f) new Vector2f(ship.getVelocity()).scale(travelTime), null);
            if (CollisionUtils.getCollides(unguided.getLocation(), futureProjectileLocation, futureTestPoint, radius)){
                estimatedHits.add(unguided);
            }
        }

        // estimate the missile flight time and check if it will hit the ship
        for (MissileAPI guided : nearbyGuided){
            float radius = Misc.getTargetingRadius(guided.getLocation(), ship, false);
            if(MathUtils.isPointWithinCircle(guided.getLocation(), ship.getLocation(), radius)){
                estimatedHits.add(guided);
                continue;
            }
            float travelTime = missileTravelTime(guided.getMoveSpeed(), guided.getMaxSpeed(), guided.getAcceleration(),
                    guided.getMaxTurnRate(), VectorUtils.getFacing(guided.getVelocity()), guided.getLocation(), ship.getLocation(), radius );

            if ((travelTime < secondsToEstimate) && (travelTime < guided.getMaxFlightTime() - guided.getFlightTime())){
                estimatedHits.add(guided);
            }
        }

        float estimatedHullDamage = 0f;
        float estimatedShieldHardFlux = 0f;
        float estimatedEMPDamage = 0f;
        // sum up all the actual damage after armor
        float armorValue = getWeakestTotalArmor(ship);
        for(DamagingProjectileAPI hit : estimatedHits){
            if (Global.getSettings().isDevMode()) Global.getCombatEngine().addSmoothParticle(hit.getLocation(), hit.getVelocity(), 30f, 5f, 0.1f, Color.magenta);
            float hullDamageTaken = damageAfterArmor(hit.getDamageType(), hit.getDamageAmount(), hit.getDamageAmount(), armorValue);
            float shieldHardFlux = damageToShield(hit.getDamageType(), hit.getDamageAmount(), ship.getShield().getFluxPerPointOfDamage());

            estimatedHullDamage += hullDamageTaken;
            estimatedShieldHardFlux += shieldHardFlux;
            estimatedEMPDamage += ship.getMutableStats().getEmpDamageTakenMult().getModifiedValue() * hit.getEmpAmount();

            armorValue = Math.max(0, armorValue - hullDamageTaken);
        }

        /*TODO: rewrite this entire section, currently I assume beam speed is instant so estimateBlockableUnfiredDamage() is already counting beam damage,
                no need to count twice until I figure out how to get actual beam speed from a WeaponAPI*/
        // Handle beams with line-circle collision checks
        // TODO: check if getBeams() can be replaced with something like getAllProjectilesInRange() that uses the object grid
        List<BeamAPI> nearbyBeams = Global.getCombatEngine().getBeams();
        for (BeamAPI beam : nearbyBeams) {
            if (beam.getSource().getOwner() != ship.getOwner() && CollisionUtils.getCollides(beam.getFrom(), beam.getTo(), ship.getLocation(), Misc.getTargetingRadius(beam.getFrom(), ship, true))) {
                WeaponAPI beamWeapon = beam.getWeapon();
                WeaponAPI.DerivedWeaponStatsAPI beamStats = beamWeapon.getDerivedStats();

                float totalDamage;
                float EMPDamageTaken;
                if(beamWeapon.isBurstBeam()){
                    float burstRemainingLevel = (Math.min(beamWeapon.getBurstFireTimeRemaining(), secondsToEstimate) / beamStats.getBurstFireDuration());
                    totalDamage = burstRemainingLevel * beamStats.getBurstDamage();
                    EMPDamageTaken = burstRemainingLevel * beamWeapon.getDerivedStats().getEmpPerShot() * ship.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
                }
                else {
                    totalDamage = beam.getWeapon().getDamage().computeDamageDealt(secondsToEstimate);
                    EMPDamageTaken = beamStats.getEmpPerSecond() * secondsToEstimate * ship.getMutableStats().getEmpDamageTakenMult().getModifiedValue();
                }

                float hullDamageTaken = damageAfterArmor(beamWeapon.getDamageType(), totalDamage, beamWeapon.getDamage().getDamage(), armorValue);
                // TODO: figure out how to check which beams actually do hardflux
                //float shieldHardFlux = damageToShield(beamWeapon.getDamageType(), totalDamage, ship.getShield().getFluxPerPointOfDamage());

                estimatedHullDamage += hullDamageTaken;
                // estimatedShieldHardFlux += shieldHardFlux;
                estimatedEMPDamage += EMPDamageTaken;

                armorValue = Math.max(0, armorValue - hullDamageTaken);
            }
        }

        return new float[] {estimatedHullDamage, estimatedShieldHardFlux, estimatedEMPDamage};
    }

    public static float[] estimateBlockableUnfiredDamage(ShipAPI ship, float timeToEstimate){
        float estimatedHullDamage = 0f;
        float estimatedShieldHardFlux = 0f;
        float estimatedEMPDamage = 0f;
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship,3000f);
        for (ShipAPI enemy: nearbyEnemies) {
            // ignore venting/overloaded enemies
            if(enemy.getFluxTracker().isOverloaded() || enemy.getFluxTracker().isVenting())
                continue;
            float armorValue = getWeakestTotalArmor(ship);

            for (WeaponAPI weapon: enemy.getAllWeapons()){
                // ignore decorative / weapons out of ammo
                if(weapon.isDecorative() || (weapon.usesAmmo() && weapon.getAmmo() == 0))
                    continue;


                // ignore weapon if out of range
                float distanceFromWeaponSquared = MathUtils.getDistanceSquared(weapon.getLocation(), ship.getLocation());
                float targetingRadius = Misc.getTargetingRadius(enemy.getLocation(), ship, false);
                if((weapon.getRange()+targetingRadius)*(weapon.getRange()+targetingRadius) < distanceFromWeaponSquared)
                    continue;

                // assume omni/phase shields can block everything
                if (ship.getShield().getType() == ShieldAPI.ShieldType.FRONT &&
                        Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), weapon.getLocation()))) > ship.getShield().getArc()/2)
                    continue;

                // distanceFromArc returns 0 only if ship is in arc
                boolean inArc = weapon.distanceFromArc(ship.getLocation()) == 0;

                // calculate aim time
                float aimTime = 0;
                if (weapon.hasAIHint(WeaponAPI.AIHints.DO_NOT_AIM) || weapon.hasAIHint(WeaponAPI.AIHints.GUIDED_POOR))
                    aimTime = 0;
                else if(inArc)
                    aimTime = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), VectorUtils.getAngle(weapon.getLocation(), ship.getLocation())))/weapon.getTurnRate();
                else // skip weapon if it cannot hit
                    continue;

                // calculate time spent disabled
                float disabledTime = 0;
                if (weapon.isDisabled())
                    disabledTime = 15 * (1 - weapon.getCurrHealth()/weapon.getMaxHealth()); //TODO: no clue where to actually get the real repair time

                // calculate travel time
                float travelTime;

                MutableShipStatsAPI stats = enemy.getMutableStats();
                if(weapon.isBeam()){
                    travelTime = 0; //TODO: Not actually true, beam speed is a thing, however I have no clue where to get it
                }
                else if(weapon.getSpec().getProjectileSpec() instanceof MissileSpecAPI){
                    ShipHullSpecAPI.EngineSpecAPI missileEngine = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getHullSpec().getEngineSpec();
                    float launchSpeed = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getLaunchSpeed();
                    float maxSpeed = stats.getMissileMaxSpeedBonus().computeEffective(missileEngine.getMaxSpeed());
                    float acceleration = stats.getMissileAccelerationBonus().computeEffective(missileEngine.getAcceleration());
                    float maxTurnRate = stats.getMissileMaxTurnRateBonus().computeEffective(missileEngine.getMaxTurnRate());
                    travelTime = missileTravelTime(launchSpeed, maxSpeed, acceleration, maxTurnRate, weapon.getCurrAngle(), weapon.getLocation(), ship.getLocation(), targetingRadius);
                }
                else if (weapon.getSpec().getProjectileSpec() instanceof ProjectileSpecAPI) {
                    //TODO: include the MutableShipStats for energy and ballistic weapons as well
                    travelTime = (float) (Math.sqrt(distanceFromWeaponSquared) / ((ProjectileSpecAPI) weapon.getSpec().getProjectileSpec()).getMoveSpeed(stats, weapon));
                }
                else{
                    continue; // this should never happen;
                }

                float dpsTime = Math.max(timeToEstimate - aimTime - disabledTime - travelTime, 0);

                if (Global.getSettings().isDevMode() && dpsTime > 0)
                    Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), enemy.getVelocity(), 30f, 50f, 0.1f, Color.blue);

                /* //TODO: need to rewrite this entire section for beams, currently I am assuming beam speed is instant, thus beam damage is captured in estimateBlockableDamage.
                // computeDamageDealt only works for non burst beams
                if(weapon.isBeam() && !weapon.isBurstBeam()){
                    WeaponAPI.DerivedWeaponStatsAPI beamStats = weapon.getDerivedStats();

                    float hullDamageTaken = damageAfterArmor(weapon.getDamageType(), weapon.getDamage().computeDamageDealt(dpsTime), weapon.getDamage().getDamage(), armorValue);
                    float shieldHardFlux = damageToShield(weapon.getDamageType(), weapon.getDamage().computeDamageDealt(dpsTime), ship.getShield().getFluxPerPointOfDamage());

                    estimatedHullDamage += hullDamageTaken;
                    estimatedShieldHardFlux += shieldHardFlux;
                    estimatedEMPDamage += beamStats.getEmpPerSecond() * dpsTime;

                    armorValue = Math.max(0, armorValue - hullDamageTaken);
                }
                else if(weapon.isBurstBeam()){ // special case burst beams as they are instant
                    float trailingDamageTime = weapon.getCooldownRemaining() - (weapon.getCooldown() - weapon.getSpec().getBeamChargedownTime()*2);
                    if (trailingDamageTime > 0){
                        float hullDamageTaken = damageAfterArmor(weapon.getDamageType(), weapon.getDamage().getDamage(), weapon.getDamage().getDamage(), armorValue) * trailingDamageTime;
                        float shieldHardFlux = damageToShield(weapon.getDamageType(), weapon.getDamage().getDamage(), ship.getShield().getFluxPerPointOfDamage()) * trailingDamageTime;

                        estimatedHullDamage += hullDamageTaken;
                        estimatedShieldHardFlux += shieldHardFlux;
                        estimatedEMPDamage += weapon.getDerivedStats().getEmpPerShot() * trailingDamageTime; // This isnt correct, the correct value should be emp/s during the burst

                        armorValue = Math.max(0, armorValue - hullDamageTaken);
                    }

                    if(weapon.isFiring()){
                        float hullDamageTaken = damageAfterArmor(weapon.getDamageType(), weapon.getDamage().getDamage(), weapon.getDamage().getDamage(), armorValue);
                        float shieldHardFlux = damageToShield(weapon.getDamageType(), weapon.getDamage().getDamage(), ship.getShield().getFluxPerPointOfDamage());
                        if (weapon.getDerivedStats().getBurstFireDuration() > 0) {
                            hullDamageTaken *= weapon.getSpec().getBurstSize() * Math.min(dpsTime / weapon.getDerivedStats().getBurstFireDuration(), 1);
                            shieldHardFlux *= weapon.getSpec().getBurstSize() * Math.min(dpsTime / weapon.getDerivedStats().getBurstFireDuration(), 1);
                        }

                        estimatedHullDamage += hullDamageTaken;
                        estimatedShieldHardFlux += shieldHardFlux;
                        estimatedEMPDamage += ship.getMutableStats().getEmpDamageTakenMult().getModifiedValue() * weapon.getDerivedStats().getEmpPerShot();
                    }
                }*/
                if(!weapon.isBeam()){
                    // if it will hit in the time given, add damage instances until the time is over
                    dpsTime -= weapon.getCooldownRemaining() - weapon.getSpec().getChargeTime();
                    while (dpsTime > 0){
                        float hullDamageTaken = damageAfterArmor(weapon.getDamageType(), weapon.getDamage().getDamage(), weapon.getDamage().getDamage(), armorValue);
                        float shieldHardFlux = damageToShield(weapon.getDamageType(), weapon.getDamage().getDamage(), ship.getShield().getFluxPerPointOfDamage());
                        if (weapon.getDerivedStats().getBurstFireDuration() > 0) {
                            hullDamageTaken *= weapon.getSpec().getBurstSize() * Math.min(dpsTime / weapon.getDerivedStats().getBurstFireDuration(), 1);
                            shieldHardFlux *= weapon.getSpec().getBurstSize() * Math.min(dpsTime / weapon.getDerivedStats().getBurstFireDuration(), 1);
                        }

                        estimatedHullDamage += hullDamageTaken;
                        estimatedShieldHardFlux += shieldHardFlux;
                        estimatedEMPDamage += ship.getMutableStats().getEmpDamageTakenMult().getModifiedValue() * weapon.getDerivedStats().getEmpPerShot();

                        armorValue = Math.max(0, armorValue - hullDamageTaken);

                        dpsTime -= weapon.getCooldown() + weapon.getDerivedStats().getBurstFireDuration() + weapon.getSpec().getChargeTime();
                    }
                }
            }
        }

        return new float[] {estimatedHullDamage, estimatedShieldHardFlux, estimatedEMPDamage};
    }

    public static float missileTravelTime(float startingSpeed, float maxSpeed, float acceleration, float maxTurnRate,
                                          float missileStartingAngle, Vector2f missileStartingLocation, Vector2f targetLocation, float targetRadius){
        // for guided, do some complex math to figure out the time it takes to hit
        float missileTurningRadius = (float) (maxSpeed/ (maxTurnRate* Math.PI / 180));
        float missileCurrentAngle = missileStartingAngle;
        Vector2f missileCurrentLocation = missileStartingLocation;
        float missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, targetLocation);
        float missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
        Vector2f missileRotationCenter = MathUtils.getPointOnCircumference(missileStartingLocation, missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? 90 : -90));

        float missileRotationSeconds = 0;
        do {
            missileRotationSeconds += Math.abs(missileRotationNeeded)/maxTurnRate;
            missileCurrentAngle = missileTargetAngle;
            missileCurrentLocation = MathUtils.getPointOnCircumference(missileRotationCenter, missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? -90 : 90));

            missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, targetLocation);
            missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
        } while (missileRotationSeconds < 30f && Math.abs(missileRotationNeeded) > 1f);

        float missileStraightSeconds = (MathUtils.getDistance(missileCurrentLocation, targetLocation)-targetRadius) / maxSpeed;

        float totalDistance = (missileRotationSeconds + missileStraightSeconds) * maxSpeed;

        float t1 = (maxSpeed - startingSpeed) / acceleration;
        float d1 = startingSpeed * t1 + 0.5f * acceleration * (float) Math.pow(t1, 2);
        if(totalDistance >= d1) {
            float d2 = totalDistance - d1;
            float t2 = d2 / maxSpeed;

            return t1 + t2;
        }
        else {
            float discriminant = (float) Math.pow(startingSpeed, 2) + 2 * acceleration * totalDistance;
            if(discriminant > 0)
                return (float) (-startingSpeed + Math.sqrt(discriminant)) / acceleration;
            else
                return 0;
        }
    }

    public static float getWeakestTotalArmor(ShipAPI ship){
        if (ship == null || !Global.getCombatEngine().isEntityInPlay(ship)) {
            return 0f;
        }
        ArmorGridAPI armorGrid = ship.getArmorGrid();

        org.lwjgl.util.Point worstPoint = DefenseUtils.getMostDamagedArmorCell(ship);
        if(worstPoint != null){
            float totalArmor = 0;
            for (int x = 0; x < armorGrid.getGrid().length; x++) {
                for (int y = 0; y < armorGrid.getGrid()[x].length; y++) {
                    if(x >= worstPoint.getX()-2 && x <= worstPoint.getX()+2 && y >= worstPoint.getY()-2 && y <= worstPoint.getY()+2){
                        totalArmor += Math.max(armorGrid.getArmorValue(worstPoint.getX(), worstPoint.getY())/2, armorGrid.getMaxArmorInCell() * 0.025f);
                    }
                    if(x >= worstPoint.getX()-1 && x <= worstPoint.getX()+1 && y >= worstPoint.getY()-1 && y <= worstPoint.getY()+1){
                        totalArmor += Math.max(armorGrid.getArmorValue(worstPoint.getX(), worstPoint.getY())/2, armorGrid.getMaxArmorInCell() * 0.025f);
                    }
                }
            }
            return totalArmor;
        } else{
            return armorGrid.getMaxArmorInCell() * 9.5f;
        }
    }

    public static float damageAfterArmor(DamageType damageType, float damage, float hitStrength, float armorValue){
        switch (damageType) {
            case FRAGMENTATION:
                hitStrength *= 0.25f;
                break;
            case KINETIC:
                hitStrength *= 0.5f;
                break;
            case HIGH_EXPLOSIVE:
                hitStrength *= 2f;
                break;
            default:
                break;
        }

        float damageMultiplier = Math.max(hitStrength / (armorValue + hitStrength),  0.15f);

        return (damage * damageMultiplier);
    }

    public static float damageToShield(DamageType damageType, float damage, float shieldEfficiency){
        switch (damageType) {
            case FRAGMENTATION:
                damage *= 0.25f;
                break;
            case KINETIC:
                damage *= 2f;
                break;
            case HIGH_EXPLOSIVE:
                damage *= 0.5f;
                break;
            default:
                break;
        }

        return (damage * shieldEfficiency);
    }
}
