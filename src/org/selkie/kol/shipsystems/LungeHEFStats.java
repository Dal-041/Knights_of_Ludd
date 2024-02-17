package org.selkie.kol.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LungeHEFStats extends BaseShipSystemScript {

    public static final float INSTANT_BOOST_FLAT = 300f;
    public static final float INSTANT_BOOST_MULT = 5f;
    private static final Color ENGINE_COLOR = new Color(255, 125, 25, 185); //using EIS_LTBOOST values
    private static final Color BOOST_COLOR = new Color(255, 175, 175, 200);
    private static final Vector2f ZERO = new Vector2f();
    private final Object ENGINEKEY2 = new Object();
    private final Map<Integer, Float> engState = new HashMap<>();
    private boolean ended = false;
    private float boostScale = 0.75f;
    private float boostVisualDir = 0f;
    private boolean boostForward = false;
    private boolean doOnce = true;
    private final CombatEngineAPI engine = Global.getCombatEngine();

    public static final float DAMAGE_BONUS_PERCENT = 50f;
    public static final float HEF_BUFF_DURATION = 5f;

    private static Color HEF_COLOR = new Color(255, 0, 191);
    private static final float MAX_GLOW_PERCENT = 0.8f;
    private static final float FADE_IN_OUT_TIME = 0.2f;
    private float HEFTimer = 0f;
    private final EnumSet<WeaponAPI.WeaponType> WEAPON_TYPES = EnumSet.of(WeaponAPI.WeaponType.ENERGY);

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine.isPaused() || stats.getEntity() == null) {
            return;
        }
        if (doOnce) {
            doOnce = false;
        }
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        float shipRadius = ship.getCollisionRadius();
        String HEFBuffId = this.getClass().getName() + "_" + ship.getId();
        float amount = engine.getElapsedInLastFrame();
        if (Global.getCombatEngine().isPaused()) {
            amount = 0f;
        }

        ship.getEngineController().extendFlame(ENGINEKEY2, 0f, 1f * effectLevel, 3f * effectLevel);

        if (!ended) {
            /* Unweighted direction calculation for visual purposes - 0 degrees is forward */
            Vector2f direction = new Vector2f();
            if (ship.getEngineController().isAccelerating()) {
                direction.y += 1f;
            } else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
                direction.y -= 1f;
            }
            if (ship.getEngineController().isStrafingLeft()) {
                direction.x -= 1f;
            } else if (ship.getEngineController().isStrafingRight()) {
                direction.x += 1f;
            }
            if (direction.length() <= 0f) {
                direction.y = 1f;
            }
            boostVisualDir = MathUtils.clampAngle(VectorUtils.getFacing(direction) - 90f);
        }

        if (state == State.IN) {
            List<ShipEngineControllerAPI.ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
            for (int i = 0; i < engList.size(); i++) {
                ShipEngineControllerAPI.ShipEngineAPI eng = engList.get(i);
                if (eng.isSystemActivated()) {
                    float targetLevel = getSystemEngineScale(eng, boostVisualDir) * 0.4f;
                    Float currLevel = engState.get(i);
                    if (currLevel == null) {
                        currLevel = 0f;
                    }
                    if (currLevel > targetLevel) {
                        currLevel = Math.max(targetLevel, currLevel - (amount / 0.1f));
                    } else {
                        currLevel = Math.min(targetLevel, currLevel + (amount / 0.1f));
                    }
                    engState.put(i, currLevel);
                    ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
                }
            }
        }

        if (state == State.ACTIVE) {
            ship.getEngineController().getExtendLengthFraction().advance(amount * 2f);
            ship.getEngineController().getExtendWidthFraction().advance(amount * 2f);
            ship.getEngineController().getExtendGlowFraction().advance(amount * 2f);
            List<ShipEngineControllerAPI.ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
            for (int i = 0; i < engList.size(); i++) {
                ShipEngineControllerAPI.ShipEngineAPI eng = engList.get(i);
                if (eng.isSystemActivated()) {
                    float targetLevel = getSystemEngineScale(eng, boostVisualDir);
                    Float currLevel = engState.get(i);
                    if (currLevel == null) {
                        currLevel = 0f;
                    }
                    if (currLevel > targetLevel) {
                        currLevel = Math.max(targetLevel, currLevel - (amount / 0.1f));
                    } else {
                        currLevel = Math.min(targetLevel, currLevel + (amount / 0.1f));
                    }
                    engState.put(i, currLevel);
                    ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
                }
            }
        }

        if (state == State.OUT) {
            // ENGINE STUFF
            List<ShipEngineControllerAPI.ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
            for (int i = 0; i < engList.size(); i++) {
                ShipEngineControllerAPI.ShipEngineAPI eng = engList.get(i);
                if (eng.isSystemActivated()) {
                    float targetLevel = getSystemEngineScale(eng, boostVisualDir) * effectLevel;
                    if (targetLevel >= (1f - 0.15f / 0.9f)) {
                        targetLevel = 1f;
                    } else {
                        targetLevel = targetLevel / (1f - 0.15f / 0.9f);
                    }
                    engState.put(i, targetLevel);
                    ship.getEngineController().setFlameLevel(eng.getEngineSlot(), targetLevel);
                }
            }

            // LUNGE STUFF
            /* Black magic to counteract the effects of maneuvering penalties/bonuses on the effectiveness of this system */
            float decelMult = Math.max(0.5f, Math.min(2f, stats.getDeceleration().getModifiedValue() / stats.getDeceleration().getBaseValue()));
            float adjFalloffPerSec = 0.45f * (float) Math.pow(decelMult, 0.5); //0.55f for frigates
            float maxDecelPenalty = 1f / decelMult;

            //stats.getMaxTurnRate().unmodify(id);
            stats.getDeceleration().modifyMult(id, (1f - effectLevel) * 1f * maxDecelPenalty);
            //stats.getTurnAcceleration().modifyPercent(id, TURN_ACCEL_BONUS * effectLevel);

            if (boostForward) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
                ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
            } else {
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            }

            if (amount > 0f) {
                ship.getVelocity().scale((float) Math.pow(adjFalloffPerSec, amount));
            }

            // LUNGE AND FX STUFF
            if (!ended) {
                Vector2f direction = new Vector2f();
                boostForward = false;
                boostScale = 0.75f; // 0.75f biggest sneed ever
                if (ship.getEngineController().isAccelerating()) {
                    direction.y += 0.55f; //0.75f - 0.2f
                    boostScale -= 0.1f;
                    boostForward = true;
                } else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
                    direction.y -= 0.4f; //0.75f - 0.35f ?
                    boostScale -= 0.35f;
                }
                if (ship.getEngineController().isStrafingLeft()) {
                    direction.x -= 1f;
                    boostScale += 0.3f; //from 0.25f an increase
                    boostForward = false;
                } else if (ship.getEngineController().isStrafingRight()) {
                    direction.x += 1f;
                    boostScale += 0.3f;
                    boostForward = false;
                }
                if (direction.length() <= 0f) {
                    direction.y = 0.55f; //0.75f - 0.2f ?
                    boostScale -= 0.2f;
                }
                Misc.normalise(direction);
                VectorUtils.rotate(direction, ship.getFacing() - 90f, direction);
                direction.scale(((ship.getMaxSpeedWithoutBoost() * INSTANT_BOOST_MULT) + INSTANT_BOOST_FLAT) * boostScale);
                Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());
                ended = true;

                float duration = (float) Math.sqrt(shipRadius) / 25f;
                ship.getEngineController().getExtendLengthFraction().advance(1f);
                ship.getEngineController().getExtendWidthFraction().advance(1f);
                ship.getEngineController().getExtendGlowFraction().advance(1f);
                for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
                    float level = 1f;
                    if (eng.isSystemActivated()) {
                        level = getSystemEngineScale(eng, boostVisualDir);
                    }
                    if ((eng.isActive() || eng.isSystemActivated()) && (level > 0f)) {
                        Color bigBoostColor = new Color(
                                Math.round(0.1f * ENGINE_COLOR.getRed()),
                                Math.round(0.1f * ENGINE_COLOR.getGreen()),
                                Math.round(0.1f * ENGINE_COLOR.getBlue()),
                                Math.round(0.3f * ENGINE_COLOR.getAlpha() * level));
                        Color boostColor = new Color(BOOST_COLOR.getRed(), BOOST_COLOR.getGreen(), BOOST_COLOR.getBlue(),
                                                     Math.round(BOOST_COLOR.getAlpha() * level));
                        Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, bigBoostColor,
                                                                2.5f * 4f * boostScale * eng.getEngineSlot().getWidth(), duration);
                        Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, boostColor,
                                                                2.5f * 2f * boostScale * eng.getEngineSlot().getWidth(), 0.15f);
                    }
                }
            }

            // HEF BUFF STUFF
            HEFTimer = HEF_BUFF_DURATION;
            stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS_PERCENT);
        }

        // HEFTimer BUFF STUFF
        if (HEFTimer > 0f) {
            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip(HEFBuffId, "graphics/icons/hullsys/high_energy_focus.png", "High Energy Focus"
                        , "+" + (int) DAMAGE_BONUS_PERCENT + "% energy weapon damage. Remaining duration: " + Misc.getRoundedValueMaxOneAfterDecimal(HEFTimer)
                        , false);
            }
            HEFTimer -= amount;
            Global.getSoundPlayer().playLoop("system_high_energy_focus_loop", ship, 1f, 0.6f, ship.getLocation(), ship.getVelocity());

            // COLOR / GLOW STUFF
            // aaaaaaaaaaaaaa Selkie why did you make me do this
            // don't swap the max and mins, they're there to clamp glow between 0 and 1 in case of float errors or something
            // i'll kill you.
            if (HEFTimer > HEF_BUFF_DURATION - FADE_IN_OUT_TIME) {
                ship.setWeaponGlow(Math.min(MAX_GLOW_PERCENT
                        , -MAX_GLOW_PERCENT / FADE_IN_OUT_TIME * HEFTimer + HEF_BUFF_DURATION * MAX_GLOW_PERCENT / FADE_IN_OUT_TIME), HEF_COLOR, WEAPON_TYPES);
            } else if (HEFTimer < FADE_IN_OUT_TIME) {
                ship.setWeaponGlow(Math.max(0f, MAX_GLOW_PERCENT / FADE_IN_OUT_TIME * HEFTimer), HEF_COLOR, WEAPON_TYPES);
            } else {
                ship.setWeaponGlow(MAX_GLOW_PERCENT, HEF_COLOR, WEAPON_TYPES);
            }
        }
        if (HEFTimer <= 0f) {
            stats.getEnergyWeaponDamageMult().unmodify(id);
            ship.setWeaponGlow(0f, HEF_COLOR, WEAPON_TYPES);
        }

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        ended = false;
        boostScale = 0.75f;
        boostVisualDir = 0f;
        boostForward = false;
        engState.clear();

        stats.getMaxTurnRate().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (ship != null) {
            if (ship.getEngineController().isFlamedOut()) {
                return "CANNOT THRUST";
            }
        }
        return null;
    }

    private static float getSystemEngineScale(ShipEngineControllerAPI.ShipEngineAPI engine, float direction) {
        float engAngle = engine.getEngineSlot().getAngle();
        if (Math.abs(MathUtils.getShortestRotation(engAngle, direction)) > 100f) {
            return 1f;
        } else {
            return 0f;
        }
    }

    // use maintainStatus instead since it's on a right-click system
    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && HEFTimer > 0f) {
            return new StatusData("+" + (int) DAMAGE_BONUS_PERCENT + "% energy weapon damage", false);
        }
        return null;
    }

}