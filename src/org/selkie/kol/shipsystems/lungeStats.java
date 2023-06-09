package org.selkie.kol.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;


public class lungeStats extends BaseShipSystemScript {

    private static final Color FLICKER_COLOR = new Color(113, 129, 97, 131);
    private static final Color AFTERIMAGE_COLOR = new Color(129, 112, 98, 61);
    private Color color = new Color(100,255,100,255);
    public static final float MAX_TIME_MULT = 2f;

    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        float TimeMult = 1f + effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        ship.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);

        float driftAmount = engine.getElapsedInLastFrame();

        stats.getMaxTurnRate().modifyMult(id,5f);
        stats.getTurnAcceleration().modifyMult(id, 10f);

        if (state == State.IN) {

            ship.getMutableStats().getAcceleration().modifyFlat(id, 5000f);
            ship.getMutableStats().getDeceleration().modifyFlat(id, 5000f);

        } else if (state == State.ACTIVE) {

            interval.advance(engine.getElapsedInLastFrame());

            if (interval.intervalElapsed()) {
                SpriteAPI sprite = ship.getSpriteAPI();
                float offsetX = sprite.getWidth() / 2 - sprite.getCenterX();
                float offsetY = sprite.getHeight() / 2 - sprite.getCenterY();

                float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
                float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;

                Vector2f trueLocation = new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY);

                MagicRender.battlespace(
                        Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                        MathUtils.getRandomPointInCircle(trueLocation,MathUtils.getRandomNumberInRange(0f,20f)),
                        new Vector2f(0, 0),
                        new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                        new Vector2f(0, 0),
                        ship.getFacing() - 90f,
                        0f,
                        AFTERIMAGE_COLOR,
                        true,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        0.1f,
                        0.1f,
                        1f,
                        CombatEngineLayers.BELOW_SHIPS_LAYER);
            }

            ship.setJitter(ship,FLICKER_COLOR,0.7f,10,25f,50f);

            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);

            float speed = ship.getVelocity().length();
            if (speed <= 0.1f) {
                ship.getVelocity().set(VectorUtils.getDirectionalVector(ship.getLocation(), ship.getVelocity()));
            }
            if (speed < 900f) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed + driftAmount * 3600f);
            }
        } else {
            float speed = ship.getVelocity().length();
            if (speed > ship.getMutableStats().getMaxSpeed().getModifiedValue()) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed - driftAmount * 3600f);
            }
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().modifyMult(id,20f);
        }


    }

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        stats.getTimeMult().unmodify(id);

        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);

    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


    public float getActiveOverride(ShipAPI ship) {
        return -1;
    }

    public float getInOverride(ShipAPI ship) {
        return -1;
    }

    public float getOutOverride(ShipAPI ship) {
        return -1;
    }

    public float getRegenOverride(ShipAPI ship) {
        return -1;
    }

    public int getUsesOverride(ShipAPI ship) {
        return -1;
    }
}


