package org.selkie.kol.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import kotlin.Triple;
import org.lazywizard.lazylib.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class StarficzAIUtils {
    public static final boolean DEBUG_ENABLED = false;

    public static class FutureHit{
        public float timeToHit;
        public float angle;
        public DamageType damageType;
        public float hitStrength;
        public float damage;
        public float empDamage;
        public boolean softFlux;
        public String enemyId;
    }

    public static final float SINGLE_FRAME = (float) 1 /60;
    public static List<FutureHit> incomingProjectileHits(ShipAPI ship, Vector2f testPoint){
        ArrayList<FutureHit> futureHits = new ArrayList<>();
        float MAX_RANGE = 3000f;

        Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(testPoint, MAX_RANGE * 2, MAX_RANGE * 2);
        while (iterator.hasNext()) {
            FutureHit futurehit = new FutureHit();
            Object next = iterator.next();
            if (!(next instanceof DamagingProjectileAPI)) continue;
            DamagingProjectileAPI threat = (DamagingProjectileAPI) next;
            if(threat.isFading()) continue;
            if(threat.getSource() == ship && !(threat instanceof MissileAPI && ((MissileAPI) threat).isMine())) continue;
            if(threat.getOwner() == ship.getOwner() && EnumSet.of(CollisionClass.PROJECTILE_NO_FF, CollisionClass.MISSILE_NO_FF, CollisionClass.HITS_SHIPS_ONLY_NO_FF).contains(threat.getCollisionClass())) continue;

            float shipRadius = Misc.getTargetingRadius(threat.getLocation(), ship, false);
            // Guided missiles and mines get dealt with here
            if (threat instanceof MissileAPI){
                MissileAPI missile = (MissileAPI) threat;
                if (missile.isFlare()) continue; // ignore flares

                if (missile.isMine()){
                    if(MathUtils.isPointWithinCircle(testPoint, missile.getLocation(),shipRadius + missile.getMineExplosionRange() * 1.1f )) {
                        futurehit.timeToHit = missile.getUntilMineExplosion() - 0.1f;
                        futurehit.angle = VectorUtils.getAngle(testPoint, missile.getLocation());
                        futurehit.damageType = missile.getDamageType();
                        futurehit.softFlux = missile.getDamage().isSoftFlux();
                        float damage = calculateTrueDamage(ship, missile.getDamageAmount(), missile.getWeapon(), missile.getSource().getMutableStats());
                        futurehit.hitStrength = damage;
                        futurehit.damage = damage * Math.max(missile.getMirvNumWarheads(), 1);
                        futureHits.add(futurehit);
                    }
                    continue; // skip to next object if not hit, this point should be a complete filter of mines
                }

                if (missile.isGuided() && missile.getFlightTime() < missile.getMaxFlightTime() && (missile.getWeapon() == null || !(missile.getWeapon().getId().equals("squall") && missile.getFlightTime() > 1f))){ // special case the squall
                    boolean hit = false;
                    float travelTime = 0f;
                    float collisionSize = missile.getSpec().getExplosionSpec() == null ? missile.getCollisionRadius() : missile.getSpec().getExplosionSpec().getRadius();
                    if(MathUtils.isPointWithinCircle(missile.getLocation(), testPoint, shipRadius + collisionSize)) hit = true;
                    else {
                        // I hate mirvs
                        float missileMaxSpeed = missile.isMirv() ? missile.getMaxSpeed()*4 :  missile.getMaxSpeed();
                        float missileAccel = missile.isMirv() ? missile.getAcceleration()*4 :  missile.getAcceleration();
                        travelTime = missileTravelTime(missile.getMoveSpeed(), missileMaxSpeed, missileAccel, missile.getMaxTurnRate(),
                                VectorUtils.getFacing(missile.getVelocity()), missile.getLocation(), testPoint, shipRadius + collisionSize);
                        if (travelTime < (missile.getMaxFlightTime() + (missile.isArmedWhileFizzling() ? missile.getSpec().getFlameoutTime() : 0f) - missile.getFlightTime())) hit = true;
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
            if(threat.getWeapon() == null) continue;
            float range = threat.getWeapon().getRange();
            float maxDistance = range - threat.getElapsed() * threat.getMoveSpeed();

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
                Triple<Vector2f, Float, Float> collision = intersectCircle(threat.getLocation(), futureProjectileLocation, testPoint, ship.getShieldRadiusEvenIfNoShield());
                if(collision != null){
                    intersectAngle = collision.getSecond();
                    travelTime = collision.getThird()/relativeVelocity.length();
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

    /**ChatGPT generated function, returns the intersection point, angle of the intersection, and length: from to intersection**/
    public static Triple<Vector2f, Float, Float> intersectCircle(Vector2f from, Vector2f to, Vector2f circleLoc, float radius) {
        // Calculate the vector from A to B and from A to C
        Vector2f ab = Vector2f.sub(to, from, null);
        Vector2f ac = Vector2f.sub(circleLoc, from, null);

        // Calculate the projection of C onto the line AB
        Vector2f projection = (Vector2f) new Vector2f(ab).scale(Vector2f.dot(ac, ab) / Vector2f.dot(ab, ab));
        Vector2f.add(from, projection, projection);

        // Calculate the distance from C to the line AB
        float distance = Vector2f.sub(projection, circleLoc, null).length();

        // If the distance is greater than the radius, there's no intersection
        if (distance > radius) {
            return null;
        }

        // Calculate the distance from A to the projection
        float h = Vector2f.sub(projection, from, null).length();

        // Calculate the distance from the projection to the intersection points
        float d = (float) Math.sqrt(radius*radius - distance*distance);

        // The intersection points are then A + t * AB, where t is h - d or h + d
        Vector2f intersection1 = (Vector2f) new Vector2f(ab).scale((h - d) / ab.length());
        Vector2f.add(from, intersection1, intersection1);
        Vector2f intersection2 = (Vector2f) new Vector2f(ab).scale((h + d) / ab.length());
        Vector2f.add(from, intersection2, intersection2);

        // Choose the intersection point closer to A
        Vector2f intersection;
        float intersection1Length = Vector2f.sub(intersection1, from, null).length();
        float intersection2Length =  Vector2f.sub(intersection2, from, null).length();
        float length;
        if (intersection1Length < intersection2Length) {
            intersection = intersection1;
            length = intersection1Length;
        } else {
            intersection = intersection2;
            length = intersection2Length;
        }

        // Calculate the angle from C to the intersection
        Vector2f vector = Vector2f.sub(intersection, circleLoc, null);

        return new Triple<>(intersection, (float) Math.toDegrees(FastTrig.atan2(vector.y, vector.x)), length);
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
        maxTime = Math.min(maxTime, 20); // limit to 20 seconds in the future
        ArrayList<FutureHit> futureHits = new ArrayList<>();
        float MAX_RANGE = 3000f;
        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship,MAX_RANGE);
        for (ShipAPI enemy: nearbyEnemies) {
            float ventOverloadTime = Math.max(enemy.getFluxTracker().getOverloadTimeRemaining(),
                    enemy.getFluxTracker().isVenting() ? enemy.getFluxTracker().getTimeToVent() : 0f);
            // ignore ship if shooting through other ship
            boolean occluded = false;
            for(ShipAPI occlusion : nearbyEnemies){
                if (occlusion == enemy) continue;
                if (occlusion.getParentStation() == enemy) continue;
                Vector2f closestPoint = MathUtils.getNearestPointOnLine(occlusion.getLocation(), ship.getLocation(), enemy.getLocation());
                // bias the size down 75 units, hack to compensate for the fact that this assumes everything is static
                if (MathUtils.getDistance(closestPoint, occlusion.getLocation()) < Misc.getTargetingRadius(closestPoint, occlusion, occlusion.getShield() != null && occlusion.getShield().isOn()) - 75f){
                    occluded = true;
                }
            }
            if (occluded) continue;

            for (WeaponAPI weapon: enemy.getAllWeapons()){

                if(weapon.isDecorative()) continue;

                // if weapon out of range, add time for ship to reach you
                float distanceFromWeapon = MathUtils.getDistance(weapon.getLocation(), testPoint);
                float targetingRadius = Misc.getTargetingRadius(enemy.getLocation(), ship, false);
                float outOfRangeTime = ventOverloadTime;
                if(distanceFromWeapon > (weapon.getRange() + targetingRadius) ){
                    outOfRangeTime = (distanceFromWeapon - weapon.getRange()+targetingRadius)/ship.getMaxSpeed();
                    distanceFromWeapon = weapon.getRange();
                }


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

                // if not guided, calculate aim time if in arc, otherwise add time for ships to rotate (overestimates by allowing all weapons to hit, but better to over then underestimate)
                float aimTime = 0f;
                if(!(weapon.hasAIHint(WeaponAPI.AIHints.DO_NOT_AIM) || weapon.hasAIHint(WeaponAPI.AIHints.GUIDED_POOR))){
                    aimTime = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), MathUtils.clampAngle(shipToWeaponAngle + 180f)))/weapon.getTurnRate();
                    if(!inArc){
                        aimTime += weapon.distanceFromArc(ship.getLocation())/(enemy.getMaxTurnRate()/2);
                    }
                }
                float preAimedTime = disabledTime + aimTime + outOfRangeTime;

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
                float beamDelay = 0f; //TODO: get real beam speed
                // normal beams
                if(weapon.isBeam() && !weapon.isBurstBeam()){
                    float currentTime = preAimedTime;
                    while(currentTime < maxTime - beamDelay){
                        FutureHit futurehit = new FutureHit();
                        futurehit.enemyId = enemy.getId();
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
                    }
                    else if(weapon.getCooldownRemaining() > 0){ // weapon is in chargedown, chargedown and cooldown overlap by Starsector's standards (Blame Alex)
                        cooldownTime = weapon.getCooldown() - weapon.getSpec().getBeamChargedownTime();
                        chargedownTime = weapon.getCooldownRemaining() - cooldownTime;
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
                                futurehit.enemyId = enemy.getId();
                                futurehit.timeToHit = currentTime;
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = true;
                                futurehit.hitStrength = trueSingleInstanceDamage / 6;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 30;
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 30;
                                futureHits.add(futurehit);
                            }
                            chargeupTime -= 0.1f;
                            currentTime += 0.1f;
                        }

                        activeTime += chargeupTime; // carry over borrowed time
                        while (activeTime > 0) { // resolve active damage
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.enemyId = enemy.getId();
                                futurehit.timeToHit = currentTime;
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = true;
                                futurehit.hitStrength = trueSingleInstanceDamage / 2;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 10;
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 10;
                                futureHits.add(futurehit);
                            }
                            activeTime -= 0.1f;
                            currentTime += 0.1f;
                        }

                        chargedownTime += activeTime; // carry over borrowed time
                        while (chargedownTime > 0) { // resolve chargedown damage
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.enemyId = enemy.getId();
                                futurehit.timeToHit = currentTime;
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = true;
                                futurehit.hitStrength = trueSingleInstanceDamage / 6;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels) / 30;
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels) / 30;
                                futureHits.add(futurehit);
                            }
                            chargedownTime -= 0.1f;
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
                    MissileSpecAPI spec = (MissileSpecAPI) weapon.getSpec().getProjectileSpec();
                    ShipHullSpecAPI.EngineSpecAPI missileEngine = spec.getHullSpec().getEngineSpec();
                    float launchSpeed = ((MissileSpecAPI) weapon.getSpec().getProjectileSpec()).getLaunchSpeed();
                    float maxSpeed = stats.getMissileMaxSpeedBonus().computeEffective(missileEngine.getMaxSpeed());
                    float acceleration = stats.getMissileAccelerationBonus().computeEffective(missileEngine.getAcceleration());
                    float maxTurnRate = stats.getMissileMaxTurnRateBonus().computeEffective(missileEngine.getMaxTurnRate());

                    // I hate mirvs
                    if(spec.getBehaviorSpec() != null && spec.getBehaviorSpec().getBehavorString().contains("MIRV")){
                        maxSpeed *= 3;
                        acceleration *= 2;
                        maxTurnRate *= 3;
                    }
                    travelTime = missileTravelTime(launchSpeed, maxSpeed, acceleration, maxTurnRate, weapon.getCurrAngle(), weapon.getLocation(), ship.getLocation(), targetingRadius);
                }
                else {
                    Vector2f projectileVector = VectorUtils.resize(VectorUtils.getDirectionalVector(weapon.getLocation(), testPoint), weapon.getProjectileSpeed());
                    Vector2f relativeVelocity = Vector2f.add(Vector2f.sub(projectileVector, ship.getVelocity(), null), enemy.getVelocity(), null);
                    travelTime = (distanceFromWeapon-targetingRadius) / relativeVelocity.length();
                }
                travelTime = Math.max(travelTime, 0); // travel time cant be negative

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
                        currentTime += Math.max(0, chargeupTime);
                        if(currentTime > preAimedTime){
                            FutureHit futurehit = new FutureHit();
                            futurehit.enemyId = enemy.getId();
                            futurehit.timeToHit = (currentTime + travelTime);
                            futurehit.angle = shipToWeaponAngle;
                            futurehit.damageType = weapon.getDamageType();
                            futurehit.softFlux = weapon.getDamage().isSoftFlux();
                            futurehit.hitStrength = trueSingleInstanceDamage;
                            futurehit.damage = (trueSingleInstanceDamage * linkedBarrels);
                            futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels);
                            futureHits.add(futurehit);
                        }
                        currentTime += Math.max(0, cooldownTime);
                        currentTime = Math.max(currentTime, preAimedTime); // wait for weapon to finish aiming if not yet aimed

                        currentTime += (chargeupTime + cooldownTime) <= SINGLE_FRAME ? SINGLE_FRAME : 0; // make sure to not get stuck in an infinite

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
                        currentTime += Math.max(0, chargeupTime);
                        while(burstTime > 0.01f) { // avoid floating point jank
                            if (currentTime > preAimedTime) {
                                FutureHit futurehit = new FutureHit();
                                futurehit.enemyId = enemy.getId();
                                futurehit.timeToHit = (currentTime + travelTime);
                                futurehit.angle = shipToWeaponAngle;
                                futurehit.damageType = weapon.getDamageType();
                                futurehit.softFlux = weapon.getDamage().isSoftFlux();
                                futurehit.hitStrength = trueSingleInstanceDamage;
                                futurehit.damage = (trueSingleInstanceDamage * linkedBarrels);
                                futurehit.empDamage = (trueSingleInstanceEMPDamage * linkedBarrels);
                                futureHits.add(futurehit);
                            }
                            burstTime -= Math.max(SINGLE_FRAME, burstDelay);
                            currentTime += Math.max(SINGLE_FRAME, burstDelay);
                            if(currentTime > (maxTime - travelTime)) break;
                        }
                        currentTime += Math.max(0, cooldownTime);
                        currentTime = Math.max(currentTime, preAimedTime); // wait for weapon to finish aiming if not yet aimed

                        currentTime += (chargeupTime + cooldownTime) <= SINGLE_FRAME ? SINGLE_FRAME : 0; // make sure to not get stuck in an infinite

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

    public static float getCurrentArmorRating(ShipAPI ship){

        if (ship == null || !Global.getCombatEngine().isEntityInPlay(ship)) return 0f;

        ArmorGridAPI armorGrid = ship.getArmorGrid();
        float[][] armorGridGrid = armorGrid.getGrid();
        List<Float> armorList = new ArrayList<>();
        org.lwjgl.util.Point worstPoint = DefenseUtils.getMostDamagedArmorCell(ship);
        if(worstPoint != null){
            float totalArmor = 0;
            for (int x = 0; x < armorGridGrid.length; x++) {
                for (int y = 0; y < armorGridGrid[x].length; y++) {
                    armorList.add(armorGridGrid[x][y]);
                }
            }
            Collections.sort(armorList);
            for(int i = 0; i < 21; i++){
                if(i < 9) totalArmor += armorList.get(i);
                else  totalArmor += armorList.get(i)/2;
            }
            return totalArmor;
        } else{
            return armorGrid.getMaxArmorInCell() * 15f;
        }
    }

    public static Pair<Float, Float> damageAfterArmor(DamageType damageType, float damage, float hitStrength, float armorValue, ShipAPI ship){
        MutableShipStatsAPI stats = ship.getMutableStats();

        float armorMultiplier = stats.getArmorDamageTakenMult().getModifiedValue();
        float effectiveArmorMult = stats.getEffectiveArmorBonus().getMult();
        float hullMultiplier = stats.getHullDamageTakenMult().getModifiedValue();
        float minArmor = stats.getMinArmorFraction().getModifiedValue();
        float maxDR = stats.getMaxArmorDamageReduction().getModifiedValue();

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

        damage *= Math.max((1f - maxDR), ((hitStrength * armorMultiplier) / (Math.max(minArmor * ship.getArmorGrid().getArmorRating(), armorValue) * effectiveArmorMult + hitStrength * armorMultiplier)));

        float armorDamage = damage * armorMultiplier;
        float hullDamage = 0;
        if (armorDamage > armorValue){
            hullDamage = ((armorDamage - armorValue)/armorDamage) * damage * hullMultiplier;
        }

        return new Pair<>(armorDamage, hullDamage);
    }

    public static float fluxToShield(DamageType damageType, float damage, ShipAPI ship){
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

        return (damage * ship.getShield().getFluxPerPointOfDamage() * shieldMultiplier);
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

    public static Pair<Vector2f, ShipAPI> getLowestDangerTargetInRange(ShipAPI ship, Map<ShipAPI, Map<String, Float>> nearbyEnemies, float maxAngle,
                                                                       float weaponRange, boolean respectLOS, float hullLevelLimitForShipExplosion){
        float degreeDelta = 10f;

        //get the all the "outside" ships
        List<ShipAPI> enemyShipsOnConvexHull = getConvexHull(new ArrayList<>(nearbyEnemies.keySet()));

        Map<ShipAPI, List<Vector2f>> targetEnemys = new HashMap<>();

        // add all the target points from enemies on the "outside edges"
        for(ShipAPI enemy : enemyShipsOnConvexHull){
            if (!isPointWithinMap(enemy.getLocation(), 200)) continue; // skip enemies out of the map
            float optimalRange = weaponRange + Misc.getTargetingRadius(ship.getLocation(), enemy, false);
            if(enemy.getHullLevel() < hullLevelLimitForShipExplosion)
                optimalRange = Math.max(optimalRange, enemy.getShipExplosionRadius() + Misc.getTargetingRadius(enemy.getLocation(), ship, false) + 100f);
            List<Vector2f> potentialPoints = new ArrayList<>();
            float enemyAngle = VectorUtils.getAngle(enemy.getLocation(), ship.getLocation());
            float currentAngle = enemyAngle - maxAngle;
            while(currentAngle < enemyAngle + maxAngle){
                potentialPoints.add(MathUtils.getPointOnCircumference(enemy.getLocation(), optimalRange, currentAngle));
                currentAngle += degreeDelta;
            }
            targetEnemys.put(enemy, potentialPoints);
        }

        // remove the points that requires ship to fly through other ships
        for(ShipAPI enemy : Global.getCombatEngine().getShips()){
            // Ignore fighters, yourself, your own modules, dead ships, ally ships, and ships that are too far away
            if (enemy.isFighter() || enemy == ship || enemy.getParentStation() == ship || !enemy.isAlive() || enemy.getOwner() == ship.getOwner() || !MathUtils.isWithinRange(enemy, ship, 3000f))
                continue;
            for(Map.Entry<ShipAPI, List<Vector2f>> targetEnemy : targetEnemys.entrySet()){
                List<Vector2f> pointsToRemove = new ArrayList<>();
                for(Vector2f potentialPoint : targetEnemy.getValue()){
                    if(!respectLOS && targetEnemy.getKey() == enemy) continue;
                    float keepoutRadius = enemy.getCollisionRadius() + 100f;
                    if(CollisionUtils.getCollides(potentialPoint, ship.getLocation(), enemy.getLocation(), keepoutRadius)){
                        pointsToRemove.add(potentialPoint);
                    }
                }
                targetEnemy.getValue().removeAll(pointsToRemove);
            }
        }


        Map<Vector2f, Triple<Float, Float, ShipAPI>> pointData = new HashMap<>();
        float highestDanger = Float.NEGATIVE_INFINITY;
        float lowestDanger = Float.POSITIVE_INFINITY;
        float highestHardfluxLevel = Float.NEGATIVE_INFINITY;
        float lowestHardfluxLevel = Float.POSITIVE_INFINITY;

        for(Map.Entry<ShipAPI, List<Vector2f>> targetEnemy : targetEnemys.entrySet()) {
            for (Vector2f potentialPoint : targetEnemy.getValue()) {
                float pointDanger = getPointDanger(nearbyEnemies, potentialPoint);

                if (pointDanger < lowestDanger) lowestDanger = pointDanger;
                if (pointDanger > highestDanger) highestDanger = pointDanger;


                float hardfluxPerDistance = ship.getPhaseCloak() != null ? ship.getPhaseCloak().getFluxPerSecond()/(ship.getMaxSpeed()*3) : ship.getShield().getUpkeep()/ship.getMaxSpeed();
                float hardfluxAtPoint = MathUtils.getDistance(ship.getLocation(), potentialPoint) * hardfluxPerDistance + ship.getFluxTracker().getHardFlux();
                float hardfluxLevelAtPoint = hardfluxAtPoint/ship.getMaxFlux();


                if (hardfluxLevelAtPoint < lowestHardfluxLevel) lowestHardfluxLevel = hardfluxLevelAtPoint;
                if (hardfluxLevelAtPoint > highestHardfluxLevel) highestHardfluxLevel = hardfluxLevelAtPoint;

                pointData.put(potentialPoint, new Triple<>(pointDanger, hardfluxLevelAtPoint, targetEnemy.getKey()));
            }
        }

        Vector2f optimalStrafePoint = null;
        ShipAPI target = null;
        float lowestCombinedFactor = Float.POSITIVE_INFINITY;
        for(Vector2f potentialPoint : pointData.keySet()) {

            float pointDanger = pointData.get(potentialPoint).getFirst();
            float pointHardfluxLevel = pointData.get(potentialPoint).getSecond();

            float dangerFactor = (pointDanger - lowestDanger)/(highestDanger - lowestDanger);
            dangerFactor = Float.isNaN(dangerFactor) ? 0f : dangerFactor;

            float hardfluxFactor = (pointHardfluxLevel - lowestHardfluxLevel)/(highestHardfluxLevel - lowestHardfluxLevel);
            hardfluxFactor = Float.isNaN(hardfluxFactor) ? 0f : hardfluxFactor;

            if (dangerFactor + hardfluxFactor < lowestCombinedFactor){
                lowestCombinedFactor = dangerFactor + hardfluxFactor;
                optimalStrafePoint = potentialPoint;
                target = pointData.get(potentialPoint).getThird();
            }

            if(DEBUG_ENABLED){
                Global.getCombatEngine().addFloatingText(potentialPoint, String.valueOf(pointDanger), 10, Color.white, null, 0, 0);
            }
        }
        return new Pair<>(optimalStrafePoint, target);
    }

    public static boolean isPointWithinMap(Vector2f point, float pad){
        CombatEngineAPI engine = Global.getCombatEngine();
        return (point.getX() < (engine.getMapWidth() / 2 - pad)) &&
                (point.getX() > (pad - engine.getMapWidth() / 2)) &&
                (point.getY() < (engine.getMapHeight() / 2 - pad)) &&
                (point.getY() > (pad - engine.getMapHeight() / 2));
    }

    public static Vector2f getBackingOffStrafePoint(ShipAPI ship){
        float secondsInFuture = 1f;
        float degreeDelta = 5f;
        Vector2f futureLocation= new Vector2f();
        Vector2f.add(ship.getLocation(), ship.getVelocity(), futureLocation);
        futureLocation.scale(secondsInFuture);
        List<Vector2f> potentialPoints = MathUtils.getPointsAlongCircumference(futureLocation, 1000f, (int) (360f/degreeDelta), 0);
        CollectionUtils.CollectionFilter<Vector2f> filterBorder = new CollectionUtils.CollectionFilter<Vector2f>() {
            @Override
            public boolean accept(Vector2f point) {
                return isPointWithinMap(point, 200);
            }
        };


        potentialPoints = CollectionUtils.filter(potentialPoints, filterBorder);
        Vector2f safestPoint = null;
        float furthestPointSumDistance = 0;
        List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, 3000f);
        for (Vector2f potentialPoint : potentialPoints) {
            float currentPointSumDistance = 0;
            for(ShipAPI enemy : enemies){
                if(enemy.getHullSize() != ShipAPI.HullSize.FIGHTER)
                    currentPointSumDistance += MathUtils.getDistance(enemy, potentialPoint);
            }
            if(currentPointSumDistance > furthestPointSumDistance){
                furthestPointSumDistance = currentPointSumDistance;
                safestPoint = potentialPoint;
            }
        }

        return safestPoint;
    }

    public static Map<String, Float> getShipStats(ShipAPI enemy, float defaultRange){

        float deltaAngle = 1f;
        float currentRelativeAngle = 0f;
        Map<String, Float> highLowDPS = new HashMap<>();
        float highestDPS = 0;
        float lowestDPS = Float.POSITIVE_INFINITY;
        float highestDPSAngle = 0;
        float maxRange = 0;
        float minRange = Float.POSITIVE_INFINITY;

        while(currentRelativeAngle <= 360){
            float potentialDPS = 0;
            for (WeaponAPI weapon: enemy.getAllWeapons()){
                if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                    if((Math.abs(weapon.getArcFacing() - currentRelativeAngle) < weapon.getArc()/2) && (defaultRange < weapon.getRange())){
                        potentialDPS += Math.max(weapon.getDerivedStats().getDps(), weapon.getDerivedStats().getBurstDamage());
                    }
                    if(!weapon.hasAIHint(WeaponAPI.AIHints.PD)){
                        maxRange = Math.max(weapon.getRange(), maxRange);
                        minRange = Math.min(weapon.getRange(), minRange);
                    }
                }
            }
            if(potentialDPS > highestDPS){
                highestDPS = potentialDPS;
            }
            if(potentialDPS < lowestDPS){
                lowestDPS = potentialDPS;
            }

            currentRelativeAngle += deltaAngle;
        }

        currentRelativeAngle = -1f;

        float lastDPS = highestDPS;
        float highZoneStart = 0;
        float highZoneEnd = 0;

        while(currentRelativeAngle <= 360){
            float potentialDPS = 0;
            for (WeaponAPI weapon: enemy.getAllWeapons()){
                if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                    if((Math.abs(weapon.getArcFacing() - currentRelativeAngle) < weapon.getArc()/2) && (defaultRange < weapon.getRange())){
                        potentialDPS += Math.max(weapon.getDerivedStats().getDps(), weapon.getDerivedStats().getBurstDamage());
                    }
                }
            }
            if(potentialDPS > highestDPS*0.8f && lastDPS < highestDPS*0.8f){
                highZoneStart = currentRelativeAngle;
            }
            if(potentialDPS < highestDPS*0.8f && lastDPS > highestDPS*0.8f){
                highZoneEnd = currentRelativeAngle;
            }
            lastDPS = potentialDPS;
            currentRelativeAngle += deltaAngle;
        }

        highestDPSAngle = highZoneEnd - (highZoneEnd - highZoneStart)/2;

        highLowDPS.put("HighestDPS", highestDPS);
        highLowDPS.put("LowestDPS", lowestDPS);
        highLowDPS.put("HighestDPSAngle", highestDPSAngle);
        highLowDPS.put("MaxRange", maxRange);
        highLowDPS.put("MinRange", minRange);
        return highLowDPS;
    }

    public static int orientation(Vector2f p, Vector2f q, Vector2f r) {
        float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
        if (val == 0)
            return 0; // collinear
        return (val > 0) ? 1 : 2; // clock or counterclockwise
    }

    public static List<ShipAPI> getConvexHull(List<ShipAPI> ships) {
        int n = ships.size();
        if (n < 3)
            return ships;

        List<ShipAPI> hull = new ArrayList<>();

        // Find the leftmost point
        int leftmost = 0;
        for (int i = 1; i < n; i++) {
            if (ships.get(i).getLocation().x < ships.get(leftmost).getLocation().x)
                leftmost = i;
        }

        int p = leftmost, q;
        do {
            hull.add(ships.get(p));
            q = (p + 1) % n;
            for (int i = 0; i < n; i++) {
                if (orientation(ships.get(p).getLocation(), ships.get(i).getLocation(), ships.get(q).getLocation()) == 2)
                    q = i;
            }
            p = q;
        } while (p != leftmost && hull.size() < n);

        return hull;
    }

    public static float getPointDanger(Map<ShipAPI, Map<String, Float>> nearbyEnemies, Vector2f testPoint){
        float currentPointDanger = 0f;
        if (testPoint == null)
            return 0f;

        for (ShipAPI enemy: nearbyEnemies.keySet()) {
            Map<String, Float> enemyStat = nearbyEnemies.get(enemy);

            //float currentTargetBias = (enemy == target && MathUtils.getDistance(ship.getLocation(), enemy.getLocation()) < targetRange * 1.2) ? 0.1f : 1f;  , ShipAPI ship, float targetRange, ShipAPI target
            float currentTargetBias = 1;

            float highestDPSAngle = enemy.getFacing() + enemyStat.get("HighestDPSAngle");
            float alpha = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(enemy.getLocation(), testPoint), highestDPSAngle))/180f;

            float DPSDanger = (1f - alpha) * enemyStat.get("HighestDPS") + alpha * enemyStat.get("LowestDPS");

            float shipDistance = MathUtils.getDistance(enemy.getLocation(), testPoint);
            float currentlyOccluded = 1;
            for (ShipAPI otherEnemy: nearbyEnemies.keySet()) {
                if (otherEnemy != enemy && CollisionUtils.getCollides(enemy.getLocation(), testPoint, otherEnemy.getLocation(), otherEnemy.getCollisionRadius())){
                    currentlyOccluded = 0;
                }
            }
            if(shipDistance < enemyStat.get("MaxRange")){
                currentPointDanger += (DPSDanger) * (1 - Math.max(0, Math.min((shipDistance - enemyStat.get("MinRange"))/enemyStat.get("MaxRange"), 1)))
                        * (1-enemy.getFluxLevel()) * enemy.getHullLevel() * currentTargetBias * currentlyOccluded;
            }
        }
        return currentPointDanger;
    }

    public static void strafeToPointV3(ShipAPI ship, Vector2f targetPoint, Vector2f targetVelocity){
        Vector2f relVelocity = Vector2f.sub(ship.getVelocity(), targetVelocity, null);
        Vector2f position = new Vector2f(ship.getLocation());

        float bufferRange = 50f;

        // strafing uses forwards accel, but with a penalty
        float strafeAccelFactor =
            ship.getHullSize() == ShipAPI.HullSize.FIGHTER ? 1f :
            ship.getHullSize() == ShipAPI.HullSize.FRIGATE ? 1f :
            ship.getHullSize() == ShipAPI.HullSize.DESTROYER ? 0.75f :
            ship.getHullSize() == ShipAPI.HullSize.CRUISER ? 0.5f :
            ship.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP ? 0.25f : 0;

        Vector2f forwardsAccel = (Vector2f) Misc.getUnitVectorAtDegreeAngle(ship.getFacing()).scale(ship.getAcceleration());
        Vector2f backwardsAccel = (Vector2f) Misc.getUnitVectorAtDegreeAngle(ship.getFacing()+180f).scale(ship.getDeceleration());
        Vector2f leftAccel = (Vector2f) Misc.getUnitVectorAtDegreeAngle(ship.getFacing()+90).scale(ship.getAcceleration() * strafeAccelFactor);
        Vector2f rightAccel = leftAccel.negate(null);
        Vector2f decel = ship.getVelocity().lengthSquared() > 0 ? (Vector2f) ship.getVelocity().negate().normalise().scale(ship.getDeceleration()) : new Vector2f();

        // 8 directions to accelerate in
        List<Triple<Vector2f, List<ShipCommand>, Pair<Float, Float>>> accelOptions = new ArrayList<>();
        accelOptions.add(new Triple<>(forwardsAccel, Collections.singletonList(ShipCommand.ACCELERATE), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(backwardsAccel, Collections.singletonList(ShipCommand.ACCELERATE_BACKWARDS), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(leftAccel, Collections.singletonList(ShipCommand.STRAFE_LEFT), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(rightAccel, Collections.singletonList(ShipCommand.STRAFE_RIGHT), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(decel, Collections.singletonList(ShipCommand.DECELERATE), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(Vector2f.add(forwardsAccel, leftAccel, null),
                Arrays.asList(ShipCommand.ACCELERATE, ShipCommand.STRAFE_LEFT), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(Vector2f.add(forwardsAccel, rightAccel, null),
                Arrays.asList(ShipCommand.ACCELERATE, ShipCommand.STRAFE_RIGHT), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(Vector2f.add(backwardsAccel, leftAccel, null),
                Arrays.asList(ShipCommand.ACCELERATE_BACKWARDS, ShipCommand.STRAFE_LEFT), new Pair<>(0f,0f)));
        accelOptions.add(new Triple<>(Vector2f.add(backwardsAccel, rightAccel, null),
                Arrays.asList(ShipCommand.ACCELERATE_BACKWARDS, ShipCommand.STRAFE_RIGHT), new Pair<>(0f,0f)));

        // find the overall closest when accelerating in any direction, and closest for each direction
        float overallClosestSquared = Float.POSITIVE_INFINITY;
        for(Triple<Vector2f, List<ShipCommand>, Pair<Float, Float>> direction : accelOptions){
            Vector2f accel = direction.getFirst();
            float closestDistanceSquared = Float.POSITIVE_INFINITY;
            float speedAtClosestDistanceSquared = 0f;

            for(float t = 0; t < 3f; t += 0.1f){
                Vector2f futurePos = new Vector2f(position);
                Vector2f.add(futurePos, (Vector2f) new Vector2f(relVelocity).scale(t), futurePos);
                Vector2f.add(futurePos, (Vector2f) new Vector2f(accel).scale(0.5f * t * t), futurePos);

                float distanceSquared = MathUtils.getDistanceSquared(futurePos, targetPoint);
                if(distanceSquared > closestDistanceSquared) continue;

                closestDistanceSquared = distanceSquared;
                Vector2f futureVel = new Vector2f(relVelocity);
                Vector2f.add(futureVel, (Vector2f) new Vector2f(accel).scale(t), futureVel);
                speedAtClosestDistanceSquared = futureVel.lengthSquared();
            }
            direction.getThird().one = closestDistanceSquared;
            direction.getThird().two = speedAtClosestDistanceSquared;

            if(closestDistanceSquared < overallClosestSquared) overallClosestSquared = closestDistanceSquared;
        }

        // within some buffer target, find the option that minimises speed
        float lowestSpeedSquared = Float.POSITIVE_INFINITY;
        float overallClosest = (float) Math.sqrt(overallClosestSquared);
        List<ShipCommand> bestCommands = new ArrayList<>();
        for(Triple<Vector2f, List<ShipCommand>, Pair<Float, Float>> direction : accelOptions){

            if(lowestSpeedSquared < direction.getThird().two) continue;
            float distanceSquared = (float) Math.sqrt(direction.getThird().one);
            if(distanceSquared < overallClosest + bufferRange){ // d1 < d2 + bufferRange
                lowestSpeedSquared = direction.getThird().two;
                bestCommands = direction.getSecond();
            }
        }

        // Issue the best commands to the ship and block the others
        EnumSet<ShipCommand> movementCommands = java.util.EnumSet.of(
                ShipCommand.ACCELERATE,
                ShipCommand.ACCELERATE_BACKWARDS,
                ShipCommand.STRAFE_LEFT,
                ShipCommand.STRAFE_RIGHT
        );

        for (ShipCommand command : movementCommands) {
            if (bestCommands.contains(command)) {
                // Issue the command
                ship.giveCommand(command, null, 0);
            } else {
                // Block the command for one frame
                ship.blockCommandForOneFrame(command);
            }
        }
    }

    public static void strafeToPointV2(ShipAPI ship, Vector2f strafePoint){
        // Calculate the unit vector toward the target
        Vector2f uTarget = VectorUtils.getDirectionalVector(ship.getLocation(), strafePoint);

        // Calculate the forward unit vector based on current facing
        Vector2f uFwd = MathUtils.getPoint(new Vector2f(0f,0f),1f, ship.getFacing());

        // Calculate unit vectors for right and left directions
        Vector2f uRight = VectorUtils.rotate(new Vector2f(uFwd), -90f);
        Vector2f uLeft = VectorUtils.rotate(new Vector2f(uFwd), 90f);

        // Boolean flags for each command
        boolean[][] commandFlags = {
                // Format: {ACCELERATE, ACCELERATE_BACKWARDS, STRAFE_LEFT, STRAFE_RIGHT}
                {false, false, false, false},
                {true, false, false, false},   // ACCELERATE
                {false, true, false, false},   // ACCELERATE_BACKWARDS
                {false, false, true, false},   // STRAFE_LEFT
                {false, false, false, true},   // STRAFE_RIGHT
                {true, false, true, false},    // ACCELERATE + STRAFE_LEFT
                {true, false, false, true},    // ACCELERATE + STRAFE_RIGHT
                {false, true, true, false},    // ACCELERATE_BACKWARDS + STRAFE_LEFT
                {false, true, false, true}     // ACCELERATE_BACKWARDS + STRAFE_RIGHT
        };

        // Variables to track the best command combination
        float maxProjection = Float.NEGATIVE_INFINITY;
        java.util.List<ShipCommand> bestCommands = new java.util.ArrayList<>();

        // strafing uses forwards accel, but with a penalty
        HashMap<ShipAPI.HullSize, Float> strafeAccelFactor = new HashMap<ShipAPI.HullSize, Float>();
        strafeAccelFactor.put(ShipAPI.HullSize.FIGHTER, 1f);
        strafeAccelFactor.put(ShipAPI.HullSize.FRIGATE, 1f);
        strafeAccelFactor.put(ShipAPI.HullSize.DESTROYER, 0.75f);
        strafeAccelFactor.put(ShipAPI.HullSize.CRUISER, 0.5f);
        strafeAccelFactor.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.25f);

        // Iterate over all valid combinations of commands
        for (boolean[] flags : commandFlags) {
            boolean accelerate = flags[0];
            boolean accelerateBackwards = flags[1];
            boolean strafeLeft = flags[2];
            boolean strafeRight = flags[3];

            // Skip invalid combinations
            if (accelerate && accelerateBackwards) continue;
            if (strafeLeft && strafeRight) continue;

            java.util.List<ShipCommand> commands = new java.util.ArrayList<>();
            Vector2f accelVector = new Vector2f(0f, 0f);
            ship.getAcceleration();

            // Handle ACCELERATE
            if (accelerate) {
                commands.add(ShipCommand.ACCELERATE);
                Vector2f temp = new Vector2f(uFwd);
                temp.scale(ship.getAcceleration());
                Vector2f.add(accelVector, temp, accelVector);
            }

            // Handle ACCELERATE_BACKWARDS
            if (accelerateBackwards) {
                commands.add(ShipCommand.ACCELERATE_BACKWARDS);
                Vector2f temp = new Vector2f(uFwd);
                temp.negate();
                temp.scale(ship.getDeceleration());
                Vector2f.add(accelVector, temp, accelVector);
            }

            // Handle STRAFE_LEFT
            if (strafeLeft) {
                commands.add(ShipCommand.STRAFE_LEFT);
                Vector2f temp = new Vector2f(uLeft);
                temp.scale(ship.getAcceleration() * strafeAccelFactor.get(ship.getHullSize()));
                Vector2f.add(accelVector, temp, accelVector);
            }

            // Handle STRAFE_RIGHT
            if (strafeRight) {
                commands.add(ShipCommand.STRAFE_RIGHT);
                Vector2f temp = new Vector2f(uRight);
                temp.scale(ship.getAcceleration() * strafeAccelFactor.get(ship.getHullSize()));
                Vector2f.add(accelVector, temp, accelVector);
            }


            // Apply acceleration to current speed to get new speed
            Vector2f newSpeed = new Vector2f();
            Vector2f.add(ship.getVelocity(), accelVector, newSpeed);

            // Check if new speed exceeds top speed
            if (newSpeed.length() > ship.getMaxSpeed()) {
                // Adjust the speed components to bring the speed back to topSpeed
                adjustSpeedToMax(newSpeed, ship.getMaxSpeed(), accelVector);
            }

            // Compute the projection of the new speed onto the target direction
            float projection = Vector2f.dot(newSpeed, uTarget);

            if (projection > maxProjection) {
                maxProjection = projection;
                bestCommands = commands;
            }
        }

        // Issue the best commands to the ship and block the others
        EnumSet<ShipCommand> movementCommands = java.util.EnumSet.of(
            ShipCommand.ACCELERATE,
            ShipCommand.ACCELERATE_BACKWARDS,
            ShipCommand.STRAFE_LEFT,
            ShipCommand.STRAFE_RIGHT
        );

        for (ShipCommand command : movementCommands) {
            if (bestCommands.contains(command)) {
                // Issue the command
                ship.giveCommand(command, null, 0);
            } else {
                // Block the command for one frame
                ship.blockCommandForOneFrame(command);
            }
        }
    }

    public static void adjustSpeedToMax(Vector2f speed, float maxSpeed, Vector2f accelVector) {
        // Calculate the excess speed
        float speedMagnitude = speed.length();
        float excessSpeed = speedMagnitude - maxSpeed;

        // Desired speed direction based on acceleration
        Vector2f desiredDirection = new Vector2f(accelVector);
        if (desiredDirection.lengthSquared() != 0f) {
            desiredDirection.normalise();
        } else {
            // If no acceleration, desired direction is current speed direction
            desiredDirection.set(speed);
            desiredDirection.normalise();
        }

        // Compute the desired speed components
        float desiredSpeedX = desiredDirection.x * maxSpeed;
        float desiredSpeedY = desiredDirection.y * maxSpeed;

        // Compute deviations for each component
        float deviationX = Math.abs(speed.x - desiredSpeedX);
        float deviationY = Math.abs(speed.y - desiredSpeedY);

        // Total deviation
        float totalDeviation = deviationX + deviationY;

        // If total deviation is zero, scale down the speed vector proportionally
        if (totalDeviation == 0f) {
            speed.normalise();
            speed.scale(maxSpeed);
            return;
        }

        // Calculate the proportion of excess speed to reduce from each component
        float reductionX = (deviationX / totalDeviation) * excessSpeed * (speed.x > desiredSpeedX ? 1 : -1);
        float reductionY = (deviationY / totalDeviation) * excessSpeed * (speed.y > desiredSpeedY ? 1 : -1);

        // Adjust the speed components
        speed.x -= reductionX;
        speed.y -= reductionY;

        // Ensure the adjusted speed magnitude does not exceed maxSpeed due to rounding errors
        speedMagnitude = speed.length();
        if (speedMagnitude > maxSpeed) {
            speed.normalise();
            speed.scale(maxSpeed);
        }
    }

    public static boolean isWithinFiringRange(ShipAPI ship, ShipAPI target, float range){
        return MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < Math.pow(Misc.getTargetingRadius(ship.getLocation(), target, false) + range, 2);
    }

    public static void turnToPoint(ShipAPI ship, Vector2f turnPoint){
        float turnAngle = VectorUtils.getAngle(ship.getLocation(), turnPoint);
        float rotAngle = MathUtils.getShortestRotation(ship.getFacing(), turnAngle);
        ship.getTurnAcceleration();
        ship.getAngularVelocity();

        boolean decel = false;
        if (ship.getAngularVelocity() * rotAngle > 0){ // make sure velocity and angle have the same sign, (only slow down if it makes sense)
            if (Math.pow(ship.getAngularVelocity(), 2) / (2 * ship.getTurnAcceleration()) > Math.abs(rotAngle)){ // basic kinematic solution
                decel = true;
            }
        }

        if ((rotAngle > 0) ^ decel) {
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
            ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
        } else{
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
            ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
        }
    }

    public static void stayStill(ShipAPI ship){
        ship.giveCommand(ShipCommand.DECELERATE, null, 0);
        ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
        ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
        ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
        ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
        ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
        ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
        ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
    }

    public static void holdFire(ShipAPI ship){
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if(!weapon.isDecorative()){
                weapon.setForceNoFireOneFrame(true);
            }
        }
    }

    public static void applyDamper(ShipAPI ship, String id, float level){
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(id, 0.000001f);
        stats.getArmorDamageTakenMult().modifyMult(id, 0.000001f);
        stats.getEmpDamageTakenMult().modifyMult(id, 0.000001f);
        stats.getWeaponDamageTakenMult().modifyMult(id, 0.000001f);
        stats.getEngineDamageTakenMult().modifyMult(id, 0.000001f);
        stats.getCombatEngineRepairTimeMult().modifyMult(id, 0.000001f);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, 0.000001f);


        ship.getShield().toggleOff();
        ship.fadeToColor(id + ship.getId(), new Color(75,75,75,255), 0.1f, 0.1f, level);
        ship.setJitterUnder(id + ship.getId(), new Color(100,165,255,255), level, 15, 0f, 15f);
    }

    public static void unapplyDamper(ShipAPI ship, String id){
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getWeaponDamageTakenMult().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);
        stats.getCombatEngineRepairTimeMult().unmodify(id);
        stats.getCombatWeaponRepairTimeMult().unmodify(id);
        ship.fadeToColor(id + ship.getId(), new Color(75,75,75,255), 0.1f, 0.1f, 0);
        ship.setJitterUnder(id + ship.getId(), new Color(100,165,255,255), 0, 15, 0f, 15f);
    }

    public static float lowestWeaponAmmoLevel(ShipAPI ship){
        float lowestAmmoLevel = 1f;
        for (WeaponAPI weapon: ship.getAllWeapons()){
            if(!weapon.isDecorative() && !weapon.hasAIHint(WeaponAPI.AIHints.PD) && (weapon.getType() != WeaponAPI.WeaponType.MISSILE) && weapon.usesAmmo()){
                float currentAmmoLevel = (float) weapon.getAmmo() / weapon.getMaxAmmo();
                if (currentAmmoLevel < lowestAmmoLevel){
                    lowestAmmoLevel = currentAmmoLevel;
                }
            }
        }
        return lowestAmmoLevel;
    }

    public static float DPSPercentageOfWeaponsOnCooldown(ShipAPI ship) {
        float totalDPS = 0f;
        float totalDPSOnCooldown = 0f;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.isDecorative() || weapon.hasAIHint(WeaponAPI.AIHints.PD)) continue;

            float weaponDPS = weapon.getDerivedStats().getSustainedDps();
            totalDPS += weaponDPS;

            if (weapon.isBeam() && !weapon.isBurstBeam()) {
                // Continuous beams are never on cooldown
                continue;
            } else if ((weapon.isBeam() && weapon.isBurstBeam()) || weapon.getSpec().getBurstSize() == 1) {
                // Burst beams/Single-shot projectiles are on cooldown if getCooldownRemaining() > 0
                if (weapon.getCooldownRemaining() > 0) totalDPSOnCooldown += weaponDPS;
            } else if (weapon.getSpec().getBurstSize() > 1) {
                // Burst projectiles: Only count cooldown if between bursts (not in burst)
                if (weapon.getCooldownRemaining() > 0 && !weapon.isInBurst()) totalDPSOnCooldown += weaponDPS;
            }
        }

        return (totalDPS == 0f) ? 0f : totalDPSOnCooldown / totalDPS;
    }


    /**
     * An implementation of the Hungarian algorithm for solving the assignment
     * problem. An instance of the assignment problem consists of a number of
     * workers along with a number of jobs and a cost matrix which gives the cost of
     * assigning the i'th worker to the j'th job at position (i, j). The goal is to
     * find an assignment of workers to jobs so that no job is assigned more than
     * one worker and so that no worker is assigned to more than one job in such a
     * manner so as to minimize the total cost of completing the jobs.
     * <p>
     *
     * An assignment for a cost matrix that has more workers than jobs will
     * necessarily include unassigned workers, indicated by an assignment value of
     * -1; in no other circumstance will there be unassigned workers. Similarly, an
     * assignment for a cost matrix that has more jobs than workers will necessarily
     * include unassigned jobs; in no other circumstance will there be unassigned
     * jobs. For completeness, an assignment for a square cost matrix will give
     * exactly one unique worker to each job.
     * <p>
     *
     * This version of the Hungarian algorithm runs in time O(n^3), where n is the
     * maximum among the number of workers and the number of jobs.
     *
     * @author Kevin L. Stern
     */
    public static class HungarianAlgorithm {
        private final float[][] costMatrix;
        private final int rows, cols, dim;
        private final float[] labelByWorker, labelByJob;
        private final int[] minSlackWorkerByJob;
        private final float[] minSlackValueByJob;
        private final int[] matchJobByWorker, matchWorkerByJob;
        private final int[] parentWorkerByCommittedJob;
        private final boolean[] committedWorkers;

        /**
         * Construct an instance of the algorithm.
         *
         * @param costMatrix
         *          the cost matrix, where matrix[i][j] holds the cost of assigning
         *          worker i to job j, for all i, j. The cost matrix must not be
         *          irregular in the sense that all rows must be the same length; in
         *          addition, all entries must be non-infinite numbers.
         */
        public HungarianAlgorithm(float[][] costMatrix) {
            this.dim = Math.max(costMatrix.length, costMatrix[0].length);
            this.rows = costMatrix.length;
            this.cols = costMatrix[0].length;
            this.costMatrix = new float[this.dim][this.dim];
            for (int w = 0; w < this.dim; w++) {
                if (w < costMatrix.length) {
                    if (costMatrix[w].length != this.cols) {
                        throw new IllegalArgumentException("Irregular cost matrix");
                    }
                    for (int j = 0; j < this.cols; j++) {
                        if (Float.isInfinite(costMatrix[w][j])) {
                            throw new IllegalArgumentException("Infinite cost");
                        }
                        if (Float.isNaN(costMatrix[w][j])) {
                            throw new IllegalArgumentException("NaN cost");
                        }
                    }
                    this.costMatrix[w] = Arrays.copyOf(costMatrix[w], this.dim);
                } else {
                    this.costMatrix[w] = new float[this.dim];
                }
            }
            labelByWorker = new float[this.dim];
            labelByJob = new float[this.dim];
            minSlackWorkerByJob = new int[this.dim];
            minSlackValueByJob = new float[this.dim];
            committedWorkers = new boolean[this.dim];
            parentWorkerByCommittedJob = new int[this.dim];
            matchJobByWorker = new int[this.dim];
            Arrays.fill(matchJobByWorker, -1);
            matchWorkerByJob = new int[this.dim];
            Arrays.fill(matchWorkerByJob, -1);
        }

        /**
         * Compute an initial feasible solution by assigning zero labels to the
         * workers and by assigning to each job a label equal to the minimum cost
         * among its incident edges.
         */
        protected void computeInitialFeasibleSolution() {
            for (int j = 0; j < dim; j++) {
                labelByJob[j] = Float.POSITIVE_INFINITY;
            }
            for (int w = 0; w < dim; w++) {
                for (int j = 0; j < dim; j++) {
                    if (costMatrix[w][j] < labelByJob[j]) {
                        labelByJob[j] = costMatrix[w][j];
                    }
                }
            }
        }

        /**
         * Execute the algorithm.
         *
         * @return the minimum cost matching of workers to jobs based upon the
         *         provided cost matrix. A matching value of -1 indicates that the
         *         corresponding worker is unassigned.
         */
        public int[] execute() {
            /*
             * Heuristics to improve performance: Reduce rows and columns by their
             * smallest element, compute an initial non-zero dual feasible solution and
             * create a greedy matching from workers to jobs of the cost matrix.
             */
            reduce();
            computeInitialFeasibleSolution();
            greedyMatch();

            int w = fetchUnmatchedWorker();
            while (w < dim) {
                initializePhase(w);
                executePhase();
                w = fetchUnmatchedWorker();
            }
            int[] result = Arrays.copyOf(matchJobByWorker, rows);
            for (w = 0; w < result.length; w++) {
                if (result[w] >= cols) {
                    result[w] = -1;
                }
            }
            return result;
        }

        /**
         * Execute a single phase of the algorithm. A phase of the Hungarian algorithm
         * consists of building a set of committed workers and a set of committed jobs
         * from a root unmatched worker by following alternating unmatched/matched
         * zero-slack edges. If an unmatched job is encountered, then an augmenting
         * path has been found and the matching is grown. If the connected zero-slack
         * edges have been exhausted, the labels of committed workers are increased by
         * the minimum slack among committed workers and non-committed jobs to create
         * more zero-slack edges (the labels of committed jobs are simultaneously
         * decreased by the same amount in order to maintain a feasible labeling).
         * <p>
         *
         * The runtime of a single phase of the algorithm is O(n^2), where n is the
         * dimension of the internal square cost matrix, since each edge is visited at
         * most once and since increasing the labeling is accomplished in time O(n) by
         * maintaining the minimum slack values among non-committed jobs. When a phase
         * completes, the matching will have increased in size.
         */
        protected void executePhase() {
            while (true) {
                int minSlackWorker = -1, minSlackJob = -1;
                float minSlackValue = Float.POSITIVE_INFINITY;
                for (int j = 0; j < dim; j++) {
                    if (parentWorkerByCommittedJob[j] == -1) {
                        if (minSlackValueByJob[j] < minSlackValue) {
                            minSlackValue = minSlackValueByJob[j];
                            minSlackWorker = minSlackWorkerByJob[j];
                            minSlackJob = j;
                        }
                    }
                }
                if (minSlackValue > 0) {
                    updateLabeling(minSlackValue);
                }
                parentWorkerByCommittedJob[minSlackJob] = minSlackWorker;
                if (matchWorkerByJob[minSlackJob] == -1) {
                    /*
                     * An augmenting path has been found.
                     */
                    int committedJob = minSlackJob;
                    int parentWorker = parentWorkerByCommittedJob[committedJob];
                    while (true) {
                        int temp = matchJobByWorker[parentWorker];
                        match(parentWorker, committedJob);
                        committedJob = temp;
                        if (committedJob == -1) {
                            break;
                        }
                        parentWorker = parentWorkerByCommittedJob[committedJob];
                    }
                    return;
                } else {
                    /*
                     * Update slack values since we increased the size of the committed
                     * workers set.
                     */
                    int worker = matchWorkerByJob[minSlackJob];
                    committedWorkers[worker] = true;
                    for (int j = 0; j < dim; j++) {
                        if (parentWorkerByCommittedJob[j] == -1) {
                            float slack = costMatrix[worker][j] - labelByWorker[worker]
                                    - labelByJob[j];
                            if (minSlackValueByJob[j] > slack) {
                                minSlackValueByJob[j] = slack;
                                minSlackWorkerByJob[j] = worker;
                            }
                        }
                    }
                }
            }
        }

        /**
         *
         * @return the first unmatched worker or {@link #dim} if none.
         */
        protected int fetchUnmatchedWorker() {
            int w;
            for (w = 0; w < dim; w++) {
                if (matchJobByWorker[w] == -1) {
                    break;
                }
            }
            return w;
        }

        /**
         * Find a valid matching by greedily selecting among zero-cost matchings. This
         * is a heuristic to jump-start the augmentation algorithm.
         */
        protected void greedyMatch() {
            for (int w = 0; w < dim; w++) {
                for (int j = 0; j < dim; j++) {
                    if (matchJobByWorker[w] == -1 && matchWorkerByJob[j] == -1
                            && costMatrix[w][j] - labelByWorker[w] - labelByJob[j] == 0) {
                        match(w, j);
                    }
                }
            }
        }

        /**
         * Initialize the next phase of the algorithm by clearing the committed
         * workers and jobs sets and by initializing the slack arrays to the values
         * corresponding to the specified root worker.
         *
         * @param w
         *          the worker at which to root the next phase.
         */
        protected void initializePhase(int w) {
            Arrays.fill(committedWorkers, false);
            Arrays.fill(parentWorkerByCommittedJob, -1);
            committedWorkers[w] = true;
            for (int j = 0; j < dim; j++) {
                minSlackValueByJob[j] = costMatrix[w][j] - labelByWorker[w]
                        - labelByJob[j];
                minSlackWorkerByJob[j] = w;
            }
        }

        /**
         * Helper method to record a matching between worker w and job j.
         */
        protected void match(int w, int j) {
            matchJobByWorker[w] = j;
            matchWorkerByJob[j] = w;
        }

        /**
         * Reduce the cost matrix by subtracting the smallest element of each row from
         * all elements of the row as well as the smallest element of each column from
         * all elements of the column. Note that an optimal assignment for a reduced
         * cost matrix is optimal for the original cost matrix.
         */
        protected void reduce() {
            for (int w = 0; w < dim; w++) {
                float min = Float.POSITIVE_INFINITY;
                for (int j = 0; j < dim; j++) {
                    if (costMatrix[w][j] < min) {
                        min = costMatrix[w][j];
                    }
                }
                for (int j = 0; j < dim; j++) {
                    costMatrix[w][j] -= min;
                }
            }
            float[] min = new float[dim];
            for (int j = 0; j < dim; j++) {
                min[j] = Float.POSITIVE_INFINITY;
            }
            for (int w = 0; w < dim; w++) {
                for (int j = 0; j < dim; j++) {
                    if (costMatrix[w][j] < min[j]) {
                        min[j] = costMatrix[w][j];
                    }
                }
            }
            for (int w = 0; w < dim; w++) {
                for (int j = 0; j < dim; j++) {
                    costMatrix[w][j] -= min[j];
                }
            }
        }

        /**
         * Update labels with the specified slack by adding the slack value for
         * committed workers and by subtracting the slack value for committed jobs. In
         * addition, update the minimum slack values appropriately.
         */
        protected void updateLabeling(float slack) {
            for (int w = 0; w < dim; w++) {
                if (committedWorkers[w]) {
                    labelByWorker[w] += slack;
                }
            }
            for (int j = 0; j < dim; j++) {
                if (parentWorkerByCommittedJob[j] != -1) {
                    labelByJob[j] -= slack;
                } else {
                    minSlackValueByJob[j] -= slack;
                }
            }
        }
    }

    public static Pair<Float, Float> calculateFiringAngle(Vector2f shooterPos, Vector2f shooterVel, Vector2f targetPos, Vector2f targetVel, float bulletSpeed, float maxRange) {

        // Step 1: Relative Motion
        Vector2f relativePos = Vector2f.sub(targetPos, shooterPos, null);
        Vector2f relativeVel = Vector2f.sub(targetVel, shooterVel, null);

        // Step 2: Quadratic Equation (ax^2 + bx + c = 0)
        float a = Vector2f.dot(relativeVel, relativeVel) - bulletSpeed * bulletSpeed;
        float b = 2 * Vector2f.dot(relativePos, relativeVel);
        float c = Vector2f.dot(relativePos, relativePos);

        // Step 3: Solve the quadratic equation
        float discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            // No solution (bullet cannot reach the target)
            return null;
        }

        float sqrtDiscriminant = (float) Math.sqrt(discriminant);
        float t1 = (-b + sqrtDiscriminant) / (2 * a);
        float t2 = (-b - sqrtDiscriminant) / (2 * a);


        if (Math.max(t1, t2) < 0) {
            // No solution in the future
            return null;
        }

        // Choose the smaller positive time
        float timeToIntercept = t1 < 0 ? t2 : (t2 < 0 ? t1 : Math.min(t1, t2));

        // Step 4: Angle Calculation
        Vector2f interceptPoint = new Vector2f(
                targetPos.x + targetVel.x * timeToIntercept,
                targetPos.y + targetVel.y * timeToIntercept);

        Vector2f shooterPoint = new Vector2f(
                shooterPos.x + shooterVel.x * timeToIntercept,
                shooterPos.y + shooterVel.y * timeToIntercept);

        // No solution (out of range of shooter)
        if(MathUtils.getDistanceSquared(interceptPoint, shooterPoint) > maxRange * maxRange){
            return null;
        }

        Vector2f firingDirection = Vector2f.sub(interceptPoint, shooterPos, null);
        float firingAngle;
        if(firingDirection.length() != 0){
            firingDirection.normalise();
            firingAngle = (float) Math.toDegrees(Math.atan2(firingDirection.y, firingDirection.x));
        } else{
            firingAngle = 0;
        }


        return new Pair<>(firingAngle, timeToIntercept);
    }
}
