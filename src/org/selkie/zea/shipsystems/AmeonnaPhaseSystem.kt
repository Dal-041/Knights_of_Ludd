package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import org.lwjgl.opengl.GL11
import org.selkie.kol.ReflectionUtilsV2
import java.util.*
import kotlin.collections.ArrayList

class AmeonnaPhaseSystem : BaseShipSystemScript() {

    var init = false
    var activated = false
    //lateinit var renderer: AmeonnaPhaseRenderer

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {

        val ship = stats.entity as ShipAPI ?: return

        if (!init) {
            init = true
            //renderer = AmeonnaPhaseRenderer(ship, ship.system)

            var modules = ArrayList<ShipAPI>()
            modules.add(ship)
            modules.addAll(ship.childModulesCopy)

            for (module in modules) {
                var renderer = AmeonnaPhaseRenderer(module, ship.system)
                Global.getCombatEngine().addLayeredRenderingPlugin(renderer)
            }


        }

        if (!activated) {
            activated = true
        }



    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {

    }

    class AmeonnaPhaseRenderer(var ship: ShipAPI, var system: ShipSystemAPI) : BaseCombatLayeredRenderingPlugin() {

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

                startDepthMask(ship, false)

                glow1.angle = ship.facing-90
                glow1.renderAtCenter(ship.location.x, ship.location.y)

                glow2.setAdditiveBlend()
                glow2.alphaMult = 0.3f
                glow2.angle = ship.facing-90
                glow2.renderAtCenter(ship.location.x, ship.location.y)

                endDepthMask()
            }
            //Split the ship in to a phased and non phased part
            else {
                //Set alpha to the normal amount
                ship.extraAlphaMult = 1f

                val x = ship.location.x
                val y = ship.location.y
                startDepthMask(ship, true)
                ReflectionUtilsV2.invoke("render", ship, layer, viewport)
                endDepthMask()

                //Set alpha to 0.25, so that the base game render renders this half, instead of having to render it an additional time
                ship.extraAlphaMult = 0.25f
            }
        }

        fun startDepthMask(ship: ShipAPI, renderInside: Boolean) {
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
            val x = ship.location.x
            val y = ship.location.y

            GL11.glRectf(x, y-500, x +500, y+500)

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


    }
}