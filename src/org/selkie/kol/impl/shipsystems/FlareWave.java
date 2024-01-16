package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class FlareWave extends BaseShipSystemScript {
    float lastCooldown = 0f;
    int flaresWavesLeft = 3;
    IntervalUtil flareTimer = null;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if(((ShipAPI) stats.getEntity()).getSystem().getCooldownRemaining() > lastCooldown){
            flareTimer = new IntervalUtil(0.5f,0.5f);
            spawnFlares(stats);
        }
        lastCooldown = ((ShipAPI) stats.getEntity()).getSystem().getCooldownRemaining();
        if (flareTimer != null){
            flareTimer.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if(flareTimer.intervalElapsed()){
                spawnFlares(stats);
            }
        }
    }

    public void spawnFlares(MutableShipStatsAPI stats){
        ShipAPI ship = ((ShipAPI) stats.getEntity());
        Vector2f launcherA = MathUtils.getPointOnCircumference(ship.getLocation(), 50, ship.getFacing()-80f);
        Vector2f launcherB = MathUtils.getPointOnCircumference(ship.getLocation(), 50, ship.getFacing()+80f);
        Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", launcherA, ship.getFacing()-10f, ship.getVelocity());
        Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", launcherA, ship.getFacing()-15f, ship.getVelocity());
        Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", launcherA, ship.getFacing()-20f, ship.getVelocity());
        Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", launcherB, ship.getFacing()+10f, ship.getVelocity());
        Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", launcherB, ship.getFacing()+15f, ship.getVelocity());
        Global.getCombatEngine().spawnProjectile(ship, null, "flarelauncher3", launcherB, ship.getFacing()+20f, ship.getVelocity());
        flaresWavesLeft--;
        if (flaresWavesLeft <= 0) {
            flareTimer = null;
            flaresWavesLeft = 3;
        }
    }
}
