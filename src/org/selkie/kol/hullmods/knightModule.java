package org.selkie.kol.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;

import java.util.Collection;

    // Reduces High Explosive damage taken by modules by [greentext] 50% [/g].
    // Hullmods are applied to modular armor panels.

public class knightModule extends BaseHullMod {

    public static float expReductionMult = 0.5f;
    public static float armorEffectMult = 0.1f;
    private final String id = "knightModule";

    public void init(HullModSpecAPI spec) {

    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHighExplosiveDamageTakenMult().modifyMult(id, expReductionMult);
        stats.getEffectiveArmorBonus().modifyMult(id, armorEffectMult);
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
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {

        //Manual method
        if(!ship.isAlive())return;

        if(Global.getCombatEngine().getTotalElapsedTime(false)<=2.05 && Global.getCombatEngine().getTotalElapsedTime(false)>2) {
            if (ship.getParentStation() != null && ship.getParentStation().isAlive()) {

                Collection<String> mods = ship.getParentStation().getVariant().getHullMods();

                if (mods.contains("heavyarmor")) {
//                    ship.getMutableStats().getArmorBonus().modifyFlat(id, 150);
                    float fakeArmor = ship.getHullSpec().getArmorRating() / (ship.getHullSpec().getArmorRating() + 100);
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, fakeArmor);
                }
                if (mods.contains("reinforcedhull")) {
//                    ship.getMutableStats().getHullBonus().modifyPercent(id, 40);
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 0.72f);
                }
            }
        }
    }
}
