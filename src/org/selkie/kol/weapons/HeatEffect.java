package org.selkie.kol.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.awt.Color;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;

//By Tartiflette

public class HeatEffect implements EveryFrameWeaponEffectPlugin {
    
    private boolean alive = true, runOnce = false;
    private int range = 0;
    private ShipAPI ship;
    
    private IntervalUtil timer = new IntervalUtil(0.05f,0.15f);
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        
        if (engine.isPaused() || !alive) {return;}
        
        if (!runOnce){
            runOnce=true;
            ship=weapon.getShip();
        }
        
        timer.advance(amount);
        if(timer.intervalElapsed()){
            
            float flux = ship.getFluxTracker().getFluxLevel();
            if (flux>0){
                weapon.getAnimation().setFrame(1);                
            } else {
                weapon.getAnimation().setFrame(0);
            }
            
            float red = Math.min(
                    1,
                    Math.max(
                            0,
                            (float)FastTrig.cos( MathUtils.FPI/2  * (1-flux))
                    )
            );
            
            weapon.getSprite().setColor(new Color(red,flux,flux,1));
            
            if (ship != null && !ship.isAlive()) {
                if (range>=1){
                    weapon.getAnimation().setFrame(0);
                    alive = false;
                }
                range++;
            }
        }
    }
}