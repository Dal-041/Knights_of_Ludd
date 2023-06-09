package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class kol_vulcan extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getVariant().assignUnassignedWeapons();
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyFlat(id, 300f);
    }
}