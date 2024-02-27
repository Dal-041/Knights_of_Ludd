package org.selkie.kol.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class ModuleVulcan extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getVariant().assignUnassignedWeapons();
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyFlat(id, 300f);
    }
}