package org.selkie.kol.impl.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import lunalib.backend.ui.components.base.Filters;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.awt.geom.Ellipse2D;

public class ConformalShields extends BaseHullMod {

    public float eWidth = 0;
    public float eHeight = 0;
    public float maxX = 0;
    public float maxY = 0;
    private boolean inited = false;
    private Vector2f origin = null;
    private float radius = 0;
    private float offset = 20f;
    //private Ellipse2D e = null;
    private Vector2f center;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!inited && ship.getShield() != null) {
            origin = ship.getShield().getLocation();
            radius = ship.getShield().getRadius();
            //debug stuff
            /*
            runcode $print("Ship: " + Global.getCombatEngine().getPlayerShip().getFacing()); $print("Shield: " + Global.getCombatEngine().getPlayerShip().getShield().getFacing()); $print("Shield-ship: " + (Global.getCombatEngine().getPlayerShip().getShield().getFacing()-Global.getCombatEngine().getPlayerShip().getFacing()));
            */
            for (BoundsAPI.SegmentAPI seg : ship.getExactBounds().getOrigSegments()) {
                if (Math.abs(seg.getP1().getX()) > maxX) maxX = seg.getP1().getX(); //height
                if (Math.abs(seg.getP1().getY()) > maxY) maxY = seg.getP1().getY(); //width
            }
            //eWidth = maxY;
            //eHeight = maxX;
            center = ship.getLocation();
            inited = true;
        }
        if (ship.getShield() != null) {
            float shieldFacing = ship.getShield().getFacing();
            float shipFacing = ship.getFacing(); //Right = 0, upward = 90, etc
            shieldFacing -= shipFacing; // Relative rotation around the ship
            shieldFacing -= 180; //-180 to 180, will get opposite side from facing

            float shieldX = 0;
            float shieldY = 0;
            float a = maxY;
            float b = maxX;
            float rad = (float) Math.toRadians(shieldFacing);
            shieldX = (float) ((a*b)/Math.sqrt(b*b + a*a * Math.tan(rad)*Math.tan(rad)));
            if (rad > Math.PI/2 && rad < 3*Math.PI/2 || rad < -Math.PI/2 && rad > -3*Math.PI/2) { //Rear-facing
                shieldX = shieldX*-1;
            }
            shieldY = (float) (shieldX * Math.tan(rad));

            if (maxY > maxX) { //wider
                //ship.getShield().setCenter(shieldX, shieldY*-1);
                ship.getShield().setCenter(shieldX, shieldY);
            } else { //taller
                //ship.getShield().setCenter(shieldY, shieldX*-1); //invert
                ship.getShield().setCenter(shieldY, shieldX); //invert
            }
        }
    }
}