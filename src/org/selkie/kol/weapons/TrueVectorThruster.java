package org.selkie.kol.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;

/**
 * Manages vectoring or vernier-style attitude thrusters.
 *
 * <pre>"everyFrameEffect":"org.magiclib.weapons.MagicVectorThruster"</pre>
 * <p>
 * Just needs to be assigned to a deco weapon with a "flame" animation or a "cover" sprite in their weapon file. Supports both moving vectoring-style thrusters and fixed vernier-style ones.
 * WARNING, this script may have a negative performance impact, use it sparingly.
 *
 * <img src="https://static.wikia.nocookie.net/starfarergame/images/4/44/MagicVectorThruster_fixedVernierExample.gif/revision/latest?cb=20181024100639" />
 *
 * @author Tartiflette
 */
public class TrueVectorThruster implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false, accel = false, turn = false;
    private ShipAPI SHIP;
    private HashMap<Integer, ThrusterData> thrusters;
    private ShipEngineControllerAPI ENGINES;
    private float previousThrust = 0;

    //Smooth thrusting prevents instant changes in directions and levels of thrust, lower is smoother
    private float MAX_THRUST_CHANGE_PER_SECOND = 0;
    private float MAX_ANGLE_CHANGE_PER_SECOND = 0;
    private float TURN_RIGHT_ANGLE = 0, THRUST_TO_TURN = 0, NEUTRAL_ANGLE = 0, OFFSET = 0;
    //sprite size, could be scaled with the engine width to allow variable engine length

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (!runOnce) {
            runOnce = true;

            SHIP = weapon.getShip();
            ENGINES = SHIP.getEngineController();
            ENGINES.forceShowAccelerating();
            thrusters = new HashMap<>();
            //find the ship engine associated with the deco thruster
            for (ShipEngineAPI e : SHIP.getEngineController().getShipEngines()) {
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 2)) {
                    ThrusterData t = new ThrusterData(e);
                    thrusters.put(MathUtils.getRandom().nextInt(),t);
                }
            }

            //desync the engines wobble
            OFFSET = (float) (Math.random() * MathUtils.FPI);

            //"rest" angle when not in use
            NEUTRAL_ANGLE = weapon.getSlot().getAngle();
            //ideal aim angle to rotate the ship (allows free-form placement on the hull)
            TURN_RIGHT_ANGLE = MathUtils.clampAngle(VectorUtils.getAngle(SHIP.getLocation(), weapon.getLocation()));
            TURN_RIGHT_ANGLE = MathUtils.getShortestRotation(SHIP.getFacing(), TURN_RIGHT_ANGLE) + 90;
            //is the thruster performant at turning the ship? Engines closer to the center of mass will concentrate more on dealing with changes of velocity.
            THRUST_TO_TURN = smooth(MathUtils.getDistance(SHIP.getLocation(), weapon.getLocation()) / SHIP.getCollisionRadius());

            MAX_ANGLE_CHANGE_PER_SECOND = weapon.getTurnRate();
            MAX_THRUST_CHANGE_PER_SECOND = weapon.getDerivedStats().getDps()/100f;
        }

        if (engine.isPaused() || SHIP.getOriginalOwner() == -1) {
            return;
        }

        //check for death/engine disabled
        boolean dead = true;
        for (ThrusterData e : thrusters.values()) {
            if (e.engine.isActive()) dead = false;
        }
        if (!SHIP.isAlive() || dead) {
            previousThrust = 0;
            return;
        }


        //check what the ship is doing
        float accelerateAngle = NEUTRAL_ANGLE;
        float turnAngle = NEUTRAL_ANGLE;
        float thrust = 0;

        if (ENGINES.isAccelerating()) {
            accelerateAngle = 180;
            thrust = 1.5f;
            accel = true;
        } else if (ENGINES.isAcceleratingBackwards()) {
            accelerateAngle = 0;
            thrust = 1.5f;
            accel = true;
        } else if (ENGINES.isDecelerating()) {
            accelerateAngle = NEUTRAL_ANGLE;
            thrust = 0.5f;
            accel = true;
        } else {
            accel = false;
        }

        if (ENGINES.isStrafingLeft()) {
            if (thrust == 0) {
                accelerateAngle = -90;
            } else {
                accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, -90) / 2 + accelerateAngle;
            }
            thrust = Math.max(1, thrust);
            accel = true;
        } else if (ENGINES.isStrafingRight()) {
            if (thrust == 0) {
                accelerateAngle = 90;
            } else {
                accelerateAngle = MathUtils.getShortestRotation(accelerateAngle, 90) / 2 + accelerateAngle;
            }
            thrust = Math.max(1, thrust);
            accel = true;
        }

        if (ENGINES.isTurningRight()) {
            turnAngle = TURN_RIGHT_ANGLE;
            thrust = Math.max(1, thrust);
            turn = true;
        } else if (ENGINES.isTurningLeft()) {
            turnAngle = MathUtils.clampAngle(180 + TURN_RIGHT_ANGLE);
            thrust = Math.max(1, thrust);
            turn = true;
        } else {
            turn = false;
        }


        //calculate the corresponding vector thrusting
        if (thrust > 0) {
            SHIP.getEngineController().forceShowAccelerating();
            //SHIP.getEngineController().extendFlame(this, 2f, 0.5f, 0.5f);
            //DEBUG
            Vector2f offset = new Vector2f(weapon.getLocation().x - SHIP.getLocation().x, weapon.getLocation().y - SHIP.getLocation().y);
            VectorUtils.rotate(offset, -SHIP.getFacing(), offset);
            float thrustChangeModifier = (SHIP.getMutableStats().getTurnAcceleration().computeMultMod() + SHIP.getMutableStats().getAcceleration().computeMultMod()) / 2;

            if (!turn) {
                //thrust only, easy.
                thrust(weapon, accelerateAngle, thrustChangeModifier, amount);

            } else {
                if (!accel) {
                    //turn only, easy too.
                    thrust(weapon, turnAngle, thrustChangeModifier, amount);


                } else {
                    //combined turn and thrust, aka the funky part.

                    // I think this is pointless -Starficz
                    /*
                    //aim-to-mouse clamp, helps to avoid flickering when the ship is almost facing the cursor and not turning much.
                    float clampedThrustToTurn = THRUST_TO_TURN * Math.min(1, Math.abs(SHIP.getAngularVelocity()) / 10);
                    clampedThrustToTurn = smooth(clampedThrustToTurn);
                    */

                    //start from the neutral angle
                    float combinedAngle = NEUTRAL_ANGLE;

                    //adds both thrust and turn angle at their respective thrust-to-turn ratio. Gives a "middleground" angle
                    combinedAngle = MathUtils.clampAngle(combinedAngle + MathUtils.getShortestRotation(NEUTRAL_ANGLE, accelerateAngle));
                    combinedAngle = MathUtils.clampAngle(combinedAngle + THRUST_TO_TURN * MathUtils.getShortestRotation(accelerateAngle, turnAngle));

                    // I think this is pointless -Starficz
                    /*
                    //get the total thrust with mults
                    float combinedThrust = thrust;
                    combinedThrust *= (SHIP.getMutableStats().getTurnAcceleration().computeMultMod() + SHIP.getMutableStats().getAcceleration().computeMultMod()) / 2;

                    //calculate how much appart the turn and thrust angle are
                    //bellow 90 degrees, the engine is kept at full thrust
                    //if they are further appart, the engine is less useful and it's output get reduced
                    float offAxis = Math.abs(MathUtils.getShortestRotation(turnAngle, accelerateAngle));
                    offAxis = Math.max(0, offAxis - 90);
                    offAxis /= 45;

                    combinedThrust *= 1 - Math.max(0, Math.min(1, offAxis));
                    */
                    thrust(weapon, combinedAngle, thrustChangeModifier, amount);
                }
            }

        } else {
            thrust(weapon, NEUTRAL_ANGLE, 0, amount);
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
        float optimalAim = angle + SHIP.getFacing();

        //how far from the target angle the engine is aimed at

        float aimDirection = MathUtils.getShortestRotation(weapon.getCurrAngle(), optimalAim);

        //Global.getCombatEngine().addSmoothParticle(MathUtils.getPointOnCircumference(weapon.getLocation(), 100, weapon.getCurrAngle()), SHIP.getVelocity(), 20f, 100f,0.1f, Color.red);
        //Global.getCombatEngine().addSmoothParticle(MathUtils.getPointOnCircumference(weapon.getLocation(), 30, weapon.getArcFacing() + SHIP.getFacing()+ 180f), SHIP.getVelocity(), 20f, 100f,0.1f, Color.magenta);
        //Global.getCombatEngine().addSmoothParticle(MathUtils.getPointOnCircumference(weapon.getLocation(), 80, angle + SHIP.getFacing()), SHIP.getVelocity(), 20f, 100f,0.1f, Color.green);

        //thrust is reduced while the engine isn't facing the target angle

        float targetThrust = MathUtils.clamp((1 - (Math.abs(aimDirection) / 90)),0, 1);
        if (thrustChangeModifier == 0) targetThrust = 0;

        float currentThrust;
        if (previousThrust < targetThrust){
            currentThrust = previousThrust + amount * MAX_THRUST_CHANGE_PER_SECOND;
            if (currentThrust > targetThrust)
                currentThrust = targetThrust;
        }
        else {
            currentThrust = previousThrust - amount * MAX_THRUST_CHANGE_PER_SECOND;
            if (currentThrust < targetThrust)
                currentThrust = targetThrust;
        }
        currentThrust = MathUtils.clamp(currentThrust, 0, 1);
        previousThrust = currentThrust;

        //engine wobble
        float targetAim = optimalAim + ((Math.abs(aimDirection) < 10f) ? (float) (2 * FastTrig.cos(SHIP.getFullTimeDeployed() * 5 + OFFSET)) : 0f);
        aimDirection = MathUtils.getShortestRotation(weapon.getCurrAngle(), targetAim);
        if (isAngleWithinArc(weapon.getCurrAngle(), angle + SHIP.getFacing(), weapon.getArcFacing() + SHIP.getFacing() + 180))
            aimDirection = -aimDirection;

        float turnBonus = Misc.interpolate(1, 2, Math.abs(aimDirection)/180);
        if(aimDirection > 0){
            weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() + amount * MAX_ANGLE_CHANGE_PER_SECOND * turnBonus));
        } else{
            weapon.setCurrAngle(MathUtils.clampAngle(weapon.getCurrAngle() - amount * MAX_ANGLE_CHANGE_PER_SECOND * turnBonus));
        }

        float offset = weapon.getSprite().getHeight() - weapon.getSprite().getCenterY();
        for (ThrusterData thruster : thrusters.values()) {
            EngineSlotAPI engineSlot = thruster.engine.getEngineSlot();
            float startingLevel = (thruster.length - offset) / thruster.length;
            float compensatedLevel = Misc.interpolate(startingLevel, 1f, currentThrust);
            SHIP.getEngineController().setFlameLevel(engineSlot, compensatedLevel);
            ((com.fs.starfarer.loading.specs.EngineSlot) engineSlot).setGlowParams(thruster.width, thruster.width*compensatedLevel,1f,1f); // no clue what v2 and v3 do
            engineSlot.setAngle(weapon.getCurrAngle() - weapon.getShip().getFacing());
            engineSlot.setGlowSizeMult(0f);
        }
    }

    static class ThrusterData {
        private final Float length;
        private final Float width;
        private final ShipEngineAPI engine;

        ThrusterData(ShipEngineAPI engine) {
            length = engine.getEngineSlot().getLength();
            width = engine.getEngineSlot().getWidth();
            this.engine = engine;
        }

    }

    //////////////////////////////////////////
    //                                      //
    //           SMOOTH DAT MOVE            //
    //                                      //
    //////////////////////////////////////////

    public float smooth(float x) {
        return 0.5f - ((float) (Math.cos(x * MathUtils.FPI) / 2));
    }
}
