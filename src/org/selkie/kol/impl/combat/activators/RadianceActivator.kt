package org.selkie.kol.impl.combat.activators

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.ZERO
import com.fs.starfarer.combat.entities.BallisticProjectile
import com.fs.starfarer.combat.entities.PlasmaShot
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.setAlpha
import org.magiclib.subsystems.MagicSubsystem
import org.magiclib.util.MagicRender
import org.selkie.kol.combat.ParticleData
import org.selkie.kol.impl.hullmods.CoronalCapacitor
import org.selkie.kol.plugins.KOL_ModPlugin
import java.awt.Color


class RadianceActivator(ship: ShipAPI?) : MagicSubsystem(ship) {
    private val PARTICLE_INTERVAL = IntervalUtil(1f, 1f)
    private var dummyMine: CombatEntityAPI? = null

    private val spriteRing = Global.getSettings().getSprite("fx", "zea_ring_targeting")

    private val glowColorFull = Color(200, 150, 50, 200)
    private val glowColorEmpty = Color(255, 100, 25, 0)

    private val EFFECT_RANGE =
        DAMAGE_RANGE - (DAMAGE_RANGE * 0.05f) //"For "player visual purposes" we reduce it by 5% See some random player theory crap."
    private val effectSize = Vector2f(EFFECT_RANGE * 2, EFFECT_RANGE * 2)

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

        CombatUtils.getEntitiesWithinRange(ship.location, DAMAGE_RANGE)
            .filterNot { it.owner == ship.owner || it is BallisticProjectile || it is PlasmaShot }
            .map { it to 1f - MathUtils.getDistance(ship, it).coerceAtLeast(ship.collisionRadius) / DAMAGE_RANGE }
            .forEach { (target, rangeRatio) ->
                val effectiveDamage = rangeRatio * explosionDamage
                val bypassShields = target.shield == null || target.shield.activeArc < 360f
                Global.getCombatEngine().applyDamage(
                    target,
                    target.location,
                    effectiveDamage,
                    DamageType.HIGH_EXPLOSIVE,
                    effectiveDamage,
                    bypassShields,
                    false,
                    ship,
                    false
                )
            }
    }

    override fun advance(amount: Float, isPaused: Boolean) {
        if (isPaused) return
        if (ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] == null) return; //Hopefully just one frame

        val glowColorNow = Misc.interpolateColor(
            glowColorEmpty,
            glowColorFull,
            ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float
        )
        if (!Global.getCombatEngine().isUIShowingHUD || Global.getCombatEngine().isUIShowingDialog || Global.getCombatEngine().combatUI.isShowingCommandUI) {
            return;
        } else {
            if (!ship.isHulk && !ship.isPiece && ship.isAlive) {
                MagicRender.singleframe(spriteRing, ship.location, effectSize, 0f, glowColorNow, true)
            }
        }

        if (ship.isAlive) {
            Global.getCombatEngine()
                .addSmoothParticle(
                    ship.location, ZERO, DAMAGE_RANGE * 4f, 0.25f, 0.01f * amount, glowColorNow.setAlpha(
                        (glowColorNow.alpha * 0.7f).toInt()
                    )
                )

            if (KOL_ModPlugin.hasGraphicsLib) {
                PARTICLE_INTERVAL.advance(amount)
                if (PARTICLE_INTERVAL.intervalElapsed()) {
                    val light = StandardLight(Vector2f(), Vector2f(), Vector2f(), null)
                    light.size = DAMAGE_RANGE / 1.1f
                    light.intensity = 0.33f
                    light.setLifetime(0.66f)
                    light.autoFadeOutTime = 1f
                    light.setColor(Color(255, 125, 25))
                    light.location = ship.location
                    LightShader.addLight(light)

                    val ripple = RippleDistortion(Vector2f(), Vector2f())
                    ripple.size = DAMAGE_RANGE / 1.075f
                    ripple.intensity = 5f
                    ripple.frameRate = 60f
                    ripple.fadeInSize(1f)
                    ripple.fadeOutIntensity(2f)
                    ripple.velocity = Vector2f(1f, 1f)
                    ripple.location = ship.location
                    DistortionShader.addDistortion(ripple)
                }
            }
        }

        if (canActivate()) {
            if (dummyMine == null) {
                dummyMine = Global.getCombatEngine()
                    .spawnProjectile(ship, null, "zea_radiance_dummy_wpn", ship.location, 0f, Vector2f())
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

    override fun getStateText(): String {
        return ""
    }

    override fun getBarFill(): Float {
        return ((ship.customData[CoronalCapacitor.CAPACITY_FACTOR_KEY] as Float? ?: 0f) / 0.5f).coerceIn(0f..1f)
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