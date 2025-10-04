package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import java.util.*

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

        var layers = EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER, CombatEngineLayers.PHASED_SHIPS_LAYER)
        override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
            return layers
        }

        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {

            ship.alphaMult = 1f

            if (layer == CombatEngineLayers.UNDER_SHIPS_LAYER) {


            }

            if (layer == CombatEngineLayers.PHASED_SHIPS_LAYER) {


            }

            ship.alphaMult = 0f

        }
    }
}