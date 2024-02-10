package org.selkie.kol.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.io.IOException;

public class ModuleVulcan extends BaseHullMod {
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.getVariant().assignUnassignedWeapons();
        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyFlat(id, 300f);
        try {
            Global.getSettings().loadTexture("graphics/weapons/vulcan_cannon_turret.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if(Global.getCurrentState() != GameState.COMBAT) return;
        if(!Global.getCombatEngine().isEntityInPlay(ship) || !ship.isAlive()) return;

        // fake snowdrops vulcan cannons
        if(ship.getHullSpec().getHullId().contains("kol_snowdrop")){
            for(WeaponAPI weapon : ship.getAllWeapons()){
                if (weapon.getSpec().getWeaponId().contains("vulcan")){

                    SpriteAPI turret = Global.getSettings().getSprite("fx", "fake_vulcan");
                    float turretAngle = weapon.getCurrAngle()-90f;
                    MagicRender.singleframe(turret, weapon.getLocation(), new Vector2f(turret.getWidth(), turret.getHeight()), turretAngle, Color.white, false, CombatEngineLayers.CONTRAILS_LAYER);
                }
            }
        }
    }
}