package org.selkie.kol.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.combat.entities.Ship;
import exerelin.utilities.ReflectionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Collection;

public class KnightModule extends BaseHullMod {
    private final String id = "knightModule";
    public void init(HullModSpecAPI spec) {
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        //if(!ship.hasListenerOfClass(Hulkinator.class)) ship.addListener(new Hulkinator(ship));

    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCurrentState() == GameState.COMBAT && !ship.hasTag("KOL_moduleHulked")){
            Vector2f.add(ship.getLocation(), new Vector2f(10000,0), ship.getLocation());
            ship.setHulk(true);
            ship.setDrone(true);
            ship.addTag("KOL_moduleHulked");
        }
        boolean moduleDead = (ship.getParentStation() != null && !ship.getParentStation().isAlive()) || ship.getHitpoints() <= 0.0f;

        // make sure to actually kill and explode armor when dead, as hulk means the game wont auto do this
        if(moduleDead && !ship.hasTag("KOL_moduleDead")){
            ship.addTag("KOL_moduleDead");
            if (ship.getStationSlot() != null) {
                ship.setStationSlot(null);
                ((Ship) ship).setSpawnDebris(false);
                Ship.disable((Ship) ship, ship);
            }
        }

        if(!moduleDead){
            for(WeaponAPI weapon : ship.getAllWeapons()){
                if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.SYSTEM){
                    //weapon.setForceFireOneFrame(true);
                }
            }
        }
    }
}
