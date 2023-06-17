package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
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
                float estimatedBlockableDamage = estimateBlockableProjectileDamage(ship, shield.getUnfoldTime()) + estimateBlockableUnfiredDamage(ship, shield.getUnfoldTime());

                // usually false, only true when currently unfolding shield. stops shield flickering.
                boolean wantToShield = shield.getActiveArc() > 1f && shield.getActiveArc() < shield.getArc() * 0.9f;

                if(estimatedBlockableDamage > 200 && shieldT < MAX_SHIELD * 0.4f)
                    wantToShield = true;
                if(estimatedBlockableDamage > 600 && shieldT < MAX_SHIELD * 0.6f)
                    wantToShield = true;
                if(estimatedBlockableDamage > 800 && shieldT < MAX_SHIELD * 0.8f)
                    wantToShield = true;
                if(estimatedBlockableDamage > 4000) // save 20% for reapers, ect
                    wantToShield = true;

                if(ship.getFluxLevel() > 0.95f) // dont overload
                    wantToShield = false;


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

    public static float estimateBlockableProjectileDamage(ShipAPI ship, float secondsToEstimate){
        float MAX_SPEED_OF_PROJECTILE = 2000f;

        Set<DamagingProjectileAPI> nearbyUnguided = new HashSet<>();
        Set<MissileAPI> nearbyGuided = new HashSet<>();
        float angleToShip;
        // Sort all projectiles into guided or unguided
        for (DamagingProjectileAPI threat : getAllProjectilesInRange(ship.getLocation(), secondsToEstimate * MAX_SPEED_OF_PROJECTILE)) {

            // assume omni shields can block everything
            if (ship.getShield().getType() == ShieldAPI.ShieldType.FRONT)
                angleToShip = MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(), threat.getLocation()), ship.getFacing());
            else if (ship.getShield().getType() == ShieldAPI.ShieldType.OMNI)
                angleToShip = 0;
            else
                return 0;

            if (!threat.isFading() && threat.getOwner() != ship.getOwner() && angleToShip < ship.getShield().getArc()/2) {
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

        float estimatedDamage = 0f;
        // sum up all the actual damage after armor
        for(DamagingProjectileAPI hit : estimatedHits){
            if (Global.getSettings().isDevMode()) Global.getCombatEngine().addSmoothParticle(hit.getLocation(), hit.getVelocity(), 30f, 5f, 0.1f, Color.magenta);
            estimatedDamage += damageAfterArmor(hit.getDamageType(), hit.getDamageAmount(), ship) + hit.getEmpAmount()/4;
        }

        // Handle beams with line-circle collision checks
        // TODO: check if getBeams() can be replaced with something like getAllProjectilesInRange() that uses the object grid
        List<BeamAPI> nearbyBeams = Global.getCombatEngine().getBeams();
        for (BeamAPI beam : nearbyBeams) {
            if (beam.getSource().getOwner() != ship.getOwner() && CollisionUtils.getCollides(beam.getFrom(), beam.getTo(), ship.getLocation(), Misc.getTargetingRadius(beam.getFrom(), ship, false))) {
                float damage;
                float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond();
                if (beam.getWeapon().getDerivedStats().getSustainedDps() < beam.getWeapon().getDerivedStats().getDps()) {
                    damage = beam.getWeapon().getDerivedStats().getBurstDamage() / beam.getWeapon().getDerivedStats().getBurstFireDuration();
                } else {
                    damage = beam.getWeapon().getDerivedStats().getDps();
                }
                estimatedDamage += damageAfterArmor(beam.getWeapon().getDamageType(), damage, ship) + emp/4;
            }
        }

        return estimatedDamage;
    }

    public static float estimateBlockableUnfiredDamage(ShipAPI ship, float timeToEstimate){
        float totalDamage = 0f;
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship,3000f);
        for (ShipAPI enemy: nearbyEnemies) {
            // ignore venting/overloaded enemies
            if(enemy.getFluxTracker().isOverloaded() || enemy.getFluxTracker().isVenting())
                continue;

            for (WeaponAPI weapon: enemy.getAllWeapons()){

                // ignore decorative / weapons out of ammo
                if(weapon.isDecorative() || (weapon.usesAmmo() && weapon.getAmmo() == 0))
                    continue;

                // ignore weapon if out of range
                float distanceFromWeaponSquared = MathUtils.getDistanceSquared(weapon.getLocation(), ship.getLocation());
                if(weapon.getRange()*weapon.getRange() < distanceFromWeaponSquared)
                    continue;

                // ignore weapon if not in shield arc
                float angleToShip;
                // assume omni shields can block everything
                if (ship.getShield().getType() == ShieldAPI.ShieldType.FRONT)
                    angleToShip = Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), weapon.getLocation())));
                else if (ship.getShield().getType() == ShieldAPI.ShieldType.OMNI)
                    angleToShip = 0;
                else
                    return 0;

                if(angleToShip > ship.getShield().getArc()/2)
                    continue;

                // distanceFromArc returns 0 only if ship is in arc
                boolean inArc = weapon.distanceFromArc(ship.getLocation()) == 0;
                float timeToAim = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), VectorUtils.getAngle(weapon.getLocation(), ship.getLocation())))/weapon.getTurnRate();

                // estimate the actual time spent firing
                float firingTime = timeToEstimate - weapon.getCooldownRemaining();
                if (weapon.isDisabled())
                    firingTime -= 15 * weapon.getCurrHealth()/weapon.getMaxHealth(); //TODO: no clue where to actually get the real repair time

                // special case the beam, as beam damage is complicated. no clue if the inbuilt computeDamageDealt() is accurate or not, lets hope it is.
                if(weapon.isBeam() && inArc){
                    if (Global.getSettings().isDevMode()) Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), enemy.getVelocity(), 30f, 5f, 0.1f, Color.blue);

                    firingTime = Math.max(firingTime - timeToAim, 0);
                    totalDamage += damageAfterArmor(weapon.getDamageType(), weapon.getDamage().computeDamageDealt(firingTime), ship);
                } // otherwise if in weapon arc or weapon is guided, calculate the unfired damage that should hit the ship
                else if(inArc || weapon.hasAIHint(WeaponAPI.AIHints.DO_NOT_AIM) || weapon.hasAIHint(WeaponAPI.AIHints.GUIDED_POOR)){
                    if (Global.getSettings().isDevMode()) Global.getCombatEngine().addSmoothParticle(weapon.getLocation(), enemy.getVelocity(), 30f, 5f, 0.1f, Color.blue);

                    MutableShipStatsAPI stats = enemy.getMutableStats();
                    // calculate the initial damage latency (ie: time until the first projectile/missile will hit)
                    if (weapon.getSpec().getProjectileSpec() instanceof ProjectileSpecAPI) {
                        firingTime = Math.max(firingTime - timeToAim - weapon.getSpec().getChargeTime(), 0);
                        firingTime -= distanceFromWeaponSquared / ((ProjectileSpecAPI) weapon.getSpec().getProjectileSpec()).getMoveSpeed(stats, weapon);
                    }
                    else if (weapon.getSpec().getProjectileSpec() instanceof MissileSpecAPI) {
                        ShipHullSpecAPI.EngineSpecAPI missileEngine = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getHullSpec().getEngineSpec();
                        float launchSpeed = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getLaunchSpeed();
                        float maxSpeed = stats.getMissileMaxSpeedBonus().computeEffective(missileEngine.getMaxSpeed());
                        float acceleration = stats.getMissileAccelerationBonus().computeEffective(missileEngine.getAcceleration());
                        float maxTurnRate = stats.getMissileMaxTurnRateBonus().computeEffective(missileEngine.getMaxTurnRate());
                        float radius = Misc.getTargetingRadius(weapon.getLocation(), ship, false);
                        float travelTime = missileTravelTime(launchSpeed, maxSpeed, acceleration, maxTurnRate, weapon.getCurrAngle(), weapon.getLocation(), ship.getLocation(), radius );
                        firingTime -= travelTime;
                    }

                    // if it will hit in the time given, add damage instances until the time is over
                    while (firingTime > 0){
                        totalDamage += damageAfterArmor(weapon.getDamageType(), weapon.getDamage().getDamage(), ship);
                        firingTime -= weapon.getRefireDelay() + weapon.getSpec().getChargeTime();
                    }
                }
            }
        }

        return totalDamage;
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

    public static float damageAfterArmor(DamageType damageType, float damage, ShipAPI ship){
        float armorValue = getWeakestTotalArmor(ship);
        float armorDamage = 0;
        switch (damageType) {
            case FRAGMENTATION:
                armorDamage = damage * 0.25f;
                break;
            case KINETIC:
                armorDamage = damage * 0.5f;
                break;
            case HIGH_EXPLOSIVE:
                armorDamage = damage * 2f;
                break;
            default:
                armorDamage = damage;
                break;
        }

        float damageMultiplier = Math.max(armorDamage / (armorValue + armorDamage),  0.15f);

        return (damage * damageMultiplier);
    }
}
