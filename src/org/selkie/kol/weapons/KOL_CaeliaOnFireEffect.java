package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class KOL_CaeliaOnFireEffect implements OnFireEffectPlugin {
    
    private static final Color FLASH_COLOR = new Color(255,160,40,100);
    
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
    	
            ShipAPI ship = weapon.getShip();
            Vector2f ship_velocity = ship.getVelocity();
            Vector2f proj_location = projectile.getLocation();
            
            engine.spawnExplosion(proj_location, ship_velocity, FLASH_COLOR.brighter(), 20f, 0.16f);
            engine.addHitParticle(proj_location, ship_velocity, 50f, 1f, 0.1f, FLASH_COLOR);
            
        	for (int i=0; i < 11; i++) {
    			float angle1 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-3.5f, 3.5f);
                Vector2f smokeVel = MathUtils.getPointOnCircumference(ship_velocity, i * 5f, angle1);
                
                engine.addNebulaParticle(proj_location, smokeVel,
                		MathUtils.getRandomNumberInRange(9f, 18f),
                		1.4f, //endsizemult
                		0.1f, //rampUpFraction
                		0.3f, //fullBrightnessFraction
                		MathUtils.getRandomNumberInRange(1.5f, 2.0f), //totalDuration
                		new Color(33,48,52,111),
                		true);
                
                for (int j=0; j < 2; j++) {
                	
        			float angle2 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-31f, 31f);
                    Vector2f sparkVel = MathUtils.getPointOnCircumference(ship_velocity, MathUtils.getRandomNumberInRange(11f, 121f), angle2);
                    
                    engine.addSmoothParticle(MathUtils.getRandomPointInCircle(proj_location, 3f),
                    		sparkVel,
            				MathUtils.getRandomNumberInRange(3f, 6f), //size
            				1.8f - (i * 0.1f), //brightness
            				MathUtils.getRandomNumberInRange(0.35f, 0.6f), //duration
            				new Color(255,160,40,150));
                	}
        	}
        	
    }
  }