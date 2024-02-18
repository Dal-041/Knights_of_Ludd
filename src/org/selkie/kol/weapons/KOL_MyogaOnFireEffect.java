package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class KOL_MyogaOnFireEffect implements OnFireEffectPlugin {
    
    private static final Color FLASH_COLOR = new Color(160,40,225,100);
    
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
    	
            ShipAPI ship = weapon.getShip();
            Vector2f ship_velocity = ship.getVelocity();
            Vector2f proj_location = projectile.getLocation();
            Vector2f fxLoc = MathUtils.getPointOnCircumference(proj_location, 23f, projectile.getFacing());
            
            engine.spawnExplosion(fxLoc, ship_velocity, FLASH_COLOR.brighter(), 33f, 0.22f);
            engine.addHitParticle(fxLoc, ship_velocity, 90f, 0.5f, 0.1f, FLASH_COLOR.brighter());
            
        	for (int i=0; i < 12; i++) {
        		Vector2f smokeLoc = MathUtils.getPointOnCircumference(proj_location, 23f + (i * 6f), projectile.getFacing());
        		Vector2f smokeVel = MathUtils.getPointOnCircumference(ship_velocity, 13f - i, projectile.getFacing());
                
                engine.addNebulaParticle(smokeLoc, smokeVel,
                		MathUtils.getRandomNumberInRange(11f, 22f),
                		1.6f, //endsizemult
                		0.15f, //rampUpFraction
                		0.6f, //fullBrightnessFraction
                		MathUtils.getRandomNumberInRange(2.1f, 3f) - (i * 0.14f), //totalDuration
                		new Color(103,65,120,69),
                		true);
                
            	for (int j=0; j < 3; j++) {
            		float angle1 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-4f, 4f);

                    Vector2f sparkVel = MathUtils.getPointOnCircumference(ship_velocity, MathUtils.getRandomNumberInRange(0f, 11f), angle1);
                    Vector2f sparkLoc = MathUtils.getPointOnCircumference(proj_location, MathUtils.getRandomNumberInRange(16f, 18f) + (i * 8f), angle1);
            		
                    engine.addSmoothParticle(sparkLoc,
                    		sparkVel,
            				MathUtils.getRandomNumberInRange(2f, 4f), //size
            				2f - (i * 0.1f), //brightness
            				MathUtils.getRandomNumberInRange(0.5f, 2.1f), //duration
            				new Color(255,220,85,225));
            		
            	}
        	}
        	
    }
  }