package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.util.ArrayList;
import java.util.List;

public class SupernovaStats extends BaseShipSystemScript {
    private static final String FIRED_INFERNO_CANNON = "FIRED_INFERNO_CANNON";
    private static final String FIRED_INFERNO_CANNON_IDS = "FIRED_INFERNO_CANNON_IDS";
    private static final List<String> INFERNO_CANNON_IDS = new ArrayList<>();

    static {
        INFERNO_CANNON_IDS.add("zea_nian_maingun_l");
        INFERNO_CANNON_IDS.add("zea_nian_maingun_r");
    }

    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (state == State.IN) {
            ship.setCustomData(FIRED_INFERNO_CANNON, false);
        }

        if (state != State.OUT) {
            for (WeaponAPI lidar : getLidars(ship)) {
                lidar.setForceFireOneFrame(true);
            }
        }

        if (state == State.ACTIVE && !((Boolean) ship.getCustomData().get(FIRED_INFERNO_CANNON))) {
            WeaponAPI infernoCannon = getInfernoCannon(ship);
            infernoCannon.setForceNoFireOneFrame(false);
            infernoCannon.setForceFireOneFrame(true);

            setFiredInfernoCannon(ship, infernoCannon);
            ship.setCustomData(FIRED_INFERNO_CANNON, true);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        ship.getCustomData().remove(id + "drone");
    }

    private static void setFiredInfernoCannon(ShipAPI ship, WeaponAPI infernoCannon) {
        List<WeaponAPI> firedCannons = new ArrayList<>();
        if (ship.getCustomData().containsKey(FIRED_INFERNO_CANNON_IDS)) {
            firedCannons = (List<WeaponAPI>) ship.getCustomData().get(FIRED_INFERNO_CANNON_IDS);
        } else {
            ship.setCustomData(FIRED_INFERNO_CANNON_IDS, firedCannons);
        }
        firedCannons.add(infernoCannon);
    }

    public static WeaponAPI getInfernoCannon(ShipAPI ship) {
        List<WeaponAPI> cannons = new ArrayList<>();
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (INFERNO_CANNON_IDS.contains(weapon.getId())) {
                cannons.add(weapon);
            }
        }

        WeaponAPI cannonToFire = cannons.get(0);
        if (ship.getCustomData().containsKey(FIRED_INFERNO_CANNON_IDS)) {
            List<WeaponAPI> firedCannons = (List<WeaponAPI>) ship.getCustomData().get(FIRED_INFERNO_CANNON_IDS);
            if (firedCannons.size() == cannons.size()) {
                firedCannons.clear();
            } else {
                cannons.removeAll(firedCannons);
                cannonToFire = cannons.get(0);
            }
        }

        return cannonToFire;
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
            if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR) && w.getRange() > lidarMaxRange) {
                lidarMaxRange = w.getRange();
            }
        }
        return lidarMaxRange;
    }
}
