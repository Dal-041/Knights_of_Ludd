package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class SmallFluxNegationStats extends BaseShipSystemScript {
    public static Color WEAPON_GLOW = new Color(255,50,50,155);
    private HashMap<WeaponAPI, Boolean> fluxRefunded = new HashMap<>();
    private List<WeaponAPI> beams = new ArrayList<>();
    private boolean inited = false;
    private ShipAPI ship;
    public void apply(MutableShipStatsAPI stats, final String id, State state, float effectLevel) {
        init((ShipAPI) stats.getEntity());
        float dissipationBuff = 0f;
        float flatFluxRefund = 0f;


        for(WeaponAPI weapon : beams){
            weapon.setGlowAmount(effectLevel, WEAPON_GLOW);
            if(weapon.isFiring()) dissipationBuff += weapon.getFluxCostToFire();
        }
        for(WeaponAPI weapon : fluxRefunded.keySet()){
            weapon.setGlowAmount(effectLevel, WEAPON_GLOW);
            if(weapon.isFiring()){
                if(!fluxRefunded.get(weapon)){
                    fluxRefunded.put(weapon, true);
                    flatFluxRefund += weapon.getFluxCostToFire();
                }
            } else if(!weapon.isInBurst()){
                fluxRefunded.put(weapon, false);
            }
        }
        float maxFluxRefund = ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux();
        ship.getFluxTracker().decreaseFlux(Math.min(maxFluxRefund, flatFluxRefund));
        stats.getFluxDissipation().modifyFlat(id, dissipationBuff);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getFluxDissipation().unmodify(id);
        for(WeaponAPI weapon : beams){weapon.setGlowAmount(0, null);}
        for(WeaponAPI weapon : fluxRefunded.keySet()){weapon.setGlowAmount(0, null);}
    }

    private void init(ShipAPI ship){
        if (inited) return;
        this.ship = ship;
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if(weapon.getSize() == WeaponAPI.WeaponSize.SMALL && !weapon.isDecorative()){
                if (weapon.isBeam() && !weapon.isBurstBeam()){
                    beams.add(weapon);
                }
                else{
                    fluxRefunded.put(weapon, true);
                }
            }
        }
        inited = true;
    }
}
