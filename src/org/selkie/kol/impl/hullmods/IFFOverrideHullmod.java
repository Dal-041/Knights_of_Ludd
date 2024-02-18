package org.selkie.kol.impl.hullmods;

import org.magiclib.activators.ActivatorManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.selkie.kol.impl.combat.activators.IFFOverrideActivator;

public class IFFOverrideHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ActivatorManager.addActivator(ship, new IFFOverrideActivator(ship));
    }
}
