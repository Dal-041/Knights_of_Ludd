package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class SmallFluxNegationAI implements ShipSystemAIScript {
    ShipAPI ship;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        int inRange = 0;
        int smallWeapons = 0;
        if ((!Global.getCombatEngine().isUIAutopilotOn() || Global.getCombatEngine().getPlayerShip() != ship)){
            for(WeaponAPI weapon : ship.getAllWeapons()){
                if(weapon.getSize() == WeaponAPI.WeaponSize.SMALL && !weapon.isDecorative() && weapon.getFluxCostToFire() > 0){
                    smallWeapons++;
                    ShipAPI targetShip = ship.getShipTarget();
                    if (targetShip != null) {
                        Vector2f targetLocation = targetShip.getLocation();
                        Vector2f endPoint = MathUtils.getPointOnCircumference(weapon.getLocation(), weapon.getRange(), weapon.getCurrAngle());
                        Vector2f closestPoint = MathUtils.getNearestPointOnLine(targetLocation, weapon.getLocation(), endPoint);

                        if (MathUtils.getDistance(closestPoint, targetLocation) < Misc.getTargetingRadius(closestPoint, targetShip, targetShip.getShield() == null ? false : targetShip.getShield().isOn())) {
                            if(ship.getSystem().isActive()) weapon.setForceFireOneFrame(true);
                            inRange++;
                        }
                    }
                }
            }
        }
        if (AIUtils.canUseSystemThisFrame(ship) && smallWeapons > 0 && (float) inRange /smallWeapons > 0.5f){
            ship.useSystem();
        }
    }
}
