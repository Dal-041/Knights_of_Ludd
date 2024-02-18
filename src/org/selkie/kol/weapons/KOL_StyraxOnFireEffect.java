package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class KOL_StyraxOnFireEffect implements OnFireEffectPlugin {
    
    private static final Color FLASH_COLOR = new Color(140,140,255,70);
    
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
    	
            ShipAPI ship = weapon.getShip();
            Vector2f ship_velocity = ship.getVelocity();
            Vector2f proj_location = projectile.getLocation();
            
            //engine.spawnExplosion(proj_location, ship_velocity, FLASH_COLOR.brighter(), 20f, 0.15f);
            engine.addHitParticle(proj_location, ship_velocity, 50f, 1f, 0.1f, FLASH_COLOR.brighter());
            
        	for (int i=0; i < 9; i++) {
                Vector2f smokeVel = MathUtils.getPointOnCircumference(ship_velocity, i * 3f, projectile.getFacing());
                
                engine.addNebulaParticle(proj_location, smokeVel,
                		MathUtils.getRandomNumberInRange(8f, 16f),
                		1.4f, //endsizemult
                		0.1f, //rampUpFraction
                		0.3f, //fullBrightnessFraction
                		MathUtils.getRandomNumberInRange(1.6f, 2.1f), //totalDuration
                		new Color(90,90,110,30),
                		true);
                
            	for (int j=0; j < 2; j++) {
            		float angle1 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-4f, 4f);

                    Vector2f sparkVel = MathUtils.getPointOnCircumference(ship_velocity, MathUtils.getRandomNumberInRange(0f, 10f), angle1);
                    Vector2f sparkLoc = MathUtils.getPointOnCircumference(proj_location, i * 7f, angle1);
            		
                    engine.addSmoothParticle(sparkLoc,
                    		sparkVel,
            				MathUtils.getRandomNumberInRange(2f, 3f), //size
            				2f, //brightness
            				MathUtils.getRandomNumberInRange(0.5f, 2.1f), //duration
            				new Color(140,140,255,195));
            	}
        	}
        	
    }
  }