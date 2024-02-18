package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class KOL_LlyrOnFireEffect implements OnFireEffectPlugin {
    
    private static final Color FLASH_COLOR = new Color(235,255,215,100);
    
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
    	
            ShipAPI ship = weapon.getShip();
            Vector2f ship_velocity = ship.getVelocity();
            Vector2f proj_location = projectile.getLocation();
            
            engine.spawnExplosion(proj_location, ship_velocity, FLASH_COLOR.brighter(), 29f, 0.13f);
            engine.addHitParticle(proj_location, ship_velocity, 45f, 0.9f, 0.1f, FLASH_COLOR);
            
        	for (int i=0; i < 7; i++) {
    			float angle1 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-8f, 8f);
                Vector2f smokeVel = MathUtils.getPointOnCircumference(ship_velocity, i * 4f, angle1);
                
                engine.addNebulaParticle(proj_location, smokeVel,
                		MathUtils.getRandomNumberInRange(7f, 14f),
                		1.4f, //endsizemult
                		0.1f, //rampUpFraction
                		0.3f, //fullBrightnessFraction
                		MathUtils.getRandomNumberInRange(1.0f, 1.4f), //totalDuration
                		new Color(43,45,44,69),
                		true);
                
                for (int j=0; j < 2; j++) {
                	
        			float angle2 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-31f, 31f);
                    Vector2f sparkVel = MathUtils.getPointOnCircumference(ship_velocity, MathUtils.getRandomNumberInRange(11f, 121f), angle2);
                    
                    engine.addSmoothParticle(MathUtils.getRandomPointInCircle(proj_location, 3f),
                    		sparkVel,
            				MathUtils.getRandomNumberInRange(3f, 5f), //size
            				1.7f - (i * 0.1f), //brightness
            				MathUtils.getRandomNumberInRange(0.35f, 0.55f), //duration
            				new Color(235,255,215,150));
                	}
        	}
        	
    }
  }