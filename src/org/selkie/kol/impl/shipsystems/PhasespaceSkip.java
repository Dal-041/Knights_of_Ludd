package org.selkie.kol.impl.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.magiclib.util.MagicRender;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.selkie.kol.impl.fx.FakeSmokePlugin;

import java.awt.*;

public class PhasespaceSkip extends BaseShipSystemScript {
    //Main phase color
    private static final Color PHASE_COLOR = new Color(80, 110, 240, 200);

    //For nullspace phantoms
    private static final Color AFTERIMAGE_COLOR = new Color(30, 45, 220, 200);
    private static final float PHANTOM_DELAY = 0.3f;
    private static final float PHANTOM_ANGLE_DIFFERENCE = 5f;
    private static final float PHANTOM_DISTANCE_DIFFERENCE = 55f;
    private static final float PHANTOM_FLICKER_DIFFERENCE = 11f;
    private static final int PHANTOM_FLICKER_CLONES = 4;

    private static final float SHIP_ALPHA_MULT = 0f;
    private static final float SPEED_BONUS_MULT = 6f; // was 3, but that felt way to slow
    private static final float TURN_BONUS_MULT = 2f;
    private static final float MOBILITY_BONUS_MULT = 50f;
    public static final float PHASE_DISSIPATION_MULT = 2f;
    public static final float TIME_MULT = 3f;
    private int lastMessage = 0;
    private float phantomDelayCounter = 0f;
    private boolean runOnce = true;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Time counter
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        if (Global.getCombatEngine().isPaused()) {
            amount = 0;
        }

