package org.selkie.kol.impl.combat.activators

import activators.CombatActivator
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.util.IntervalUtil
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.setAlpha
import org.selkie.kol.impl.combat.ParticleController
import org.selkie.kol.impl.combat.ParticleData
import org.selkie.kol.impl.hullmods.CoronalCapacitor
import org.selkie.kol.plugins.KOL_ModPlugin
import java.awt.Color

class RadianceActivator(ship: ShipAPI?) : CombatActivator(ship) {
    private val PARTICLE_INTERVAL = IntervalUtil(0.05f, 0.1f)
    override fun canAssignKey(): Boolean {
        return false
    }

    override fun getDisplayText(): String {
        return "Celestial Radiance"
    }

    override fun getBaseInDuration(): Float {
        return 0f
    }

    override fun getBaseActiveDuration(): Float {
        return 0f
    }

    override fun getBaseOutDuration(): Float {
        return 0f
    }

    override fun getBaseCooldownDuration(): Float {
        return 1f
    }

    override fun shouldActivateAI(amount: Float): Boolean {
        return false
    }

    override fun canActivate(): Boolean {
        return if (ship.customData.containsKey(CoronalCapacitor.CAPACITY_FACTOR_KEY)) {
            ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float > 0.5f
        } else false
    }

    override fun onActivate() {
        //spawn an explosion with no effects
        val explosionDamage = 100f + ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float
        val explosionSpec = DamagingExplosionSpec(
            0.5f,
            DAMAGE_RANGE,
            DAMAGE_RANGE,
            explosionDamage,
            explosionDamage,
            CollisionClass.PROJECTILE_NO_FF,
            CollisionClass.PROJECTILE_FIGHTER,
            1f,
            2f,
            0.33f,
            0,
            Color(255, 200, 30, 0),
            null
        )
        explosionSpec.damageType = DamageType.HIGH_EXPLOSIVE
        explosionSpec.isShowGraphic = false
        Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, ship.location, false)
        if (KOL_ModPlugin.hasGraphicsLib) {
            val ripple = RippleDistortion(ship.location, Vector2f())
            ripple.size = DAMAGE_RANGE / 1.2f
            ripple.intensity = 22f
            ripple.frameRate = 60f
            ripple.fadeInSize(0.2f)
            ripple.fadeOutIntensity(1.5f)
            DistortionShader.addDistortion(ripple)

            val light = StandardLight(ship.location, Vector2f(), Vector2f(), null)
            light.size = DAMAGE_RANGE / 1.1f
            light.intensity = 2f
            light.setLifetime(0.66f)
            light.autoFadeOutTime = 1f
            light.setColor(Color(255, 125, 25, 255))
            LightShader.addLight(light)
        }
    }

    override fun advance(amount: Float) {
        if (canActivate()) {
            if (state == State.READY) {
                activate()
            }

            PARTICLE_INTERVAL.advance(amount)
            if (PARTICLE_INTERVAL.intervalElapsed()) {
                for (i in 0 until MathUtils.getRandomNumberInRange(6, 12)) {
                    val randomX = DAMAGE_RANGE * MathUtils.getRandomNumberInRange(-0.5f, 0.5f)
                    val randomY = DAMAGE_RANGE * MathUtils.getRandomNumberInRange(-0.5f, 0.5f)
                    val particlePos = Vector2f.add(ship.location, Vector2f(randomX, randomY), null)
                    val distance = MathUtils.getDistanceSquared(ship.location, particlePos)
                    val color = ParticleController.mergeColors(
                        Color(MathUtils.getRandomNumberInRange(255, 200), 200, 100, MathUtils.getRandomNumberInRange(75, 125)),
                        Color(MathUtils.getRandomNumberInRange(255, 200), 200, 100, MathUtils.getRandomNumberInRange(75, 125)),
                        (distance / DAMAGE_RANGE * DAMAGE_RANGE).coerceIn(0f..1f))
                    val direction = VectorUtils.getDirectionalVector(ship.location, particlePos)
                    val velocity = Vector2f(-direction.y, direction.x).scale(MathUtils.getRandomNumberInRange(35f, 45f)) as Vector2f

                    ParticleController.addParticle(
                        FlameParticleData(
                            x = ship.location.x + randomX,
                            y = ship.location.y + randomY,
                            xVel = velocity.x,
                            yVel = velocity.y,
                            angle = MathUtils.getRandomNumberInRange(0f, 360f),
                            aVel = MathUtils.getRandomNumberInRange(0f, 100f),
                            startingSize = 20f,
                            endSize = 5f,
                            ttl = 1f,
                            startingColor = color,
                            endColor = color.setAlpha(color.alpha / 2)
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val DAMAGE_RANGE = 600f
    }
}


class FlameParticleData(x: Float, y: Float, xVel: Float, yVel: Float, angle: Float, aVel: Float, ttl: Float, startingSize: Float, endSize: Float, startingColor: Color, endColor: Color)
    : ParticleData(
    sprite = Global.getSettings().getSprite("graphics/fx/cleaner_clouds00.png"),
    x = x,
    y = y,
    xVel = xVel,
    yVel = yVel,
    angle = angle,
    aVel = aVel,
    startingTime = Global.getCombatEngine().getTotalElapsedTime(false),
    ttl = ttl,
    startingSize = startingSize,
    endSize = endSize,
    startingColor = startingColor,
    endColor = endColor,
    spritesInRow = 2,
    spritesInColumn = 2)