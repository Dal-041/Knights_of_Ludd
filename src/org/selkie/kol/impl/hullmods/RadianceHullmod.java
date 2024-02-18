package org.selkie.kol.impl.hullmods;

import org.magiclib.activators.ActivatorManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.selkie.kol.impl.combat.activators.RadianceActivator;

public class RadianceHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ActivatorManager.addActivator(ship, new RadianceActivator(ship));
    }
}
