package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;

/* Twin Layer Shielding: inspired by Tart, adapted by Starficz
* We emulate an effect level as the level given to us by the system will snap from any level to 0 when shields are toggles off.
* This results in an instant snap of the shield back to frontal and the arc snapping back to full.
* */
public class TwinShieldStats extends BaseShipSystemScript {
    public boolean runOnce = true;
    public float shieldArc;
    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();
    public float lastActiveLevel = 0f;
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if(runOnce){
            runOnce=false;
            shieldArc=ship.getShield().getArc();
        }
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        if( state == State.IN ||  state == State.ACTIVE || state == State.OUT){
            lastActiveLevel = effectLevel;
        } else{
            lastActiveLevel = Math.max(lastActiveLevel - amount/ship.getSystem().getChargeDownDur(), 0);
        }

        if(lastActiveLevel > 0){
            stats.getShieldDamageTakenMult().modifyMult(id, 1-(lastActiveLevel/2));
            stats.getShieldTurnRateMult().modifyPercent(id, 300*lastActiveLevel);
            stats.getShieldUnfoldRateMult().modifyMult(id, 2-lastActiveLevel);
            ship.getShield().setArc(shieldArc-(lastActiveLevel*0.5f*shieldArc));

            if(!(state == State.IN || state == State.ACTIVE )) {
                ship.getShield().forceFacing(ship.getShield().getFacing() + MathUtils.getShortestRotation(ship.getShield().getFacing(), ship.getFacing()) * 5f * amount);
            }
        } else{
            stats.getShieldDamageTakenMult().unmodify(id);
            stats.getShieldTurnRateMult().unmodify(id);
            stats.getShieldUnfoldRateMult().modifyMult(id, 2-effectLevel);
            ship.getShield().setArc(shieldArc);
            ship.getShield().forceFacing(ship.getFacing());
        }


        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.getPlayerShip() == ship && effectLevel > 0){
            engine.maintainStatusForPlayerShip(STATUSKEY1, ship.getSystem().getSpecAPI().getIconSpriteName(),
                    "-" + (int) Math.round(50 * effectLevel) + "% Shield Damage", "-" + (int) Math.round(50 * effectLevel) + "% Shield Arc", false);
            engine.maintainStatusForPlayerShip(STATUSKEY2, ship.getSystem().getSpecAPI().getIconSpriteName(),  ship.getSystem().getSpecAPI().getName(), "Unlocked Omni Shield" , false);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
            return null;
    }
}
