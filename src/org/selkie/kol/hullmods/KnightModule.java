package org.selkie.kol.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.combat.StarficzAIUtils;

import java.util.Iterator;

public class KnightModule extends BaseHullMod {
    private final String id = "knightModule";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if(!ship.hasListenerOfClass(ModuleUnhulker.class)) ship.addListener(new ModuleUnhulker());
        if(!ship.hasListenerOfClass(ModuleSystemChild.class)) ship.addListener(new ModuleSystemChild(ship));
    }

    public static class ModuleUnhulker implements DamageListener, HullDamageAboutToBeTakenListener {
        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if(((ShipAPI) target).isHulk() && target.getHitpoints() > 0) ((ShipAPI) target).setHulk(false);
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if(ship.getHitpoints() <= damageAmount && !ship.hasTag("KOL_moduleDead")){
                ship.setHulk(false);
                ship.addTag("KOL_moduleDead");
            }
            return false;
        }
    }

    public static class ModuleSystemChild implements AdvanceableListener{
        ShipAPI ship;
        float DELAY = 0.8f;
        IntervalUtil delay = new IntervalUtil(DELAY, DELAY);
        boolean parentActivated = false;
        boolean childFired = false;

        ModuleSystemChild(ShipAPI ship){
            this.ship = ship;
        }
        @Override
        public void advance(float amount) {
            if(ship.getParentStation() == null || !ship.getParentStation().isAlive() || ship.getHitpoints() <= 0.0f) return;

            if(ship.getParentStation().getSystem().isActive()){
                parentActivated = true;
            }

            if(parentActivated && !childFired){
                delay.advance(amount);
            } else {
                delay = new IntervalUtil(DELAY, DELAY);
            }

            if(delay.intervalElapsed()){
                for(WeaponAPI weapon : ship.getAllWeapons()){
                    if(weapon.isDecorative()) continue;

                    if(weapon.getType() == WeaponAPI.WeaponType.SYSTEM){
                        weapon.setForceFireOneFrame(true);
                    }
                }
                childFired = true;
            }

            if(parentActivated && childFired && !ship.getParentStation().getSystem().isActive()){
                parentActivated = false;
                childFired = false;
            }
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(ship.getParentStation() == null || !ship.getParentStation().isAlive() || ship.getHitpoints() <= 0.0f ||
                Global.getCurrentState() != GameState.COMBAT || !Global.getCombatEngine().isEntityInPlay(ship) ) return;

        if (!ship.hasTag("KOL_moduleHulked") && ship.getLocation().getY() > -Global.getCombatEngine().getMapHeight()/2 && ship.getLocation().getY() < Global.getCombatEngine().getMapHeight()/2){

            // only teleport to inside the map border
            ship.getLocation().set(ship.getLocation().getX() > 0 ? Global.getCombatEngine().getMapWidth()/2 : -Global.getCombatEngine().getMapWidth()/2,
                    ship.getLocation().getY() > 0 ? Global.getCombatEngine().getMapHeight()/2 : -Global.getCombatEngine().getMapHeight()/2);

            ship.setHulk(true);
            ship.setDrone(true);
            ship.addTag("KOL_moduleHulked");
        } else if(!ship.isHulk()){
            ship.setHulk(true);
        }

        for(WeaponAPI weapon : ship.getAllWeapons()){
            if(weapon.isDecorative()) continue;

            if(weapon.hasAIHint(WeaponAPI.AIHints.PD)){
                aimAndFirePD(ship, weapon, amount);
            }
        }
    }

    public void aimAndFirePD(ShipAPI ship, WeaponAPI weapon, float amount){

        // get best facing angle
        float bestAngle = weapon.getArcFacing() + ship.getFacing();
        float bestInterceptTime = Float.POSITIVE_INFINITY;
        boolean isBestTargetMissile = false;
        boolean hasTarget = false;

        Iterator<Object> objectGrid = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(weapon.getLocation(), weapon.getRange()*3, weapon.getRange()*3);
        while (objectGrid.hasNext()){
            Object next = objectGrid.next();

            // if it's a ship and the best target is a missile, skip. If its neither a ship nor missile, also skip.
            if(next instanceof ShipAPI){
                if(isBestTargetMissile) continue;
            } else if(!(next instanceof MissileAPI)){
                continue;
            }

            CombatEntityAPI nextEntity = (CombatEntityAPI) next;

            // entity not dangerous
            if(nextEntity.getOwner() == ship.getParentStation().getOwner() || nextEntity.getOwner() == 100) continue;
            if(nextEntity instanceof MissileAPI && ((MissileAPI) nextEntity).isFizzling()) continue;

            Pair<Float, Float> interceptData = StarficzAIUtils.calculateFiringAngle(ship.getLocation(), ship.getVelocity(),
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
        float maxRotation = Math.min(amount * weapon.getTurnRate(), Math.abs(rotationNeeded));
        weapon.setFacing(weapon.getCurrAngle() + (rotationNeeded > 0 ? maxRotation : -maxRotation));
        weapon.setCurrHealth(weapon.getMaxHealth()); // hack until I bother to fix this
        if(Math.abs(rotationNeeded) < 1f && hasTarget) {
            weapon.setForceFireOneFrame(true);
        }
    }
}
