package org.selkie.zea.combat.titles

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.CombatEngine
import lunalib.lunaTitle.BaseLunaTitleScreenPlugin
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class ElysiaTitleScreen : BaseLunaTitleScreenPlugin() {

    var elysiaRenderer = CombatPlanetRenderer(Global.getSettings().allPlanetSpecs.find { it.planetType == "zea_red_hole" }!!, 0f, 0f, 500f)
    var blueGiantRenderer = CombatPlanetRenderer(Global.getSettings().allPlanetSpecs.find { it.planetType == "star_blue_giant" }!!, 0f, 0f, 100f)
    var neutronRenderer = CombatPlanetRenderer(Global.getSettings().allPlanetSpecs.find { it.planetType == "star_neutron" }!!, 0f, 0f, 15f)

    override fun pickBasedOnSystemCondition(lastSystemID: String, lastSystemTags: ArrayList<String>): Boolean {
        return lastSystemID == "elysia"
    }

    override fun init(engine: CombatEngineAPI?) {

        Global.getCombatEngine().backgroundColor = Color(255, 255, 255)
        CombatEngine.replaceBackground("graphics/zea/backgrounds/zea_bg_elysia.png", true)

        Global.getSoundPlayer().playCustomMusic(1, 1, "music_zea_elysia_theme", true)



        Global.getCombatEngine().addLayeredRenderingPlugin(neutronRenderer)
        Global.getCombatEngine().addLayeredRenderingPlugin(blueGiantRenderer)
        Global.getCombatEngine().addLayeredRenderingPlugin(elysiaRenderer)
        //Global.getCombatEngine().addLayeredRenderingPlugin(NullspaceTitleRenderer())

    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        var viewport = Global.getCombatEngine().viewport
        viewport.isExternalControl = true

        var llx = viewport.llx
        var lly = viewport.lly

        var width = Global.getSettings().screenWidth
        var height = Global.getSettings().screenHeight

        viewport.set(0f-width/2, 0f-height/2, width, height)

        elysiaRenderer.x = viewport.llx + width / 2
        elysiaRenderer.y = viewport.lly + height - 50f

        blueGiantRenderer.x = viewport.llx + 50
        blueGiantRenderer.y = viewport.lly + 50

        neutronRenderer.x = viewport.llx + width * 0.8f
        neutronRenderer.y = viewport.lly + height * 0.2f

    }

    override fun renderInUICoords(viewport: ViewportAPI?) {

    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {

    }

}