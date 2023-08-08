package org.selkie.kol.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

public class Blinker2 implements EveryFrameWeaponEffectPlugin {
    private Color base = null;
    private FaderUtil Fader = new FaderUtil(1f, 1f, 1f);
    private FaderUtil Pulser = new FaderUtil(1f, 2f, 2f, true, true);
    
    public Blinker2() {
        Fader.fadeIn();
        Pulser.fadeIn();
    }
    
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        ShipAPI ship = weapon.getShip();
        weapon.getSprite().setAdditiveBlend();
        Fader.advance(amount);
        Pulser.advance(amount);
        SpriteAPI sprite = weapon.getSprite();
        
        if (base == null) {base = sprite.getColor();}
        // Refit screen check
        if (ship.getOriginalOwner() == -1) {sprite.setColor(new Color(92, 92, 92, 255));return;} //blinker off ig
        if (ship.isAlive() && !ship.getFluxTracker().isOverloaded()) {
            if (ship.getFluxTracker().isVenting()) {
                Fader.fadeOut();
            } else {
                Fader.fadeIn();
            }
        }
        float alphaMult = Fader.getBrightness() * (0.75f + Pulser.getBrightness() * 0.25f);
        if (ship.getFluxTracker().isOverloaded()) {
            alphaMult = (float) Math.random() * Fader.getBrightness();
        }
        Color color = Misc.scaleAlpha(base, alphaMult);
        sprite.setColor(color);
    }
}