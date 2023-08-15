package org.selkie.kol.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class EngineHider extends BaseHullMod {

    public void init(HullModSpecAPI spec) {

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.isEngineBoostActive()) {
            for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                ship.getEngineController().setFlameLevel(engine.getEngineSlot(), 0f);
            }
        }
    }
}
