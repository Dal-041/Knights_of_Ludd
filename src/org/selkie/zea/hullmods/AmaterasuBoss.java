package org.selkie.zea.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.zea.helpers.ZeaStaticStrings.ZeaMemKeys;

public class AmaterasuBoss extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        boolean isBoss = ship.getVariant().hasTag(ZeaMemKeys.ZEA_BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().contains(ZeaMemKeys.ZEA_BOSS_TAG))));

        if(isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult("kol_boss_buff", 0.8f);
            ship.getMutableStats().getFluxCapacity().modifyMult("kol_boss_buff", 1.25f);
        }
    }
}
