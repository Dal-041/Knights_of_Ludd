package org.selkie.kol.impl.plugins;

import com.fs.starfarer.api.combat.*;

import java.awt.Color;
import java.util.Iterator;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import java.lang.Math;

public class WeightyThrustersScript implements EveryFrameWeaponEffectPlugin {
    private boolean runOnce = false;
    private boolean accel = false;
    private boolean turn = false;
    private boolean hasThruster = false;
    private boolean isMainEngine = false;
    private boolean isGlowy = false;
    private ShipAPI SHIP;
    private ShipEngineControllerAPI.ShipEngineAPI thruster;
    private ShipEngineControllerAPI EMGINES;
    private float time = 0.0F;
    private float previousThrust = 0.0F;
    private float TURN_RIGHT_ANGLE = 0.0F;
    private float THRUST_TO_TURN = 0.0F;
    private float NEUTRAL_ANGLE = 0.0F;
    private float FRAMES = 0.0F;
    private float OFFSET = 0.0F;
    private Vector2f size = new Vector2f(8.0F, 74.0F);
    private EngineSlotAPI thrusterSlot;

    private float length = 0.05f;
    private float currentBrightness = 0.5f; //no touchy
    private final float timeToChange = 0.5f; //no touch!

    private final float FREQ = 0.0166666666f;   // this sets the effective frame rate of the deco/engine rotation script.
    private final float SMOOTH_THRUSTING = 0.25F;
    private final float MAXROTATIONSPEED = 1f; //this sets the "weight" of parts. More explicitly: sets the maximum part rotation allowed per script frame in degrees
    //yes I know the implementation is hacky and bad feel free to fix it if you want
    private final float flameStrengthOffset = 0.2f; //adjusts the strength of the engine flame. length ends up slightly too low and this corrects for it.
    //can also play around with it to see what effect you get

