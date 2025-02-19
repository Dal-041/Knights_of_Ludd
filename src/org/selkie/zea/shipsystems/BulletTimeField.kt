package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.entities.*
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.getCrossProduct
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getUnitVectorAtDegreeAngle
import org.magiclib.plugins.MagicRenderPlugin
import org.selkie.kol.ReflectionUtils
import org.selkie.kol.Utils
import org.selkie.kol.combat.StarficzAIUtils
import org.selkie.kol.plugins.KOL_ModPlugin
import java.awt.Color
import java.awt.geom.Line2D
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.properties.Delegates

class BulletTimeField : BaseShipSystemScript() {
    companion object {
        const val MAX_SLOWDOWN = 0.15f
        const val SLOWDOWN_RADIUS = 400f
        const val BEAM_FOCUS_RADIUS = 200f
        const val CORE_RADIUS = 100f
        const val MAX_DEFLECTION_ANGLE = 70f
        const val MAX_ACCEL_MULT = 10f
        const val ACTIVE_COLLISION_RADIUS = 20f
        const val ACTIVE_COLLISION_RADIUS_ALPHA_MULT = 0.8f // dot alpha
        const val ACTIVE_SHIP_ALPHA_MULT = 0.2f // ship alpha
        const val ACTIVE_STRAFE_SPEED_MULT = 0.5f // 50% speed when strafing
    }

    object RayExtenderFields {
        var START_POS: String? = null
        var END_POS: String? = null
        var MAX_PULSE_LENGTH: String? = null
        var PROJ_SPEED: String? = null
        var MAX_RANGE: String? = null
        var FADE_TIME: String? = null
        var SHIP_SPEED: String? = null
    }

    object TrailExtenderFields {
        var TRAIL_POS: String? = null
        var TRAIL_FACING_VEC: String? = null
        var ELAPSED_TIME: String? = null
        var TRAIL_LENGTH: String? = null
        var TRAIL_SPEED: String? = null
        var MAX_LIFETIME: String? = null
        var FADE_TIME: String? = null
    }

    class DamagingProjectileInfo internal constructor(threat: DamagingProjectileAPI) {
        var adjustedElapsedTime: Float
        var initialSpeed: Float = 0f

        init {
            val amount = Global.getCombatEngine().elapsedInLastFrame
            adjustedElapsedTime = threat.elapsed + (MAX_SLOWDOWN * amount)
            if(threat is BallisticProjectile){
                val trailExtender = ReflectionUtils.get("trailExtender", threat)
                // init the field strings, if they are null
                if(TrailExtenderFields.ELAPSED_TIME == null) initTrailExtenderFields(trailExtender!!)
                initialSpeed = threat.velocity.length()
            }
            else if(threat is PlasmaShot) {
                initialSpeed = threat.velocity.length()
            } else if (threat is MovingRay) {
                val rayExtender = ReflectionUtils.get("rayExtender", threat)

                // init the field strings, if they are null
                if(RayExtenderFields.PROJ_SPEED == null) initRayExtenderFields(rayExtender!!)

                initialSpeed = ReflectionUtils.get(RayExtenderFields.PROJ_SPEED!!, rayExtender!!) as Float // bullet speed
            } else if (threat is MissileAPI) {
                initialSpeed = threat.maxSpeed
            }
        }

        private fun initTrailExtenderFields(trailExtender: Any){
            val knownExtender = ReflectionUtils.createNewInstanceFromExisting(
                trailExtender, Vector2f(0f, 0f), Vector2f(1f, 1f), 2f, 3f, 4f, 5f)
            val advanceMethod = ReflectionUtils.getMethodWithArguments(
                knownExtender, arrayOf(Float::class.java, Vector2f::class.java, Boolean::class.java))
            ReflectionUtils.invoke(advanceMethod!!, knownExtender, 1f, Vector2f(1f, 1f), false)

            val vectorFields = ReflectionUtils.getFieldsOfType(knownExtender, Vector2f::class.java)
            val floatFields = ReflectionUtils.getFieldsOfType(knownExtender, Float::class.java)

            for (field in vectorFields) {
                val sampleValue = ReflectionUtils.get(field, knownExtender) as Vector2f
                if (sampleValue.x == 0f) TrailExtenderFields.TRAIL_POS = field
                if (sampleValue.x == 1f) TrailExtenderFields.TRAIL_FACING_VEC = field
            }

            for (field in floatFields) {
                val sampleValue = ReflectionUtils.get(field, knownExtender) as Float
                if (sampleValue == 1f) TrailExtenderFields.ELAPSED_TIME = field
                if (sampleValue == 2f) TrailExtenderFields.TRAIL_LENGTH = field
                if (sampleValue == 3f) TrailExtenderFields.TRAIL_SPEED = field
                if (sampleValue == 4f) TrailExtenderFields.MAX_LIFETIME = field
                if (sampleValue == 5f) TrailExtenderFields.FADE_TIME = field
            }
        }

