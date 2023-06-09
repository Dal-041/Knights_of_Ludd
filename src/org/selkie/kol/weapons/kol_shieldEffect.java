package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;
//import static data.scripts.util.SKR_txt.txt;
import java.awt.Color;
import java.util.*;

//By Tartiflette

public class kol_shieldEffect implements EveryFrameWeaponEffectPlugin {
    
    private final float MAX_SHIELD=10;
    private final static float TIME_TO_ESTIMATE = 3f;
    
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


                float shieldDirection = 0;
                float shieldArc = 0;
                if (shield.getType() == ShieldAPI.ShieldType.FRONT){
                    shieldDirection = ship.getFacing();
                    shieldArc = shield.getArc();
                }
                else if (shield.getType() == ShieldAPI.ShieldType.OMNI){
                    shieldArc = 360f; // 360 means to estimate as if you can block damage anywhere
                }

                float estimatedBlockableDamage = getDamageBlockedEstimate(ship.getLocation(),ship.getVelocity(), ship.getCollisionRadius()*0.9f, TIME_TO_ESTIMATE, shieldDirection, shieldArc);


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

    private float getDamageBlockedEstimate(Vector2f testPoint, Vector2f velocity, float radius, float secondsToEstimate, float shieldDirection, float shieldArc){
        float MAX_SPEED_OF_PROJECTILE = 2000f;

        Set<DamagingProjectileAPI> nearbyUnguided = new HashSet<>();
        Set<MissileAPI> nearbyGuided = new HashSet<>();

        // Sort all projectiles into guided or unguided
        for (DamagingProjectileAPI threat : getAllProjectilesInRange(testPoint, secondsToEstimate * MAX_SPEED_OF_PROJECTILE)) {
            if (!threat.isFading() && threat.getOwner() != ship.getOwner()) {
                float threatAngle = VectorUtils.getAngle(testPoint, threat.getLocation());
                if (threat instanceof MissileAPI){
                    MissileAPI missile = (MissileAPI) threat;
                    if (!missile.isFlare()) {
                        if (!missile.isGuided()) {
                            if (Math.abs(MathUtils.getShortestRotation(shieldDirection, threatAngle)) < shieldArc / 2)
                                nearbyUnguided.add(threat);
                        } else {
                            nearbyGuided.add(missile);
                        }
                    }
                }else{
                    if(Math.abs(MathUtils.getShortestRotation(shieldDirection, threatAngle)) < shieldArc/2)
                        nearbyUnguided.add(threat);
                }
            }
        }

        Set<DamagingProjectileAPI> estimatedHits = new HashSet<>();

        // do a line-circle collision check for unguided
        for (DamagingProjectileAPI unguided : nearbyUnguided){
            float maxSpeed = (unguided instanceof MissileAPI) ? ((MissileAPI) unguided).getMaxSpeed() : unguided.getMoveSpeed();
            Vector2f futureProjectileLocation = Vector2f.add(unguided.getLocation(), VectorUtils.resize(new Vector2f(unguided.getVelocity()), secondsToEstimate*maxSpeed), null);
            float hitDistance = MathUtils.getDistance(testPoint, unguided.getLocation()) - radius;
            float travelTime = hitDistance/unguided.getMoveSpeed();
            Vector2f futureTestPoint = Vector2f.add(testPoint, (Vector2f) new Vector2f(velocity).scale(travelTime), null);
            if (CollisionUtils.getCollides(unguided.getLocation(), futureProjectileLocation, futureTestPoint, radius * 0.9f)){
                estimatedHits.add(unguided);
            }
        }

        for (MissileAPI guided : nearbyGuided){
            // for guided, check if they are currently in the ships collision bounds
            if (CollisionUtils.isPointWithinBounds(guided.getLocation(), ship)){
                estimatedHits.add(guided);
            }else{ // If not, do some complex math to figure out the time it takes to hit

                float missileTurningRadius = (float) (guided.getMaxSpeed() / (guided.getMaxTurnRate() * Math.PI / 180));
                float missileCurrentAngle = VectorUtils.getFacing(guided.getVelocity());
                Vector2f missileCurrentLocation = guided.getLocation();
                float missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, testPoint);
                float missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
                Vector2f missileRotationCenter = MathUtils.getPointOnCircumference(guided.getLocation(), missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? 90 : -90));

                float missileRotationSeconds = 0;
                do {
                    missileRotationSeconds += Math.abs(missileRotationNeeded)/guided.getMaxTurnRate();
                    missileCurrentAngle = missileTargetAngle;
                    missileCurrentLocation = MathUtils.getPointOnCircumference(missileRotationCenter, missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? -90 : 90));

                    missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, testPoint);
                    missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
                } while (missileRotationSeconds < secondsToEstimate && Math.abs(missileRotationNeeded) > 1f);




                float missileStraightSeconds = (MathUtils.getDistance(missileCurrentLocation, testPoint)-radius) / guided.getMaxSpeed();

                if ((missileRotationSeconds + missileStraightSeconds < secondsToEstimate) && (missileRotationSeconds + missileStraightSeconds < guided.getMaxFlightTime() - guided.getFlightTime())){
                    if(Math.abs(MathUtils.getShortestRotation(shieldDirection, missileTargetAngle+180)) < shieldArc/2) {
                        estimatedHits.add(guided);
                    }
                }
            }
        }

        float estimatedDamage = 0f;
        // sum up projectile damage
        for(DamagingProjectileAPI hit : estimatedHits){
            engine.addSmoothParticle(hit.getLocation(), hit.getVelocity(), 30f, 5f, 0.1f, Color.magenta);
            estimatedDamage += convertDamageType(hit.getDamageType(), hit.getDamageAmount()) + hit.getEmpAmount()/4;
        }

        // Handle beams with line-circle collision checks
        // TODO: check if engine.getBeams() can be replaced with something like getAllProjectilesInRange() that uses the object grid
        List<BeamAPI> nearbyBeams = engine.getBeams();
        for (BeamAPI beam : nearbyBeams) {
            float threatAngle = VectorUtils.getAngle(testPoint, beam.getFrom());
            if (Math.abs(MathUtils.getShortestRotation(shieldDirection, threatAngle)) < shieldArc / 2){
                if (beam.getSource().getOwner() != ship.getOwner() && CollisionUtils.getCollides(beam.getFrom(), beam.getTo(), testPoint, Misc.getTargetingRadius(beam.getFrom(), ship, false))) {
                    float damage;
                    float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond();
                    if (beam.getWeapon().getDerivedStats().getSustainedDps() < beam.getWeapon().getDerivedStats().getDps()) {
                        damage = beam.getWeapon().getDerivedStats().getBurstDamage() / beam.getWeapon().getDerivedStats().getBurstFireDuration();
                    } else {
                        damage = beam.getWeapon().getDerivedStats().getDps();
                    }
                    estimatedDamage += convertDamageType(beam.getWeapon().getDamageType(), damage) + emp/4;
                }
            }
        }

        return estimatedDamage;
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

    private float getWeakestTotalArmor(ShipAPI ship){
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

    private float convertDamageType(DamageType damageType, float damage){
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