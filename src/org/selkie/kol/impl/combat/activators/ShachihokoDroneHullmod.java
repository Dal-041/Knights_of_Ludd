package org.selkie.kol.impl.combat.activators;

import activators.ActivatorManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class ShachihokoDroneHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ActivatorManager.addActivator(ship, new ShachihokoDroneActivator(ship));
    }
}
