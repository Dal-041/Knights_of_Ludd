package org.selkie.zea.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.zea.shipsystems.GraciousLightStats;

public class GraciousLightAI implements ShipSystemAIScript {
    private ShipAPI ship;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (!ship.isAlive() || !AIUtils.canUseSystemThisFrame(ship)) return;

        int fighterCount = 0;
        for (ShipAPI other : AIUtils.getNearbyAllies(ship, GraciousLightStats.HEALING_LIGHT_RANGE)) {
            if (other.isFighter() && other.isAlive()) {
                fighterCount++;
            }
        }

        if (!ship.getSystem().isActive()) {
            if (ship.getFluxLevel() <= 0.8f) {
                if (fighterCount >= 3) {
                    ship.useSystem();
                }
            }
        }
    }
}