package org.selkie.kol.impl.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class kol_phasecloak extends BaseShipSystemScript {

    private static final Color JITTER_COLOR = new Color(15,150,255,255);
    private static final float JITTER_FADE_TIME = 0.5f;
    
    private static final Map<HullSize, Float> mag = new HashMap();
    static {
        mag.put(ShipAPI.HullSize.FIGHTER, 60f);
        mag.put(ShipAPI.HullSize.FRIGATE, 60f);
        mag.put(ShipAPI.HullSize.DESTROYER, 50f);
        mag.put(ShipAPI.HullSize.CRUISER, 40f);
        mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 30f);
    }
    
    private static final float SHIP_ALPHA_MULT = 0.25f;
    private static final float VULNERABLE_FRACTION = 0f;
	
    private static final float MAX_TIME_MULT = 3f;
	
    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();
    protected Object STATUSKEY3 = new Object();
    protected Object STATUSKEY4 = new Object();
    
    private void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
	float level = effectLevel;
	float f = VULNERABLE_FRACTION;
	
	ShipSystemAPI cloak = playerShip.getPhaseCloak();
	if (cloak == null) cloak = playerShip.getSystem();
	if (cloak == null) return;
	
	if (level > f) {
		Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
				cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
	} else { }
    }
    
    @Override
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
        
        if (player) {
            maintainStatus(ship, state, effectLevel);
        }
        
        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }
        
        float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
        stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
        
        float level = effectLevel;
	//float f = VULNERABLE_FRACTION;
	
	float jitterLevel = 0f;
	float jitterRangeBonus = 0f;
	float levelForAlpha = level;
	
	ShipSystemAPI cloak = ship.getPhaseCloak();
	if (cloak == null) cloak = ship.getSystem();
        
        if (state == State.IN || state == State.ACTIVE) {
            ship.setPhased(true);
            levelForAlpha = level;
            
            stats.getAcceleration().modifyFlat(id, mag.get(ship.getHullSize()) * effectLevel);
            stats.getDeceleration().modifyFlat(id, mag.get(ship.getHullSize()) * effectLevel);

            stats.getTurnAcceleration().modifyPercent(id, 180f * effectLevel);
            stats.getMaxTurnRate().modifyPercent(id, 140f);

            stats.getMaxSpeed().modifyFlat(id, mag.get(ship.getHullSize()) * effectLevel);
        } else if (state == State.OUT) {
            if (level > 0.5f) { 
                ship.setPhased(true);
            } else {
                ship.setPhased(false);
            }
            levelForAlpha = level;
            
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
            stats.getMaxTurnRate().unmodify(id);
        }
        
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
	ship.setApplyExtraAlphaToEngines(true);
	
	
	float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * levelForAlpha;
	stats.getTimeMult().modifyMult(id, shipTimeMult);
	if (player) {
		Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
	} else {
		Global.getCombatEngine().getTimeMult().unmodify(id);
	}
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship;
	if (stats.getEntity() instanceof ShipAPI) {
		ship = (ShipAPI) stats.getEntity();
	} else {
		return;
	}
        
        Global.getCombatEngine().getTimeMult().unmodify(id);
	stats.getTimeMult().unmodify(id);
	
	ship.setPhased(false);
	ship.setExtraAlphaMult(1f);
        
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxSpeed().unmodifyPercent(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
}