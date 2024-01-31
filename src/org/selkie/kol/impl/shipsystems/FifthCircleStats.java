package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.ArrayList;
import java.util.List;

public class FifthCircleStats extends BaseShipSystemScript {
    private static final String INFERNO_CANNON_ID = "zea_nian_maingun";

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();

        for (WeaponAPI lidar : getLidars(ship)) {
            lidar.setForceFireOneFrame(true);
        }

        if (state == State.ACTIVE) {
            WeaponAPI infernoCannon = getInfernoCannon(ship);
            if (infernoCannon != null) {
                infernoCannon.setForceNoFireOneFrame(false);
                infernoCannon.setForceFireOneFrame(true);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.getCustomData().remove(id + "drone");
    }

    public static WeaponAPI getInfernoCannon(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getId().equals(INFERNO_CANNON_ID)) {
                return weapon;
            }
        }
        return null;
    }

    public static List<WeaponAPI> getLidars(ShipAPI ship) {
        List<WeaponAPI> lidars = new ArrayList<>();
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
                lidars.add(w);
            }
        }
        return lidars;
    }

    private static float getMaxRange(List<WeaponAPI> weapons) {
        float lidarMaxRange = 0f;
        for (WeaponAPI w : weapons) {
            if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR) && w.getRange() >  lidarMaxRange) {
                lidarMaxRange = w.getRange();
            }
        }
        return lidarMaxRange;
    }
}
