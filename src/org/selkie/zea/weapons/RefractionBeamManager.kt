package org.selkie.zea.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.*
import kotlin.properties.Delegates

class RefractionBeamManager : EveryFrameWeaponEffectPlugin {
    lateinit var primaryBeamDrone: ShipAPI
    lateinit var primaryOverfireBeamDrone: ShipAPI
    var primaryWeaponRange by Delegates.notNull<Float>()
    var overfireWeaponRange by Delegates.notNull<Float>()
    var init = false
    var currentBeamLength = 0f
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        val ship = weapon.ship
        if(!engine.isEntityInPlay(ship) || (ship.hullSize == ShipAPI.HullSize.FIGHTER && ship.isDrone)) return

        if(!init){
            primaryBeamDrone = org.selkie.zea.combat.utils.makeNewRedirectionDrone(ship, weapon)
            primaryOverfireBeamDrone = org.selkie.zea.combat.utils.makeNewRedirectionDrone(ship, "zea_refraction_overfire")
            overfireWeaponRange = primaryOverfireBeamDrone.allWeapons[0].range
            primaryWeaponRange = weapon.range
            init = true
        }

        // make sure original weapon does 0 damage, and make it invisible
        weapon.damage.modifier.modifyMult("RefractionBeam", 0f)
        for(beam in weapon.beams){
            beam.coreColor =  Color(0,0,0,0)
            beam.fringeColor =  Color(0,0,0,0)
        }

        // move the drones
        primaryBeamDrone.facing = weapon.currAngle
        primaryOverfireBeamDrone.facing = weapon.currAngle

        val weaponFirePointToBeamIntersect = Vector2f.sub(weapon.getFirePoint(0), primaryOverfireBeamDrone.allWeapons[0].getFirePoint(0), null)
        primaryOverfireBeamDrone.location.set(Vector2f.add(primaryOverfireBeamDrone.location, weaponFirePointToBeamIntersect, null))
        primaryBeamDrone.location.set(weapon.location)

        val crossOverRadius = ship.shield?.radius ?: ship.collisionRadius
        // sync the weapon
        var overfireBeamRangeReached = false
        var maxBeamDistance = OverfireBeamRange(weapon.getFirePoint(0), ship.shieldCenterEvenIfNoShield, weapon.currAngle, crossOverRadius+15f)
        val overfireBeamTo = Vector2f()
        primaryOverfireBeamDrone.allWeapons[0].apply {
            setForceFireOneFrame(weapon.isFiring || isFiring)
            setFacing(primaryOverfireBeamDrone.facing)
            updateBeamFromPoints()
            beams.getOrNull(0)?.let { droneBeam ->
                //droneBeam.coreColor = Color.RED
                //droneBeam.fringeColor = Color.RED
                val currentRange = MathUtils.getDistance(droneBeam.from, droneBeam.to)
                if(currentRange > maxBeamDistance*0.99f){
                    overfireBeamRangeReached = true
                    maxBeamDistance = maxOf(currentRange, maxBeamDistance)
                    primaryOverfireBeamDrone.mutableStats.beamWeaponRangeBonus.modifyMult("RefractionBeam", maxBeamDistance/overfireWeaponRange)

                }
                if(isFiring) currentBeamLength = maxOf(currentBeamLength, currentRange)
            }
            if(!isFiring) primaryOverfireBeamDrone.mutableStats.beamWeaponRangeBonus.modifyMult("RefractionBeam", 1f)
        }
        // shrink main beam by a bit to cover fadeout
        Vector2f.add(weapon.getFirePoint(0), Misc.getUnitVectorAtDegreeAngle(weapon.currAngle).scale(maxBeamDistance-10f) as Vector2f, overfireBeamTo)

        if(overfireBeamRangeReached){
            val beamEndPointToBeamIntersect = Vector2f.sub(overfireBeamTo, primaryBeamDrone.allWeapons[0].getFirePoint(0), null)
            primaryBeamDrone.location.set(Vector2f.add(primaryBeamDrone.location, beamEndPointToBeamIntersect, null))

            primaryBeamDrone.allWeapons[0].apply {
                setForceFireOneFrame(weapon.isFiring || isFiring)
                setFacing(primaryBeamDrone.facing)
                updateBeamFromPoints()
                primaryBeamDrone.mutableStats.beamWeaponRangeBonus.modifyMult("RefractionBeam", 1-maxBeamDistance/primaryWeaponRange)
                if(isFiring){
                    beams.getOrNull(0)?.let { droneBeam ->
                        val currentRange = MathUtils.getDistance(weapon.getFirePoint(0), droneBeam.to)
                        currentBeamLength = maxOf(currentBeamLength, currentRange)
                    }
                }
            }
        }
    }

    fun OverfireBeamRange(weaponLocation: Vector2f, shieldCenter: Vector2f, weaponAngle: Float, shieldRadius: Float): Float {
        // Compute vector from shield center to weapon location.
        val p = Vector2f(weaponLocation.x - shieldCenter.x, weaponLocation.y - shieldCenter.y)
        // If the weapon is already outside (or exactly on) the shield, no distance is needed.
        if (p.lengthSquared() >= shieldRadius * shieldRadius) return 0f

        // Compute the direction vector from the weapon's angle.
        val d = Vector2f(cos(Math.toRadians(weaponAngle.toDouble()).toFloat()), sin(Math.toRadians(weaponAngle.toDouble())).toFloat())

        // Compute the dot product (p • d)
        val dot = Vector2f.dot(p, d)

        // Compute the discriminant of the quadratic.
        val pLengthSquared = p.lengthSquared()
        val discriminant = dot * dot - (pLengthSquared - shieldRadius * shieldRadius)

        // Should not happen if weapon is inside the shield.
        if (discriminant < 0f) return 0f

        // The positive solution is the distance to exit the shield.
        return -dot + sqrt(discriminant.toDouble()).toFloat()
    }
}