package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.ext.plus
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import org.selkie.kol.helpers.MathHelpers.computeSpriteCenter
import org.selkie.zea.helpers.ZeaStaticStrings.GfxCat
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

class PhaseTransporter: BaseShipSystemScript() {
    val futureImageSpacing = 100f
    val bufferDistance = 50f
    val AFTERIMAGE_COLOR: Color = Color(200, 45, 10, 100)
    val phaseJitter = 15f

    var startPos: Vector2f? = null
    var startAngle: Float? = null

    val targetPos: Vector2f = Vector2f(0f, 0f)
    var targetAngle: Float? = null
    var targetShip: ShipAPI? = null


    override fun apply(
        stats: MutableShipStatsAPI,
        id: String?,
        state: ShipSystemStatsScript.State,
        effectLevel: Float
    ) {
        val ship = stats.entity as ShipAPI
        val engine = Global.getCombatEngine()

        if (state == ShipSystemStatsScript.State.COOLDOWN) return
        if (state == ShipSystemStatsScript.State.IDLE){
            // update target location
            val mouseTarget = ship.mouseTarget

            var targetFound = false
            engine.aiGridShips.getCheckIterator(mouseTarget, 500f, 500f)
                .asSequence()
                .filterIsInstance<ShipAPI>()
                .filter {
                    it != ship &&
                    it.hullSize != ShipAPI.HullSize.FIGHTER
                }.forEach { potentialTarget ->

                    if (MathUtils.isWithinRange(
                            mouseTarget,
                            potentialTarget.location,
                            bufferDistance + potentialTarget.collisionRadius + ship.collisionRadius
                    )) {
                        targetShip = potentialTarget
                        targetAngle = VectorUtils.getAngle(potentialTarget.location, mouseTarget)
                        targetFound = true
                    }
                }

            if (!targetFound){
                targetShip = null
                targetAngle = null
            }

            targetPos.set(ship.mouseTarget)

            if (startPos != null){
                startPos = null
                startAngle = null
                ship.isPhased = false
                ship.extraAlphaMult = 1f
                ship.setApplyExtraAlphaToEngines(false)
            }

            renderPredictiveOutline(ship)
        } else {
            if (startPos == null){
                startPos = Vector2f(ship.location)
                startAngle = ship.facing
                ship.isPhased = true
                ship.extraAlphaMult = 0.15f
                ship.setApplyExtraAlphaToEngines(true)

            }

            phaseTransport(ship, effectLevel)
        }
    }

    fun renderPredictiveOutline(ship: ShipAPI) {
        if (targetShip == null || targetAngle == null) return

        val spriteCenter = computeSpriteCenter(ship)
        val targetDistance = ship.collisionRadius + targetShip!!.collisionRadius + bufferDistance
        val targetPos = MathUtils.getPointOnCircumference(targetShip!!.location, targetDistance, targetAngle!!)

        MagicRender.singleframe(
            Global.getSettings().getSprite(GfxCat.PHASE_GLOWS, ship.hullSpec.baseHullId + "_glow1"),
            targetPos,
            Vector2f(ship.spriteAPI.width, ship.spriteAPI.height),
            targetAngle!! + 90f,
            AFTERIMAGE_COLOR,
            true,
        )
    }

    fun phaseTransport(ship: ShipAPI, effectLevel: Float) {
        if (startPos == null || startAngle == null) return


        val targetPos = targetShip?.let {
            val targetDistance = ship.collisionRadius + it.collisionRadius + bufferDistance

            MathUtils.getPointOnCircumference(it.location, targetDistance, targetAngle!!)
        } ?: targetPos!!

        val targetAngle = targetAngle?: (ship.facing - 180f)

        ship.location.set(easeInOutVec2f(startPos!!, targetPos, effectLevel))
        ship.facing = lerpAngle(startAngle!!, 180f + targetAngle, effectLevel)

        val travelDistance = MathUtils.getDistance(startPos, targetPos)
        val totalImages = floor(travelDistance/futureImageSpacing) + 1


        for(i in 0..totalImages.toInt()){
            val futureLevel = i * 1/totalImages
            if (futureLevel > effectLevel){

                MagicRender.singleframe(
                    Global.getSettings().getSprite(GfxCat.PHASE_GLOWS, ship.hullSpec.baseHullId + "_glow1"),
                    easeInOutVec2f(startPos!!, targetPos, futureLevel) +
                            Vector2f(
                            MathUtils.getRandomNumberInRange(-phaseJitter, phaseJitter),
                            MathUtils.getRandomNumberInRange(-phaseJitter, phaseJitter)
                            ),
                    Vector2f(ship.spriteAPI.width, ship.spriteAPI.height),
                    lerpAngle(startAngle!! - 90f, targetAngle + 90f, futureLevel),
                    AFTERIMAGE_COLOR,
                    true,
                )
            }
        }
    }

    fun lerpAngle(startAngle: Float, endAngle: Float, t: Float): Float {
        var delta = endAngle - startAngle
        if (delta > 180) {
            delta -= 360
        } else if (delta < -180) {
            delta += 360
        }
        return startAngle + delta * t
    }

    fun easeInOutVec2f(start: Vector2f, end: Vector2f, t: Float): Vector2f {
        // Apply the easing function to the interpolation factor
        val easedT = -0.5f * (cos(PI * t).toFloat() - 1f)

        // Perform the linear interpolation with the eased factor
        val newX = start.x + (end.x - start.x) * easedT
        val newY = start.y + (end.y - start.y) * easedT

        return Vector2f(newX, newY)
    }
}