package org.selkie.zea.combat.titles

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.WarpingSpriteRenderer
import com.fs.starfarer.combat.CombatEngine
import com.fs.starfarer.title.TitleScreenState
import com.fs.state.AppDriver
import lunalib.lunaTitle.BaseLunaTitleScreenPlugin
import org.selkie.kol.ReflectionUtils
import org.selkie.kol.ReflectionUtils.getChildrenCopy
import org.selkie.zea.world.AbyssBackgroundWarper
import java.awt.Color

class NullspaceTitleScreen : BaseLunaTitleScreenPlugin() {

    var renderer = CombatPlanetRenderer(Global.getSettings().allPlanetSpecs.find { it.planetType == "zea_white_hole" }!!, 0f, 0f, 300f)

    override fun pickBasedOnSystemCondition(lastSystemID: String, lastSystemTags: ArrayList<String>): Boolean {
        return lastSystemID == "nullspace"
    }

    override fun init(engine: CombatEngineAPI?) {

        Global.getCombatEngine().backgroundColor = Color(255, 255, 255)
        CombatEngine.replaceBackground("graphics/zea/backgrounds/zea_bg_dusk.png", true)
        //Global.getCombatEngine().setHyperspaceMode()
        var warper = CombatBackgroundWarper(8, 0.125f)
        ReflectionUtils.setFieldOfType(WarpingSpriteRenderer::class.java, Global.getCombatEngine(), warper)

        Global.getSoundPlayer().playCustomMusic(1, 1, "music_zea_underworld_theme", true)

        Global.getCombatEngine().addLayeredRenderingPlugin(renderer)
        Global.getCombatEngine().addLayeredRenderingPlugin(NullspaceTitleRenderer())

    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        var viewport = Global.getCombatEngine().viewport
        viewport.isExternalControl = true

        var llx = viewport.llx
        var lly = viewport.lly

        var width = Global.getSettings().screenWidth
        var height = Global.getSettings().screenHeight

        viewport.set(0f-width/2, 0f-height/2, width, height)

        renderer.x = viewport.llx + width - 50f
        renderer.y = viewport.lly + height - 50f
    }

    override fun renderInUICoords(viewport: ViewportAPI?) {

    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {

    }

}