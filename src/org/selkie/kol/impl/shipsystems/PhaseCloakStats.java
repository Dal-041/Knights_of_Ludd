package org.selkie.kol.impl.shipsystems;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.util.Iterator;

public class PhaseCloakStats extends BaseShipSystemScript {

    public static float SHIP_ALPHA_MULT = 0.25f;
    public static float VULNERABLE_FRACTION = 0f;
    public static float MAX_TIME_MULT = 3f;
    protected Object STATUSKEY = new Object();


    protected void maintainStatus(ShipAPI playerShip, float effectLevel) {
        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;

        if (effectLevel > VULNERABLE_FRACTION) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY, cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
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

        float levelForAlpha = effectLevel;

        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = effectLevel;
        } else if (state == State.OUT) {
            // make sure to force the system back on if overlapping
            Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), ship.getCollisionRadius()*2 + 500f, ship.getCollisionRadius()*2 + 500f);
            while (iterator.hasNext() ) {
                Object next = iterator.next();
                if (!(next instanceof CombatEntityAPI)) continue;
                CombatEntityAPI entity = (CombatEntityAPI) next;
                if(ship == entity) continue;
                if(entity.getCollisionClass() != CollisionClass.SHIP) continue;
                float distance = Misc.getTargetingRadius(ship.getLocation(), entity, false) + Misc.getTargetingRadius(entity.getLocation(), ship, false);
                if (MathUtils.getDistanceSquared(ship.getLocation(), entity.getLocation()) > distance*distance) continue;

                ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
            }

            if (effectLevel > 0.5f) {
                ship.setPhased(true);
            } else {
                ship.setPhased(false);
            }
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
