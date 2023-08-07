package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.combat.entities.BallisticProjectile;
import com.fs.starfarer.combat.entities.MovingRay;
import com.fs.starfarer.combat.entities.PlasmaShot;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicUI;
import org.selkie.kol.ReflectionUtils;
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
    private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.3f); //Seconds
    public float lastUpdatedTime = 0f;
    public List<FutureHit> incomingProjectiles = new ArrayList<>();
    public List<FutureHit> predictedWeaponHits = new ArrayList<>();
    public List<FutureHit> combinedHits = new ArrayList<>();
    public List<Float> omniShieldDirections = new ArrayList<>();
    public float lastShieldOnTime = 0f;
    
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
                if (!disabled && (!engine.isUIAutopilotOn() || engine.getPlayerShip() != ship)) {
                    if (shield.isOn() ^ wantToShield(amount))
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

    public boolean wantToShield(float amount){
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {
            lastUpdatedTime = Global.getCombatEngine().getTotalElapsedTime(false);
            incomingProjectiles = incomingProjectileHits(ship, ship.getLocation());
            predictedWeaponHits = generatePredictedWeaponHits(ship, ship.getLocation(), shield.getUnfoldTime()*2);
            combinedHits = new ArrayList<>();
            combinedHits.addAll(incomingProjectiles);
            combinedHits.addAll(predictedWeaponHits);
            float BUFFER_ARC = 10f;

            // prefilter expensive one time functions
            if(shield.getType() == ShieldAPI.ShieldType.FRONT){
                List<FutureHit> filteredHits = new ArrayList<>();
                for(FutureHit hit : combinedHits){
                    if(Misc.isInArc(ship.getFacing(), shield.getArc() + BUFFER_ARC, hit.angle)){
                        filteredHits.add(hit);
                    }
                }
                combinedHits = filteredHits;
            }
            else{
                List<Float> candidateShieldDirections = new ArrayList<>();
                float FUZZY_RANGE = 1f;
                for(FutureHit hit : combinedHits){
                    boolean tooCLose = false;
                    for(float candidateDirection : candidateShieldDirections){
                        if (Math.abs(hit.angle - candidateDirection) < FUZZY_RANGE) { tooCLose = true; break; }
                    }
                    if(!tooCLose) candidateShieldDirections.add(hit.angle);
                }
                if (candidateShieldDirections.isEmpty()) candidateShieldDirections.add(ship.getFacing());
                omniShieldDirections = candidateShieldDirections;
            }
        }


        // calculate how much damage the ship would take if shields went down
        float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
        float timeElapsed = currentTime - lastUpdatedTime;
        float armor = getWeakestTotalArmor(ship);

        float unfoldTime = shield.getUnfoldTime();
        float bufferTime = unfoldTime/4; //TODO: get real shield activate delay, unfoldTime/4 is: "looks about right"
        if(shield.isOn()) lastShieldOnTime = currentTime;
        float delayTime = Math.max(0.5f - (currentTime - lastShieldOnTime), 0f); //TODO: get real shield deactivate time

        float shieldHardFluxSavedIfNoShield = 0f;
        float armorDamageIfNoShield = Float.POSITIVE_INFINITY;
        float hullDamageIfNoShield = Float.POSITIVE_INFINITY;
        float empDamageIfNoShield = 0f;

        List<Float> shieldDirections = new ArrayList<>();
        if(shield.getType() == ShieldAPI.ShieldType.FRONT) shieldDirections.add(ship.getFacing());
        else shieldDirections.addAll(omniShieldDirections);

        for(float shieldDirection : shieldDirections){
            float currentHullDamage = 0f;
            float currentArmor = armor;
            float currentShieldHardFluxSaved = 0f;
            float currentEMPDamage = 0f;
            float currentBufferTime = Float.POSITIVE_INFINITY;
            float bestBufferTime = 0f;
            for(FutureHit hit : combinedHits){
                float offAngle = Math.abs(MathUtils.getShortestRotation(shieldDirection, hit.angle));
                float timeToBlock = (offAngle/(shield.getArc()/2)) * unfoldTime + delayTime;
                float timeToHit = (hit.timeToHit - timeElapsed);
                if(timeToHit < -0.1f) continue; // skip hits that have already happened
                if (timeToHit < (timeToBlock + bufferTime)){
                    if (!hit.softFlux) currentShieldHardFluxSaved += fluxToShield(hit.damageType, hit.damage, shield.getFluxPerPointOfDamage(), ship);
                    Pair<Float, Float> trueDamage = damageAfterArmor(hit.damageType, hit.damage, hit.hitStrength, currentArmor, ship);
                    currentArmor = Math.max(currentArmor - trueDamage.one, 0f);
                    currentHullDamage += trueDamage.two;
                    currentEMPDamage += hit.empDamage;
                }else{
                    currentBufferTime = Math.min(timeToHit - timeToBlock, currentBufferTime);
                }
            }

            boolean betterDirectionFound = false;
            if((armor - currentArmor) < armorDamageIfNoShield) betterDirectionFound = true;
            else if((armor - currentArmor) == armorDamageIfNoShield){
                if(currentHullDamage < hullDamageIfNoShield) betterDirectionFound = true;
                if(currentHullDamage == hullDamageIfNoShield && currentBufferTime > bestBufferTime) betterDirectionFound = true;
            }

            if (betterDirectionFound){
                shieldHardFluxSavedIfNoShield = currentShieldHardFluxSaved;
                armorDamageIfNoShield = (armor - currentArmor);
                hullDamageIfNoShield = currentHullDamage;
                empDamageIfNoShield = currentEMPDamage;
            }
        }


        // consider the max of shield time or flux level to decide when to shield
        float alpha = Math.max(ship.getFluxLevel(), shieldT / MAX_SHIELD);
        float lowDamage = 0.001f; // 0.1% hull damage
        float highDamage = 0.020f; // 2.0% hull damage

        // shield based on the current flux level and shield timer
        boolean wantToShield = (hullDamageIfNoShield + armorDamageIfNoShield + empDamageIfNoShield/3) > ship.getHitpoints() * (alpha * highDamage + (1 - alpha) * lowDamage);

        // if the damage is high enough to shield, but flux is high/ shield timer is low, armor tank KE
        if (wantToShield && shieldHardFluxSavedIfNoShield/(hullDamageIfNoShield + armorDamageIfNoShield)  > (alpha * 2f + (1 - alpha) * 7f))
            wantToShield = false;

        float emergencyArcSaved = Math.min(ship.getShield().getUnfoldTime() / (ship.getShield().getArc() / 80), ship.getShield().getUnfoldTime());
        if (shieldT + emergencyArcSaved > MAX_SHIELD) // Save enough time for 1 emergency unfold
            wantToShield = false;

        if ((shieldHardFluxSavedIfNoShield + ship.getCurrFlux())/ship.getMaxFlux() > 1) // Prevent overloads...
            wantToShield = false;

        if ((hullDamageIfNoShield + armorDamageIfNoShield) > 4000f) // Unless a reaper is on the way, in that case try and block it no matter what
            wantToShield = true;

        return wantToShield;
    }

    public static class FutureHit{
        public float timeToHit;
        public float angle;
        public DamageType damageType;
        public float hitStrength;
        public float damage;
        public float empDamage;
        public boolean softFlux;
    }

    public static List<FutureHit> incomingProjectileHits(ShipAPI ship, Vector2f testPoint){
        ArrayList<FutureHit> futureHits = new ArrayList<>();
        float MAX_RANGE = 3000f;
        boolean useBackup = false;

        Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(testPoint, MAX_RANGE * 2, MAX_RANGE * 2);
        while (iterator.hasNext()) {
            FutureHit futurehit = new FutureHit();
            Object next = iterator.next();
            if (!(next instanceof DamagingProjectileAPI)) continue;
            DamagingProjectileAPI threat = (DamagingProjectileAPI) next;
            if(threat.isFading()) continue;
            if(threat.getOwner() == ship.getOwner()) continue;

            float shipRadius = Misc.getTargetingRadius(threat.getLocation(), ship, false);
            // Guided missiles get dealt with here
            if (threat instanceof MissileAPI){
                MissileAPI missile = (MissileAPI) threat;
                if (missile.isFlare()) continue; // ignore flares
                if (missile.isGuided() && (missile.getWeapon() == null || !(missile.getWeapon().getId().equals("squall") && missile.getFlightTime() > 1f))){ // special case the squall
                    boolean hit = false;
                    float travelTime = 0f;
                    if(MathUtils.isPointWithinCircle(missile.getLocation(), testPoint, shipRadius)) hit = true;
                    else {
                        travelTime = missileTravelTime(missile.getMoveSpeed(), missile.getMaxSpeed(), missile.getAcceleration(), missile.getMaxTurnRate(),
                                VectorUtils.getFacing(missile.getVelocity()), missile.getLocation(), testPoint, shipRadius);
                        if (travelTime < (missile.getMaxFlightTime() - missile.getFlightTime())) hit = true;
                    }
                    if(hit) {
                        futurehit.timeToHit = travelTime;
                        futurehit.angle = VectorUtils.getAngle(testPoint, missile.getLocation());
                        futurehit.damageType = missile.getDamageType();
                        futurehit.softFlux = missile.getDamage().isSoftFlux();
                        float damage = calculateTrueDamage(ship, missile.getDamageAmount(), missile.getWeapon(), missile.getSource().getMutableStats());
                        futurehit.hitStrength = damage;
                        futurehit.damage = damage * Math.max(missile.getMirvNumWarheads(), 1);
                        futureHits.add(futurehit);
                    }
                    continue; // skip to next object if not hit, this point should be a complete filter of guided missiles
                }
            }

            // Non Guided projectiles (including missiles that have stopped tracking)
            float maxDistance;
            if (useBackup) {
                maxDistance = threat.getWeapon().getRange();
                if (threat.getWeapon() != null){
                    maxDistance -= MathUtils.getDistance(threat.getWeapon().getLocation(), threat.getLocation());
                }
            }
            else {
                try { // do really funky reflection to get max distance of the projectile
                    if(threat instanceof MissileAPI){
                        MissileAPI missile = (MissileAPI) threat;
                        maxDistance = (missile.getMaxFlightTime() - missile.getFlightTime()) * missile.getMaxSpeed();
                    }
                    else if (threat instanceof PlasmaShot){
                        PlasmaShot nearbyPlasma = (PlasmaShot) threat;
                        float flightTime = (float) ReflectionUtils.INSTANCE.get("flightTime", nearbyPlasma);
                        float maxFlightTime = (float) ReflectionUtils.INSTANCE.get("maxFlightTime", nearbyPlasma);
                        maxDistance = (maxFlightTime - flightTime) * threat.getMoveSpeed();
                    }
                    else if (threat instanceof BallisticProjectile) {
                        BallisticProjectile nearbyBallistic = (BallisticProjectile) threat;
                        com.fs.starfarer.combat.entities.OoOO trailExtender = (com.fs.starfarer.combat.entities.OoOO) ReflectionUtils.INSTANCE.get("trailExtender", nearbyBallistic);
                        float elapsedLifeTime = trailExtender.Ó00000();
                        float maxLifeTime = trailExtender.Õ00000();
                        maxDistance = (maxLifeTime - elapsedLifeTime) * threat.getMoveSpeed();
                    }
                    else if (threat instanceof MovingRay) {
                        MovingRay nearbyRay = (MovingRay) threat;
                        com.fs.starfarer.combat.entities.L rayExtender = (com.fs.starfarer.combat.entities.L) ReflectionUtils.INSTANCE.get("rayExtender", nearbyRay);
                        float currentRayDistance = rayExtender.Õ00000();
                        float maxRayDistance = (float) ReflectionUtils.INSTANCE.get("Ò00000", rayExtender);
                        maxDistance = maxRayDistance - currentRayDistance;
                    }
                    else continue; // skip if not one of these classes

                } catch (Exception e) { // backup use getDistance, switch over to this for all other projectiles if hit exception last time
                    useBackup = true;
                    continue;
                }
            }

            // circle-line collision checks for unguided projectiles,

            // subtract ship velocity to incorporate relative velocity
            Vector2f relativeVelocity = Vector2f.sub(threat.getVelocity(), ship.getVelocity(), null);
            Vector2f futureProjectileLocation = Vector2f.add(threat.getLocation(), VectorUtils.resize(new Vector2f(relativeVelocity), maxDistance), null);
            float hitDistance = MathUtils.getDistance(testPoint, threat.getLocation()) - shipRadius;
            float travelTime = 0f;
            float intersectAngle = 0f;
            boolean hit = false;
            if(hitDistance < 0){
                travelTime = 0;
                intersectAngle = VectorUtils.getAngle(ship.getLocation(), threat.getLocation());
                hit = true;
            }
            else {
                Pair<Float, Float> collision = intersectCircle(threat.getLocation(), futureProjectileLocation, testPoint, ship.getShieldRadiusEvenIfNoShield());
                if(collision != null){
                    intersectAngle = collision.one;
                    travelTime = collision.two/relativeVelocity.length();
                    hit = true;
                }
            }
            if (hit){
                futurehit.timeToHit = travelTime;
                futurehit.angle = intersectAngle;
                futurehit.damageType = threat.getDamageType();
                futurehit.softFlux = threat.getDamage().isSoftFlux();
                float damage = calculateTrueDamage(ship, threat.getDamageAmount(), threat.getWeapon(), threat.getSource().getMutableStats());
                futurehit.hitStrength = damage;
                futurehit.damage = damage;
                futureHits.add(futurehit);
            }
        }
        return futureHits;
    }

    //ChatGPT generated function
    public static Pair<Float, Float> intersectCircle(Vector2f a, Vector2f b, Vector2f c, float r) {
        // Calculate the vector from A to B and from A to C
        Vector2f ab = Vector2f.sub(b, a, null);
        Vector2f ac = Vector2f.sub(c, a, null);

        // Calculate the projection of C onto the line AB
        Vector2f projection = (Vector2f) new Vector2f(ab).scale(Vector2f.dot(ac, ab) / Vector2f.dot(ab, ab));
        Vector2f.add(a, projection, projection);

        // Calculate the distance from C to the line AB
        float distance = Vector2f.sub(projection, c, null).length();

        // If the distance is greater than the radius, there's no intersection
        if (distance > r) {
            return null;
        }

        // Calculate the distance from A to the projection
        float h = Vector2f.sub(projection, a, null).length();

        // Calculate the distance from the projection to the intersection points
        float d = (float) Math.sqrt(r*r - distance*distance);

        // The intersection points are then A + t * AB, where t is h - d or h + d
        Vector2f intersection1 = (Vector2f) new Vector2f(ab).scale((h - d) / ab.length());
        Vector2f.add(a, intersection1, intersection1);
        Vector2f intersection2 = (Vector2f) new Vector2f(ab).scale((h + d) / ab.length());
        Vector2f.add(a, intersection2, intersection2);

        // Choose the intersection point closer to A
        Vector2f intersection;
        float intersection1Length = Vector2f.sub(intersection1, a, null).length();
        float intersection2Length =  Vector2f.sub(intersection2, a, null).length();
        float length;
        if (intersection1Length < intersection2Length) {
            intersection = intersection1;
            length = intersection1Length;
        } else {
            intersection = intersection2;
            length = intersection2Length;
        }

        // Calculate the angle from C to the intersection
        Vector2f vector = Vector2f.sub(intersection, c, null);

        return new Pair<>((float) Math.toDegrees(FastTrig.atan2(vector.y, vector.x)), length);
    }

    public static float calculateTrueDamage(ShipAPI ship, float baseDamage, WeaponAPI weapon, MutableShipStatsAPI stats){
        if(weapon == null) return baseDamage;

        if(weapon.isBeam()) baseDamage *= stats.getBeamWeaponDamageMult().getModifiedValue();

        switch (weapon.getType()){
            case BALLISTIC: baseDamage *= stats.getBallisticWeaponDamageMult().getModifiedValue(); break;
            case ENERGY: baseDamage *= stats.getEnergyWeaponDamageMult().getModifiedValue(); break;
            case MISSILE: baseDamage *= stats.getMissileWeaponDamageMult().getModifiedValue(); break;
            default: break;
        }

        switch (ship.getHullSize()){
            case FIGHTER: baseDamage *= stats.getDamageToFighters().getModifiedValue(); break;
            case FRIGATE: baseDamage *= stats.getDamageToFrigates().getModifiedValue(); break;
            case DESTROYER: baseDamage *= stats.getDamageToDestroyers().getModifiedValue(); break;
            case CRUISER: baseDamage *= stats.getDamageToCruisers().getModifiedValue(); break;
            case CAPITAL_SHIP: baseDamage *= stats.getDamageToCapital().getModifiedValue(); break;
            default: break;
        }
        return baseDamage;
    }

    public static float applyROFMulti(float baseTime, WeaponAPI weapon, MutableShipStatsAPI stats){
        if(weapon == null) return baseTime;

        switch (weapon.getType()){
            case BALLISTIC: baseTime /= stats.getBallisticRoFMult().getModifiedValue(); break;
            case ENERGY: baseTime /= stats.getEnergyRoFMult().getModifiedValue(); break;
            case MISSILE: baseTime /= stats.getMissileRoFMult().getModifiedValue(); break;
            default: break;
        }

        return baseTime;
    }

    public static List<FutureHit> generatePredictedWeaponHits(ShipAPI ship, Vector2f testPoint, float maxTime){
        ArrayList<FutureHit> futureHits = new ArrayList<>();
        float MAX_RANGE = 3000f;
        float FUZZY_RANGE = 100f;
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship,MAX_RANGE);
        for (ShipAPI enemy: nearbyEnemies) {
            enemy.getFluxTracker().getOverloadTimeRemaining();
            enemy.getFluxTracker().isVenting(); enemy.getFluxTracker().getTimeToVent();
            for (WeaponAPI weapon: enemy.getAllWeapons()){

                if(weapon.isDecorative())
                    continue;

                // ignore weapon if out of range
                float distanceFromWeaponSquared = MathUtils.getDistanceSquared(weapon.getLocation(), testPoint);
                float targetingRadius = Misc.getTargetingRadius(enemy.getLocation(), ship, false);
                if((weapon.getRange()+targetingRadius+FUZZY_RANGE)*(weapon.getRange()+targetingRadius+FUZZY_RANGE) < distanceFromWeaponSquared) continue;

                // calculate disable time if applicable
                float disabledTime = weapon.isDisabled() ? weapon.getDisabledDuration() : 0;

                // distanceFromArc returns 0 only if ship is in arc
                boolean inArc = weapon.distanceFromArc(ship.getLocation()) == 0f;

                // pre calc angle and true damage, used many times later on
                float shipToWeaponAngle = VectorUtils.getAngle(testPoint, weapon.getLocation());
                float trueSingleInstanceDamage = calculateTrueDamage(ship, weapon.getDamage().getDamage(), weapon, enemy.getMutableStats());
                float trueSingleInstanceEMPDamage = calculateTrueDamage(ship, Math.max(weapon.getDerivedStats().getEmpPerShot(), weapon.getDerivedStats().getEmpPerSecond()), weapon, enemy.getMutableStats());
                float linkedBarrels = Math.round((weapon.getDerivedStats().getDamagePerShot()/weapon.getDamage().getDamage()));
                if(linkedBarrels == 0) linkedBarrels = weapon.getSpec().getTurretFireOffsets().size(); // beam fallback

                // if not guided, calculate aim time if in arc, otherwise skip weapon
                float aimTime = 0f;
                if(!(weapon.hasAIHint(WeaponAPI.AIHints.DO_NOT_AIM) || weapon.hasAIHint(WeaponAPI.AIHints.GUIDED_POOR))){
                    if(inArc) aimTime = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), MathUtils.clampAngle(shipToWeaponAngle + 180f)))/weapon.getTurnRate();
                    else continue;
                }
                float preAimedTime = disabledTime + aimTime;

                /* TODO: track and deal with ammo
                int currentAmmo = Integer.MAX_VALUE;
                int ammoPerReload = 0;
                float reloadTime = Float.POSITIVE_INFINITY;
                float reloadTimeLeft = Float.POSITIVE_INFINITY;
                if(weapon.usesAmmo()){
                    AmmoTrackerAPI ammoTracker = weapon.getAmmoTracker();
                    currentAmmo = ammoTracker.getAmmo();
                    ammoPerReload = (int) ammoTracker.getReloadSize();
                    reloadTime = ammoPerReload/ammoTracker.getAmmoPerSecond();
                    reloadTimeLeft = reloadTime * ammoTracker.getReloadProgress();
                }
                */
                if(weapon.usesAmmo()){
                    AmmoTrackerAPI ammoTracker = weapon.getAmmoTracker();
                    if (ammoTracker.getAmmo() == 0) continue;
                }
                float beamDelay = 0.1f; //TODO: get real beam speed
                // normal beams
                if(weapon.isBeam() && !weapon.isBurstBeam()){
                    float currentTime = preAimedTime;
                    while(currentTime < maxTime - beamDelay){
                        FutureHit futurehit = new FutureHit();
                        futurehit.timeToHit = currentTime + beamDelay;
                        futurehit.angle = shipToWeaponAngle;
                        futurehit.damageType = weapon.getDamageType();
                        futurehit.softFlux = true;
                        futurehit.hitStrength = trueSingleInstanceDamage / 2;
                        futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 10;
                        futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 10;
                        futureHits.add(futurehit);
                        currentTime += 0.1f;
                    }
                    continue;
                }

                // burst beams
                if(weapon.isBurstBeam()){
                    // derive the actual times spent in each phase from all the whack ass API calls
                    float chargeupTime = 0, activeTime = 0, chargedownTime = 0, cooldownTime = 0;
                    if(!weapon.isFiring()){ // weapon is in cooldown/idle
                        cooldownTime = weapon.getCooldownRemaining();
                        if(cooldownTime != 0) cooldownTime += weapon.getSpec().getBeamChargedownTime(); // TODO: delete when vanilla bug is fixed
                    }
                    else if(weapon.getCooldownRemaining() > 0){ // weapon is in chargedown, chargedown and cooldown overlap by Starsector's standards (Blame Alex)
                        cooldownTime = weapon.getCooldown() - weapon.getSpec().getBeamChargedownTime();
                        chargedownTime = weapon.getCooldownRemaining() - cooldownTime;
                        chargedownTime += weapon.getSpec().getBeamChargedownTime(); // TODO: delete when vanilla bug is fixed
                    }
                    else if(weapon.getBurstFireTimeRemaining() < weapon.getSpec().getBurstDuration()){ // weapon is in active
                        activeTime = weapon.getBurstFireTimeRemaining();
                        chargedownTime = weapon.getSpec().getBeamChargedownTime();
                        cooldownTime = weapon.getCooldown() - chargedownTime;
                    }
                    else if(weapon.getBurstFireTimeRemaining() > weapon.getSpec().getBurstDuration()){
                        activeTime = weapon.getSpec().getBurstDuration();
                        chargeupTime = weapon.getBurstFireTimeRemaining() - activeTime;
                        chargedownTime = weapon.getSpec().getBeamChargedownTime();
                        cooldownTime = weapon.getCooldown() - chargedownTime;
                    }

                    // apply ROF multis
                    chargeupTime = applyROFMulti(chargeupTime, weapon, enemy.getMutableStats()); //TODO: check if ROF effects active time of burst beams
                    chargedownTime = applyROFMulti(chargedownTime, weapon, enemy.getMutableStats());
                    cooldownTime = applyROFMulti(cooldownTime, weapon, enemy.getMutableStats());

                    float currentTime = beamDelay;
                    while(currentTime < maxTime) {
                        while (chargeupTime > 0) { // resolve chargeup damage
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.timeToHit = currentTime;
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = true;
                                futurehit.hitStrength = trueSingleInstanceDamage / 6;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 30;
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 30;
                                futureHits.add(futurehit);
                            }
                            chargeupTime -= 0.1;
                            currentTime += 0.1f;
                        }

                        activeTime += chargeupTime; // carry over borrowed time
                        while (activeTime > 0) { // resolve active damage
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.timeToHit = currentTime;
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = true;
                                futurehit.hitStrength = trueSingleInstanceDamage / 2;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 10;
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 10;
                                futureHits.add(futurehit);
                            }
                            activeTime -= 0.1;
                            currentTime += 0.1f;
                        }

                        chargedownTime += activeTime; // carry over borrowed time
                        while (chargedownTime > 0) { // resolve chargedown damage
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.timeToHit = currentTime;
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = true;
                                futurehit.hitStrength = trueSingleInstanceDamage / 6;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 30;
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 30;
                                futureHits.add(futurehit);
                            }
                            chargedownTime -= 0.1;
                            currentTime += 0.1f;
                        }

                        cooldownTime += chargedownTime; // carry over borrowed time
                        currentTime += cooldownTime;
                        currentTime = Math.max(currentTime, preAimedTime); // wait for weapon to finish aiming if not yet aimed

                        // reset times
                        chargeupTime = applyROFMulti(weapon.getSpec().getBeamChargeupTime(), weapon, enemy.getMutableStats());
                        activeTime = weapon.getSpec().getBurstDuration(); //TODO: check if ROF effects active time of burst beams
                        chargedownTime = applyROFMulti(weapon.getSpec().getBeamChargedownTime(), weapon, enemy.getMutableStats());
                        cooldownTime = applyROFMulti(weapon.getCooldown() - chargedownTime, weapon, enemy.getMutableStats());
                    }
                    continue;
                }


                // calculate travel time
                float travelTime;
                MutableShipStatsAPI stats = enemy.getMutableStats();
                if(weapon.getSpec().getProjectileSpec() instanceof MissileSpecAPI){
                    ShipHullSpecAPI.EngineSpecAPI missileEngine = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getHullSpec().getEngineSpec();
                    float launchSpeed = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getLaunchSpeed();
                    float maxSpeed = stats.getMissileMaxSpeedBonus().computeEffective(missileEngine.getMaxSpeed());
                    float acceleration = stats.getMissileAccelerationBonus().computeEffective(missileEngine.getAcceleration());
                    float maxTurnRate = stats.getMissileMaxTurnRateBonus().computeEffective(missileEngine.getMaxTurnRate());
                    travelTime = missileTravelTime(launchSpeed, maxSpeed, acceleration, maxTurnRate, weapon.getCurrAngle(), weapon.getLocation(), ship.getLocation(), targetingRadius);
                }
                else {
                    Vector2f projectileVector = VectorUtils.resize(VectorUtils.getDirectionalVector(weapon.getLocation(), testPoint), weapon.getProjectileSpeed());
                    Vector2f relativeVelocity = Vector2f.add(Vector2f.sub(projectileVector, ship.getVelocity(), null), enemy.getVelocity(), null);
                    travelTime = (float) (Math.sqrt(distanceFromWeaponSquared)-targetingRadius) / relativeVelocity.length();
                }


                if(weapon.getSpec().getBurstSize() == 1){ // non burst projectile weapons
                    // derive the actual times spent in each phase from all the whack ass API calls
                    float chargeupTime = 0, cooldownTime = 0;
                    if(weapon.getCooldownRemaining() == 0 && weapon.isFiring()){
                        chargeupTime = (1f - weapon.getChargeLevel()) * weapon.getSpec().getChargeTime();
                        cooldownTime = weapon.getCooldown();
                    }
                    else if(weapon.isFiring()){
                        cooldownTime = weapon.getCooldownRemaining();
                    }

                    // apply ROF multis
                    chargeupTime = applyROFMulti(chargeupTime, weapon, enemy.getMutableStats());
                    cooldownTime = applyROFMulti(cooldownTime, weapon, enemy.getMutableStats());

                    float currentTime = 0f;
                    while(currentTime < (maxTime - travelTime)){
                        currentTime += chargeupTime;
                        if(currentTime > preAimedTime){
                            FutureHit futurehit = new FutureHit();
                            futurehit.timeToHit = (currentTime + travelTime);
                            futurehit.angle = shipToWeaponAngle;
                            futurehit.damageType = weapon.getDamageType();
                            futurehit.softFlux = weapon.getDamage().isSoftFlux();
                            futurehit.hitStrength = trueSingleInstanceDamage;
                            futurehit.damage = (trueSingleInstanceDamage * linkedBarrels);
                            futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels);
                            futureHits.add(futurehit);
                        }
                        currentTime += cooldownTime;
                        currentTime = Math.max(currentTime, preAimedTime); // wait for weapon to finish aiming if not yet aimed

                        // reset chargeup/cooldown to idle weapon stats
                        chargeupTime = applyROFMulti(weapon.getSpec().getChargeTime(), weapon, enemy.getMutableStats());
                        cooldownTime = applyROFMulti(weapon.getCooldown(), weapon, enemy.getMutableStats());
                    }
                }
                else{ // burst projectile weapons
                    // derive the actual times spent in each phase from all the whack ass API calls
                    float chargeupTime = 0, burstTime = 0f, cooldownTime = 0;
                    float burstDelay = ((ProjectileWeaponSpecAPI) weapon.getSpec()).getBurstDelay();
                    if(weapon.getCooldownRemaining() == 0 && !weapon.isInBurst() && weapon.isFiring()){
                        chargeupTime = (1f - weapon.getChargeLevel()) * weapon.getSpec().getChargeTime();
                        burstTime = weapon.getDerivedStats().getBurstFireDuration();
                        cooldownTime = weapon.getCooldown();
                    }
                    else if(weapon.isInBurst() && weapon.isFiring()){
                        chargeupTime = (weapon.getCooldownRemaining()/weapon.getCooldown()) * burstDelay; //TODO: check this if something is breaking, uses a bug in the api call
                        burstTime = weapon.getBurstFireTimeRemaining();
                        cooldownTime = weapon.getCooldown();
                    } else if(weapon.getCooldownRemaining() != 0 && !weapon.isInBurst() && weapon.isFiring()){
                        cooldownTime = weapon.getCooldownRemaining();
                    }

                    // apply ROF multis
                    chargeupTime = applyROFMulti(chargeupTime, weapon, enemy.getMutableStats());
                    burstTime = applyROFMulti(burstTime, weapon, enemy.getMutableStats());
                    burstDelay = applyROFMulti(burstDelay, weapon, enemy.getMutableStats());
                    cooldownTime = applyROFMulti(cooldownTime, weapon, enemy.getMutableStats());

                    float currentTime = 0f;
                    while(currentTime < (maxTime - travelTime)){
                        currentTime += chargeupTime;
                        while(burstTime > 0.01f) { // avoid floating point jank
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.timeToHit = (currentTime + travelTime);
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = weapon.getDamage().isSoftFlux();
                                futurehit.hitStrength = trueSingleInstanceDamage;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels);
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels);
                                futureHits.add(futurehit);
                            }
                            burstTime -= burstDelay;
                            currentTime += burstDelay;
                        }
                        currentTime += cooldownTime;
                        currentTime = Math.max(currentTime, preAimedTime); // wait for weapon to finish aiming if not yet aimed

                        // reset chargeup/cooldown to idle weapon stats
                        chargeupTime = applyROFMulti(weapon.getSpec().getChargeTime(), weapon, enemy.getMutableStats());
                        burstTime = applyROFMulti(weapon.getDerivedStats().getBurstFireDuration(), weapon, enemy.getMutableStats());
                        cooldownTime = applyROFMulti(weapon.getCooldown(), weapon, enemy.getMutableStats());
                    }
                }
            }
        }
        return futureHits;
    }
    public static float getWeakestTotalArmor(ShipAPI ship){
        if (ship == null || !Global.getCombatEngine().isEntityInPlay(ship)) {
            return 0f;
        }
        ArmorGridAPI armorGrid = ship.getArmorGrid();
        org.lwjgl.util.Point worstPoint = DefenseUtils.getMostDamagedArmorCell(Global.getCombatEngine().getPlayerShip());
        if(worstPoint != null){
            float totalArmor = 0;
            for (int x = 0; x < armorGrid.getGrid().length; x++) {
                for (int y = 0; y < armorGrid.getGrid()[x].length; y++) {
                    if(x >= worstPoint.getX()-2 && x <= worstPoint.getX()+2 && y >= worstPoint.getY()-2 && y <= worstPoint.getY()+2){
                        totalArmor += armorGrid.getArmorValue(worstPoint.getX(), worstPoint.getY())/2;
                    }
                    if(x >= worstPoint.getX()-1 && x <= worstPoint.getX()+1 && y >= worstPoint.getY()-1 && y <= worstPoint.getY()+1){
                        totalArmor += armorGrid.getArmorValue(worstPoint.getX(), worstPoint.getY())/2;
                    }
                }
            }
            return totalArmor;
        } else{
            return armorGrid.getMaxArmorInCell() * 15f;
        }
    }
    public static Pair<Float, Float> damageAfterArmor(DamageType damageType, float damage, float hitStrength, float armorValue, ShipAPI ship){
        MutableShipStatsAPI stats = ship.getMutableStats();

        float armorMultiplier = stats.getArmorDamageTakenMult().getModifiedValue();
        float hullMultiplier = stats.getHullDamageTakenMult().getModifiedValue();
        float minArmor = stats.getMinArmorFraction().getModifiedValue();
        float maxDR = stats.getMaxArmorDamageReduction().getModifiedValue();

        armorValue = Math.max(minArmor * ship.getArmorGrid().getArmorRating(), armorValue);
        switch (damageType) {
            case FRAGMENTATION:
                armorMultiplier *= (0.25f * stats.getFragmentationDamageTakenMult().getModifiedValue());
                hullMultiplier *= stats.getFragmentationDamageTakenMult().getModifiedValue();
                break;
            case KINETIC:
                armorMultiplier *= (0.5f * stats.getKineticDamageTakenMult().getModifiedValue());
                hullMultiplier *= stats.getKineticDamageTakenMult().getModifiedValue();
                break;
            case HIGH_EXPLOSIVE:
                armorMultiplier *= (2f * stats.getHighExplosiveDamageTakenMult().getModifiedValue());
                hullMultiplier *= stats.getHighExplosiveDamageTakenMult().getModifiedValue();
                break;
            case ENERGY:
                armorMultiplier *= stats.getEnergyDamageTakenMult().getModifiedValue();
                hullMultiplier *= stats.getEnergyDamageTakenMult().getModifiedValue();
                break;
        }

        damage *= Math.max((1f - maxDR), ((hitStrength * armorMultiplier) / (armorValue + hitStrength * armorMultiplier)));

        float armorDamage = damage * armorMultiplier;
        float hullDamage = 0;
        if (armorDamage > armorValue){
            hullDamage = ((armorDamage - armorValue)/armorDamage) * damage * hullMultiplier;
        }

        return new Pair<>(armorDamage, hullDamage);
    }
    public static float fluxToShield(DamageType damageType, float damage, float shieldEfficiency, ShipAPI ship){
        MutableShipStatsAPI stats = ship.getMutableStats();

        float shieldMultiplier = stats.getShieldDamageTakenMult().getModifiedValue();

        switch (damageType) {
            case FRAGMENTATION:
                shieldMultiplier *= (0.25f * stats.getFragmentationDamageTakenMult().getModifiedValue());
                break;
            case KINETIC:
                shieldMultiplier *= (2f * stats.getKineticDamageTakenMult().getModifiedValue());
                break;
            case HIGH_EXPLOSIVE:
                shieldMultiplier *= (0.5f * stats.getHighExplosiveDamageTakenMult().getModifiedValue());
                break;
            case ENERGY:
                shieldMultiplier *= stats.getEnergyDamageTakenMult().getModifiedValue();
                break;
        }

        return (damage * shieldEfficiency * shieldMultiplier);
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

}
