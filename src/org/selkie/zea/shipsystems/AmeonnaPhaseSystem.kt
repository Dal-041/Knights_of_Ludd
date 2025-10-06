package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.ReflectionUtilsV2
import org.selkie.zea.hullmods.DuskWormController.DuskWormSegment
import java.util.*
import kotlin.collections.ArrayList

class AmeonnaPhaseSystem : BaseShipSystemScript() {

    var init = false
    var activated = false
    //lateinit var renderer: AmeonnaPhaseRenderer
    lateinit var ship: ShipAPI
    lateinit var headSegment: DuskWormSegment
    lateinit var segmentsToPhase: ArrayList<ShipAPI>

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {

        ship = stats.entity as ShipAPI ?: return
        headSegment = ship.getListeners(DuskWormSegment::class.java).firstOrNull() ?: return
        ship.phaseCloak ?: return

        segmentsToPhase = getAllSegments()

        var system = ship.phaseCloak

        var modules = ArrayList<ShipAPI>()
        modules.add(ship)
        modules.addAll(ship.childModulesCopy)

        if (!init) {
            init = true
            //renderer = AmeonnaPhaseRenderer(ship, ship.system)

            for (module in modules) {
                var renderer = AmeonnaPhaseRenderer(this, module, headSegment, ship.system)
                Global.getCombatEngine().addLayeredRenderingPlugin(renderer)
            }


        }

        if (system.isActive && !activated) {
            activated = true
        }

        if (!system.isActive && activated) {
            activated = false
        }

        if (system.isActive && !Global.getCombatEngine().isPaused) {

        }

    }

    fun getAllSegments() : ArrayList<ShipAPI> {
        var modules = ArrayList<ShipAPI>()
        modules.add(ship)
        modules.addAll(ship.childModulesCopy.reversed())
        return modules
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {

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
                ship.setApplyExtraAlphaToEngines(true)

                val x = ship.location.x
                val y = ship.location.y
                startDepthMask(false)
                ReflectionUtilsV2.invoke("render", ship, layer, viewport)
                endDepthMask()

                ship.extraAlphaMult = 0.25f
                startDepthMask(true)
                ReflectionUtilsV2.invoke("render", ship, layer, viewport)
                endDepthMask()

                //Set alpha to 0.25, so that the base game render renders this half, instead of having to render it an additional time

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

        fun drawMask(){
            GL11.glBegin(GL11.GL_QUAD_STRIP)

            var currentSegment: DuskWormSegment = headSegment
            var nextSegment: DuskWormSegment? = headSegment.segmentBehind
            val allSegments = generateSequence(headSegment) { it.segmentBehind }.toList()
            // * 1.5 as estimate of how long head/tail is
            val totalLength =
                allSegments.first().ship.collisionRadius * 1.5f +
                allSegments.last().ship.collisionRadius * 1.5f +
                allSegments.sumOf { (it.segmentLength ?: 0f).toDouble() }

            var phasedLength = headSegment.ship.phaseCloak!!.effectLevel * totalLength

            // start strip with 2 fixed points
            var currentTopRight = MathUtils.getPointOnCircumference(
                currentSegment.ship.location,
                currentSegment.ship.collisionRadius * 2f,
                currentSegment.ship.facing+45f
            )
            var currentTopLeft = MathUtils.getPointOnCircumference(
                currentSegment.ship.location,
                currentSegment.ship.collisionRadius * 2f,
                currentSegment.ship.facing-45f
            )
            GL11.glVertex2f(currentTopRight.x, currentTopRight.y)
            GL11.glVertex2f(currentTopLeft.x, currentTopLeft.y)

            // while we still have phase length left,
            while(phasedLength > 0) {
                // find the inbetween angle of the ships if possible
                val inBetweenAngle = nextSegment?.let {
                    MathUtils.getShortestRotation(
                        currentSegment.ship.facing,
                        it.ship.facing
                    )/2 + currentSegment.ship.facing
                }

                // if found, next segment exists thus we should use those points
                val nextTopRight = if (inBetweenAngle != null) {
                    MathUtils.getPointOnCircumference(
                        nextSegment.jointAheadLocation,
                        nextSegment.ship.collisionRadius * 1.5f,
                        inBetweenAngle+90f
                    )
                } else { // otherwise, do fixed points behind the ship as the tail
                    MathUtils.getPointOnCircumference(
                        currentSegment.ship.location,
                        currentSegment.ship.collisionRadius * 2f,
                        currentSegment.ship.facing+135f
                    )
                }

                val nextTopLeft = if (inBetweenAngle != null) {
                    MathUtils.getPointOnCircumference(
                        nextSegment.jointAheadLocation,
                        nextSegment.ship.collisionRadius * 1.5f,
                        inBetweenAngle-90f
                    )
                } else{
                    MathUtils.getPointOnCircumference(
                        currentSegment.ship.location,
                        currentSegment.ship.collisionRadius * 2f,
                        currentSegment.ship.facing-135f
                    )
                }

                // figure out if we need to draw the entire segment or need to do interpolation depending on phase length left
                val currentSegmentLength = (currentSegment.segmentLength ?: (currentSegment.ship.collisionRadius * 1.5f))
                if(phasedLength > currentSegmentLength){
                    GL11.glVertex2f(nextTopRight.x, nextTopRight.y)
                    GL11.glVertex2f(nextTopLeft.x, nextTopLeft.y)
                    currentTopRight = nextTopRight
                    currentTopLeft = nextTopLeft
                }
                else {
                    val phasedProgress = (phasedLength/currentSegmentLength).toFloat()
                    val middleRight = Vector2f(
                        currentTopRight.x + (nextTopRight.x - currentTopRight.x) * phasedProgress,
                        currentTopRight.y + (nextTopRight.y - currentTopRight.y) * phasedProgress
                    )

                    val middleLeft = Vector2f(
                        currentTopLeft.x + (nextTopLeft.x - currentTopLeft.x) * phasedProgress,
                        currentTopLeft.y + (nextTopLeft.y - currentTopLeft.y) * phasedProgress
                    )

                    GL11.glVertex2f(middleRight.x, middleRight.y)
                    GL11.glVertex2f(middleLeft.x, middleLeft.y)
                }

                // move on to next segment, kinda hacky, but we never do another loop when next segment is null anyways
                phasedLength -= currentSegmentLength
                currentSegment = nextSegment ?: currentSegment
                nextSegment = nextSegment?.segmentBehind
            }

            GL11.glEnd()
        }
    }
}