package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;

public class ConformalShields extends BaseHullMod {

    private boolean inited;
    float maxX;
    float maxY;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        inited = false;
        maxX = 0;
        maxY = 0;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getShield() == null) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "kol_conformal_shield", "shieldshunt");
            return;
        }
        if (Global.getSettings().isDevMode() || true) return;
        //if (ship.getVariant().hasHullMod("advancedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "advancedshieldemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("adaptiveshields")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "adaptiveshields", "kol_conformal_shield");
        //if (ship.getVariant().hasHullMod("frontemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "frontemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("extendedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "extendedshieldemitter", "kol_conformal_shield");
        if (!ship.getVariant().hasHullMod("frontemitter")
                && ship.getHullSpec().getShieldType().equals(ShieldAPI.ShieldType.OMNI)) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "kol_conformal_shield", "adaptiveshields");
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused()) return;
        if (!inited && ship.getShield() != null) {
            float radiusO = ship.getShield().getRadius();
            maxX = Math.abs(Misc.getTargetingRadius(MathUtils.getPointOnCircumference(ship.getLocation(), 100f, 0f), ship, false));
            maxY = Math.abs(Misc.getTargetingRadius(MathUtils.getPointOnCircumference(ship.getLocation(), 100f, 90f), ship, false));

            //center = ship.getLocation();
            //float radiusNew = (Math.max(maxX, maxY) + Math.min(maxX, maxY) + 30f;
            //ship.getShield().setRadius(radiusNew);
            //float ratioRad = radiusO / radiusNew;
            if (maxX != 0) inited = true;
        }
        if (ship.getShield() != null) {
            float shieldFacing = ship.getShield().getFacing();
            float shipFacing = ship.getFacing(); //Right = 0, upward = 90, etc
            if (shipFacing < 0) shipFacing += 360;
            if (shieldFacing < 0) shieldFacing += 360;

            shieldFacing -= shipFacing; // Relative rotation around the ship
            shieldFacing -= 180; //Get opposite side from facing
            if (shieldFacing < 0) shieldFacing += 360;

            float rad = (float) Math.toRadians((shieldFacing) % 360);


            if (maxX > maxY) { //Wider
                Vector2f pos = getEllipsePosition(maxX, maxY, rad);
                ship.getShield().setCenter(pos.getX(), pos.getY()); //x is forward-back, y is left-right
            } else { //Taller
                Vector2f pos = getEllipsePosition(maxY, maxX, rad + (float)Math.toRadians(-90));
                ship.getShield().setCenter(pos.getY() * -1, pos.getX());
            }

            Vector2f shipLocX = new Vector2f(ship.getLocation());
            Vector2f shipLocY = new Vector2f(ship.getLocation());
            shipLocX.setX(shipLocX.getX()+maxX);
            shipLocY.setY(shipLocY.getY()+maxY);

            if (Global.getSettings().isDevMode() || true) Global.getCombatEngine().addFloatingText(ship.getShieldCenterEvenIfNoShield(), "o", 12, Color.white, ship, 0, 0);
            if (Global.getSettings().isDevMode() || true) Global.getCombatEngine().addFloatingText(shipLocX, "x", 24, Color.white, ship, 0, 0);
            if (Global.getSettings().isDevMode() || true) Global.getCombatEngine().addFloatingText(shipLocY, "y", 24, Color.white, ship, 0, 0);
//debug runcode $print("X: " + (Global.getCombatEngine().getPlayerShip().getShield().getLocation().getX() - Global.getCombatEngine().getPlayerShip().getLocation().getX()) + "\nY: " + (Global.getCombatEngine().getPlayerShip().getShield().getLocation().getY() - Global.getCombatEngine().getPlayerShip().getLocation().getY()));
        }
    }

    public static Vector2f getEllipsePosition(float a, float b, float rad) {
        Vector2f pos = new Vector2f();

        pos.setX((float) ((a*b)/Math.sqrt(b*b + a*a * Math.tan(rad)*Math.tan(rad))));
        if (rad > Math.toRadians(90) && rad < Math.toRadians(270) || rad < Math.toRadians(-90) && rad > Math.toRadians(-270)) { //Rear-facing
            pos.setX(pos.getX()*-1);
        }
        pos.setY((float) (pos.getX() * Math.tan(rad)));

        return pos;
    }
}