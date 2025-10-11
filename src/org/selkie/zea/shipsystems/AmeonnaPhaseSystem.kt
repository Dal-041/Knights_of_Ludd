package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.setAlpha
import org.selkie.kol.ReflectionUtilsV2
import org.selkie.zea.hullmods.DuskWormController.DuskWormSegment
import java.awt.Color
import java.util.*
import kotlin.collections.ArrayList

class AmeonnaPhaseSystem : BaseShipSystemScript() {

    var init = false
    var activated = false
    //lateinit var renderer: AmeonnaPhaseRenderer
    lateinit var ship: ShipAPI
    lateinit var headSegment: DuskWormSegment
    lateinit var segmentsToPhase: ArrayList<ShipAPI>

    var TIME_MULT = 2f

    var particleInterval = IntervalUtil(0.025f, 0.03f)

    var portalFade = 0f
    var portalAngle = 0f

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {

        ship = stats.entity as ShipAPI ?: return
        headSegment = ship.getListeners(DuskWormSegment::class.java).firstOrNull() ?: return
        ship.phaseCloak ?: return
        var player = ship === Global.getCombatEngine().playerShip

        updateBounds()

        segmentsToPhase = getAllSegments()

        var system = ship.phaseCloak

        var segments = ArrayList<ShipAPI>()
        segments.add(ship)
        segments.addAll(ship.childModulesCopy)

        if (!init) {
            init = true
            //renderer = AmeonnaPhaseRenderer(ship, ship.system)

            var portalRender = AmeonnaPortalRenderer(this, ship)
            Global.getCombatEngine().addLayeredRenderingPlugin(portalRender)

            for (segment in segments) {
                var renderer = AmeonnaPhaseRenderer(this, segment, headSegment, ship.system)
                Global.getCombatEngine().addLayeredRenderingPlugin(renderer)
            }
        }

        if ((system.isChargeup || system.isChargedown) && !activated) {
            activated = true
            portalAngle = ship.facing+90
        }

        if ((!system.isChargeup && !system.isChargedown) && activated) {
            activated = false
        }

        for (segment in segments) {
            var color = Color(80, 50, 255, 255)
            segment.engineController.extendFlame(this, -0.3f*effectLevel, -0.3f*effectLevel, -0.3f*effectLevel)
            segment.engineController.fadeToOtherColor(this, color, color.setAlpha(5), effectLevel, 0.5f)
        }

        val shipTimeMult = 1f + (TIME_MULT - 1f) * effectLevel
        stats.timeMult.modifyMult(id, shipTimeMult)
        if (player) {
            Global.getCombatEngine().timeMult.modifyMult(id, 1f / shipTimeMult)
        } else {
            Global.getCombatEngine().timeMult.unmodify(id)
        }

        val allSegments = generateSequence(headSegment) { it.segmentBehind }.toList()
        for (segment in allSegments) {
            segment.ship.isPhased = segment.isPhased
        }


        var amount = Global.getCombatEngine().elapsedInLastFrame

        if (!Global.getCombatEngine().isPaused) {
            if (system.isActive) {


            }

            if (system.isChargeup || system.isChargedown) {
                ship.mutableStats.maxTurnRate.modifyMult("phase_turn_decrease", effectLevel)
                if (system.isChargedown) ship.mutableStats.maxTurnRate.modifyMult("phase_turn_decrease", 1-(effectLevel))
                particleInterval.advance(amount)

                if (maskEndPosition != null) {
                    portalFade += 0.75f * amount
                }

               /* if (particleInterval.intervalElapsed()) {
                    if (maskEndPosition != null && maskEndAngle != null) {
                        var color = Color(255, 255, 255)
                        for (i in 0 until 5) {
                            var width = MathUtils.getRandomNumberInRange(-140f, 140f)
                            var loc = MathUtils.getPointOnCircumference(maskEndPosition, width, maskEndAngle!!+90f)
                            //var vel = Vector2f(ship.velocity).scale(-1f) as Vector2f
                            var vel = Vector2f()
                            Global.getCombatEngine().addNegativeParticle(loc, vel , MathUtils.getRandomNumberInRange(80f, 120f), 0.0f, 2f, color)

                        }
                    }
                }*/

            } else{
                ship.mutableStats.maxTurnRate.unmodify("phase_turn_decrease")
            }

            if (maskEndPosition == null) {
                portalFade -= 2 * amount
            }

            portalFade = MathUtils.clamp(portalFade, 0f, 1f)

        }




    }

