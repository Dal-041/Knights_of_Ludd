package org.selkie.zea.weapons.util

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.combat.StarficzAIUtils
import kotlin.math.abs
import kotlin.math.pow

class TrueTemporalField : EveryFrameWeaponEffectPlugin {
    var previousTime = 0f
    override fun advance(amount: Float, engine: CombatEngineAPI?, weapon: WeaponAPI?) {
        var ship = weapon?.ship
        if (ship == null || engine == null) return

        if(engine.isPaused){
            var pausedAmount = engine.getTotalElapsedTime(true)-previousTime
            ship.location.set( Vector2f.add(ship.location, Vector2f(ship.velocity).scale(pausedAmount) as Vector2f?, null))
            ship.facing += ship.angularVelocity * pausedAmount

            // Calculate the forward unit vector based on current facing
            val uFwd = MathUtils.getPoint(Vector2f(0f, 0f), 1f, ship.facing)

            // Calculate unit vectors for right and left directions
            val uRight = VectorUtils.rotate(Vector2f(uFwd), -90f)
            val uLeft = VectorUtils.rotate(Vector2f(uFwd), 90f)


            // strafing uses forwards accel, but with a penalty
            val strafeAccelFactor = HashMap<HullSize, Float>()
            strafeAccelFactor[HullSize.FIGHTER] = 1f
            strafeAccelFactor[HullSize.FRIGATE] = 1f
            strafeAccelFactor[HullSize.DESTROYER] = 0.75f
            strafeAccelFactor[HullSize.CRUISER] = 0.5f
            strafeAccelFactor[HullSize.CAPITAL_SHIP] = 0.25f

            val accelVector = Vector2f(0f, 0f)

            // Handle ACCELERATE
            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                val temp = Vector2f(uFwd)
                temp.scale(ship.acceleration*pausedAmount)
                Vector2f.add(accelVector, temp, accelVector)
            }

            // Handle ACCELERATE_BACKWARDS
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                val temp = Vector2f(uFwd)
                temp.negate()
                temp.scale(ship.deceleration*pausedAmount)
                Vector2f.add(accelVector, temp, accelVector)
            }

            // Handle STRAFE_LEFT
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                val temp = Vector2f(uLeft)
                temp.scale(ship.acceleration * strafeAccelFactor[ship.hullSize]!! * pausedAmount)
                Vector2f.add(accelVector, temp, accelVector)
            }

            // Handle STRAFE_RIGHT
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                val temp = Vector2f(uRight)
                temp.scale(ship.acceleration * strafeAccelFactor[ship.hullSize]!! * pausedAmount)
                Vector2f.add(accelVector, temp, accelVector)
            }


            // Apply acceleration to current speed to get new speed
            val newVelocity = Vector2f()
            Vector2f.add(ship.velocity, accelVector, newVelocity)

            // Check if new speed exceeds top speed
            if (newVelocity.length() > ship.maxSpeed) {
                // Adjust the speed components to bring the speed back to topSpeed
                StarficzAIUtils.adjustSpeedToMax(newVelocity, ship.maxSpeed, accelVector)
            }
            ship.velocity.set(newVelocity)

            val turnAngle = VectorUtils.getAngle(ship.location, ship.mouseTarget)
            val rotAngle = MathUtils.getShortestRotation(ship.facing, turnAngle)
            ship.turnAcceleration
            ship.angularVelocity

            var decel = false
            if (ship.angularVelocity * rotAngle > 0) { // make sure velocity and angle have the same sign, (only slow down if it makes sense)
                if (ship.angularVelocity.toDouble().pow(2.0) / (2 * ship.turnAcceleration) > abs(rotAngle.toDouble())) { // basic kinematic solution
                    decel = true
                }
            }

            if ((rotAngle > 0) xor decel) {
                ship.angularVelocity = (ship.angularVelocity + ship.turnAcceleration * pausedAmount).coerceAtMost(ship.maxTurnRate)
            } else {
                ship.angularVelocity = (ship.angularVelocity - ship.turnAcceleration * pausedAmount).coerceAtLeast(-ship.maxTurnRate)
            }
        }

        previousTime = engine.getTotalElapsedTime(true)
    }
}