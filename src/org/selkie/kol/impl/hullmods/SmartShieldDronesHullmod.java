package org.selkie.kol.impl.hullmods;

import activators.ActivatorManager;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.selkie.kol.impl.combat.activators.SmartShieldDronesActivator;
import org.selkie.kol.impl.hullmods.CoronalCapacitor;

import java.awt.*;

public class SmartShieldDronesHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ActivatorManager.addActivator(ship, new SmartShieldDronesActivator(ship));
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        SmartShieldDronesActivator activator = new SmartShieldDronesActivator(ship);
        ShipHullSpecAPI drone = Global.getSettings().getVariant(activator.getDroneVariant()).getHullSpec();
        String health = String.valueOf(Math.round(drone.getFluxCapacity() / drone.getShieldSpec().getFluxPerDamageAbsorbed() + drone.getArmorRating() + drone.getHitpoints()));

        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        tooltip.addPara("Deploys %s smart shield drones that can each absorb %s damage around the ship. Drones regenerate once every %s seconds.",
                opad, h, String.valueOf(activator.getMaxDeployedDrones()), health, String.valueOf(Math.round(activator.getBaseChargeRechargeDuration())));

        tooltip.setBulletedListMode(null);
    }
}