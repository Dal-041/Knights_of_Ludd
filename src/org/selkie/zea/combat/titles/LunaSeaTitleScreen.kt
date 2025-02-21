package org.selkie.zea.combat.titles

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.campaign.WarpingSpriteRenderer
import com.fs.starfarer.combat.CombatEngine
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver
import lunalib.lunaTitle.BaseLunaTitleScreenPlugin
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.ReflectionUtils
import java.awt.Color

class LunaSeaTitleScreen : BaseLunaTitleScreenPlugin() {

    var renderer = CombatPlanetRenderer(Global.getSettings().allPlanetSpecs.find { it.planetType == "star_blue_giant" }!!, 0f, 0f, 600f)

    override fun pickBasedOnSystemCondition(lastSystemID: String, lastSystemTags: ArrayList<String>): Boolean {
        return lastSystemID == "the luna sea"
    }

    override fun init(engine: CombatEngineAPI?) {

        Global.getCombatEngine().backgroundColor = Color(255, 255, 255)
        CombatEngine.replaceBackground("graphics/zea/backgrounds/zea_bg_dawn.png", true)
        //Global.getCombatEngine().setHyperspaceMode()
        var warper = CombatBackgroundWarper(8, 0.25f)
        ReflectionUtils.setFieldOfType(WarpingSpriteRenderer::class.java, Global.getCombatEngine(), warper)

        Global.getSoundPlayer().playCustomMusic(1, 1, "music_zea_lunasea_theme", true)

        Global.getCombatEngine().addLayeredRenderingPlugin(renderer)



    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        var viewport = Global.getCombatEngine().viewport
        viewport.isExternalControl = true

        var llx = viewport.llx
        var lly = viewport.lly

        var width = Global.getSettings().screenWidth * 2
        var height = Global.getSettings().screenHeight * 2

        viewport.set(0f-width/2, 0f-height/2, width, height)

        renderer.x = viewport.llx + width / 2
        renderer.y = viewport.lly + height / 2
    }

    override fun renderInUICoords(viewport: ViewportAPI?) {

    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {

    }

}