    var bounds = ArrayList<Vector2f>()
    var maskEndPosition: Vector2f? = null
    fun updateBounds() {
        val allSegments = generateSequence(headSegment) { it.segmentBehind }.toList()
        if (allSegments.isEmpty()) return

        maskEndPosition = null
        bounds.clear()

        // === 1. Pre-calculate shared values ===
        // * 1.5 as an estimate of how long the head/tail tip is

        var totalLength =
            allSegments.first().ship.collisionRadius * 1.5f +
                    allSegments.last().ship.collisionRadius * 1.5f +
                    allSegments.sumOf { (it.segmentLength ?: 0f).toDouble() }

        var phasedLength = (headSegment.ship.phaseCloak!!.effectLevel) * totalLength

        val fromHeadToTail = headSegment.ship.phaseCloak!!.isChargeup

        // === 2. Configure direction-dependent variables ===
        val segmentsToProcess = if (fromHeadToTail) allSegments else allSegments.reversed()
        val getNextSegment: (DuskWormSegment) -> DuskWormSegment? =
            if (fromHeadToTail) { segment -> segment.segmentBehind }
            else { segment -> segment.segmentAhead }

        // Define the angles for the start and end "caps" of the worm.
        val startTipAngles = if (fromHeadToTail) Pair(45f, -45f) else Pair(135f, -135f)
        val endTipAngles = if (fromHeadToTail) Pair(135f, -135f) else Pair(45f, -45f)

        // === 3. Unified Drawing Logic ===
        var currentSegment = segmentsToProcess.first()
        var nextSegment = getNextSegment(currentSegment)

        // Start the strip with the initial tip (either head or tail)
        var currentTopRight = MathUtils.getPointOnCircumference(
            currentSegment.ship.location,
            currentSegment.ship.collisionRadius * 2f,
            currentSegment.ship.facing + startTipAngles.first
        )
        var currentTopLeft = MathUtils.getPointOnCircumference(
            currentSegment.ship.location,
            currentSegment.ship.collisionRadius * 2f,
            currentSegment.ship.facing + startTipAngles.second
        )
        /*GL11.glVertex2f(currentTopRight.x, currentTopRight.y)
        GL11.glVertex2f(currentTopLeft.x, currentTopLeft.y)*/
        bounds.add(Vector2f(currentTopRight.x, currentTopRight.y))
        bounds.add(Vector2f(currentTopLeft.x, currentTopLeft.y))

        // Main loop to draw the body and the final tip
        while (phasedLength > 0) {
            val inBetweenAngle = nextSegment?.let {
                MathUtils.getShortestRotation(
                    currentSegment.ship.facing,
                    it.ship.facing
                ) / 2 + currentSegment.ship.facing
            }

            // Determine the location of the joint between the current and next segment.
            // The joint's location is stored on the segment that is closer to the tail.
            val jointLocation = if (nextSegment != null) {
                if (fromHeadToTail) nextSegment.jointAheadLocation else currentSegment.jointAheadLocation
            } else null

            // Calculate the next pair of vertices. If a next segment exists, use the joint.
            // Otherwise, create the final "tip" of the worm.
            val nextTopRight = if (inBetweenAngle != null && jointLocation != null) {
                MathUtils.getPointOnCircumference(
                    jointLocation,
                    nextSegment!!.ship.collisionRadius * 1.5f,
                    inBetweenAngle + 90f
                )
            } else { // Draw the end tip
                MathUtils.getPointOnCircumference(
                    currentSegment.ship.location,
                    currentSegment.ship.collisionRadius * 2f,
                    currentSegment.ship.facing + endTipAngles.first
                )
            }

            val nextTopLeft = if (inBetweenAngle != null && jointLocation != null) {
                MathUtils.getPointOnCircumference(
                    jointLocation,
                    nextSegment!!.ship.collisionRadius * 1.5f,
                    inBetweenAngle - 90f
                )
            } else { // Draw the end tip
                MathUtils.getPointOnCircumference(
                    currentSegment.ship.location,
                    currentSegment.ship.collisionRadius * 2f,
                    currentSegment.ship.facing + endTipAngles.second
                )
            }

            // The length of the current piece. The fallback is for the final tip.
            val currentPieceLength = (currentSegment.segmentLength ?: (currentSegment.ship.collisionRadius * 1.5f))

            // Draw either the full piece or an interpolated partial piece.
            if (phasedLength > currentPieceLength) {
                /* GL11.glVertex2f(nextTopRight.x, nextTopRight.y)
                 GL11.glVertex2f(nextTopLeft.x, nextTopLeft.y)*/
                bounds.add(Vector2f(nextTopRight.x, nextTopRight.y))
                bounds.add(Vector2f(nextTopLeft.x, nextTopLeft.y))
                currentTopRight = nextTopRight
                currentTopLeft = nextTopLeft
            } else {
                val phasedProgress = (phasedLength / currentPieceLength).toFloat()
                val middleRight = Vector2f(
                    currentTopRight.x + (nextTopRight.x - currentTopRight.x) * phasedProgress,
                    currentTopRight.y + (nextTopRight.y - currentTopRight.y) * phasedProgress
                )
                val middleLeft = Vector2f(
                    currentTopLeft.x + (nextTopLeft.x - currentTopLeft.x) * phasedProgress,
                    currentTopLeft.y + (nextTopLeft.y - currentTopLeft.y) * phasedProgress
                )
                /*GL11.glVertex2f(middleRight.x, middleRight.y)
                GL11.glVertex2f(middleLeft.x, middleLeft.y)*/
                bounds.add(Vector2f(middleRight.x, middleRight.y))
                bounds.add(Vector2f(middleLeft.x, middleLeft.y))

                maskEndPosition = Vector2f(
                    (middleRight.x + middleLeft.x) / 2f,
                    (middleRight.y + middleLeft.y) / 2f
                )

                updateSegment(currentSegment, phasedProgress)

                //println(currentSegment.ship.hullSpec.hullId + ": $phasedProgress")
            }

            // Advance to the next segment. Exit if we've reached the end.
            phasedLength -= currentPieceLength
            if (nextSegment == null) {
               /* maskEndPosition = Vector2f(
                    (nextTopRight.x + nextTopLeft.x) / 2f,
                    (nextTopRight.y + nextTopLeft.y) / 2f
                )
                maskEndAngle = currentSegment.ship.facing*/
                break
            }

            currentSegment = nextSegment
            nextSegment = getNextSegment(currentSegment)
        }

      /*  if (maskEndPosition == null || maskEndAngle == null) {
            val lastSegment = segmentsToProcess.last()
            maskEndPosition = Vector2f(lastSegment.ship.location.x, lastSegment.ship.location.y)
            maskEndAngle = lastSegment.ship.facing
        }*/
    }

