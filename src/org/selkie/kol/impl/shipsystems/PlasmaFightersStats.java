package org.selkie.kol.impl.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import org.magiclib.subsystems.drones.MagicDroneSubsystem;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PlasmaFightersStats extends BaseShipSystemScript {
    private static float SPEED_BONUS = 125f;
    private static float TURN_BONUS = 20f;
    private static final Color color = new Color(100, 255, 100, 255);

    @Override
    public void apply(MutableShipStatsAPI systemStats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) systemStats.getEntity();
        List<MutableShipStatsAPI> statsToModify = getStatsForShipFightersAndDrones(ship);
        statsToModify.add(systemStats);

        for (MutableShipStatsAPI stats : statsToModify) {
            if (state == State.OUT) {
                stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
                stats.getMaxTurnRate().unmodify(id);
            } else {
                stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS);
                stats.getAcceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
                stats.getDeceleration().modifyPercent(id, SPEED_BONUS * 3f * effectLevel);
                stats.getTurnAcceleration().modifyFlat(id, TURN_BONUS * effectLevel);
                stats.getTurnAcceleration().modifyPercent(id, TURN_BONUS * 5f * effectLevel);
                stats.getMaxTurnRate().modifyFlat(id, 15f);
                stats.getMaxTurnRate().modifyPercent(id, 100f);
            }

            if (stats.getEntity() instanceof ShipAPI) {
                ShipAPI statsShip = (ShipAPI) stats.getEntity();

                statsShip.getEngineController().fadeToOtherColor(this, color, new Color(0, 0, 0, 0), effectLevel, 0.67f);
                //ship.getEngineController().fadeToOtherColor(this, Color.white, new Color(0,0,0,0), effectLevel, 0.67f);
                statsShip.getEngineController().extendFlame(this, 1.5f * effectLevel, 0f * effectLevel, 0f * effectLevel);
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI systemStats, String id) {
        ShipAPI ship = (ShipAPI) systemStats.getEntity();
        List<MutableShipStatsAPI> statsToModify = getStatsForShipFightersAndDrones(ship);
        statsToModify.add(systemStats);

        for (MutableShipStatsAPI stats : statsToModify) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0) {
            return new StatusData("improved maneuverability", false);
        } else if (index == 1) {
            return new StatusData("+" + (int) SPEED_BONUS + " top speed", false);
        }
        return null;
    }

    public static List<MutableShipStatsAPI> getStatsForShipFightersAndDrones(ShipAPI ship) {
        List<MutableShipStatsAPI> returnedStats = new ArrayList<>();
        for (ShipAPI wing : Global.getCombatEngine().getShips()) {
            if (!wing.isFighter()) continue;
            if (wing.getWing() == null) continue;
            if (wing.getWing().getSourceShip() == ship) {
                returnedStats.add(wing.getMutableStats());
            }
        }

        List<MagicSubsystem> activators = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (activators != null) {
            for (MagicSubsystem activator : activators) {
                if (activator instanceof MagicDroneSubsystem) {
                    MagicDroneSubsystem droneActivator = (MagicDroneSubsystem) activator;
                    for (ShipAPI drone : droneActivator.getActiveWings().keySet()) {
                        returnedStats.add(drone.getMutableStats());
                    }
                }
            }
        }

        return returnedStats;
    }
}
