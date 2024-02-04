package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class PhasespaceSkipAI implements ShipSystemAIScript {
    ShipAPI ship;
    boolean regenSystem = false;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if(AIUtils.canUseSystemThisFrame(ship)){
            int firingWeapons = 0;
            int totalWeapons = 1;
            for(WeaponAPI weapon : ship.getAllWeapons()){
                if(!weapon.isDecorative() && !weapon.hasAIHint(WeaponAPI.AIHints.PD) && weapon.getRefireDelay() > 2f){
                    if(weapon.isFiring()) firingWeapons += weapon.getSize() == WeaponAPI.WeaponSize.LARGE ? 4 : 2;
                    totalWeapons += 1;
                }
            }
            if(ship.getFluxLevel() - ship.getHardFluxLevel() > 0.2f && firingWeapons/totalWeapons <= 2) ship.useSystem();
            if(ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.PURSUING) || ship.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF) ) ship.useSystem();
        }
    }
}
