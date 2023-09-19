package org.selkie.kol.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class hideDeco implements EveryFrameWeaponEffectPlugin {

    ShipAPI module = null;

    public hideDeco() {
    }

    private ShipAPI getCorrespondingModule(ShipAPI mainShip, String weapID) {
        if (mainShip == null) {
            return null; //Shouldn't be possible, but anyway
        }

        //check if the suffixes match
        if (weapID.length() >= 2) {
            weapID = weapID.substring(weapID.length() - 2);
        }
        for(ShipAPI s : mainShip.getChildModulesCopy()){
            if (s.getId().substring(s.getId().length() - 2) == weapID) {
                return s;
            }
        }

        return null;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null || engine.isPaused()) return;

        // Refit screen check
        if (ship.getOriginalOwner() == -1) {
            return;
        }

        if (module == null) {
            ShipAPI module = getCorrespondingModule(ship, weapon.getId());
        }
        if (module != null && !module.isAlive()) {
            weapon.getSprite().setAdditiveBlend();
            SpriteAPI sprite = weapon.getSprite();
            sprite.setAlphaMult(0f);
        }
        return;
    }
}