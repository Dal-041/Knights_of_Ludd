package org.selkie.kol.weapons;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicLensFlare;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.util.IntervalUtil;

public class KOL_FlareBeamEffect implements BeamEffectPlugin {

    private final IntervalUtil interval1 = new IntervalUtil(0.1f,0.2f);
    private final IntervalUtil interval2 = new IntervalUtil(0.25f,0.5f);
    
    private static Map<WeaponSize, Float> wepFlareAlphaMult = new HashMap<WeaponSize, Float>();
	static {
		wepFlareAlphaMult.put(WeaponSize.SMALL, 0.15f);
		wepFlareAlphaMult.put(WeaponSize.MEDIUM, 0.25f);
		wepFlareAlphaMult.put(WeaponSize.LARGE, 0.35f);
	}
	
	private static Map<WeaponSize, Float> wepFlareSizeMult = new HashMap<WeaponSize, Float>();
	static {
		wepFlareSizeMult.put(WeaponSize.SMALL, 0.29f);
		wepFlareSizeMult.put(WeaponSize.MEDIUM, 0.37f);
		wepFlareSizeMult.put(WeaponSize.LARGE, 0.45f);
	}
    
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

		interval1.advance(engine.getElapsedInLastFrame());
        if (interval1.intervalElapsed()) {

			float width = beam.getWidth() / 2f; // halving due to unusually wide beams for wide beam glow visual

            float size = width * MathUtils.getRandomNumberInRange(1.9f, 2.2f);
            
            float dur = MathUtils.getRandomNumberInRange(0.1f,0.2f);
            
            engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), width, 0.6f, dur, beam.getCoreColor());
            engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), size, 0.6f, dur, beam.getFringeColor().brighter());
            
            if (beam.didDamageThisFrame()) {
                engine.addHitParticle(beam.getTo(), beam.getSource().getVelocity(), size * 2.8f, 0.8f, dur, beam.getFringeColor());
                
        		MagicLensFlare.createSharpFlare(
        			    engine,
        			    beam.getSource(),
        			    beam.getTo(),
						width * 0.5f,
						width * 10f,
        			    beam.getWeapon().getCurrAngle() + 90f,
        			    beam.getFringeColor(),
        			    beam.getCoreColor());
        		
            }
        }
        
        interval2.advance(engine.getElapsedInLastFrame());
        if (interval2.intervalElapsed()) {

			float width = beam.getWidth() / 2f;

        	Vector2f flarePoint = MathUtils.getPointOnCircumference(beam.getFrom(), MathUtils.getRandomNumberInRange(0f, width), beam.getWeapon().getCurrAngle());
        	
    		MagicLensFlare.createSharpFlare(
    			    engine,
    			    beam.getSource(),
    			    flarePoint,
					width * wepFlareSizeMult.get(beam.getWeapon().getSize()),
					width * wepFlareSizeMult.get(beam.getWeapon().getSize()) * 20f,
    			    beam.getWeapon().getCurrAngle() + 90f,
    			    new Color (beam.getFringeColor().getRed(),beam.getFringeColor().getGreen(),beam.getFringeColor().getBlue(),(int)(beam.getFringeColor().getAlpha() * wepFlareAlphaMult.get(beam.getWeapon().getSize()))),
    			    new Color (beam.getCoreColor().getRed(),beam.getCoreColor().getGreen(),beam.getCoreColor().getBlue(),(int)(beam.getCoreColor().getAlpha() * wepFlareAlphaMult.get(beam.getWeapon().getSize()))));
        	
        }
        
    }
}