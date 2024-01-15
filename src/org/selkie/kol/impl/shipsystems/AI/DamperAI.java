package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class DamperAI implements ShipSystemAIScript {
    ShipAPI ship;
    boolean regenSystem = false;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if(AIUtils.canUseSystemThisFrame(ship)){
            if (ship.getSystem().getAmmo() == 3 && ship.getFluxLevel() > 0.6 + (regenSystem ? 0.2 : 0)){
                ship.useSystem();
            }
            if (ship.getSystem().getAmmo() == 2 && ship.getFluxLevel() > 0.7 + (regenSystem ? 0.1 : 0)){
                ship.useSystem();
            }
            if (ship.getSystem().getAmmo() == 1 && ship.getFluxLevel() > 0.8){
                regenSystem = true;
                ship.useSystem();
            }
        }
        if(ship.getSystem().getAmmo() == 3) regenSystem = false;
        if(ship.getSystem().getAmmo() > 0 && !regenSystem){
            ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.BACK_OFF);
            ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.BACKING_OFF);
        }
    }
}
