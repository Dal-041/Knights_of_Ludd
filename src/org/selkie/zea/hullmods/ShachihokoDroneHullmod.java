package org.selkie.zea.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.selkie.zea.combat.subsystems.ShachihokoDroneSubsystem;

import java.awt.*;

public class ShachihokoDroneHullmod extends BaseHullMod {
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MagicSubsystemsManager.addSubsystemToShip(ship, new ShachihokoDroneSubsystem(ship));
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        if (Global.CODEX_TOOLTIP_MODE) {
            tooltip.addPara("Deploys bubble shield drones that can each absorb damage around the ship. Activate subsystem after targeting ally ship to redirect drone to ally.", 0f);
            return;
        }

        ShachihokoDroneSubsystem activator = new ShachihokoDroneSubsystem(ship);
        ShipHullSpecAPI drone = Global.getSettings().getVariant(activator.getDroneVariant()).getHullSpec();
        String health = String.valueOf(Math.round(drone.getFluxCapacity() / drone.getShieldSpec().getFluxPerDamageAbsorbed() + drone.getArmorRating() + drone.getHitpoints()));

        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        tooltip.addPara("Deploys %s bubble shield drones that can each absorb %s damage around the ship.\nActivate subsystem after targeting ally ship to redirect drone to ally.\nDrones regenerate once every %s seconds.",
                opad, h, String.valueOf(activator.getMaxDeployedDrones()), health, String.valueOf(Math.round(activator.getBaseChargeRechargeDuration())));

        tooltip.setBulletedListMode(null);
    }
}