    fun updateSegment(segment: DuskWormSegment, segmentLevel: Float) {
        //segment.isPhased = level >= 0.5 //Caused a ConcurrentModificationException
        //TODO Logic for stopping weapon fire, etc.
        var isPhased = segmentLevel >= 0.5f
        segment.isPhased = isPhased
    }

    fun getAllSegments() : ArrayList<ShipAPI> {
        var modules = ArrayList<ShipAPI>()
        modules.add(ship)
        modules.addAll(ship.childModulesCopy.reversed())
        return modules
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {

    }



    override fun isUsable(system: ShipSystemAPI, ship: ShipAPI?): Boolean {
        return super.isUsable(system, ship) && !system.isChargeup
    }

    class AmeonnaPortalRenderer(var systemScript: AmeonnaPhaseSystem, var ship: ShipAPI) : BaseCombatLayeredRenderingPlugin() {

        lateinit var portal: SpriteAPI

        init {
            var path = "graphics/zea/ships/ameonna/zea_ameonna_portal.png"
            Global.getSettings().loadTexture(path)
            portal = Global.getSettings().getSprite(path)
        }

        override fun getRenderRadius(): Float {
            return 1000000f
        }

        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER)
        }

        var lastPortalLoc = Vector2f()
        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {

            if (systemScript.maskEndPosition != null) {
                lastPortalLoc = systemScript.maskEndPosition!!
            }

            var loc = Vector2f(lastPortalLoc.x, lastPortalLoc.y)

            portal.setNormalBlend()
            portal.alphaMult = systemScript.portalFade
            portal.angle = systemScript.portalAngle
            portal.renderAtCenter(loc.x, loc.y)

            doJitter(loc, portal, portal.alphaMult * 0.3f, jitterLocs, 15, 20f)

        }