        private fun initRayExtenderFields(rayExtender: Any){
            val knownExtender = ReflectionUtils.createNewInstanceFromExisting(
                rayExtender, Vector2f(0f, 0f),  Vector2f(1f, 1f), 1f, 2f, 3f, 4f, Vector2f(2f, 2f))
            val vectorFields = ReflectionUtils.getFieldsOfType(knownExtender, Vector2f::class.java)
            val floatFields = ReflectionUtils.getFieldsOfType(knownExtender, Float::class.java)

            for(field in vectorFields){
                val sampleValue = ReflectionUtils.get(field, knownExtender) as Vector2f
                if(sampleValue.x == 0f) RayExtenderFields.START_POS = field
                if(sampleValue.x == 1f) RayExtenderFields.END_POS = field
                if(sampleValue.x == 2f) RayExtenderFields.SHIP_SPEED = field
            }

            for(field in floatFields){
                val sampleValue = ReflectionUtils.get(field, knownExtender) as Float
                if(sampleValue == 1f) RayExtenderFields.MAX_PULSE_LENGTH = field
                if(sampleValue == 2f) RayExtenderFields.PROJ_SPEED = field
                if(sampleValue == 3f) RayExtenderFields.MAX_RANGE = field
                if(sampleValue == 4f) RayExtenderFields.FADE_TIME = field
            }
        }
    }

    val rand = Random()
    var init: Boolean = false
    var collisionRadius by Delegates.notNull<Float>()
    var shieldRadius by Delegates.notNull<Float>()
    var shieldArc by Delegates.notNull<Float>()
    val slowedProjectiles: MutableMap<DamagingProjectileAPI, DamagingProjectileInfo> = HashMap()
    var distortion: RippleDistortion? = null
    val activeBeams: MutableMap<BeamAPI, ShipAPI> = mutableMapOf()
    var shieldOnBeforeSystem = false
    var currentSpeedMult = 1f;

    fun init(ship: ShipAPI) {
        if (!init) {
            shieldRadius = ship.shield.radius
            shieldArc = ship.shield.arc
            collisionRadius = ship.collisionRadius
            init = true
        }
    }

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {

        val ship = stats.entity as ShipAPI
        init(ship)
        shieldOnBeforeSystem = shieldOnBeforeSystem || ship.shield?.isOn == true

        val strafeSpeed = abs(ship.velocity.getCrossProduct(ship.facing.getUnitVectorAtDegreeAngle()))
        currentSpeedMult = min((ship.maxSpeed*ACTIVE_STRAFE_SPEED_MULT) / strafeSpeed, 1f)
        applyStatChanges(stats, id, effectLevel)

        // make ship translucent and force shields off
        ship.alphaMult = Misc.interpolate(1f, ACTIVE_SHIP_ALPHA_MULT, effectLevel)

        // render the ship hitbox with a dot
        val shipCollisionCircle = Global.getSettings().getSprite("graphics/fx/circle64.png")
        shipCollisionCircle.color = Color.GREEN
        shipCollisionCircle.alphaMult = Misc.interpolate(0f, ACTIVE_COLLISION_RADIUS_ALPHA_MULT, effectLevel)
        val hitboxSize = Misc.interpolate(collisionRadius, ACTIVE_COLLISION_RADIUS, effectLevel)
        shipCollisionCircle.setSize(hitboxSize, hitboxSize)
        MagicRenderPlugin.addSingleframe(shipCollisionCircle, ship.location, CombatEngineLayers.BELOW_INDICATORS_LAYER)

        // render fixed distortion
        if (KOL_ModPlugin.hasGraphicsLib) {
            if(distortion != null){
                DistortionShader.removeDistortion(distortion)
            }
            distortion = RippleDistortion(ship.location, Vector2f())
            distortion!!.size = Misc.interpolate(10f, SLOWDOWN_RADIUS, effectLevel)
            distortion!!.intensity = 50f
            distortion!!.currentFrame = 50f
            distortion!!.frameRate = 0f
            DistortionShader.addDistortion(distortion)
        }

        slowdownProjectiles(ship, effectLevel)
        cutBeamsShort(ship, effectLevel)
    }

