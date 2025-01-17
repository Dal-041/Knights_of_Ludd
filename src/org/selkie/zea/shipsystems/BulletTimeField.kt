package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.entities.BallisticProjectile
import com.fs.starfarer.combat.entities.DamagingExplosion
import com.fs.starfarer.combat.entities.MovingRay
import com.fs.starfarer.combat.entities.PlasmaShot
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.plugins.MagicRenderPlugin
import org.selkie.kol.ReflectionUtils
import org.selkie.kol.Utils
import org.selkie.kol.combat.StarficzAIUtils
import org.selkie.kol.hullmods.LunariaArmorModuleVectoring.LunariaArmorModuleVectoringListener
import org.selkie.kol.plugins.KOL_ModPlugin
import org.selkie.zea.combat.WeaponRangeSetter
import org.selkie.zea.combat.WeaponRangeSetter.Companion.MAP_KEY
import org.selkie.zea.combat.WeaponRangeSetter.WeaponRangeModData
import java.awt.Color
import java.util.*
import kotlin.properties.Delegates

class BulletTimeField : BaseShipSystemScript() {
    companion object {
        const val MAX_SLOWDOWN: Float = 0.15f
        const val SLOWDOWN_RADIUS: Float = 400f
        const val CORE_RADIUS: Float = 100f
        const val MAX_DEFLECTION_ANGLE: Float = 70f
        const val ACTIVE_COLLISION_RADIUS: Float = 20f
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


    var init: Boolean = false
    var collisionRadius by Delegates.notNull<Float>()
    var shieldRadius by Delegates.notNull<Float>()
    var shieldArc by Delegates.notNull<Float>()
    val slowedProjectiles: MutableMap<DamagingProjectileAPI, DamagingProjectileInfo> = HashMap()
    var distortion: RippleDistortion? = null

    fun init(ship: ShipAPI) {
        if (!init) {
            shieldRadius = ship.shield.radius
            shieldArc = ship.shield.arc
            collisionRadius = ship.collisionRadius
            init = true
        }
    }

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val amount = Global.getCombatEngine().elapsedInLastFrame
        val ship = stats.entity as ShipAPI
        init(ship)

        applyStatChanges(stats, id, effectLevel)

        // make ship translucent and force shields off
        ship.alphaMult = Misc.interpolate(1f, 0.2f, effectLevel)

        // render the ship hitbox with a dot
        val shipCollisionCircle = Global.getSettings().getSprite("graphics/fx/circle64.png")
        shipCollisionCircle.color = Color.GREEN
        shipCollisionCircle.alphaMult = Misc.interpolate(0f, 0.8f, effectLevel)
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



        // check for all new threats around the ship, and give them some initial deflection
        val iterator = Global.getCombatEngine().allObjectGrid.getCheckIterator(
            stats.entity.location, (SLOWDOWN_RADIUS * 2.2f), (SLOWDOWN_RADIUS * 2.2f))
        val rand = Random()
        while (iterator.hasNext()) {
            val threat = iterator.next() as? DamagingProjectileAPI ?: continue
            if (threat.source === stats.entity) continue
            // Add threats in range to be slowed
            val threatDistance = MathUtils.getDistance(threat.location, stats.entity.location)
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
        slowedProjectiles.entries.removeAll{ it.key.isExpired }

        // update everything currently being slowed
        for (threat in slowedProjectiles.keys) {
            val threatInfo = slowedProjectiles[threat]
            threatInfo!!.adjustedElapsedTime += amount * MAX_SLOWDOWN
            val threatDistance = MathUtils.getDistance(threat.location, stats.entity.location)
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
        for(otherShip in Global.getCombatEngine().ships){
            if(!otherShip.hasListenerOfClass(WeaponRangeSetter::class.java)) otherShip.addListener(WeaponRangeSetter())
            for(weapon in otherShip.usableWeapons){
                if(!weapon.isBeam) continue
                WeaponRangeSetter.initMap(otherShip)
                val weaponRangeMap: HashMap<WeaponAPI, WeaponRangeModData> = otherShip.customData[MAP_KEY] as HashMap<WeaponAPI, WeaponRangeModData>
                val weaponRangeModData = weaponRangeMap[weapon]
                var baseRange: Float
                if (weaponRangeModData != null) {
                    weaponRangeModData.ModEnabled = false
                    baseRange = weapon.range
                    weaponRangeModData.ModEnabled = true
                } else{
                    baseRange = weapon.range
                }
                val fromPoint = weapon.getFirePoint(0)
                val toPoint = MathUtils.getPointOnCircumference(fromPoint, baseRange, weapon.currAngle)
                val intersect = StarficzAIUtils.intersectCircle(fromPoint, toPoint, ship.location, Misc.interpolate(10f, SLOWDOWN_RADIUS-20f, effectLevel))
                if (intersect == null) {
                    weaponRangeMap.remove(weapon)
                } else {
                    weapon.setForceFireOneFrame(true)
                    val intersectRange = intersect.two
                    if (weaponRangeModData != null) {
                        weaponRangeModData.BaseRange = baseRange
                        weaponRangeModData.TargetRange = intersectRange
                    } else {
                        weaponRangeMap[weapon] = WeaponRangeModData(weapon.range, 1f, true)
                    }
                }
            }
        }
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
    }

    fun applyStatChanges(stats: MutableShipStatsAPI, id: String, effectLevel: Float){
        stats.maxSpeed.modifyMult(id, Misc.interpolate(1f, 0.5f, effectLevel))
        stats.acceleration.modifyMult(id, Misc.interpolate(1f, 10f, effectLevel))
        stats.deceleration.modifyMult(id, Misc.interpolate(1f, 10f, effectLevel))
        stats.beamDamageTakenMult.modifyMult(id, Misc.interpolate(1f, 0f, effectLevel))

        //stats.entity.shield.arc = 360f
        //ship.getShield().setRadius(10f);
        stats.entity.shield.toggleOff()
        //stats.entity.shield.innerColor = Color(100, 255, 100)
        //stats.entity.shield.ringColor = Color(100, 255, 100)
        stats.entity.collisionRadius = Misc.interpolate(collisionRadius, ACTIVE_COLLISION_RADIUS, effectLevel)
    }

    fun revertStatChanges(stats: MutableShipStatsAPI, id: String){
        stats.maxSpeed.unmodify(id)
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        stats.beamDamageTakenMult.unmodify(id)

        //stats.entity.shield.arc = shieldArc
        //stats.entity.shield.radius = shieldRadius
        stats.entity.collisionRadius = collisionRadius
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State?, effectLevel: Float): StatusData? {
        return when(index){
            0 -> StatusData("top speed halved", true)
            1 -> StatusData("improved maneuverability", false)
            else -> null
        }
    }
}
