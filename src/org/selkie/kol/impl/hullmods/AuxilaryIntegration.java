package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class AuxilaryIntegration extends BaseHullMod {

    private float OP_COST_REDUCTION = 5f;

    @Override
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats, final String id) {
        stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyFlat(id, -OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyFlat(id, -OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyFlat(id, -OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyFlat(id, -OP_COST_REDUCTION);

        stats.getDynamic().getMod(Stats.MEDIUM_PD_MOD).modifyFlat(id, -OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.LARGE_PD_MOD).modifyFlat(id, -OP_COST_REDUCTION);


        stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD).modifyFlat(id, -OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD).modifyFlat(id, -OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD).modifyFlat(id, -OP_COST_REDUCTION);
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public String getDescriptionParam(final int index, final ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0:
                return "5";
            default:
                break;
        }
        return null;
    }
}