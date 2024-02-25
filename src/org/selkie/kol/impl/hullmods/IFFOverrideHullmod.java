package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.selkie.kol.impl.combat.activators.IFFOverrideActivator;

public class IFFOverrideHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MagicSubsystemsManager.addSubsystemToShip(ship, new IFFOverrideActivator(ship));
    }
}
