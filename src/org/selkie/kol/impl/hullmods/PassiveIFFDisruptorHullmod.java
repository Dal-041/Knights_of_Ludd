package org.selkie.kol.impl.hullmods;

import activators.ActivatorManager;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.selkie.kol.impl.combat.activators.IFFOverrideActivator;
import org.selkie.kol.impl.combat.activators.PassiveIFFDisruptorActivator;

public class PassiveIFFDisruptorHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ActivatorManager.addActivator(ship, new PassiveIFFDisruptorActivator(ship));
    }
}