        //Unapplies all our applied stats if we are not using the system currently
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }

        if (state == State.OUT && runOnce) {
            runOnce = false;
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            if (ship.getAngularVelocity() > stats.getMaxTurnRate().getModifiedValue()) {
                ship.setAngularVelocity((ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity())) * stats.getMaxTurnRate().getModifiedValue());
            }
            if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue()) {
                ship.getVelocity().set((ship.getVelocity().x / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity().y / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue());
            }
            return;
        }
        //Checks if we should be phased or not, and applies the related mobility bonuses
        if (state == State.IN || state == State.ACTIVE || state == State.OUT) {
            ship.setPhased(true);
            float speedBonus = 1f + ((SPEED_BONUS_MULT - 1f) * effectLevel);
            float mobilityBonus = 1f + ((MOBILITY_BONUS_MULT - 1f) * effectLevel);
            stats.getMaxSpeed().modifyMult(id, speedBonus);
            stats.getAcceleration().modifyMult(id, mobilityBonus);
            stats.getDeceleration().modifyMult(id, mobilityBonus);
            stats.getMaxTurnRate().modifyMult(id, TURN_BONUS_MULT);
            stats.getTurnAcceleration().modifyMult(id, mobilityBonus);

            stats.getFluxDissipation().modifyMult(id, PHASE_DISSIPATION_MULT);
            stats.getBallisticRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
            stats.getEnergyRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
            stats.getMissileRoFMult().modifyMult(id, PHASE_DISSIPATION_MULT);
            stats.getBallisticAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
            stats.getEnergyAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);
            stats.getMissileAmmoRegenMult().modifyMult(id, PHASE_DISSIPATION_MULT);

            stats.getTimeMult().modifyMult(id, 1 + (TIME_MULT * effectLevel));
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / (1 + (TIME_MULT * effectLevel)));
        }

        //Handles ship opacity
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);
        ship.setApplyExtraAlphaToEngines(true);


        //Moves the "phantom" to its appropriate location
        Vector2f phantomPos = MathUtils.getRandomPointInCircle(null, PHANTOM_DISTANCE_DIFFERENCE);
        phantomPos.x += ship.getLocation().x;
        phantomPos.y += ship.getLocation().y;

        //If we are outside the screenspace, don't do the extra visual effects
        if (!Global.getCombatEngine().getViewport().isNearViewport(phantomPos, ship.getCollisionRadius() * 1.5f)) {
            return;
        }

        //If enough time has passed, render a new phantom
        phantomDelayCounter += amount;
        if (phantomDelayCounter > PHANTOM_DELAY) {
            float angleDifference = MathUtils.getRandomNumberInRange(-PHANTOM_ANGLE_DIFFERENCE, PHANTOM_ANGLE_DIFFERENCE) - 90f;

            for (int i = 0; i < PHANTOM_FLICKER_CLONES; i++) {
                Vector2f modifiedPhantomPos = new Vector2f(MathUtils.getRandomNumberInRange(-PHANTOM_FLICKER_DIFFERENCE, PHANTOM_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-PHANTOM_FLICKER_DIFFERENCE, PHANTOM_FLICKER_DIFFERENCE));
                modifiedPhantomPos.x += phantomPos.x;
                modifiedPhantomPos.y += phantomPos.y;
                MagicRender.battlespace(Global.getSettings().getSprite("kol_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom2"), modifiedPhantomPos, new Vector2f(0f, 0f),
                        new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                        new Vector2f(0f, 0f), ship.getFacing() + angleDifference,
                        0f, AFTERIMAGE_COLOR, true, 0.1f, 0f, 0.3f);
            }

            //Special, "Semi-Fixed" phantom
            Color colorToUse = new Color(((float) PHASE_COLOR.getRed() / 255f), ((float) PHASE_COLOR.getGreen() / 255f), ((float) PHASE_COLOR.getBlue() / 255f), ((float) PHASE_COLOR.getAlpha() / 255f) * effectLevel);
            MagicRender.objectspace(Global.getSettings().getSprite("kol_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom"),
                    ship,  new Vector2f(0f, 0f),  new Vector2f(0f, 0f), new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),  new Vector2f(0f, 0f), 180, 0, true, colorToUse, true, 0.1f, 0.1f, 0.2f, true);
            MagicRender.objectspace(Global.getSettings().getSprite("kol_fx", "" + ship.getHullSpec().getBaseHullId() + "_phantom2"),
                    ship,  new Vector2f(0f, 0f),  new Vector2f(0f, 0f), new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),  new Vector2f(0f, 0f), 180, 0, true, colorToUse, true, 0.1f, 0.1f, 0.2f, true);




            phantomDelayCounter -= PHANTOM_DELAY;
        }

        //Always render smoke at the phantom's position...
        for (int i = 0; i < (900 * amount); i++) {
            Vector2f pointToSpawnAt = MathUtils.getRandomPointInCircle(phantomPos, ship.getCollisionRadius());
            int emergencyCounter = 0;
            while (!CollisionUtils.isPointWithinBounds(pointToSpawnAt, ship) && emergencyCounter < 1000) {
                pointToSpawnAt = MathUtils.getRandomPointInCircle(phantomPos, ship.getCollisionRadius());
                emergencyCounter++;
            }
            pointToSpawnAt = MathUtils.getRandomPointInCircle(pointToSpawnAt, 50f);
            FakeSmokePlugin.addFakeSmoke(MathUtils.getRandomNumberInRange(0.32f, 0.68f), MathUtils.getRandomNumberInRange(55f, 75f),
                    pointToSpawnAt, MathUtils.getRandomPointInCircle(null, 10f),
                    MathUtils.getRandomNumberInRange(-15f, 15f), 0.85f, new Color(0f, 0f, 0f));
        }
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }
        ship.setPhased(false);
        //ship.setExtraAlphaMult(1f);

        if(ship.getShield() != null && !runOnce){
            ship.getShield().toggleOn();
            ship.getShield().setActiveArc(ship.getShield().getArc());
        }

        runOnce = true;

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        stats.getFluxDissipation().unmodifyMult(id);
        stats.getBallisticRoFMult().unmodifyMult(id);
        stats.getEnergyRoFMult().unmodifyMult(id);
        stats.getMissileRoFMult().unmodifyMult(id);
        stats.getBallisticAmmoRegenMult().unmodifyMult(id);
        stats.getEnergyAmmoRegenMult().unmodifyMult(id);
        stats.getMissileAmmoRegenMult().unmodifyMult(id);

        stats.getTimeMult().unmodify(id);
        Global.getCombatEngine().getTimeMult().unmodify(id);

        if (Math.abs(ship.getAngularVelocity()) > stats.getMaxTurnRate().getModifiedValue()) {
            ship.setAngularVelocity((ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity())) * stats.getMaxTurnRate().getModifiedValue());
        }
        if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue()) {
            ship.getVelocity().set((ship.getVelocity().x / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity().y / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue());
        }

        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (state == State.ACTIVE) {
            if (index == 0 && (Math.random() < (1f / 77f) || lastMessage == 1 || lastMessage == 4)) {
                if (lastMessage == 0) {
                    lastMessage = 1;
                } else if (lastMessage == 1) {
                    lastMessage = 4;
                } else {
                    lastMessage = 0;
                }
                return new StatusData("it screams", false);
            } else if (index == 0 && (Math.random() < (1f / 76f) || lastMessage == 2 || lastMessage == 5)) {
                if (lastMessage == 0) {
                    lastMessage = 2;
                } else if (lastMessage == 2) {
                    lastMessage = 5;
                } else {
                    lastMessage = 0;
                }
                return new StatusData("it hates", false);
            } else if (index == 0 && (Math.random() < (1f / 75f) || lastMessage == 3 || lastMessage == 6)) {
                if (lastMessage == 0) {
                    lastMessage = 3;
                } else if (lastMessage == 3) {
                    lastMessage = 6;
                } else {
                    lastMessage = 0;
                }
                return new StatusData("it rejects", false);
            } else if (index == 0) {
                lastMessage = 0;
                return new StatusData("breaching phasespace", false);
            }
        }
        return null;
    }
}