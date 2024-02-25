package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.selkie.kol.impl.combat.activators.PDDroneActivator;

import java.awt.*;

public class PDDronesHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MagicSubsystemsManager.addSubsystemToShip(ship, new PDDroneActivator(ship));
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
        PDDroneActivator activator = new PDDroneActivator(ship);

        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        tooltip.addPara("Deploys %s PD drones behind the ship. Drones regenerate once every %s seconds.",
                opad, h, String.valueOf(activator.getMaxDeployedDrones()), String.valueOf(Math.round(activator.getBaseChargeRechargeDuration())));

        tooltip.setBulletedListMode(null);
    }
}