    fun slowdownProjectiles(ship: ShipAPI, effectLevel: Float){
        // check for all new threats around the ship, and give them some initial deflection
        val iterator = Global.getCombatEngine().allObjectGrid.getCheckIterator(
                ship.location, (SLOWDOWN_RADIUS * 2.2f), (SLOWDOWN_RADIUS * 2.2f))

        while (iterator.hasNext()) {
            val threat = iterator.next() as? DamagingProjectileAPI ?: continue
            if (threat.source === ship) continue
            // Add threats in range to be slowed
            val threatDistance = MathUtils.getDistance(threat.location, ship.location)
            if (threatDistance < Misc.interpolate(10f, SLOWDOWN_RADIUS, effectLevel)) {
                if (!slowedProjectiles.containsKey(threat) && threat !is DamagingExplosion) {
                    val threatInfo = DamagingProjectileInfo(threat)
                    slowedProjectiles[threat] = threatInfo

                    // spread bullets around
                    val angle = (rand.nextGaussian() * MAX_DEFLECTION_ANGLE / 3).toFloat() // not technically max, but eh good enough
                    // slowdown more as things get closer
                    val slowdownMult = Utils.linMap(MAX_SLOWDOWN, 1f, CORE_RADIUS, SLOWDOWN_RADIUS, threatDistance)
                    // anything that's an entity that follows velocity can be directly changed
                    if (threat is BallisticProjectile || threat is MissileAPI || threat is PlasmaShot) {
                        VectorUtils.rotate(threat.velocity, angle)
                        VectorUtils.resize(threat.velocity, threatInfo.initialSpeed * slowdownMult)
                    } else if (threat is MovingRay) { // raycasts need reflection
                        val rayExtender = ReflectionUtils.get("rayExtender", threat)
                        ReflectionUtils.set(RayExtenderFields.PROJ_SPEED!!, rayExtender!!, threatInfo.initialSpeed * slowdownMult)
                    }
                    threat.facing += angle
                }
            } else { // remove threats that have gone out of range
                if (slowedProjectiles.containsKey(threat)) {
                    resetDamagingProjectile(threat)
                    slowedProjectiles.remove(threat)
                }
            }
        }

        // Stop tracking expired threats
        slowedProjectiles.entries.removeAll{ it.key.isExpired || (it.key as? BaseEntity)?.wasRemoved() ?: false }

        // update everything currently being slowed
        for (threat in slowedProjectiles.keys) {
            val threatInfo = slowedProjectiles[threat]
            threatInfo!!.adjustedElapsedTime += Global.getCombatEngine().elapsedInLastFrame * MAX_SLOWDOWN
            val threatDistance = MathUtils.getDistance(threat.location, ship.location)
            val slowdownMult = Utils.linMap(MAX_SLOWDOWN, 1f, CORE_RADIUS, SLOWDOWN_RADIUS, threatDistance)

            // draw a red collision circle for all the projectiles
            if (threat !is MovingRay) {
                val threatCollisionCircle = Global.getSettings().getSprite("graphics/fx/circle64.png")
                threatCollisionCircle.color = Color.red
                threatCollisionCircle.alphaMult = 0.7f
                threatCollisionCircle.setSize(threat.collisionRadius, threat.collisionRadius)
                MagicRenderPlugin.addSingleframe(threatCollisionCircle, threat.location, CombatEngineLayers.BELOW_INDICATORS_LAYER)
            }

            // draw the threat collision path as a line
            /*
            val threatHitLine = Global.getSettings().getSprite("graphics/fx/beam_laser_core.png")
            threatHitLine.color = Color.red.darker()
            threatHitLine.alphaMult = 0.7f
            threatHitLine.setSize(500f, if(threat is MovingRay) threat.collisionRadius/2 else threat.collisionRadius*2)
            threatHitLine.angle = threat.facing
            MagicRenderPlugin.addSingleframe(threatHitLine, threat.location, CombatEngineLayers.BELOW_INDICATORS_LAYER)
            */

            // all slowed objects need to have their internal timers also slowed down, otherwise they would expire early before their max range
            // also make sure to constantly set their speeds/velocity back to the slower value (for things under constant acceleration)
            if (threat is BallisticProjectile) {
                val trailExtender = ReflectionUtils.get("trailExtender", threat)
                ReflectionUtils.set(TrailExtenderFields.ELAPSED_TIME!!, trailExtender!!, threatInfo!!.adjustedElapsedTime) //elapsed time
                ReflectionUtils.set("elapsed", threat, threatInfo.adjustedElapsedTime)
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult)
            } else if (threat is MovingRay) {
                ReflectionUtils.set("elapsed", threat, threatInfo!!.adjustedElapsedTime)
                val rayExtender = ReflectionUtils.get("rayExtender", threat)
                ReflectionUtils.set(RayExtenderFields.PROJ_SPEED!!, rayExtender!!, threatInfo.initialSpeed * slowdownMult)
            } else if (threat is MissileAPI) {
                ReflectionUtils.set("elapsed", threat, threatInfo!!.adjustedElapsedTime)
                threat.flightTime = threatInfo.adjustedElapsedTime
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult)
            } else if (threat is PlasmaShot) {
                ReflectionUtils.set("flightTime", threat, threatInfo!!.adjustedElapsedTime)
                VectorUtils.resize(threat.getVelocity(), threatInfo.initialSpeed * slowdownMult)
            }
        }
    }

    fun cutBeamsShort(ship: ShipAPI, effectLevel: Float){
        val currentReflectedBeams: MutableList<BeamAPI> = mutableListOf()
        for(otherShip in Global.getCombatEngine().ships){
            if(otherShip.collisionClass == CollisionClass.NONE) continue
            for(weapon in otherShip.usableWeapons){
                if(!weapon.isBeam) continue
                for(beam in weapon.beams){
                    val reflected = interceptBeam(ship, otherShip, effectLevel, beam)
                    if (reflected) currentReflectedBeams.add(beam)
                }
            }
        }
        activeBeams.keys.retainAll {
            if(it !in currentReflectedBeams) {
                Global.getCombatEngine().removeEntity(activeBeams[it]!!)
            }
            it in currentReflectedBeams
        }
    }

    fun interceptBeam(ship: ShipAPI, beamShipAPI: ShipAPI, effectLevel: Float, beam: BeamAPI): Boolean {
        val slowdownRadius = Misc.interpolate(10f, SLOWDOWN_RADIUS, effectLevel)
        val travelDistance = VectorUtils.getDirectionalVector(beam.from, beam.to)
        travelDistance.scale(Global.getCombatEngine().elapsedInLastFrame * (beam.weapon.spec as BeamWeaponSpecAPI).beamSpeed)
        val futureBeamTo = Vector2f.add(beam.to, travelDistance, null)
        // return if beam starts from inside the field
        if(MathUtils.getDistanceSquared(ship.location, beam.from) < slowdownRadius*slowdownRadius) return false
        val closestDistanceSq = Line2D.Float.ptSegDistSq(beam.from.x.toDouble(), beam.from.y.toDouble(),
                futureBeamTo.x.toDouble(), futureBeamTo.y.toDouble(), ship.location.x.toDouble(), ship.location.y.toDouble())

        // return if beam never crosses the field
        if(closestDistanceSq > slowdownRadius*slowdownRadius) return false

        val intersect = StarficzAIUtils.intersectCircle(beam.from, futureBeamTo, ship.location, slowdownRadius)
        if(intersect == null) return false

        val beamSetPoint = Vector2f.sub(intersect.first, travelDistance, null)

        beam.to.set(beamSetPoint)

        if(beam is BeamWeaponRay) beam.forceShowGlow()

        val redirectionDrone = activeBeams.getOrPut(beam){
            org.selkie.zea.combat.utils.makeNewRedirectionDrone(beamShipAPI, beam.weapon)
        }

        redirectionDrone.owner = beamShipAPI.owner
        redirectionDrone.facing = VectorUtils.getAngle(intersect.first, MathUtils.getPointOnCircumference(ship.location, BEAM_FOCUS_RADIUS, ship.facing))

        val weaponFirePointToBeamIntersect = Vector2f.sub(intersect.first, redirectionDrone.allWeapons[0].getFirePoint(0), null)
        redirectionDrone.location.set(Vector2f.add(redirectionDrone.location, weaponFirePointToBeamIntersect, null))

        redirectionDrone.velocity.set(ship.velocity)

        val rangeMod = 1f-(MathUtils.getDistance(beam.from, intersect.first)/beam.weapon.range)
        redirectionDrone.mutableStats.beamWeaponRangeBonus.modifyMult("dem", rangeMod)
        for(weapon in redirectionDrone.allWeapons){
            weapon.setForceFireOneFrame(true)
            weapon.setFacing(redirectionDrone.facing)
            weapon.updateBeamFromPoints()
            for(droneBeam in weapon.beams){
                if(droneBeam is CombatEntityAPI) droneBeam.collisionClass = CollisionClass.RAY_FIGHTER
            }
        }
        return true
    }

    fun resetDamagingProjectile(threat: DamagingProjectileAPI) {
        // reset velocities if possible
        if (threat is BallisticProjectile || threat is MissileAPI || threat is PlasmaShot) {
            VectorUtils.resize(threat.velocity, slowedProjectiles[threat]!!.initialSpeed)
        } else if (threat is MovingRay) {
            val rayExtender = ReflectionUtils.get("rayExtender", threat)
            ReflectionUtils.set(RayExtenderFields.PROJ_SPEED!!, rayExtender!!, slowedProjectiles[threat]!!.initialSpeed)
        }

        VectorUtils.resize(threat.velocity, slowedProjectiles[threat]!!.initialSpeed)
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        init(stats.entity as ShipAPI)

        // reset the mutable stats
        revertStatChanges(stats, id)

        // make the ship opaque again
        (stats.entity as ShipAPI).alphaMult = 1f

        // reset the slowed projectiles
        for (threat in slowedProjectiles.keys){ resetDamagingProjectile(threat) }
        slowedProjectiles.clear()

        // remove distortion
        if(distortion != null){
            DistortionShader.removeDistortion(distortion)
        }

        if(shieldOnBeforeSystem){
            stats.entity.shield.toggleOn()
            shieldOnBeforeSystem = false
        }
    }

    fun applyStatChanges(stats: MutableShipStatsAPI, id: String, effectLevel: Float){

        stats.maxSpeed.modifyMult(id, Misc.interpolate(1f, currentSpeedMult, effectLevel))
        stats.acceleration.modifyMult(id, Misc.interpolate(1f, MAX_ACCEL_MULT, effectLevel))
        stats.deceleration.modifyMult(id, Misc.interpolate(1f, MAX_ACCEL_MULT, effectLevel))
        stats.engineDamageTakenMult.modifyMult(id, 0f)
        //stats.entity.shield.arc = 360f
        //ship.getShield().setRadius(10f);
        stats.entity.shield.toggleOff()
        //stats.entity.shield.innerColor = Color(100, 255, 100)
        //stats.entity.shield.ringColor = Color(100, 255, 100)
        stats.entity.collisionRadius = Misc.interpolate(collisionRadius, ACTIVE_COLLISION_RADIUS, effectLevel)
        stats.entity.exactBounds
    }

    fun revertStatChanges(stats: MutableShipStatsAPI, id: String){
        stats.maxSpeed.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        stats.beamDamageTakenMult.unmodify(id)
        stats.engineDamageTakenMult.unmodify(id)

        //stats.entity.shield.arc = shieldArc
        //stats.entity.shield.radius = shieldRadius
        stats.entity.collisionRadius = collisionRadius
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State?, effectLevel: Float): StatusData? {
        return when(index){
            0 -> StatusData("strafe speed halved", true)
            1 -> StatusData("instant maneuverability", false)
            else -> null
        }
    }
}
