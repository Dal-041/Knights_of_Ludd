package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.awt.*;

public class ConformalShields extends BaseHullMod {

    public static class ConformalListener implements AdvanceableListener {
        ShipAPI ship;
        float shipX, shipY;
        ConformalListener(ShipAPI ship) {
            this.ship = ship;
            shipY = Math.abs(Misc.getTargetingRadius(MathUtils.getPointOnCircumference(ship.getLocation(), 100f, 0f), ship, false));
            shipX = Math.abs(Misc.getTargetingRadius(MathUtils.getPointOnCircumference(ship.getLocation(), 100f, 90f), ship, false));
        }

        @Override
        public void advance(float amount) {
            if (Global.getCombatEngine().isPaused()) return;
            if (ship.getShield() != null) {
                float shieldFacing = ship.getShield().getFacing();
                float shipFacing = ship.getFacing(); //Right = 0, upward = 90, etc
                if (shipFacing < 0) shipFacing += 360;
                if (shieldFacing < 0) shieldFacing += 360;

                shieldFacing -= shipFacing; // Relative rotation around the ship
                shieldFacing -= 180; //Get opposite side from facing
                if (shieldFacing < 0) shieldFacing += 360;

                float rad = (float) Math.toRadians((shieldFacing) % 360);

                if (shipX > shipY) { //Wider
                    Vector2f pos = getEllipsePosition(shipX, shipY, rad);
                    ship.getShield().setCenter(pos.getX(), pos.getY()); //x is forward-back, y is left-right
                } else { //Taller
                    Vector2f pos = getEllipsePosition(shipY, shipX, rad + (float)Math.toRadians(-90));
                    ship.getShield().setCenter(pos.getY() * -1, pos.getX());
                }

                if (Global.getSettings().isDevMode()) {
                    Vector2f shipLocX = new Vector2f(ship.getLocation());
                    Vector2f shipLocY = new Vector2f(ship.getLocation());
                    shipLocX.setX(shipLocX.getX()+shipX);
                    shipLocY.setY(shipLocY.getY()+shipY);

                    Global.getCombatEngine().addFloatingText(ship.getShieldCenterEvenIfNoShield(), "o", 24, Color.green, ship, 0, 0);
                    Global.getCombatEngine().addFloatingText(shipLocX, "x", 24, Color.white, ship, 0, 0);
                    Global.getCombatEngine().addFloatingText(shipLocY, "y", 24, Color.white, ship, 0, 0);
                }
            }
        }
    }

    protected final float maxArc = 80f;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        if (ship.getShield() == null) {
            MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "kol_conformal_shield", "shieldshunt");
            return;
        }
        if(!ship.hasListenerOfClass(ConformalListener.class)) ship.addListener(new ConformalListener(ship));
        //if (Global.getSettings().isDevMode()) return;
        //if (ship.getVariant().hasHullMod("advancedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "advancedshieldemitter", "kol_refit");
        //if (ship.getVariant().hasHullMod("frontemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "frontemitter", "kol_refit");
        if (ship.getVariant().hasHullMod("extendedshieldemitter")) MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), "extendedshieldemitter", "kol_conformal_shield");
        if (ship.getShield().getArc() >= maxArc) ship.getShield().setArc(maxArc);
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