    // this is 100% the magicvector thruster but its had terrible sketchy things done to it and also a glow script added for some reason
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!this.runOnce) {
            this.runOnce = true;
            this.SHIP = weapon.getShip();
            this.EMGINES = this.SHIP.getEngineController();

            Iterator i$ = this.SHIP.getEngineController().getShipEngines().iterator();

            while(i$.hasNext()) {
                ShipEngineControllerAPI.ShipEngineAPI e = (ShipEngineControllerAPI.ShipEngineAPI)i$.next();
                if (MathUtils.isWithinRange(e.getLocation(), weapon.getLocation(), 6.5F)) {
                    this.thruster = e;
                    this.thrusterSlot=e.getEngineSlot();
                }
            }

            if (thruster != null) {
                hasThruster = true;
                if (thrusterSlot.getAngle() == 180f) isMainEngine = true;
            }
            if (weapon.getId().contains("_glow")) isGlowy = true; //if the deco is supposed to be a glowy thing thats the same color as the engine plume

            this.OFFSET = (float)(Math.random() * 3.1415927410125732);
            this.NEUTRAL_ANGLE = weapon.getSlot().getAngle();
            this.TURN_RIGHT_ANGLE = MathUtils.clampAngle(VectorUtils.getAngle(this.SHIP.getLocation(), weapon.getLocation()));
            this.TURN_RIGHT_ANGLE = MathUtils.getShortestRotation(this.SHIP.getFacing(), this.TURN_RIGHT_ANGLE) + 90.0F;
            this.THRUST_TO_TURN = this.smooth(MathUtils.getDistance(this.SHIP.getLocation(), weapon.getLocation()) / this.SHIP.getCollisionRadius());
        }

        if (isGlowy) {
            glowies(weapon, amount);
        }

        if (!engine.isPaused() && this.SHIP.getOriginalOwner() != -1) {
            if (this.SHIP.isAlive() && (this.thruster == null || !this.thruster.isDisabled())) {
                if(hasThruster) {
                    if (!isMainEngine) EMGINES.setFlameLevel(thrusterSlot, MathUtils.clamp(length,0f,1.01f));
                }
                this.time += amount;
                if (this.time >= FREQ) {

                    this.time = 0.0F;
                    float accelerateAngle = this.NEUTRAL_ANGLE;
                    float turnAngle = this.NEUTRAL_ANGLE;
                    float thrust = 0.0F;
                    if (this.EMGINES.isAccelerating()) {
                        accelerateAngle = 180.0F;
                        thrust = 1.5F;
                        this.accel = true;
                    } else if (this.EMGINES.isAcceleratingBackwards()) {
                        accelerateAngle = 0.0F;
                        thrust = 1.5F;
                        this.accel = true;
                    } else if (this.EMGINES.isDecelerating()) {
                        accelerateAngle = this.NEUTRAL_ANGLE;
                        thrust = 0.5F;
                        this.accel = true;
                    } else {
                        this.accel = false;
                    }

                    if (this.EMGINES.isStrafingLeft()) {
                        if (thrust == 0.0F) {
                            accelerateAngle = -90.0F;
                        } else {
                            accelerateAngle += MathUtils.getShortestRotation(accelerateAngle, -90.0F) / 2.0F;
                        }

                        thrust = Math.max(1.0F, thrust);
                        this.accel = true;
                    } else if (this.EMGINES.isStrafingRight()) {
                        if (thrust == 0.0F) {
                            accelerateAngle = 90.0F;
                        } else {
                            accelerateAngle += MathUtils.getShortestRotation(accelerateAngle, 90.0F) / 2.0F;
                        }
                        thrust = Math.max(1.0F, thrust);
                        this.accel = true;
                    }

                    if (this.EMGINES.isTurningRight()) {
                        turnAngle = this.TURN_RIGHT_ANGLE;
                        thrust = Math.max(1.0F, thrust);
                        this.turn = true;
                    } else if (this.EMGINES.isTurningLeft()) {
                        turnAngle = MathUtils.clampAngle(180.0F + this.TURN_RIGHT_ANGLE);
                        thrust = Math.max(1.0F, thrust);
                        this.turn = true;
                    } else {
                        this.turn = false;
                    }

                    if (thrust > 0.0F) {
                        Vector2f offset = new Vector2f(weapon.getLocation().x - this.SHIP.getLocation().x, weapon.getLocation().y - this.SHIP.getLocation().y);
                        VectorUtils.rotate(offset, -this.SHIP.getFacing(), offset);
                        if (!this.turn) {
                            this.thrust(weapon, accelerateAngle, thrust * this.SHIP.getMutableStats().getAcceleration().computeMultMod(), 0.25F);
                        } else if (!this.accel) {
                            this.thrust(weapon, turnAngle, thrust * this.SHIP.getMutableStats().getTurnAcceleration().computeMultMod(), 0.25F);
                        } else {
                            float clampedThrustToTurn = this.THRUST_TO_TURN * Math.min(1.0F, Math.abs(this.SHIP.getAngularVelocity()) / 10.0F);
                            clampedThrustToTurn = this.smooth(clampedThrustToTurn);
                            float combinedAngle = this.NEUTRAL_ANGLE;
                            combinedAngle = MathUtils.clampAngle(combinedAngle + MathUtils.getShortestRotation(this.NEUTRAL_ANGLE, accelerateAngle));
                            combinedAngle = MathUtils.clampAngle(combinedAngle + clampedThrustToTurn * MathUtils.getShortestRotation(accelerateAngle, turnAngle));
                            float combinedThrust = thrust * ((this.SHIP.getMutableStats().getTurnAcceleration().computeMultMod() + this.SHIP.getMutableStats().getAcceleration().computeMultMod()) / 2.0F);
                            float offAxis = Math.abs(MathUtils.getShortestRotation(turnAngle, accelerateAngle));
                            offAxis = Math.max(0.0F, offAxis - 90.0F);
                            offAxis /= 45.0F;
                            combinedThrust *= 1.0F - Math.max(0.0F, Math.min(1.0F, offAxis));
                            this.thrust(weapon, combinedAngle, combinedThrust, 0.125F);
                        }
                    } else {
                        this.thrust(weapon, this.NEUTRAL_ANGLE, 0.0F, 0.25F);
                    }
                }
            }
        }
    }

    /*
    private void rotate(WeaponAPI weapon, float angle, float thrust, float smooth) {
        float aim = angle + this.SHIP.getFacing();
        aim = MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);
        aim = (float)((double)aim + 5.0 * FastTrig.cos((double)(this.SHIP.getFullTimeDeployed() * 5.0F * thrust + this.OFFSET)));
        aim *= smooth;
        weapon.setCurrAngle(weapon.getCurrAngle()+MathUtils.clamp(aim, -1, 1));
        thrusterSlot.setAngle(weapon.getCurrAngle()-SHIP.getFacing()+MathUtils.clamp(aim, -1, 1));
        EMGINES.setFlameLevel(thrusterSlot,0f);
    }*/

    private void thrust(WeaponAPI weapon, float angle, float thrust, float smooth) {

        //SpriteAPI sprite = weapon.getSprite();
        float aim = angle + this.SHIP.getFacing();
        aim = MathUtils.getShortestRotation(weapon.getCurrAngle(), aim);
        length = 0.01f;
        if(hasThruster) {
            length = thrust * Math.max(0.0F, 1.0F - Math.abs(aim) / 90.0F);
            length -= this.previousThrust;
            length *= smooth;
            length += this.previousThrust;
            this.previousThrust = length;
            length += flameStrengthOffset;
        }
        float wooble = 0f;
        // wooble causes the vibrating effect on standalone fins/plate decos.
        // Comment the line below out if you dont want it. If you need this adjusted and dont know wtf its doing then ask Joe or the modding chat
        if (!hasThruster && (EMGINES.isAccelerating() || EMGINES.isAcceleratingBackwards() || EMGINES.isDecelerating()))
            wooble = (float) (23f*smooth*Math.random()*FastTrig.cos(SHIP.getFullTimeDeployed()*thrust+this.OFFSET));
        if (SHIP.getSystem().isActive()) wooble *= 3f;
        if (weapon.getSlot().getLocation().x < 0) wooble *= -1f;
        aim = (float)((double)aim + wooble);
        //aim = (float)((double)aim + 5.0 * FastTrig.cos((double)(this.SHIP.getFullTimeDeployed() * 5.0F * thrust + this.OFFSET)));
        aim *= smooth;
        weapon.setCurrAngle(weapon.getCurrAngle()+MathUtils.clamp(aim, -MAXROTATIONSPEED, MAXROTATIONSPEED));
        if(hasThruster) {
            thrusterSlot.setAngle(weapon.getCurrAngle() - SHIP.getFacing() + MathUtils.clamp(aim, -MAXROTATIONSPEED, MAXROTATIONSPEED));
            //if (!isMainEngine) EMGINES.setFlameLevel(thrusterSlot, length);
        }
    /*float width = length * this.size.x / 2.0F + this.size.x / 2.0F;
    float height = length * this.size.y + (float)Math.random() * 3.0F + 3.0F;
    sprite.setSize(width, height);
    sprite.setCenter(width / 2.0F, height / 2.0F);
    //length = Math.max(0.0F, Math.min(1.0F, length));
    */

    }

    public float smooth(float x) {
        return 0.5F - (float)(Math.cos((double)(x * 3.1415927F)) / 2.0);
    }

    public void glowies(WeaponAPI weapon, Float amount) {


        //default brightness is idle
        float targetBrightness = 0.5f;

        //change brightness depending on current actions
        if (EMGINES.isAccelerating() || EMGINES.isAcceleratingBackwards()) {
            targetBrightness = 1f;
        } else if (EMGINES.isTurningLeft() || EMGINES.isTurningRight()) {
            targetBrightness = 0.75f;
        }

        //smooth glow change
        if (currentBrightness > targetBrightness) {
            currentBrightness -= amount / timeToChange;
            if (currentBrightness < targetBrightness)
                currentBrightness = targetBrightness;
        } else if (currentBrightness < targetBrightness) {
            currentBrightness += amount / timeToChange;
            if (currentBrightness > targetBrightness)
                currentBrightness = targetBrightness;
        }

        //set glow to 0 if flame out
        if (thruster.isDisabled()) {
            currentBrightness = 0f;
        }
        if (SHIP.isHulk() || SHIP.isPhased()) {
            currentBrightness = 0f;
        }

        //make color and apply it to sprite
        //Color colorToUse = new Color(COLOR_NORMAL[0], COLOR_NORMAL[1], COLOR_NORMAL[2], currentBrightness * MAX_OPACITY);
        int Red = thruster.getEngineColor().getRed();
        int Green = thruster.getEngineColor().getGreen();
        int Blue = thruster.getEngineColor().getBlue();
        if (SHIP.getVariant().hasHullMod("safetyoverrides")) {
            Red = Math.round((Red * 0.8f) + (255 * 0.2f)) - 1;
            Green = Math.round((Green * 0.8f) + (100 * 0.2f)) - 1;
            Blue = Math.round((Blue * 0.8f) + (255 * 0.2f)) - 1;
        }
        Color colorToUse = new Color(Red, Green, Blue, Math.round(currentBrightness * 255));
        weapon.getSprite().setColor(colorToUse);
    }
}
