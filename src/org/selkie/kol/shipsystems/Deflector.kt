package org.selkie.kol.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.*


class Deflector : BaseShipSystemScript() {
    val MAX_RANGE = 1000f
    override fun apply(stats: MutableShipStatsAPI?, id: String?, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        val ship = stats?.entity ?: return
        val iterator = Global.getCombatEngine().allObjectGrid.getCheckIterator(ship.location, MAX_RANGE * 2, MAX_RANGE * 2)
        while (iterator.hasNext()) {
            val entity = iterator.next()
            if(entity is DamagingProjectileAPI && entity.owner != ship.owner){
                var deflectionAngle = getMinimumDeflectionAngle(entity, ship, ship.collisionRadius)
                if(deflectionAngle != 0f){
                    VectorUtils.rotate(entity.velocity, Misc.DEG_PER_RAD * deflectionAngle)
                    entity.facing += (Misc.DEG_PER_RAD * deflectionAngle)
                }
            }
        }
    }

    fun getMinimumDeflectionAngle(projectile: DamagingProjectileAPI, ship: CombatEntityAPI, shipRadius: Float): Float {
        if(projectile.velocity.lengthSquared() < 100f) return 0.0f // if slower than 10f, don't bother

        // Compute the difference vector between ship and projectile positions
        val positionDelta = Vector2f.sub(ship.location, projectile.location, null)
        val velocityDelta = Vector2f.sub(projectile.velocity, ship.velocity, null)

        // Calculate the magnitude squared of the velocity and delta vectors
        val velocityMagnitudeSquared = velocityDelta.lengthSquared()
        val deltaMagnitudeSquared = positionDelta.lengthSquared()

        // Compute the cross product of the velocity and delta vectors
        val crossProduct = (velocityDelta.x * positionDelta.y) - (velocityDelta.y * positionDelta.x)

        // Compute the dot product of the velocity and delta vectors
        val dotProduct = (velocityDelta.x * positionDelta.x) + (velocityDelta.y * positionDelta.y)

        // Return zero if the projectile is moving away from the ship
        if (dotProduct <= 0.0f) return 0.0f

        // Check if the line of travel intersects the ship's circle
        val distanceSquared = (crossProduct * crossProduct) / velocityMagnitudeSquared
        if (distanceSquared > ((shipRadius-10f) * (shipRadius-10f))) return 0.0f // 20f grace distance

        // Calculate the distance between the ship and the projectile
        val distance = sqrt(deltaMagnitudeSquared.toDouble()).toFloat()

        // Calculate angles using sine values
        val sinAlpha = (shipRadius / distance).coerceIn(-1.0f, 1.0f)
        val sinTheta = (crossProduct / sqrt(velocityMagnitudeSquared * deltaMagnitudeSquared)).coerceIn(-1.0f, 1.0f)

        // Approximate arcsine calculations
        val theta = sinTheta * (1 + 0.165f * sinTheta * sinTheta)
        val alpha = sinAlpha * (1 + 0.165f * sinAlpha * sinAlpha)

        // Compute the minimal deflection angle with the correct sign
        return (alpha - abs(theta)) * -sign(crossProduct)
    }
}
