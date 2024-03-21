package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class KOL_SparkleBeamEffect implements BeamEffectPlugin {

    private final IntervalUtil interval1 = new IntervalUtil(0.1f,0.2f);
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		
		interval1.advance(engine.getElapsedInLastFrame());
        if (interval1.intervalElapsed()) {

            float width = beam.getWidth() / 2f; // halved due to unusally wide beams for wide beam glow visual
        	
            float size = width * MathUtils.getRandomNumberInRange(1.9f, 2.1f);
            
            float dur = MathUtils.getRandomNumberInRange(0.1f,0.2f);
            
            engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), width, 0.6f, dur, beam.getCoreColor());
            engine.addHitParticle(beam.getFrom(), beam.getSource().getVelocity(), size, 0.6f, dur, beam.getFringeColor().brighter());
            
    		if (beam.getBrightness() >= 0.9f) {
    			for (int i=0; i < size; i+=4) {
    				
    				float rangeScale = MathUtils.getRandomNumberInRange(0.1f, 1.0f);
    				float timeScale = 0.5f + (0.5f * (1.1f - rangeScale));
    				
    				Color sparkleColor = new Color (beam.getFringeColor().getRed(),beam.getFringeColor().getGreen(),beam.getFringeColor().getBlue(),(int)(beam.getFringeColor().getAlpha() * timeScale));
    				
                	Vector2f sparklePoint = MathUtils.getPointOnCircumference(beam.getFrom(), size * 4.4f * rangeScale, beam.getWeapon().getCurrAngle());
                	engine.addSmoothParticle(MathUtils.getRandomPointInCircle(sparklePoint, width * 0.5f), beam.getSource().getVelocity(), size * MathUtils.getRandomNumberInRange(0.15f, 0.35f), 0.69f, dur * 3f * timeScale, sparkleColor);
                }
                
        	}
    		
            if (beam.didDamageThisFrame()) {
                engine.addHitParticle(beam.getTo(), beam.getSource().getVelocity(), size * 2.8f, 0.8f, dur, beam.getFringeColor());
            }
        }
        
    }
}