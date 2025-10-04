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
    lateinit var renderer: AmeonnaPhaseRenderer

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {

        val ship = stats.entity as ShipAPI ?: return

        if (!init) {
            init = true
            renderer = AmeonnaPhaseRenderer(ship, ship.system)
            Global.getCombatEngine().addLayeredRenderingPlugin(renderer)
        }

        if (!activated) {
            activated = true
        }



    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {

    }

    class AmeonnaPhaseRenderer(var ship: ShipAPI, var system: ShipSystemAPI) : BaseCombatLayeredRenderingPlugin() {

        override fun getRenderRadius(): Float {
            return 1000000f
        }

        var layers = ReflectionUtilsV2.get("layers", ship) as EnumSet<CombatEngineLayers>
        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return layers
        }

        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {

            //if (layer == CombatEngineLayers.STATION_WEAPONS_LAYER) return


           /* ship.alphaMult = 1f
            ship.extraAlphaMult = 1f
            ship.extraAlphaMult2 = 0.5f

            startStencil(ship, true)
            ReflectionUtilsV2.invoke("render", ship, layer, viewport)
            endStencil()*/

            var modules = ArrayList<ShipAPI>()
            modules.add(ship)
            modules.addAll(ship.childModulesCopy)

            for (module in modules) {
               module.extraAlphaMult = 1f
            }

            val x = ship.location.x
            val y = ship.location.y
            startDepthMask(ship)
            ReflectionUtilsV2.invoke("render", ship, layer, viewport)
            endDepthMask()
            for (module in modules) {
                module.extraAlphaMult = 0.25f
            }
        }

        fun startDepthMask(ship: ShipAPI) {
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
            GL11.glDepthFunc(GL11.GL_NOTEQUAL)
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