package org.selkie.kol.weapons;

import java.awt.Color;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.util.IntervalUtil;

public class KOL_SmokeBeamEffect implements BeamEffectPlugin {

    private final IntervalUtil interval1 = new IntervalUtil(0.1f,0.2f);
    private final IntervalUtil interval2 = new IntervalUtil(0.2f,0.3f);
	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {

		interval1.advance(engine.getElapsedInLastFrame());
        if (interval1.intervalElapsed()) {
        	
            if (beam.didDamageThisFrame()) {
                float size = beam.getWidth() * MathUtils.getRandomNumberInRange(5.1f, 6f);
                float dur = MathUtils.getRandomNumberInRange(0.1f,0.2f);
                
                engine.addHitParticle(beam.getTo(), beam.getSource().getVelocity(), size, 0.8f, dur, beam.getFringeColor());
            }
        }
        
        interval2.advance(engine.getElapsedInLastFrame());
        if (interval2.intervalElapsed()) {

            Vector2f smokePos = MathUtils.getPointOnCircumference(beam.getFrom(), MathUtils.getRandomNumberInRange(6f, 12f), beam.getWeapon().getCurrAngle());
            Vector2f smokeVel = MathUtils.getPointOnCircumference(beam.getSource().getVelocity(), MathUtils.getRandomNumberInRange(3f, 15f), beam.getWeapon().getCurrAngle());
            
            engine.addNebulaParticle(
            		smokePos,
            		smokeVel,
            		MathUtils.getRandomNumberInRange(36f, 54f),
            		1.35f,
            		0.1f,
            		0.3f,
            		MathUtils.getRandomNumberInRange(0.69f, 1.6f),
            		new Color(150,150,150,(int)(80f * (beam.getBrightness() + 0.1f))),
            		true);
            
        }
        
    }
}