package org.selkie.kol.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class BratSystemAI implements ShipSystemAIScript {

    private static float DELAY = 0.9f; // Change this to delay you want
    private ShipAPI ship;

    private final IntervalUtil delay = new IntervalUtil(DELAY,DELAY);

    private Boolean hasTriggered = false;
    private Boolean hasFired = false;
    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

        if (ship.getParentStation() == null || ship.getParentStation().getSystem() == null) return;

        if (ship.getParentStation().getSystem().isActive()) {
            hasTriggered = true;
        } else {
            if (hasFired) {
                hasTriggered = false;
                hasFired = false;
                delay.advance(amount);
            }
        }

        if (hasTriggered && !hasFired) {
            delay.advance(amount);
        }
        
        if (delay.intervalElapsed()) {
            ship.useSystem();
            hasFired = true;
        }
    }
}