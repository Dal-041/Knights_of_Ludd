package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;

public class DamperbluStats extends BaseShipSystemScript {

    //private static final float BASE_SPEED_BONUS = 80f;
    //private static final float SPEED_PER_MODULE = 10f;
    //private static final float ACCELERATION_BONUS = 100f;
    private static final float DAMAGE_MULT = 0.5f;

    private static final Color JITTER_COLOR = new Color(15, 150, 230, 55);
    private static final Color JITTER_UNDER_COLOR = new Color(15, 150, 230, 110);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        @SuppressWarnings("unused")
		int dead = 0;
        for (ShipAPI child : ship.getChildModulesCopy()) {
            if ((child.getHitpoints() > 0f)) {
                child.setJitter(id, JITTER_COLOR, effectLevel, 2, 5);
                child.setJitterUnder(id, JITTER_UNDER_COLOR, effectLevel, 25, 7);
                child.getMutableStats().getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
                child.getMutableStats().getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);
                child.getMutableStats().getEmpDamageTakenMult().modifyMult(id, DAMAGE_MULT);
            } else {
                child.setJitter(id, JITTER_COLOR, 0, 2, 5);
                child.setJitterUnder(id, JITTER_UNDER_COLOR, 0, 25, 7);
                dead++;
            }
        }

        //stats.getMaxSpeed().modifyFlat(id, BASE_SPEED_BONUS + SPEED_PER_MODULE * dead);
        //stats.getAcceleration().modifyFlat(id, ACCELERATION_BONUS + SPEED_PER_MODULE * dead);

    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        for (ShipAPI child : ship.getChildModulesCopy()) {
            child.getMutableStats().getArmorDamageTakenMult().unmodify(id);
            child.getMutableStats().getHullDamageTakenMult().unmodify(id);
            child.getMutableStats().getEmpDamageTakenMult().unmodify(id);
            child.setJitter(id, JITTER_COLOR, 0, 2, 5);
            child.setJitterUnder(id, JITTER_UNDER_COLOR, 0, 25, 7);
        }

        //stats.getAcceleration().unmodify(id);
        //stats.getMaxSpeed().unmodify(id);
        ;
    }
}
