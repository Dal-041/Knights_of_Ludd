package org.selkie.kol.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.selkie.kol.combat.StarficzAIUtils;

import java.awt.*;

public class hideDeco implements EveryFrameWeaponEffectPlugin {
    boolean hidden = false;
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon){
        if(hidden || engine.isPaused() || Global.getCurrentState() != GameState.COMBAT) return;
        ShipAPI ship = weapon.getShip();
        if(ship == null || !ship.isAlive()){
            hidden = true;
        }
        else{
            // note that the modules are named like "kol_snowdrop_tl" and the decos "kol_deco_snowdrop_tl", "kol_deco_snowdrop_vulcan_tl", ect
            String weaponID = weapon.getSpec().getWeaponId().substring(weapon.getSpec().getWeaponId().length() - 2);
            boolean moduleDead = true;
            for(ShipAPI module : ship.getChildModulesCopy()){
                String moduleID = module.getHullSpec().getBaseHullId().substring(module.getHullSpec().getBaseHullId().length() - 2);
                if (weaponID.equals(moduleID) && module.getHitpoints() > 0){
                    moduleDead = false;
                    float healthLevel = StarficzAIUtils.getCurrentArmorRating(module) / module.getArmorGrid().getArmorRating();
                    weapon.setCurrHealth(weapon.getMaxHealth() * healthLevel);
                    if(weapon.getSpec().getWeaponId().contains("vulcan")){
                        for(WeaponAPI moduleWeapon : module.getAllWeapons()){
                            if(moduleWeapon.getSpec().getWeaponId().contains("vulcan")){
                                weapon.setFacing(moduleWeapon.getCurrAngle());
                            }
                        }
                    }
                }
            }
            if(moduleDead) hidden = true;
        }

        if(hidden){
            weapon.getSprite().setNormalBlend();
            SpriteAPI sprite = weapon.getSprite();
            sprite.setColor(new Color(0,0,0,0));

            if (weapon.getBarrelSpriteAPI() != null){
                weapon.getBarrelSpriteAPI().setColor(new Color(0,0,0,0));
            }
        }
    }
}