package org.selkie.kol.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class BottomSystemAI implements ShipSystemAIScript {

    private ShipAPI ship;

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
            }
        }

        if (hasTriggered && !hasFired) {
            ship.useSystem();
            hasFired = true;
        }

    }
}