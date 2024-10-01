package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.selkie.kol.combat.StarficzAIUtils;
import org.selkie.kol.impl.helpers.ZeaStaticStrings;

public class AmaterasuBoss extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        boolean isBoss = ship.getVariant().hasTag(ZeaStaticStrings.MemKeys.BOSS_TAG) || (ship.getFleetMember() != null && (ship.getFleetMember().getFleetData() != null &&
                (ship.getFleetMember().getFleetData().getFleet() != null && ship.getFleetMember().getFleetData().getFleet().getMemoryWithoutUpdate().contains(ZeaStaticStrings.MemKeys.BOSS_TAG))));

        if(isBoss || StarficzAIUtils.DEBUG_ENABLED) {
            ship.getMutableStats().getShieldDamageTakenMult().modifyMult("kol_boss_buff", 0.8f);
            ship.getMutableStats().getFluxCapacity().modifyMult("kol_boss_buff", 1.25f);
        }
    }
}
