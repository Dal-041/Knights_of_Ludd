package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import exerelin.utilities.ReflectionUtils;

import java.awt.*;

public class hideDeco implements EveryFrameWeaponEffectPlugin {

    ShipAPI module = null;
    boolean done = false;
    boolean gotMod = false;

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
        for(ShipAPI module : mainShip.getChildModulesCopy()){
            if (module.getHullSpec().getBaseHullId().substring(module.getHullSpec().getBaseHullId().length() - 2).equals(weapID)) {
                return module;
            }
        }

        return null;
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!done) {
            ShipAPI ship = weapon.getShip();
            if (ship == null || engine.isPaused()) return;

            // Refit screen check
            if (ship.getOriginalOwner() == -1) {
                return;
            }

            if (!gotMod) {
                module = getCorrespondingModule(ship, weapon.getSpec().getWeaponId());
                if (module != null) gotMod = true;
            }
            if (gotMod && (module.getHitpoints() <= 0f)) {
                weapon.getSprite().setNormalBlend();
                SpriteAPI sprite = weapon.getSprite();
                sprite.setColor(new Color(0,0,0,0));
                done = true;
            }
        }
        return;
    }
}