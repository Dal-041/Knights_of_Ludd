package org.selkie.kol.impl.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;
import org.selkie.kol.impl.fx.FakeSmokePlugin;

import java.awt.*;
import java.util.Iterator;

import static org.selkie.kol.helpers.MathHelpers.computeSpriteCenter;

public class PhasespaceSkip extends BaseShipSystemScript {
    //Main phase color
    private static final Color PHASE_COLOR = new Color(70, 90, 240, 230);
    //For nullspace phantoms
    private static final Color AFTERIMAGE_COLOR = new Color(30, 45, 220, 200);
    private static final float PHANTOM_DELAY = 0.2f;
    private static final float PHANTOM_ANGLE_DIFFERENCE = 10f;
    private static final float PHANTOM_DISTANCE_DIFFERENCE = 70f;
    private static final float PHANTOM_FLICKER_DIFFERENCE = 11f;
    private static final int PHANTOM_FLICKER_CLONES = 4;

    private static final float SHIP_ALPHA_MULT = 0.15f;
    private static final float SPEED_BONUS_FLAT = 150f;
    private static final float TURN_BONUS_MULT = 2f;
    private static final float MOBILITY_BONUS_MULT = 50f;
    public static final float PHASE_DISSIPATION_MULT = 2f;
    public static final float TIME_MULT = 3f;
    private int lastMessage = 0;
    private float phantomDelayCounter = 0f;
    private boolean runOnce = false;

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }


        //Time counter
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        if (Global.getCombatEngine().isPaused()) {
            amount = 0;
        }
        runOnce = true;
        //Checks if we should be phased or not, and applies the related mobility bonuses

        ship.setPhased(true);
        float speedBonus = SPEED_BONUS_FLAT * effectLevel;
        float mobilityBonus = 1f + ((MOBILITY_BONUS_MULT - 1f) * effectLevel);
        stats.getMaxSpeed().modifyFlat(id, speedBonus);
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

        stats.getTimeMult().modifyMult(id, 1 + ((TIME_MULT - 1f) * effectLevel));
        if(ship == Global.getCombatEngine().getPlayerShip())
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / (1 + ((TIME_MULT - 1f) * effectLevel)));


        //Handles ship opacity
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);
        ship.setApplyExtraAlphaToEngines(true);

        //We need to compute the actual sprite center to account for the ship center offset
        Vector2f spriteCenter = computeSpriteCenter(ship);

        //Moves the "phantom" to its appropriate location
        Vector2f phantomPos = MathUtils.getRandomPointInCircle(null, PHANTOM_DISTANCE_DIFFERENCE);
        phantomPos.x += spriteCenter.x;
        phantomPos.y += spriteCenter.y;

        //If we are outside the screenspace, don't do the extra visual effects
        if (!Global.getCombatEngine().getViewport().isNearViewport(phantomPos, ship.getCollisionRadius() * 1.5f)) {
            return;
        }

        //Special, "Semi-Fixed" phantom
        Color colorToUse = new Color(((float) PHASE_COLOR.getRed() / 255f), ((float) PHASE_COLOR.getGreen() / 255f), ((float) PHASE_COLOR.getBlue() / 255f), ((float) PHASE_COLOR.getAlpha() / 255f) * effectLevel);
        MagicRender.singleframe(Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow1"), spriteCenter,
                new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()), ship.getFacing() - 90f, colorToUse, true);
        MagicRender.singleframe(Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow2"), spriteCenter,
                new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()), ship.getFacing() - 90f, colorToUse, true);

        //If enough time has passed, render a new phantom
        phantomDelayCounter += amount;
        if (phantomDelayCounter > PHANTOM_DELAY) {
            float angleDifference = MathUtils.getRandomNumberInRange(-PHANTOM_ANGLE_DIFFERENCE, PHANTOM_ANGLE_DIFFERENCE) - 90f;

            for (int i = 0; i < PHANTOM_FLICKER_CLONES; i++) {
                Vector2f modifiedPhantomPos = new Vector2f(MathUtils.getRandomNumberInRange(-PHANTOM_FLICKER_DIFFERENCE, PHANTOM_FLICKER_DIFFERENCE), MathUtils.getRandomNumberInRange(-PHANTOM_FLICKER_DIFFERENCE, PHANTOM_FLICKER_DIFFERENCE));
                modifiedPhantomPos.x += phantomPos.x;
                modifiedPhantomPos.y += phantomPos.y;
                MagicRender.battlespace(Global.getSettings().getSprite("zea_phase_glows", "" + ship.getHullSpec().getBaseHullId() + "_glow1"), modifiedPhantomPos, new Vector2f(0f, 0f),
                        new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                        new Vector2f(0f, 0f), ship.getFacing() + angleDifference,
                        0f, AFTERIMAGE_COLOR, true, 0.1f, 0f, 0.5f);
            }



            phantomDelayCounter -= PHANTOM_DELAY;
        }

        // make sure to push any overlapping ships away when exiting system
        if(state == State.OUT){
            Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), ship.getCollisionRadius()*2 + 500f, ship.getCollisionRadius()*2 + 500f);
            while (iterator.hasNext() ) {
                Object next = iterator.next();
                if (!(next instanceof CombatEntityAPI)) continue;
                CombatEntityAPI entity = (CombatEntityAPI) next;
                if(ship == entity) continue;
                if(entity.getCollisionClass() != CollisionClass.SHIP) continue;
                float distance = Misc.getTargetingRadius(ship.getLocation(), entity, false) + Misc.getTargetingRadius(entity.getLocation(), ship, false);
                float actualDistance = MathUtils.getDistanceSquared(ship.getLocation(), entity.getLocation());
                if (actualDistance < distance*distance) {

                     float shipMoveFactor = Misc.interpolate(0, 0.005f, entity.getMass() / (entity.getMass() + ship.getMass()*0.002f));
                    Vector2f.add(ship.getVelocity(), (Vector2f) VectorUtils.getDirectionalVector(entity.getLocation(), ship.getLocation()).scale((distance*distance - actualDistance) * shipMoveFactor), ship.getVelocity());
                    Vector2f.add(entity.getVelocity(), (Vector2f) VectorUtils.getDirectionalVector(ship.getLocation(), entity.getLocation()).scale((distance*distance - actualDistance) * (0.005f-shipMoveFactor)), entity.getVelocity());
                }
            }
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
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            id = id + "_" + ship.getId();
        } else {
            return;
        }



        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        stats.getFluxDissipation().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getMissileRoFMult().unmodify(id);
        stats.getBallisticAmmoRegenMult().unmodify(id);
        stats.getEnergyAmmoRegenMult().unmodify(id);
        stats.getMissileAmmoRegenMult().unmodify(id);

        stats.getTimeMult().unmodify(id);

        if(ship == Global.getCombatEngine().getPlayerShip())
            Global.getCombatEngine().getTimeMult().unmodify(id);

        if (Math.abs(ship.getAngularVelocity()) > stats.getMaxTurnRate().getModifiedValue()) {
            ship.setAngularVelocity((ship.getAngularVelocity() / Math.abs(ship.getAngularVelocity())) * stats.getMaxTurnRate().getModifiedValue());
        }
        if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue()) {
            ship.getVelocity().set((ship.getVelocity().x / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue(), (ship.getVelocity().y / ship.getVelocity().length()) * stats.getMaxSpeed().getModifiedValue());
        }

        if(ship.getShield() != null && runOnce){
            ship.getShield().toggleOn();
            ship.getShield().setActiveArc(ship.getShield().getArc());
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