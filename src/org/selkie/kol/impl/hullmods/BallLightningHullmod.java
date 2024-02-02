package org.selkie.kol.impl.hullmods;

import activators.ActivatorManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.selkie.kol.impl.shipsystems.BallLightningActivator;

public class BallLightningHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ActivatorManager.addActivator(ship, new BallLightningActivator(ship));
    }
}
