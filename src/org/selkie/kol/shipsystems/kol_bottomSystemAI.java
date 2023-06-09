package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class kol_bottomSystemAI implements ShipSystemAIScript {

    //private static float DELAY = 2f; // Change this to delay you want
    private ShipAPI ship;
    private ShipSystemAPI parentSys;

    //private final IntervalUtil delay = new IntervalUtil(DELAY,DELAY);

    private Boolean hasTriggered = false;
    private Boolean hasFired = false;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        if (ship.isStationModule() && ship.getParentStation() != null && ship.getParentStation().getSystem() != null)
            this.parentSys = ship.getParentStation().getSystem();
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (parentSys == null) return;

        if (parentSys.isActive()) {
            hasTriggered = true;
        } else {
            if (hasFired) {
                hasTriggered = false;
                hasFired = false;
            }
        }

        if (hasTriggered && !hasFired) {
            //delay.advance(amount);
        }
        
        //if (delay.intervalElapsed()) {
            ship.useSystem();
            hasFired = true;
        //}
    }
}