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
import org.selkie.kol.combat.ShipExplosionListener;
import org.selkie.kol.combat.StarficzAIUtils;

import java.util.Iterator;

public class KnightModule extends BaseHullMod {
    public static final String KOL_MODULE_HULKED = "kol_module_hulked";
    public static final String KOL_MODULE_DEAD = "kol_module_dead";
    @Override
    public void advanceInCombat(ShipAPI module, float amount) {
        if(module.getParentStation() == null || !module.getParentStation().isAlive() || module.getHitpoints() <= 0.0f || module.hasTag(KOL_MODULE_DEAD) ||
                Global.getCurrentState() != GameState.COMBAT || !Global.getCombatEngine().isEntityInPlay(module) ) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        /*
        Enemy AI prioritizes targeting modules over the base hull. While logical for ship/station sections, this creates issues with armor modules.
        To prevent this, I set the module to a 'hulk' state, causing the AI to ignore it.
        However, this triggers a distracting visual whiteout ship explosion the first time it is done.
        My solution is to teleport the module offscreen (currently, a map corner) before marking it as a hulk.
        This must be done after the ship is loaded into the map and within its borders to prevent the game from despawning it.
        */
        if (!module.hasTag(KOL_MODULE_HULKED) && module.getLocation().getY() > -engine.getMapHeight()/2 && module.getLocation().getY() < engine.getMapHeight()/2 &&
                module.getLocation().getX() > -engine.getMapWidth()/2 && module.getLocation().getX() < engine.getMapWidth()/2){

            // only teleport to inside the map border
            float borderEdgeX = module.getLocation().getX() > 0 ? engine.getMapWidth()/2 : -engine.getMapWidth()/2;
            float borderEdgeY = module.getLocation().getY() > 0 ? engine.getMapHeight()/2 : -engine.getMapHeight()/2;
            module.getLocation().set(borderEdgeX, borderEdgeY);
            module.setHulk(true);

            /*
            I set the modules to be station drones, this makes the enemy AI not see the ship as a group of ships. (ie: a capital with 4 frigate escorts)
            Without this enemies AI would not try to fight KoL ships as they think it is a 5v1.
            This can also be avoided by setting the hullsize to be a fighter, but that has other rendering issues. (fighters always render over everything else)
            */
            module.setDrone(true);
            module.addTag(KOL_MODULE_HULKED);
        }
        // re-set the module to be a hulk if it's not, this happens after hulk is unset for vanilla damage fx's
        else if(!module.isHulk() && module.hasTag(KOL_MODULE_HULKED)){
            module.setHulk(true);
        }

        // modules as hulks do not fire weapons (as the ship is dead), thus we need to implement custom firing AI for any weapon on modules.
        // KoL only has direct fire pd, thus I only cover that here.
        for(WeaponAPI weapon : module.getAllWeapons()){
            if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.MISSILE && weapon.hasAIHint(WeaponAPI.AIHints.PD)){
                aimAndFirePD(module, weapon, amount);
            }
        }

        // sync hardflux level with parent hull for polarized armor purposes
        float moduleFlux = module.getParentStation().getFluxLevel() * module.getMaxFlux();
        module.getFluxTracker().setCurrFlux(moduleFlux);
        module.getFluxTracker().setHardFlux(moduleFlux);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI module, String id) {
        if(!module.hasListenerOfClass(ModuleUnhulker.class)) module.addListener(new ModuleUnhulker());
        if(!module.hasListenerOfClass(ModuleSystemChild.class)) module.addListener(new ModuleSystemChild(module));
        if(!module.hasListenerOfClass(ShipExplosionListener.class)) module.addListener(new ShipExplosionListener());
        if(!module.hasListenerOfClass(KnightRefit.ExplosionOcclusionRaycast.class)) module.addListener(new KnightRefit.ExplosionOcclusionRaycast());
    }

    public static class ModuleUnhulker implements DamageListener, HullDamageAboutToBeTakenListener {
        @Override // unset hulk for right before any damage gets dealt to the module, this allows for normal processing of hit explosions
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            ShipAPI module = (ShipAPI) target;
            if(module.isHulk() && module.getHitpoints() > 0 && !module.hasTag(KOL_MODULE_DEAD)) module.setHulk(false);
        }

        @Override // for some reason the above listener doesn't catch when the module is actually going to be dead.
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI module, Vector2f point, float damageAmount) {
            if(module.getHitpoints() <= damageAmount && !module.hasTag(KOL_MODULE_DEAD)){
                module.setHulk(false);
                module.addTag(KOL_MODULE_DEAD);
            }
            return false;
        }
    }

    // Slaves the child module's system weapons to activate on parent activation. using ship.useSystem() doesn't work as the module is dead.
    public static class ModuleSystemChild implements AdvanceableListener {
        public final static float MODULE_SYSTEM_DELAY = 0.8f;
        private IntervalUtil delay = new IntervalUtil(MODULE_SYSTEM_DELAY, MODULE_SYSTEM_DELAY);
        private final ShipAPI module;
        private boolean parentActivated = false;
        private boolean childFired = false;

        ModuleSystemChild(ShipAPI module){
            this.module = module;
        }
        @Override
        public void advance(float amount) {
            if(module.getParentStation() == null || !module.getParentStation().isAlive() || module.getHitpoints() <= 0.0f || module.hasTag(KOL_MODULE_DEAD)) return;

            // note down if the parent has used system, this accounts for if parent system is shorter then delay
            if(module.getParentStation().getSystem().isActive()){
                parentActivated = true;
            }

            // only fire child after delay and make sure child can't fire again
            if(parentActivated && !childFired){
                delay.advance(amount);
                if(delay.intervalElapsed()){
                    childFired = true;
                    for(WeaponAPI weapon : module.getAllWeapons()){
                        if(!weapon.isDecorative() && weapon.getType() == WeaponAPI.WeaponType.SYSTEM){
                            weapon.setForceFireOneFrame(true);
                        }
                    }
                }
            } else {
                delay = new IntervalUtil(MODULE_SYSTEM_DELAY, MODULE_SYSTEM_DELAY);
            }

            // reset when both flags have been triggered, and parent system is back offline
            if(parentActivated && childFired && !module.getParentStation().getSystem().isActive()){
                parentActivated = false;
                childFired = false;
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
