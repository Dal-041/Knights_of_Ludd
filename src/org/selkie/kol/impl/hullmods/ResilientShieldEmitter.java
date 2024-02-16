package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class ResilientShieldEmitter extends BaseHullMod {
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getFluxTracker().isOverloaded() && ship.getShield().getType() != ShieldAPI.ShieldType.NONE) {
            ship.getShield().setType(ShieldAPI.ShieldType.NONE);
            ship.getFluxTracker().setOverloadDuration(0.1f);
        }
    }
}

