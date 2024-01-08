package org.selkie.kol.weapons;

import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.selkie.kol.shipsystems.LidarStats;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;

public class Caelia implements EveryFrameWeaponEffectPlugin {


	private ShipAPI ship;
	private boolean runOnce = false;
	private float baseRD = 20f;
	private float baseCD = 20f;
	private float rofMult = 1.4f;
	private float timeFull = 4f;
	private float rampUpTime = 0f;
	
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    	    	
        if (engine.isPaused() || weapon == null) {
            return;
        }
    	
        if (!runOnce) {
        	ship = weapon.getShip();
        	baseRD = weapon.getRefireDelay();
        	baseCD = weapon.getCooldown();
        	runOnce = true;
        }

		//Firing
        if (weapon.isFiring() || weapon.getChargeLevel() > 0) {// || wait > 0f) {
			rampUpTime += amount;
			if (rampUpTime > timeFull) rampUpTime = timeFull;

			float shipDelayReductionMult = 1/ship.getMutableStats().getBallisticRoFMult().getModifiedValue();
			float weaponDelayReductionMult = 1/Misc.interpolate(1, rofMult, rampUpTime/timeFull);
			weapon.setRefireDelay(baseRD * shipDelayReductionMult * weaponDelayReductionMult);
        } else {
			rampUpTime -= amount;
			if (rampUpTime < 0) rampUpTime = 0;
        }
    }
}