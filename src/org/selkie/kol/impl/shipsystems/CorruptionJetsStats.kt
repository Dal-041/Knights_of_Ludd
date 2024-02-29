package org.selkie.kol.impl.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.impl.combat.RiftCascadeEffect
import com.fs.starfarer.api.impl.combat.RiftLanceEffect
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.opengl.GL14
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.modify
import org.magiclib.kotlin.scaleColor
import org.magiclib.kotlin.setAlpha
import org.magiclib.subsystems.MagicSubsystemsManager
import org.magiclib.subsystems.drones.MagicDroneSubsystem
import org.selkie.kol.combat.GL14ParticleData
import org.selkie.kol.combat.ParticleController
import org.selkie.kol.combat.ParticleData
import java.awt.Color
import java.util.*

class CorruptionJetsStats : BaseShipSystemScript() {
    override fun apply(
        systemStats: MutableShipStatsAPI,
        id: String,
        state: ShipSystemStatsScript.State,
        effectLevel: Float
    ) {
        val ship = systemStats.entity as ShipAPI
        val data = getSystemData(ship)
        if (data.ended) {
            data.ended = false
            data.affectedStats = getStatsForShipFightersAndDrones(ship)
            data.didEffect = true
            doEffect(ship)

            for (stats in data.affectedStats!!) {
                if (stats.entity is ShipAPI) {
                    Global.getCombatEngine()
                        .addLayeredRenderingPlugin(CorruptedEnginesRenderPlugin(stats.entity as ShipAPI, ship))
                }
            }
        }
        val statsToModify = data.affectedStats
        statsToModify!!.add(systemStats)
        for (stats in statsToModify) {
            if (state == ShipSystemStatsScript.State.OUT) {
                stats.maxSpeed.unmodify(id) // to slow down ship to its regular top speed while powering drive down
                stats.maxTurnRate.unmodify(id)
            } else {
                stats.maxSpeed.modifyFlat(id, SPEED_BONUS)
                stats.acceleration.modifyPercent(id, SPEED_BONUS * 3f * effectLevel)
                stats.deceleration.modifyPercent(id, SPEED_BONUS * 3f * effectLevel)
                stats.turnAcceleration.modifyFlat(id, TURN_BONUS * effectLevel)
                stats.turnAcceleration.modifyPercent(id, TURN_BONUS * 5f * effectLevel)
                stats.maxTurnRate.modifyFlat(id, 15f)
                stats.maxTurnRate.modifyPercent(id, 100f)
            }
            if (stats.entity is ShipAPI) {
                val statsShip = stats.entity as ShipAPI
                statsShip.engineController.fadeToOtherColor(this, ENGINE_COLOR, Color(0, 0, 0, 0), effectLevel, 0.67f)
                //ship.getEngineController().fadeToOtherColor(this, Color.white, new Color(0,0,0,0), effectLevel, 0.67f);
                statsShip.engineController.extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel)
            }
        }
    }

    override fun unapply(systemStats: MutableShipStatsAPI, id: String) {
        val ship = systemStats.entity as ShipAPI
        val data = getSystemData(ship)
        data.ended = true
        val statsToModify = data.affectedStats
        if (statsToModify != null) {
            statsToModify.add(systemStats)
            for (stats in statsToModify) {
                stats.maxSpeed.unmodify(id)
                stats.maxTurnRate.unmodify(id)
                stats.turnAcceleration.unmodify(id)
                stats.acceleration.unmodify(id)
                stats.deceleration.unmodify(id)
            }
        }
    }

    private fun doEffect(ship: ShipAPI) {
        val darkenedColor = RiftLanceEffect.getColorForDarkening(RIFT_COLOR)
        for (i in 0 until MathUtils.getRandomNumberInRange(360, 720)) {
            val dir = Misc.getUnitVectorAtDegreeAngle(i * MathUtils.getRandomNumberInRange(1f, 1.5f))
            dir.scale(2000f)
            ParticleController.addParticle(
                DarkParticleData(
                    ship.location.x,
                    ship.location.y,
                    dir.x,
                    dir.y,
                    MathUtils.getRandomNumberInRange(0f, 360f),
                    MathUtils.getRandomNumberInRange(-6f, 6f),
                    2f,
                    200f,
                    200f,
                    darkenedColor,
                    darkenedColor
                )
            )
        }
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        if (index == 0) {
            return StatusData("improved maneuverability", false)
        } else if (index == 1) {
            return StatusData("+" + SPEED_BONUS.toInt() + " top speed", false)
        }
        return null
    }

    private class CorruptionJetsData {
        var ended = true
        var didEffect = false
        var affectedStats: MutableList<MutableShipStatsAPI>? = null
    }

    companion object {
        private const val CUSTOM_DATA_KEY = "CorruptionJetsData"
        private const val SPEED_BONUS = 125f
        private const val TURN_BONUS = 20f
        private const val BUFF_RADIUS = 2000f
        private val ENGINE_COLOR = Color(100, 255, 100, 255)
        private val RIFT_COLOR = RiftCascadeEffect.STANDARD_RIFT_COLOR
        private val UNDERCOLOR = Color(150, 0, 25, 255)

        fun getStatsForShipFightersAndDrones(ship: ShipAPI): MutableList<MutableShipStatsAPI> {
            val returnedStats: MutableSet<MutableShipStatsAPI> = HashSet()
            for (wing in Global.getCombatEngine().ships) {
                if (!wing.isFighter) continue
                if (wing.wing == null) continue
                if (wing.wing.sourceShip === ship) {
                    returnedStats.add(wing.mutableStats)
                }
            }

            for (wing in AIUtils.getNearbyAllies(ship, BUFF_RADIUS)) {
                if (!wing.isFighter) continue
                if (wing.wing == null) continue
                returnedStats.add(wing.mutableStats)
            }

            val activators = MagicSubsystemsManager.getSubsystemsForShipCopy(ship)
            if (activators != null) {
                for (activator in activators) {
                    if (activator is MagicDroneSubsystem) {
                        for (drone in activator.activeWings.keys) {
                            returnedStats.add(drone.mutableStats)
                        }
                    }
                }
            }
            return ArrayList(returnedStats)
        }

        private fun getSystemData(ship: ShipAPI): CorruptionJetsData {
            val data: CorruptionJetsData?
            if (ship.customData.containsKey(CUSTOM_DATA_KEY)) {
                data = ship.customData[CUSTOM_DATA_KEY] as CorruptionJetsData
            } else {
                data = CorruptionJetsData()
                ship.setCustomData(CUSTOM_DATA_KEY, data)
            }
            return data
        }

        fun getRandomParticleData(ship: ShipAPI): ParticleData {
            val darkenedColor = RiftLanceEffect.getColorForDarkening(RIFT_COLOR)
            val dir = MathUtils.getRandomPointOnCircumference(Vector2f(), 1f)

            val particleData =
                if (MathUtils.getRandomNumberInRange(0f, 1f) < 0.4f) {

                    val blue = (UNDERCOLOR.blue + MathUtils.getRandomNumberInRange(-20,20)).coerceIn(0,255)

                    RedEngineParticleData(
                        ship.location.x,
                        ship.location.y,
                        dir.x,
                        dir.y,
                        MathUtils.getRandomNumberInRange(0f, 360f),
                        MathUtils.getRandomNumberInRange(-6f, 6f),
                        2f,
                        ship.collisionRadius.coerceAtLeast(100f),
                        ship.collisionRadius.coerceAtLeast(100f),
                        UNDERCOLOR.modify(blue = blue),
                        UNDERCOLOR.modify(blue = blue, alpha = 0)
                    )
                } else {
                    DarkEngineParticleData(
                        ship.location.x,
                        ship.location.y,
                        dir.x,
                        dir.y,
                        MathUtils.getRandomNumberInRange(0f, 360f),
                        MathUtils.getRandomNumberInRange(-6f, 6f),
                        2f,
                        ship.collisionRadius.coerceAtLeast(100f),
                        ship.collisionRadius.coerceAtLeast(100f),
                        darkenedColor,
                        darkenedColor.setAlpha(0)
                    )
                }
            return particleData
        }

        class CorruptedEnginesRenderPlugin(val ship: ShipAPI, val baseShip: ShipAPI) :
            BaseCombatLayeredRenderingPlugin() {
            private val particleInterval = IntervalUtil(0.5f, 1f)

            override fun isExpired(): Boolean {
                return !baseShip.isAlive || !baseShip.system.isActive || !ship.isAlive
            }

            override fun getRenderRadius(): Float {
                return ship.collisionRadius * 3f
            }

            override fun advance(amount: Float) {
                particleInterval.advance(amount)
                if (particleInterval.intervalElapsed()) {
                    ParticleController.addParticle(getRandomParticleData(ship))
                }
            }
        }

        //Mimicking the reality disruptor.
        class DarkParticleData(
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
        ) : GL14ParticleData(
            sprite = Global.getSettings().getSprite("misc", "nebula_particles"),
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
        ) {

            override fun preDraw() {
                if ((MathUtils.getRandomNumberInRange(0f, 1f) <= 0.016f) && !Global.getCombatEngine().isPaused) {
                    val blue = (UNDERCOLOR.blue + MathUtils.getRandomNumberInRange(-20,20)).coerceIn(0,255)
                    ParticleController.addParticle(
                        RedParticleData(
                            x,
                            y,
                            xVel * -0.1f,
                            yVel * -0.1f,
                            MathUtils.getRandomNumberInRange(0f, 360f),
                            MathUtils.getRandomNumberInRange(-6f, 6f),
                            1f,
                            startingSize * 0.5f,
                            endSize * 3f,
                            UNDERCOLOR.modify(blue = blue),
                            UNDERCOLOR.modify(blue = blue, alpha = 0)
                        )
                    )
                }
                GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT)
            }

            override fun postDraw() {
                GL14.glBlendEquation(GL14.GL_FUNC_ADD)
            }
        }

        class DarkEngineParticleData(
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
        ) : GL14ParticleData(
            sprite = Global.getSettings().getSprite("misc", "nebula_particles"),
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
            layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER),
            spritesInRow = 4,
            spritesInColumn = 4
        ) {
            override fun preDraw() {
                GL14.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT)
            }

            override fun postDraw() {
                GL14.glBlendEquation(GL14.GL_FUNC_ADD)
            }
        }

        class RedEngineParticleData(
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
            sprite = Global.getSettings().getSprite("misc", "nebula_particles"),
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
            layers = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER),
            spritesInRow = 4,
            spritesInColumn = 4
        )

        class RedParticleData(
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
            sprite = Global.getSettings().getSprite("misc", "nebula_particles"),
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
    }
}

