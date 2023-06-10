package org.selkie.kol.weapons;

import org.selkie.kol.shipsystems.lidarStats;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class caelia implements EveryFrameWeaponEffectPlugin {

	private float count = 0f;
	private float wait = 0f;
	private float timeFull = 4f;
	private ShipAPI ship;
	private boolean runOnce = false;
	private float baseCD = 20f;
	private float newCD = 0f;
	private float reductionFactor = 0.4f;
	private float magnitude = 0f;
	private float rofMult = 0f;	
	
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    	    	
        if (engine.isPaused() || weapon == null) {
            return;
        }
    	
        if (!runOnce) {
        	ship = weapon.getShip();
        	baseCD = weapon.getRefireDelay();
        	//baseCD = weapon.getCooldown();
        	runOnce = true;
        }
        
    	count += amount; //count toward bonus
    	wait -= amount; //time till next shot
    	if (wait < 0) wait = 0;
        
		//Firing
        if (weapon.isFiring() || weapon.getChargeLevel() > 0) {// || wait > 0f) {
        	if (count > timeFull || (ship.getSystem().isOn() && ship.getSystem().getId() == lidarStats.SYSID)) { //I feel dirty. Whyyyyyy
        		count = timeFull;
        	}
        	magnitude = count/timeFull; // 0-1
        	rofMult = ship.getMutableStats().getBallisticRoFMult().computeMultMod();
        	newCD = (baseCD-(baseCD * reductionFactor * magnitude)) * rofMult;
        	wait = newCD;
    		//weapon.setRemainingCooldownTo(newCD);
            weapon.setRefireDelay(newCD);
        } else if (wait == 0) {
        	count = 0f;
        	weapon.setRefireDelay(baseCD);
        	//weapon.setRemainingCooldownTo(baseCD);
        }
        return;
    }
}