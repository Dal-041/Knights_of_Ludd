package org.selkie.kol.impl.combat.activators

import activators.CombatActivator
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.ZERO
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import org.selkie.kol.impl.combat.ParticleData
import org.selkie.kol.impl.hullmods.CoronalCapacitor
import org.selkie.kol.plugins.KOL_ModPlugin
import java.awt.Color
import java.io.IOException


class RadianceActivator(ship: ShipAPI?) : CombatActivator(ship) {
    private val PARTICLE_INTERVAL = IntervalUtil(0.05f, 0.1f)
    private var dummyMine: CombatEntityAPI? = null

    private var waveCnt = 0;
    private var rotation = 0f
    private var opacity = 0f
    private val spriteRing = Global.getSettings().getSprite("fx", "zea_ring_targeting")
    private val SPRITE_PATH = ""

    private val EFFECT_RANGE =
        DAMAGE_RANGE-(DAMAGE_RANGE*0.05f) //"For "player visual purposes" we reduce it by 5% See some random player theory crap."
    private val effectSize = Vector2f(EFFECT_RANGE*2, EFFECT_RANGE*2)

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
        return 0.1f
    }

    override fun shouldActivateAI(amount: Float): Boolean {
        return false
    }

    override fun canActivate(): Boolean {
        return if (ship.customData.containsKey(CoronalCapacitor.CAPACITY_FACTOR_KEY)) {
            ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float > 0.5f
        } else false
    }

    private fun getDamage(): Float {
        return (BASE_DAMAGE + (ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float) * DAMAGE_PER_BOOST) * baseCooldownDuration
    }

    override fun onActivate() {
        //spawn an explosion with no effects
        val explosionDamage = getDamage()
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
        explosionSpec.effect = RadianceOnHitEffect()
        Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, ship.location, false)

        if (KOL_ModPlugin.hasGraphicsLib) {
            if (waveCnt % 20 == 0) {
                val ripple = RippleDistortion(ship.location, Vector2f())
                ripple.size = DAMAGE_RANGE / 1.2f
                ripple.intensity = 5f
                ripple.frameRate = 60f
                ripple.fadeInSize(1f)
                ripple.fadeOutIntensity(2f)
                ripple.velocity = Vector2f(1f,1f)
                DistortionShader.addDistortion(ripple)

                val light = StandardLight(ship.location, Vector2f(), Vector2f(), null)
                light.size = DAMAGE_RANGE / 1.1f
                light.intensity = 2f
                light.setLifetime(0.66f)
                light.autoFadeOutTime = 1f
                light.setColor(Color(255, 125, 25, 255))
                LightShader.addLight(light)
            }

            waveCnt++
        }
    }

    override fun advance(amount: Float) {
        if (ship.isAlive) {
            val glowColorFull = Color(200,150,50, 200)
            val glowColorEmpty = Color(255,100,25, 0)
            val glowColorNow = Misc.interpolateColor(glowColorEmpty, glowColorFull, ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float)
            Global.getCombatEngine().addSmoothParticle(ship.location, ZERO, DAMAGE_RANGE*4f, 1f, 0.01f, glowColorNow)
            if (!Global.getCombatEngine().isUIShowingHUD || Global.getCombatEngine().isUIShowingDialog || Global.getCombatEngine().combatUI.isShowingCommandUI) {
                return;
            } else {
                if (!ship.isHulk && !ship.isPiece && ship.isAlive) {
                    MagicRender.singleframe(spriteRing, ship.location, effectSize, 0f, glowColorNow, true)
                }
            }
        };

        if (canActivate()) {
            if (dummyMine == null) {
                dummyMine = Global.getCombatEngine().spawnProjectile(ship, null, "zea_radiance_dummy_wpn", ship.location, 0f, Vector2f())
            } else {
                dummyMine!!.location.set(ship.location)
            }

            if (state == State.READY) {
                activate()
            }

            PARTICLE_INTERVAL.advance(amount)
            if (PARTICLE_INTERVAL.intervalElapsed()) {
                /*
                for (i in 0 until MathUtils.getRandomNumberInRange(12, 24)) {
                    val randomPos = MathUtils.getRandomPointInCircle(ship.location, DAMAGE_RANGE)
                    val distance = MathUtils.getDistanceSquared(ship.location, randomPos) * 1.5f
                    val color = ParticleController.mergeColors(
                        Color(
                            MathUtils.getRandomNumberInRange(200, 225),
                            MathUtils.getRandomNumberInRange(160, 195),
                            95,
                            MathUtils.getRandomNumberInRange(175, 255)
                        ),
                        Color(
                            MathUtils.getRandomNumberInRange(225, 255),
                            MathUtils.getRandomNumberInRange(100, 130),
                            50,
                            MathUtils.getRandomNumberInRange(175, 255)
                        ),
                        (distance / (DAMAGE_RANGE * DAMAGE_RANGE)).coerceIn(0f..1f)
                    )
                    val direction = VectorUtils.getDirectionalVector(ship.location, randomPos)
                    val velocity = Vector2f(-direction.y, direction.x).scale(
                        MathUtils.getRandomNumberInRange(
                            35f,
                            45f
                        )
                    ) as Vector2f

                    ParticleController.addParticle(
                        FlameParticleData(
                            x = randomPos.x,
                            y = randomPos.y,
                            xVel = velocity.x,
                            yVel = velocity.y,
                            angle = MathUtils.getRandomNumberInRange(0f, 360f),
                            aVel = MathUtils.getRandomNumberInRange(0f, 100f),
                            startingSize = 200f,
                            endSize = 80f,
                            ttl = 1f,
                            startingColor = color,
                            endColor = color.setAlpha(0)
                        )
                    )
                }

                for (i in 0 until MathUtils.getRandomNumberInRange(6, 12)) {
                    val randomPos = MathUtils.getPointOnCircumference(
                        ship.location,
                        DAMAGE_RANGE,
                        MathUtils.getRandomNumberInRange(20f, 60f) * i
                    )
                    val distance = MathUtils.getDistanceSquared(ship.location, randomPos)
                    val color = Color(120, 60, 10, MathUtils.getRandomNumberInRange(175, 255))
                    val direction = VectorUtils.getDirectionalVector(ship.location, randomPos)
                    val velocity = Vector2f(-direction.y, direction.x).scale(
                        MathUtils.getRandomNumberInRange(80f, 120f)
                    ) as Vector2f

                    ParticleController.addParticle(
                        FlameParticleData(
                            x = randomPos.x,
                            y = randomPos.y,
                            xVel = velocity.x,
                            yVel = velocity.y,
                            angle = MathUtils.getRandomNumberInRange(0f, 360f),
                            aVel = MathUtils.getRandomNumberInRange(0f, 100f),
                            startingSize = 120f,
                            endSize = 200f,
                            ttl = 1f,
                            startingColor = color,
                            endColor = color.setAlpha(0)
                        )
                    )
                }

                 */
            }
        } else {
            if (dummyMine != null) {
                Global.getCombatEngine().removeEntity(dummyMine)
            }
        }
    }

    override fun init() {
        super.init()
        if (spriteRing == null) {
            // Load sprite if it hasn't been loaded yet - not needed if you add it to settings.json
            try {
                Global.getSettings().loadTexture(SPRITE_PATH)
            } catch (ex: IOException) {
                throw RuntimeException("Failed to load sprite '$SPRITE_PATH'!", ex)
            }
        }
    }

    override fun getBarFill(): Float {
        return 1f
    }

    private inner class RadianceOnHitEffect : OnHitEffectPlugin {
        override fun onHit(
            projectile: DamagingProjectileAPI?,
            target: CombatEntityAPI,
            point: Vector2f,
            shieldHit: Boolean,
            damageResult: ApplyDamageResultAPI,
            engine: CombatEngineAPI
        ) {
            damageResult.empDamage = getDamage()
        }
    }

    companion object {
        private const val DAMAGE_RANGE = 600f
        private const val BASE_DAMAGE = 100f
        private const val DAMAGE_PER_BOOST = 1f
    }
}


class FlameParticleData(
    x: Float,
    y: Float,
    xVel: Float,
    yVel: Float,
    angle: Float,
    aVel: Float,
    ttl: Float,
    startingSize: Float,
    endSize: Float,
    startingColor: Color,
    endColor: Color
) : ParticleData(
    sprite = Global.getSettings().getSprite("graphics/fx/nebula_colorless.png"),
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
    spritesInRow = 4,
    spritesInColumn = 4
)