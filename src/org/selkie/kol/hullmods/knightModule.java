package org.selkie.kol.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

    // Reduces High Explosive damage taken by modules by [greentext] 50% [/g].
    // Hullmods are applied to modular armor panels.

public class knightModule extends BaseHullMod {

    public static float expReductionMult = 0.5f;
    public static float armorEffectMult = 0.1f;

    public void init(HullModSpecAPI spec) {

    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHighExplosiveDamageTakenMult().modifyMult(id, expReductionMult);
        stats.getEffectiveArmorBonus().modifyMult(id, armorEffectMult);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //ship.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult(id, expReductionMult);
        if (ship.getParentStation() != null) {
            for (String ID:ship.getVariant().getNonBuiltInHullmods()) {
                ship.getVariant().removeMod(ID);
            }
            for (String hullmodID : ship.getParentStation().getVariant().getNonBuiltInHullmods()) {
                ship.getVariant().addMod(hullmodID);
            }
            for (String hullmodID : ship.getParentStation().getVariant().getSMods()) {
                ship.getVariant().addMod(hullmodID);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

    }

    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        super.advanceInCampaign(member, amount);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
    }

}
