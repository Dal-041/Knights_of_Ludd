package org.selkie.kol.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import org.selkie.kol.impl.fx.FakeSmokePlugin;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;


public class LungeStats extends BaseShipSystemScript {

    boolean init = false;

    private static final Color FLICKER_COLOR = new Color(113, 129, 97, 131);
    private static final Color AFTERIMAGE_COLOR = new Color(129, 112, 98, 61);
    private static final Color SMOKE_COLOR = new Color(199, 188, 111, 175);
    private Color color = new Color(100,255,100,255);
    public static final float MAX_TIME_MULT = 2f;

    private SpriteAPI sprite;
    private float offsetX;
    private float offsetY;

    private final IntervalUtil interval = new IntervalUtil(0.1f, 0.1f);
    private final IntervalUtil intervalSmoke = new IntervalUtil(0.033f, 0.033f);
    private final IntervalUtil intervalDecel = new IntervalUtil(0.5f, 0.5f);

    boolean decel = true;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        if (init == false) {
            sprite = ship.getSpriteAPI();
            offsetX = sprite.getWidth() / 2 - sprite.getCenterX();
            offsetY = sprite.getHeight() / 2 - sprite.getCenterY();

            init = true;
        }

        float TimeMult = 1f + effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        ship.getEngineController().fadeToOtherColor(this, color, new Color(0,0,0,0), effectLevel, 0.67f);

        float elapsed = engine.getElapsedInLastFrame(); //Needs to handle SpeedUp multiplication

        stats.getMaxTurnRate().modifyMult(id,5f);
        stats.getTurnAcceleration().modifyMult(id, 10f);

        if (state == State.IN) {
            if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
                stats.getAcceleration().unmodify(id);
                stats.getAcceleration().modifyFlat(id, 70f);
                stats.getMaxSpeed().unmodify(id);
                stats.getMaxSpeed().modifyFlat(id, 40f);
            } else {
                stats.getAcceleration().unmodify(id);
                stats.getAcceleration().modifyFlat(id, 5000f);
                stats.getMaxSpeed().unmodify(id);
                stats.getMaxSpeed().modifyFlat(id, 600f);
            }
            ship.getMutableStats().getDeceleration().modifyFlat(id, 1000f);
            stats.getMaxSpeed().modifyFlat(id, 600f);
            intervalSmoke.advance(elapsed);
            if (intervalSmoke.intervalElapsed()) {
                for (int i = 0; i < 6; i++) {
                    Vector2f point = new Vector2f(MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius()));
                    while (!CollisionUtils.isPointWithinBounds(point, ship)) {
                        point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius());
                    }

                    engine.addNebulaParticle(
                            point,
                            MathUtils.getRandomPointInCircle(ZERO, 50f),
                            MathUtils.getRandomNumberInRange(35f, 75f),
                            0.4f,
                            0.5f,
                            0.5f,
                            MathUtils.getRandomNumberInRange(0.3f, 1.0f),
                            SMOKE_COLOR
                    );

                    while (!CollisionUtils.isPointWithinBounds(point, ship)) {
                        point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.75f);
                    }
                }
            }
            decel = false;

        } else if (state == State.ACTIVE) {

            interval.advance(elapsed);

            if (interval.intervalElapsed()) {
                renderAfterimage(ship);
                for (ShipAPI child : ship.getChildModulesCopy()) {
                    renderAfterimage(child);
                }
            }

            //ship.setJitter(ship,FLICKER_COLOR,0.7f,10,25f,50f);

            stats.getAcceleration().unmodify(id);

            /*
            float speed = ship.getVelocity().length();
            if (speed <= 0.1f) {
                ship.getVelocity().set(VectorUtils.getDirectionalVector(ship.getLocation(), ship.getVelocity()));
            }
            if (speed < 900f) {
                ship.getVelocity().normalise();
                ship.getVelocity().scale(speed + driftAmount * 3600f);
            }
            */

        }  else if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().modifyMult(id,20f);

        } else {
            if (!decel) {
                stats.getDeceleration().unmodify(id);
                decel = true;
                float speed = ship.getVelocity().length();
                if (speed > ship.getMutableStats().getMaxSpeed().getModifiedValue()) {
                    ship.getVelocity().normalise();
                    ship.getVelocity().scale(ship.getMaxSpeedWithoutBoost());
                }
            }
        }
    }

    private void renderSmoke(ShipAPI ship) {
        float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
        float trueOffsetY = (float) FastTrig.sin(Math.toRadians(ship.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(ship.getFacing() - 90f)) * offsetY;
        Vector2f trueLocation = new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY);

        FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.38F, 0.67F), MathUtils.getRandomNumberInRange(70.0F, 90.0F),
                MathUtils.getRandomPointInCircle(trueLocation, 25f),
                MathUtils.getRandomPointInCircle(null, 10.0F),
                MathUtils.getRandomNumberInRange(-12.0F, 12.0F),
                0.4F, SMOKE_COLOR);
    }

    private void renderAfterimage(ShipAPI ship) {
        //SpriteAPI sprite = ship.getSpriteAPI();
        //float offsetX = sprite.getWidth() / 2 - sprite.getCenterX();
        //float offsetY = sprite.getHeight() / 2 - sprite.getCenterY();

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

    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
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


