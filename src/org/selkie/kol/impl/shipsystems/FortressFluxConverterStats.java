package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class FortressFluxConverterStats extends BaseShipSystemScript {
    public static float DAMAGE_MULT = 0.9f;
    public boolean converted = false;
    public float fluxToConvert = 0f;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - DAMAGE_MULT * effectLevel);
        ShipAPI ship = (ShipAPI) stats.getEntity();
        stats.getShieldUpkeepMult().modifyMult(id, 0f);
        if(!converted){
            converted = true;
            fluxToConvert = ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux();
        }
        float totalTime = ship.getSystem().getChargeUpDur() + ship.getSystem().getChargeActiveDur() + ship.getSystem().getChargeDownDur();

        float fluxToRemove = fluxToConvert * (Global.getCombatEngine().getElapsedInLastFrame() / totalTime);

        ship.getFluxTracker().decreaseFlux(fluxToRemove);
        ship.getFluxTracker().increaseFlux(fluxToRemove * 0.2f, true);
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getShieldUpkeepMult().unmodify(id);
        converted = false;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("shield absorbs 10x damage", false);
        }
        return null;
    }
}
