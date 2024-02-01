package org.selkie.kol.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class LidarAI implements ShipSystemAIScript {
    ShipAPI ship;
    CombatEngineAPI engine;
    IntervalUtil interval = new IntervalUtil(0.5f,1);
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.engine = engine;

    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        float range = 0;
        int numWeps = 0;
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if (weapon.getSlot().getSlotSize() == WeaponAPI.WeaponSize.LARGE && weapon.getSlot().isTurret()){
                range += weapon.getRange();
                numWeps += 1;
                if(ship.getSystem().isActive() && ship.getFluxLevel() < 0.9f && (!engine.isUIAutopilotOn() || engine.getPlayerShip() != ship)){
                    boolean occluded = false;
                    for(ShipAPI ally : AIUtils.getNearbyAllies(ship, 2000)){
                        if(CollisionUtils.getCollides(weapon.getLocation(), MathUtils.getPointOnCircumference(weapon.getLocation(), 2000, weapon.getCurrAngle()), ally.getLocation(), ally.getCollisionRadius())){
                            occluded = true;
                            break;
                        }
                    }
                    if(!occluded)
                        weapon.setForceFireOneFrame(true);
                }
            }
        }
        range = range/numWeps;
        interval.advance(amount);
        if(interval.intervalElapsed() && AIUtils.canUseSystemThisFrame(ship) && !AIUtils.getNearbyEnemies(ship, range*1.4f).isEmpty()){
            boolean occluded = false;
            for(ShipAPI ally : AIUtils.getNearbyAllies(ship, range*1.4f)){
                if(CollisionUtils.getCollides(ship.getLocation(), MathUtils.getPointOnCircumference(ship.getLocation(), 2000, ship.getFacing()), ally.getLocation(), ally.getCollisionRadius())){
                    occluded = true;
                    break;
                }
            }
            if(!occluded){
                ship.useSystem();
            }
        }

    }
}
