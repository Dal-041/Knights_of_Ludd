package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class FlareBurnStats extends BaseShipSystemScript {
    boolean launchedFlares = false;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (state == ShipSystemStatsScript.State.ACTIVE) {
            if(!launchedFlares){
                ShipAPI ship = ((ShipAPI) stats.getEntity());
                for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines() ) {
                    Vector2f launchVelocity = (Vector2f) new Vector2f(ship.getVelocity()).scale(0.5f);
                    float engineLocationLocal = MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(), engine.getLocation()), ship.getFacing());
                    Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", engine.getLocation(), ship.getFacing() + (engineLocationLocal > 0 ? -30 : 30), launchVelocity);
                }
                launchedFlares = true;

            }
        }

        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
        } else {
            stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
            stats.getAcceleration().modifyFlat(id, 1200f * effectLevel);
        }
    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        launchedFlares = false;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("increased engine power", false);
        }
        return null;
    }
}
