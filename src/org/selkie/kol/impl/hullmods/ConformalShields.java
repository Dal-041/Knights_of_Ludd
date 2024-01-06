package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public class ConformalShields extends BaseHullMod {

    public float maxX = 0;
    public float maxY = 0;
    public final float pad = 12f;
    float radiusO;
    private boolean inited = false;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!inited && ship.getShield() != null) {

            ship.getExactBounds().update(ship.getLocation(), ship.getFacing());
            for (BoundsAPI.SegmentAPI seg : ship.getExactBounds().getOrigSegments()) {
                if (Math.abs(seg.getP1().getX()) > maxX) maxX = Math.abs(seg.getP1().getX()); //height
                if (Math.abs(seg.getP1().getY()) > maxY) maxY = Math.abs(seg.getP1().getY()); //width
            }
            radiusO = ship.getShield().getRadius();
            //backup fetching method
            //maxX = Math.abs(Misc.getTargetingRadius(MathUtils.getPointOnCircumference(ship.getLocation(), 100f, 0f), ship, false));
            //maxY = Math.abs(Misc.getTargetingRadius(MathUtils.getPointOnCircumference(ship.getLocation(), 100f, 90f), ship, false));

            //center = ship.getLocation();
            //float radiusNew = (Math.max(maxX, maxY) + Math.min(maxX, maxY))/2 + 30f;
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
            if (shieldFacing < 0) shieldFacing += 360; //Can't believe this helped.

            float rad = (float) Math.toRadians((shieldFacing) % 360);
            Vector2f pos = getEllipsePosition(maxX + pad, maxY + pad/2, rad);

            if (maxY > maxX) { //Wider, in theory. Haunted.
                ship.getShield().setCenter(pos.getX(), pos.getY()); //x is forward-back, y is left-right
            } else { //Supposed to be taller ships, instead also haunted
                ship.getShield().setCenter(pos.getY(), pos.getX());
            }
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