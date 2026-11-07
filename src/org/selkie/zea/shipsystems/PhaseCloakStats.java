package org.selkie.zea.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;

public class PhaseCloakStats extends BaseShipSystemScript {

    public static final float SHIP_ALPHA_MULT = 0.25f;
    public static final float VULNERABLE_FRACTION = 0f;
    public static final float MAX_TIME_MULT = 3f;
    // vanilla phase coil stress: top speed falls to MIN_SPEED_MULT as hard flux reaches the threshold
    public static final float MIN_SPEED_MULT = 0.33f;
    public static final float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.5f;
    protected final Object STATUSKEY = new Object();
    protected final Object STATUSKEY2 = new Object();

    protected float getSpeedMult(ShipAPI ship, float effectLevel) {
        float threshold = ship.getMutableStats().getDynamic().getMod(Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD)
                .computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
        float stress = threshold <= 0f ? 1f : Math.min(1f, ship.getHardFluxLevel() / threshold);
        return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - stress * effectLevel);
    }


    protected void maintainStatus(ShipAPI playerShip, float effectLevel) {
        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;

        if (effectLevel > VULNERABLE_FRACTION) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY, cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
            float speedMult = getSpeedMult(playerShip, effectLevel);
            if (speedMult >= 1f) {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2, cloak.getSpecAPI().getIconSpriteName(), "phase coils stable", "top speed at 100%", false);
            } else {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2, cloak.getSpecAPI().getIconSpriteName(), "phase coil stress", "top speed at " + Math.round(speedMult * 100f) + "%", true);
            }
        }
    }


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship;
        boolean player;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        if (player) maintainStatus(ship, effectLevel);

        if (Global.getCombatEngine().isPaused()) return;

        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak == null) return;

        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }

        float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
        float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
        stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
        stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
        stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

        float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
        float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
        stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
        stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
        stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

        float coilSpeedMult = getSpeedMult(ship, effectLevel);
        if (coilSpeedMult < 1f) {
            stats.getMaxSpeed().modifyMult(id + "_2", coilSpeedMult);
        } else {
            stats.getMaxSpeed().unmodifyMult(id + "_2");
        }

        float levelForAlpha = effectLevel;

        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = effectLevel;
        } else if (state == State.OUT) {
            // dynamically repulse when in phase out
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

                    float shipMoveRatio = Misc.interpolate(0, 1f, entity.getMass() / (entity.getMass() + ship.getMass()));
                    float shipMoveStrength = 0.02f * Global.getCombatEngine().getElapsedInLastFrame();
                    Vector2f.add(ship.getVelocity(), (Vector2f) VectorUtils.getDirectionalVector(entity.getLocation(), ship.getLocation()).scale((distance*distance - actualDistance) * shipMoveRatio * shipMoveStrength), ship.getVelocity());
                    Vector2f.add(entity.getVelocity(), (Vector2f) VectorUtils.getDirectionalVector(ship.getLocation(), entity.getLocation()).scale((distance*distance - actualDistance) * (1f-shipMoveRatio) * shipMoveStrength), entity.getVelocity());
                }
            }

            ship.setPhased(effectLevel > 0.5f);
            levelForAlpha = effectLevel;
        }

        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
        ship.setApplyExtraAlphaToEngines(true);

        float extra = 0f;
        float maxTimeMult = 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
        float shipTimeMult = 1f + (maxTimeMult - 1f) * levelForAlpha * (1f - extra);
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }


    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxSpeed().unmodify(id + "_2");
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);

        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);

        ShipSystemAPI cloak = ship.getPhaseCloak();
        if (cloak == null) cloak = ship.getSystem();
        if (cloak != null) {
            ((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(0f);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}