        var jitterLocs = ArrayList<Vector2f>()
        fun doJitter(loc: Vector2f, sprite: SpriteAPI, level: Float, lastLocations: ArrayList<Vector2f>, jitterCount: Int, jitterMaxRange: Float) {

            var paused = Global.getCombatEngine().isPaused
            var jitterAlpha = 0.2f

            if (!paused) {
                lastLocations.clear()
            }

            for (i in 0 until jitterCount) {

                var jitterLoc = Vector2f()

                if (!paused) {
                    var x = MathUtils.getRandomNumberInRange(-jitterMaxRange, jitterMaxRange)
                    var y = MathUtils.getRandomNumberInRange(-jitterMaxRange, jitterMaxRange)

                    jitterLoc = Vector2f(x, y)
                    lastLocations.add(jitterLoc)
                }
                else {
                    jitterLoc = lastLocations.getOrElse(i) {
                        Vector2f()
                    }
                }

                sprite.setAdditiveBlend()
                sprite.alphaMult = level * jitterAlpha
                sprite.renderAtCenter(loc.x + jitterLoc.x, loc.y + jitterLoc.y)
            }
        }

    }

    class AmeonnaPhaseRenderer(var systemScript: AmeonnaPhaseSystem, var ship: ShipAPI, var headSegment: DuskWormSegment, var system: ShipSystemAPI) : BaseCombatLayeredRenderingPlugin() {

        var glow1 = Global.getSettings().getSprite(ship.hullSpec.spriteName.replace(".png", "_glow1.png"))
        var glow2 = Global.getSettings().getSprite(ship.hullSpec.spriteName.replace(".png", "_glow2.png"))

        override fun getRenderRadius(): Float {
            return 1000000f
        }

        var layers = (ReflectionUtilsV2.get("layers", ship) as EnumSet<CombatEngineLayers>).apply {
            add(CombatEngineLayers.ABOVE_SHIPS_LAYER)
        }
        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return layers
        }

        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {



            //Render phase glows
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) {

                startDepthMask(true)

                glow1.angle = ship.facing-90
                glow1.renderAtCenter(ship.location.x, ship.location.y)

                glow2.setAdditiveBlend()
                glow2.alphaMult = 0.3f
                glow2.angle = ship.facing-90
                glow2.renderAtCenter(ship.location.x, ship.location.y)

                endDepthMask()
                //debug
//                GL11.glPolygonMode( GL11.GL_FRONT_AND_BACK, GL11.GL_LINE );
//                drawMask()
//                GL11.glPolygonMode( GL11.GL_FRONT_AND_BACK, GL11.GL_FILL );
            }
            //Split the ship in to a phased and non phased part
            else {
                //Set alpha to the normal amount
                ship.extraAlphaMult = 1f
                ship.setApplyExtraAlphaToEngines(false)


                startDepthMask(false)
                ReflectionUtilsV2.invoke("render", ship, layer, viewport)
                endDepthMask()

                //Dont render engines twice as they are additive
                ship.isRenderEngines = true

                ship.extraAlphaMult = 0.25f
                startDepthMask(true)
                ReflectionUtilsV2.invoke("render", ship, layer, viewport)
                endDepthMask()

                //Set alpha to 0.25, so that the base game render renders this half, instead of having to render it an additional time
                ship.setApplyExtraAlphaToEngines(true)
                ship.extraAlphaMult = 0.0f
            }


        }


        fun startDepthMask(renderInside: Boolean) {
            // Enable depth testing and writing
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(true)

            // Clear depth buffer
            GL11.glClearDepth(1.0)
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)

            // Set depth function to always pass and write a specific depth value
            GL11.glDepthFunc(GL11.GL_ALWAYS)
            GL11.glColorMask(false, false, false, false) // Don't draw color

            // Draw your mask quad with depth value 0.5
            GL11.glDepthRange(0.5, 0.5) // Set depth range to a fixed value

            drawMask()

            // Restore color drawing
            GL11.glColorMask(true, true, true, true)

            // Now restrict rendering to only where depth == 0.5
            GL11.glDepthFunc(if (renderInside) GL11.GL_EQUAL else GL11.GL_NOTEQUAL)
            GL11.glDepthMask(false) // Prevent further depth writes
        }

        fun endDepthMask() {
            // Disable depth test or restore default behavior
            GL11.glDepthFunc(GL11.GL_LEQUAL)
            GL11.glDepthMask(true)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
        }

        fun drawMask() {

            GL11.glBegin(GL11.GL_QUAD_STRIP)

            for (bound in systemScript.bounds) {
                GL11.glVertex2f(bound.x, bound.y)
            }

            GL11.glEnd()
        }







    }
}