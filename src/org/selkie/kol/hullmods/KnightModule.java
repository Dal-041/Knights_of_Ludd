package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.Collection;

public class KnightModule extends BaseHullMod {
    private final String id = "knightModule";
    public void init(HullModSpecAPI spec) {
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        /*
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
         */
        // avoid the enemy AI being scared of armor modules as if they are ships
        ship.setDrone(true);
       // ship.setHulk(true);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        //Manual method
        /* removed in favor of multiplier method on KnightRefit
        if(!ship.isAlive())return;
        if(Global.getCombatEngine().getTotalElapsedTime(false)<=2.05 && Global.getCombatEngine().getTotalElapsedTime(false)>2) {
            if (ship.getParentStation() != null && ship.getParentStation().isAlive()) {

                Collection<String> mods = ship.getParentStation().getVariant().getHullMods();

                if (mods.contains("heavyarmor")) {
                    float fakeArmor = ship.getHullSpec().getArmorRating() / (ship.getHullSpec().getArmorRating() + 100);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, fakeArmor);
                }
                if (mods.contains("reinforcedhull")) {
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.72f);
                }
            }
        }
        */
    }
}
