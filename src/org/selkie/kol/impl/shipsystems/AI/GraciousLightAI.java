package org.selkie.kol.impl.shipsystems.AI;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.shipsystems.GraciousLightStats;

public class GraciousLightAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private boolean wasHighFlux = false;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if(!ship.isAlive() || !AIUtils.canUseSystemThisFrame(ship)) return;

        boolean isFighterNear = false;
        for (ShipAPI other : AIUtils.getNearbyAllies(ship, GraciousLightStats.HEALING_LIGHT_RANGE)) {
            if (other.isFighter() && other.isAlive()) {
                isFighterNear = true;
            }
        }

        if (ship.getSystem().isActive()) {
            if (ship.getFluxLevel() > 0.75f) {
                wasHighFlux = true;
                ship.useSystem();
            } else if (!isFighterNear) {
                ship.useSystem();
            }
        } else {
            if (wasHighFlux) {
                if (ship.getFluxLevel() <= 0.5f) {
                    wasHighFlux = false;
                }
            } else if (isFighterNear) {
                ship.useSystem();
            }
        }

    }
}