package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class KOL_TorOnFireEffect implements OnFireEffectPlugin {
    
    private static final Color FLASH_COLOR = new Color(255,100,70,100);
    
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
    	
            ShipAPI ship = weapon.getShip();
            Vector2f ship_velocity = ship.getVelocity();
            Vector2f proj_location = projectile.getLocation();
            engine.spawnExplosion(proj_location, ship_velocity, FLASH_COLOR.brighter(), 17f, 0.13f);
            
            engine.addHitParticle(proj_location, ship_velocity, 30f, 1f, 0.1f, FLASH_COLOR.brighter());
            
        	for (int i=0; i < 7; i++) {
    			float angle1 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-2f, 2f);
                Vector2f smokeVel = MathUtils.getPointOnCircumference(ship_velocity, i * 4f, angle1);
                
                engine.addNebulaParticle(proj_location, smokeVel,
                		MathUtils.getRandomNumberInRange(7f, 14f),
                		1.4f, //endsizemult
                		0.1f, //rampUpFraction
                		0.3f, //fullBrightnessFraction
                		MathUtils.getRandomNumberInRange(1.4f, 1.9f), //totalDuration
                		new Color(50,48,45,111),
                		true);
                
        	}
        	
    }
  }