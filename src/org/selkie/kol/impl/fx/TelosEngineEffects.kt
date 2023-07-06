package org.selkie.kol.impl.fx

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.rotate
import org.lazywizard.lazylib.ext.rotateAroundPivot
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.modify
import org.magiclib.kotlin.random
import wisp.perseanchronicles.telos.boats.ShipPalette
import wisp.perseanchronicles.telos.boats.defaultShipPalette
//import wisp.questgiver.wispLib.modify
//import wisp.questgiver.wispLib.random
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Originally `tahlan_PhaseEngines`, thank you Nia.
 */
class TelosEngineEffects : EveryFrameWeaponEffectPlugin {

    var currentPalette = defaultShipPalette

    var baseNebulaColorOverride: Color? = null
    var baseSwirlyNebulaColorOverride: Color? = null
    var baseNegativeColorOverride: Color? = null

    private val interval = IntervalUtil(0.03f, 0.04f)
    private var alphaMult = 0f
    private var hasSetPalette = false
    private var customRenderer: CombatCustomRenderer = CombatCustomRenderer.instance!!

    // TODO: optimization
    override fun advance(amount: Float, engine: CombatEngineAPI, weapon: WeaponAPI) {
        interval.advance(amount)

        if (!hasSetPalette) {
            currentPalette =
                weapon.ship.captain?.tags?.firstNotNullOfOrNull { kotlin.runCatching { ShipPalette.valueOf(it) }.getOrNull() }
                    ?: defaultShipPalette
            hasSetPalette = true
        }

        // we calculate our alpha every frame since we smoothly shift it
        val ship = weapon.ship
        val ec = ship.engineController
        alphaMult = if (ec.isAccelerating || ec.isStrafingLeft || ec.isStrafingRight) {
            (alphaMult + amount * 2f).coerceAtMost(1f)
        } else if (ec.isDecelerating || ec.isAcceleratingBackwards) {
            if (alphaMult < 0.5f) (alphaMult + amount * 2f).coerceAtMost(0.5f)
            else (alphaMult - amount * 2f).coerceAtLeast(0.5f)
        } else {
            (alphaMult - amount * 2f).coerceAtLeast(0f)
        }

        // jump out if interval hasn't elapsed yet
        if (!interval.intervalElapsed()) return

        val velocityScale = .1f
        val sizeScale = 1.3f
        val durationScale = 1.0f * (when (ship.hullSize) {
            ShipAPI.HullSize.FIGHTER -> 0.3f
            ShipAPI.HullSize.FRIGATE -> 0.5f
            ShipAPI.HullSize.DESTROYER -> 0.8f
            ShipAPI.HullSize.CRUISER -> 1f
            ShipAPI.HullSize.CAPITAL_SHIP -> 1.2f
            else -> 1f
        })
        val rampUpScale = 1.0f
        val alphaScale = .45f
        val topLayerAlphaScale = .15f
        val bottomLayerAlphaScale = .40f
        val endSizeScale = 1.55f
        val densityInverted = 0.03f // Lower is more dense
        val trailMomentumScale = (when (ship.hullSize) {
//            ShipAPI.HullSize.FRIGATE -> -0.15f
            else -> -0.3f
        }) // How much the trail keeps ship momentum

        if (interval.minInterval != densityInverted) {
            interval.setInterval(densityInverted, densityInverted * 0.2f)
        }

        val vel = Vector2f(100f * velocityScale, 100f * velocityScale)
            .rotate(Random.nextFloat() * 360f)
            .let { dest ->
                val shipVel = ship.velocity.let { Vector2f(it.x * trailMomentumScale, it.y * trailMomentumScale) }
                dest.plus(shipVel)
            }

        val negativeColor =
            (baseNegativeColorOverride ?: currentPalette.baseNegative).modify(
                alpha = (1 * alphaMult * alphaScale).roundToInt().coerceIn(0..255)
            )
        val nebulaColor =
            (baseNebulaColorOverride ?: currentPalette.baseNebula).modify(
                alpha = (70 * alphaMult * alphaScale).roundToInt().coerceIn(0..255)
            )
        val swirlyNebulaColor =
            (baseSwirlyNebulaColorOverride
                ?: currentPalette.baseSwirlyNebula).modify(alpha = (55 * alphaMult * alphaScale).roundToInt().coerceIn(0..255))

        val emitters = ship.hullSpec.allWeaponSlotsCopy
            .filter { it.isSystemSlot }
            .map {
                Vector2f(it.location)
                    .translate(ship.location.x, ship.location.y)
                    .rotateAroundPivot(ship.location, ship.facing)
                    .translate(-ship.location.x, -ship.location.y)
            }

        for (location in emitters) {
            // Negative swirl under
            customRenderer.addNebula(
                location = location,
                anchorLocation = ship.location,
                velocity = vel,
                size = (40f..60f).random() * sizeScale,
                endSizeMult = endSizeScale,
                duration = (1.2f..1.5f).random() * durationScale,
                inFraction = 0.1f * rampUpScale,
                outFraction = 0.5f,
                color = negativeColor,
                layer = CombatEngineLayers.UNDER_SHIPS_LAYER,
                type = CustomRenderer.NebulaType.SWIRLY,
                negative = true
            )

            // Normal under
            customRenderer.addNebula(
                location = location,
                anchorLocation = ship.location,
                velocity = vel,
                size = (30f..50f).random() * sizeScale,
                endSizeMult = endSizeScale,
                duration = (1f..1.3f).random() * durationScale,
                inFraction = 0.1f * rampUpScale,
                outFraction = 0.5f,
                color = nebulaColor.modify(alpha = (nebulaColor.alpha * bottomLayerAlphaScale).roundToInt()),
                layer = CombatEngineLayers.UNDER_SHIPS_LAYER,
                type = CustomRenderer.NebulaType.NORMAL,
                negative = false
            )

            // Swirl under
            customRenderer.addNebula(
                location = location,
                anchorLocation = ship.location,
                velocity = vel,
                size = (30f..50f).random() * sizeScale,
                endSizeMult = endSizeScale,
                duration = (1f..1.3f).random() * durationScale,
                inFraction = 0.1f * rampUpScale,
                outFraction = 0.5f,
                color = swirlyNebulaColor.modify(alpha = (swirlyNebulaColor.alpha * bottomLayerAlphaScale).roundToInt()),
                layer = CombatEngineLayers.UNDER_SHIPS_LAYER,
                type = CustomRenderer.NebulaType.SWIRLY,
                negative = false
            )

            // Normal on top
            customRenderer.addNebula(
                location = location,
                anchorLocation = ship.location,
                velocity = vel,
                size = (30f..50f).random() * sizeScale,
                endSizeMult = endSizeScale,
                duration = (1f..1.3f).random() * durationScale,
                inFraction = 0.1f * rampUpScale,
                outFraction = 0.5f,
                color = nebulaColor.modify(alpha = (nebulaColor.alpha * topLayerAlphaScale).roundToInt()),
                layer = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER,
                type = CustomRenderer.NebulaType.NORMAL,
                negative = false
            )

            // Swirl on top
            customRenderer.addNebula(
                location = location,
                anchorLocation = ship.location,
                velocity = vel,
                size = (30f..50f).random() * sizeScale,
                endSizeMult = endSizeScale,
                duration = (1f..1.3f).random() * durationScale,
                inFraction = 0.1f * rampUpScale,
                outFraction = 0.5f,
                color = swirlyNebulaColor.modify(alpha = (nebulaColor.alpha * topLayerAlphaScale).roundToInt()),
                layer = CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER,
                type = CustomRenderer.NebulaType.SWIRLY,
                negative = false
            )
        }
    }
}