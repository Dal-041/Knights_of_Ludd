package org.selkie.kol.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

public class Blinker implements EveryFrameWeaponEffectPlugin {
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        AnimationAPI anime = weapon.getAnimation();
        ShipAPI ship = weapon.getShip();
        //if (ship.getOriginalOwner() == -1) {anime.setAlphaMult(1.15f);return;}
        weapon.getSprite().setAdditiveBlend();
        if (ship.isAlive() && !ship.getFluxTracker().isOverloadedOrVenting()) {
            anime.setAlphaMult(1f);
        } else {
            anime.setAlphaMult(0f);
        }
    }
}