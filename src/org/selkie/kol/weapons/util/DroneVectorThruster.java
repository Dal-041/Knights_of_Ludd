package org.selkie.kol.weapons.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

public class DroneVectorThruster implements EveryFrameWeaponEffectPlugin {

    boolean init = false;
    ShipAPI parentShip;
    ShipEngineControllerAPI.ShipEngineAPI actualEngine;
    ShipAPI engineModule;
    boolean startupError = false;
    boolean currentlyDisabled = false;
    float neutralAngle = 0;
    float angularVelocity = 0;
    float thrustLevel = 0;
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if(!init){
            init = true;
            parentShip = weapon.getShip();

            // pair the actual engine and the module to this script
            for (ShipEngineControllerAPI.ShipEngineAPI e : parentShip.getEngineController().getShipEngines()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)) {
                    actualEngine = e;
                }
            }
            for (ShipAPI e : parentShip.getChildModulesCopy()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)) {
                    engineModule = e;
                    e.setDrone(true);
                }
            }

            if(actualEngine == null || engineModule == null){
                startupError = true;
                return;
            }

            neutralAngle = weapon.getArcFacing();
        }

        if(startupError || !parentShip.isAlive()) return;


        // apply stat boosts as needed
        if (currentlyDisabled){
            if (actualEngine.isActive() && !engineModule.getEngineController().isFlamedOut()){
                currentlyDisabled = false;
                engineOnline(weapon.getId() + "_engine_module_offline");
            }
            else{
                engineOffline(weapon.getId() + "_engine_module_offline");
                return;
            }
        }

        // if module is dead, sync with actual engine
        if (!engineModule.isAlive()) {
            actualEngine.disable(true);
            currentlyDisabled = true;
            return;
        }

        // disable engine if module is flamed out
        if(engineModule.getEngineController().isFlamedOut() && !currentlyDisabled){
            actualEngine.disable();
            currentlyDisabled = true;
            return;
        }

        // if actual engine is disabled, flameout the module
        if(actualEngine.isDisabled() && !currentlyDisabled){
            engineModule.getEngineController().forceFlameout();
            currentlyDisabled = true;
            return;
        }


        //check what the ship is doing
        Float lateralThrustAngle = null;
        ShipEngineControllerAPI engines = parentShip.getEngineController();

        if (engines.isAccelerating()) {
            if (engines.isStrafingLeft()) {
                lateralThrustAngle = 225f;
            } else if (engines.isStrafingRight()) {
                lateralThrustAngle = 135f;
            } else{
                lateralThrustAngle = 180f;
            }
        } else if (engines.isAcceleratingBackwards()) {
            if (engines.isStrafingLeft()) {
                lateralThrustAngle = 315f;
            } else if (engines.isStrafingRight()) {
                lateralThrustAngle = 45f;
            } else{
                lateralThrustAngle = 0f;
            }
        } else if (engines.isStrafingLeft()) {
            lateralThrustAngle = 270f;
        } else if (engines.isStrafingRight()) {
            lateralThrustAngle = 90f;
        }

        Float rotationalThrustAngle = null;
        if (engines.isTurningRight()) {
            rotationalThrustAngle = MathUtils.clampAngle(neutralAngle + 90);
        } else if (engines.isTurningLeft()) {
            rotationalThrustAngle = MathUtils.clampAngle(neutralAngle - 90);
        }

        if (lateralThrustAngle != null && rotationalThrustAngle != null){
            float angleDifference = MathUtils.getShortestRotation(lateralThrustAngle, rotationalThrustAngle);
            float thrustAngle = MathUtils.clampAngle(lateralThrustAngle + angleDifference * 0.5f); // 0.5 is magic number for half lateral and half rotational
            thrust(weapon, thrustAngle, amount, true, Math.abs(angleDifference) > 120f);
        } else if (lateralThrustAngle != null) {
            thrust(weapon, lateralThrustAngle, amount, true, false);
        } else if (rotationalThrustAngle != null) {
            thrust(weapon, rotationalThrustAngle, amount, true, false);
        } else{
            thrust(weapon, neutralAngle, amount, false, false);
        }

        weapon.getArc();
        weapon.getArcFacing();

    }

    public boolean isAngleWithinArc(float startAngle, float endAngle, float testAngle) {
        startAngle = MathUtils.clampAngle(startAngle);
        endAngle = MathUtils.clampAngle(endAngle);
        testAngle = MathUtils.clampAngle(testAngle);

        float diff_ccw;
        if (startAngle <= endAngle)
            diff_ccw = endAngle - startAngle;
        else
            diff_ccw = (360 - startAngle) + endAngle;

        if (diff_ccw > 180) {
            if (startAngle >= endAngle)
                return testAngle <= startAngle && testAngle >= endAngle;
            else
                return testAngle <= startAngle || testAngle >= endAngle;
        }
        else {
            if (startAngle <= endAngle)
                return testAngle >= startAngle && testAngle <= endAngle;
            else
                return testAngle >= startAngle || testAngle <= endAngle;
        }
    }

    private void thrust(WeaponAPI weapon, float angle, float amount, boolean accelerating, boolean flaring) {
        //target angle
        float optimalAim = MathUtils.clampAngle(angle + parentShip.getFacing());
        float optimalInArc = MathUtils.clampAngle(Misc.isInArc(weapon.getArcFacing(), weapon.getArc(), angle) ? optimalAim :
                weapon.getArcFacing() + parentShip.getFacing() + (MathUtils.getShortestRotation(weapon.getArcFacing(), angle) > 0 ? 1 : -1) * weapon.getArc()/2);


        float shortestRotation = MathUtils.getShortestRotation(weapon.getCurrAngle(), optimalInArc);
        boolean useShortestRotation = !Misc.isInArc(weapon.getCurrAngle() + shortestRotation/2,  Math.abs(shortestRotation), weapon.getArcFacing() + parentShip.getFacing()+180);

        float angleDifference = useShortestRotation ? shortestRotation :( shortestRotation > 0) ? shortestRotation - 360f : shortestRotation + 360f;

        float angularAccel = weapon.getDerivedStats().getDps();
        boolean angularDecel = Math.pow(angularVelocity, 2) / (2 * angularAccel) > Math.abs(angleDifference) && (angleDifference * angularVelocity) > 0;
        angularVelocity = MathUtils.clamp(angularVelocity + (((angleDifference > 0) ^ angularDecel) ? angularAccel : -angularAccel) * amount, -weapon.getTurnRate(), weapon.getTurnRate());

        float nextCurrAngle = weapon.getCurrAngle() + amount * angularVelocity;

        if (Math.abs(nextCurrAngle - optimalInArc) < 0.01) angularVelocity = 0f;

        engineModule.setFacing(nextCurrAngle+180);
        weapon.setCurrAngle(nextCurrAngle);
        actualEngine.getEngineSlot().setAngle(nextCurrAngle);

        thrustLevel += (accelerating && Misc.isInArc(optimalAim, 120, nextCurrAngle) ? 1 : -1) * weapon.getRange()/100 * amount;
        thrustLevel = Math.max(Math.min(1, thrustLevel), 0);
        for (ShipEngineControllerAPI.ShipEngineAPI engine : engineModule.getEngineController().getShipEngines()){
            engineModule.getEngineController().setFlameLevel(engine.getEngineSlot(), Misc.interpolate(0.8f,1, thrustLevel));
        }
    }

    public void engineOffline(String id){
        MutableShipStatsAPI stats = parentShip.getMutableStats();
        stats.getMaxSpeed().modifyMult(id, 0.8f);
        stats.getMaxTurnRate().modifyMult(id, 0.8f);
        stats.getAcceleration().modifyMult(id, 0.8f);
        stats.getTurnAcceleration().modifyMult(id, 0.8f);
        engineModule.getMutableStats().getCombatEngineRepairTimeMult().unmodify(id);
        Global.getCombatEngine().maintainStatusForPlayerShip(id, "", "Engine Module Offline", "-20% Engine Stats", true);
        thrustLevel = 0;
    }

    public void engineOnline(String id){
        MutableShipStatsAPI stats = parentShip.getMutableStats();
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        engineModule.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(id, 100);
    }
}
