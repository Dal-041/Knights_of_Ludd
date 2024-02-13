package org.selkie.kol.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;

public class KnightModule extends BaseHullMod {
    private final String id = "knightModule";
    public void init(HullModSpecAPI spec) {
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if(!ship.hasListenerOfClass(ModuleUnhulker.class)) ship.addListener(new ModuleUnhulker());

    }

    public static class ModuleUnhulker implements HullDamageAboutToBeTakenListener {
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if(ship.getHitpoints() <= damageAmount && !ship.hasTag("KOL_moduleDead")) {
                ship.setHulk(false);
                ship.addTag("KOL_moduleDead");
            }
            return false;
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(ship.getParentStation() == null) return;
        if (Global.getCurrentState() == GameState.COMBAT && !ship.hasTag("KOL_moduleHulked")){
            Vector2f.add(ship.getLocation(), new Vector2f(10000,0), ship.getLocation());
            ship.setHulk(true);
            ship.setDrone(true);
            ship.addTag("KOL_moduleHulked");
        }


        boolean moduleDead = (ship.getParentStation() != null && !ship.getParentStation().isAlive()) || ship.getHitpoints() <= 0.0f;

        if(!moduleDead){
            for(WeaponAPI weapon : ship.getAllWeapons()){
                if(weapon.isDecorative() || weapon.getType() == WeaponAPI.WeaponType.SYSTEM) continue;

                // get best facing angle
                float bestAngle = weapon.getArcFacing() + ship.getFacing();
                float bestInterceptTime = Float.POSITIVE_INFINITY;
                boolean isBestTargetMissile = false;
                boolean hasTarget = false;

                Iterator<Object> objectGrid = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(weapon.getLocation(), weapon.getRange()*3, weapon.getRange()*3);
                while (objectGrid.hasNext()){
                    Object next = objectGrid.next();

                    // if its a ship and the best target is a missile, skip. If its neither a ship nor missile, also skip.
                    if(next instanceof ShipAPI){
                        if(isBestTargetMissile) continue;
                    } else if(!(next instanceof MissileAPI)){
                        continue;
                    }

                    CombatEntityAPI nextEntity = (CombatEntityAPI) next;

                    // entity not dangerous
                    if(nextEntity.getOwner() == ship.getParentStation().getOwner() || nextEntity.getOwner() == 100) continue;
                    if(nextEntity instanceof MissileAPI && ((MissileAPI) nextEntity).isFizzling()) continue;

                    Pair<Float, Float> interceptData = calculateFiringAngle(ship.getLocation(), ship.getVelocity(),
                            nextEntity.getLocation(), nextEntity.getVelocity(), weapon.getProjectileSpeed(), weapon.getRange());

                    // no intercept point
                    if(interceptData == null ) continue;

                    float interceptAngle = interceptData.one;
                    float interceptTime = interceptData.two;

                    // intercept point not reachable
                    if(!Misc.isInArc(weapon.getArcFacing() + ship.getFacing(), weapon.getArc(), interceptAngle)) continue;

                    float totalInterceptTime = MathUtils.getShortestRotation(weapon.getCurrAngle(), interceptAngle)/weapon.getTurnRate() + interceptTime;

                    if(!isBestTargetMissile && nextEntity instanceof MissileAPI){
                        hasTarget = true;
                        isBestTargetMissile = true;
                        bestAngle = interceptAngle;
                        bestInterceptTime = totalInterceptTime;
                    } else{
                        if(totalInterceptTime < bestInterceptTime){
                            hasTarget = true;
                            bestAngle = interceptAngle;
                            bestInterceptTime = totalInterceptTime;
                        }
                    }
                }

                // aim towards angle and fire
                float rotationNeeded = MathUtils.getShortestRotation(weapon.getCurrAngle(), bestAngle);
                float maxRotaion = Math.min(amount * weapon.getTurnRate(), Math.abs(rotationNeeded));
                weapon.setFacing(weapon.getCurrAngle() + (rotationNeeded > 0 ? maxRotaion : -maxRotaion));
                if(Math.abs(rotationNeeded) < 1f && hasTarget) {
                    weapon.setForceFireOneFrame(true);
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
        firingDirection.normalise();

        float firingAngle = (float) Math.toDegrees(Math.atan2(firingDirection.y, firingDirection.x));

        return new Pair<>(firingAngle, timeToIntercept);
    }
}
