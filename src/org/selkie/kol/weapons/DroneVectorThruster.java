package org.selkie.kol.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class DroneVectorThruster implements EveryFrameWeaponEffectPlugin {
    private boolean runOnce = false, accel = false, turn = false;
    private ShipAPI ship;
    private ShipAPI thrusterDrone;
    private ShipEngineControllerAPI.ShipEngineAPI thruster;
    private ShipEngineControllerAPI engines;
    private float previousThrust = 0;

    //Smooth thrusting prevents instant changes in directions and levels of thrust, lower is smoother
    private float maxThrustChangePerSecond = 0;
    private float maxAngleChangePerSecond = 0;
    private float turnRightAngle = 0, thrustToTurn = 0, neutralAngle = 0, wobbleOffset = 0;
    private float glowCompensation = 1f;

    public float smooth(float x) {
        return 0.5f - ((float) (Math.cos(x * MathUtils.FPI) / 2));
    }
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!runOnce) {
            runOnce = true;
            ship = weapon.getShip();

            engines = ship.getEngineController();

            ShipHullSpecAPI spec = Global.getSettings().getHullSpec("zea_dawn_ao_thruster_droneL");
            ShipVariantAPI v = Global.getSettings().createEmptyVariant("zea_dawn_ao_thruster_droneL", spec);

            thrusterDrone = Global.getCombatEngine().createFXDrone(v);
            thrusterDrone.setLayer(CombatEngineLayers.ABOVE_SHIPS_LAYER);
            thrusterDrone.setOwner(ship.getOriginalOwner());
            thrusterDrone.setDrone(true);
            thrusterDrone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP, 100000f, ship);
            thrusterDrone.setCollisionClass(CollisionClass.NONE);
            thrusterDrone.giveCommand(ShipCommand.SELECT_GROUP, null, 0);
            Global.getCombatEngine().addEntity(thrusterDrone);

            //find the ship engine associated with the deco thruster
            for (ShipEngineControllerAPI.ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)) {
                    thruster = e;
                }
            }

            //desync the engines wobble
            wobbleOffset = (float) (Math.random() * MathUtils.FPI);

            //"rest" angle when not in use
            neutralAngle = weapon.getSlot().getAngle();
            //ideal aim angle to rotate the ship (allows free-form placement on the hull)
            turnRightAngle = MathUtils.clampAngle(VectorUtils.getAngle(ship.getLocation(), weapon.getLocation()));
            turnRightAngle = MathUtils.getShortestRotation(ship.getFacing(), turnRightAngle) + 90;
            //is the thruster performant at turning the ship? Engines closer to the center of mass will concentrate more on dealing with changes of velocity.
            thrustToTurn = smooth(MathUtils.getDistance(ship.getLocation(), weapon.getLocation()) / ship.getCollisionRadius());

            maxAngleChangePerSecond = weapon.getTurnRate();
            maxThrustChangePerSecond = weapon.getDerivedStats().getDps()/100f;
        }

        if (engine.isPaused() || ship.getOriginalOwner() == -1) {
            return;
        }

        //check for death/engine disabled

        if (!ship.isAlive()) {
            if (thrusterDrone != null) {
                Global.getCombatEngine().removeEntity(thrusterDrone);
            }
            return;
        }

        if(!thruster.isActive()){
            previousThrust = 0;
            return;
        }

        //check what the ship is doing
        float accelerateAngle = neutralAngle;
        float turnAngle = neutralAngle;
        float thrust = 0;

        if (engines.isAccelerating()) {
            accelerateAngle = 180;
            thrust = 1.5f;
            accel = true;
        } else if (engines.isAcceleratingBackwards()) {
            accelerateAngle = 0;
            thrust = 1.5f;
            accel = true;
        } else if (engines.isDecelerating()) {
            accelerateAngle = neutralAngle;
            thrust = 0.5f;
            accel = true;
        } else {
            accel = false;
        }

        if (engines.isStrafingLeft()) {
            if (thrust == 0) {
                accelerateAngle = -90;
            } else {
                accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, -90) / 2 + accelerateAngle;
            }
            thrust = Math.max(1, thrust);
            accel = true;
        } else if (engines.isStrafingRight()) {
            if (thrust == 0) {
                accelerateAngle = 90;
            } else {
                accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, 90) / 2 + accelerateAngle;
            }
            thrust = Math.max(1, thrust);
            accel = true;
        }

        if (engines.isTurningRight()) {
            turnAngle = turnRightAngle;
            thrust = Math.max(1, thrust);
            turn = true;
        } else if (engines.isTurningLeft()) {
            turnAngle = MathUtils.clampAngle(180 + turnRightAngle);
            thrust = Math.max(1, thrust);
            turn = true;
        } else {
            turn = false;
        }


        //calculate the corresponding vector thrusting
        if (thrust > 0) {
            ship.getEngineController().forceShowAccelerating();
            //SHIP.getEngineController().extendFlame(this, 2f, 0.5f, 0.5f);
            //DEBUG
            Vector2f offset = new Vector2f(weapon.getLocation().x - ship.getLocation().x, weapon.getLocation().y - ship.getLocation().y);
            VectorUtils.rotate(offset, -ship.getFacing(), offset);
            float thrustChangeModifier = (ship.getMutableStats().getTurnAcceleration().computeMultMod() + ship.getMutableStats().getAcceleration().computeMultMod()) / 2;

            if (!turn) {
                //thrust only, easy.
                thrust(weapon, accelerateAngle, thrustChangeModifier, amount);

            } else {
                if (!accel) {
                    //turn only, easy too.
                    thrust(weapon, turnAngle, thrustChangeModifier, amount);


                } else {
                    //combined turn and thrust, aka the funky part.


                    //start from the neutral angle
                    float combinedAngle = neutralAngle;

                    //adds both thrust and turn angle at their respective thrust-to-turn ratio. Gives a "middleground" angle
                    combinedAngle = MathUtils.clampAngle(combinedAngle + MathUtils.getShortestRotation(neutralAngle, accelerateAngle));
                    combinedAngle = MathUtils.clampAngle(combinedAngle + thrustToTurn * MathUtils.getShortestRotation(accelerateAngle, turnAngle));

                    thrust(weapon, combinedAngle, thrustChangeModifier, amount);
                }
            }

        } else {
            thrust(weapon, neutralAngle, 0, amount);
        }

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


    private void thrust(WeaponAPI weapon, float angle, float thrustChangeModifier, float amount) {

        //target angle
        float optimalAim = angle + ship.getFacing();

        //how far from the target angle the engine is aimed at

        float aimDirection = MathUtils.getShortestRotation(weapon.getCurrAngle(), optimalAim);

        //thrust is reduced while the engine isn't facing the target angle

        float targetThrust = MathUtils.clamp((1 - (Math.abs(aimDirection) / 90)),0, 1);
        if (thrustChangeModifier == 0) targetThrust = 0;

        float currentThrust;
        if (previousThrust < targetThrust){
            currentThrust = previousThrust + amount * maxThrustChangePerSecond;
            if (currentThrust > targetThrust)
                currentThrust = targetThrust;
        }
        else {
            currentThrust = previousThrust - amount * maxThrustChangePerSecond * 2.5f;
            if (currentThrust < targetThrust)
                currentThrust = targetThrust;
        }
        currentThrust = MathUtils.clamp(currentThrust, 0, 1);
        previousThrust = currentThrust;

        //engine wobble
        float targetAim = optimalAim + ((Math.abs(aimDirection) < 10f) ? (float) (2 * FastTrig.cos(ship.getFullTimeDeployed() * 5 + wobbleOffset)) : 0f);
        aimDirection = MathUtils.getShortestRotation(weapon.getCurrAngle(), targetAim);
        if ( isAngleWithinArc(weapon.getCurrAngle(), angle + ship.getFacing(), weapon.getArcFacing() + ship.getFacing() + 180))
            aimDirection = -aimDirection;

        float turnBonus = Misc.interpolate(1, 2, Math.abs(aimDirection)/180);
        if(aimDirection > 0){
            weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() + amount * maxAngleChangePerSecond * turnBonus));
        } else{
            weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() - amount * maxAngleChangePerSecond * turnBonus));
        }

        EngineSlotAPI engineSlot = thruster.getEngineSlot();
        float flameLevel = 0f;
        if(currentThrust > 0) flameLevel = Misc.interpolate(0.0f, 1f, currentThrust);

        ship.getEngineController().setFlameLevel(engineSlot, 0);
        //((com.fs.starfarer.loading.specs.EngineSlot) engineSlot).setGlowParams(thruster.width * glowCompensation, thruster.length + offset,1f,1f); // no clue what v2 and v3 do
        engineSlot.setAngle(weapon.getCurrAngle() - weapon.getShip().getFacing());
        engineSlot.setGlowSizeMult(0f);


        if (thrusterDrone != null) {

            for(ShipEngineControllerAPI.ShipEngineAPI droneEngine : thrusterDrone.getEngineController().getShipEngines()){
                EngineSlotAPI droneEngineSlot = droneEngine.getEngineSlot();
                thrusterDrone.getEngineController().setFlameLevel(droneEngineSlot, flameLevel);
            }

            thrusterDrone.setOwner(ship.getOwner());
            thrusterDrone.getLocation().set(weapon.getLocation());
            thrusterDrone.setFacing(weapon.getCurrAngle()+180);
            thrusterDrone.getVelocity().set(ship.getVelocity());
        }
    }
}
