package org.selkie.zea.combat.titles

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetSpecAPI
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.combat.CombatViewport
import com.fs.starfarer.combat.entities.terrain.Planet
import com.fs.starfarer.loading.specs.PlanetSpec
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.util.*

class CombatPlanetRenderer(var planetSpec: PlanetSpecAPI, var x: Float, var y: Float, var radius: Float) : BaseCombatLayeredRenderingPlugin() {

    var rPlanet: Planet = Planet(planetSpec as PlanetSpec, radius, 1f, Vector2f())

    override fun advance(amount: Float) {
        rPlanet.advance(amount)
    }

    override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
        return EnumSet.of(CombatEngineLayers.PLANET_LAYER)
    }

    override fun getRenderRadius(): Float {
        return 1000000000f
    }

    override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {

        var llx = 0f
        var lly = 0f


        var viewport = CombatViewport(llx - radius / 2.0F, lly - radius / 2.0F, radius, radius);
        //var viewport = CombatViewport(llx - radius ,lly - radius, radius, radius);

        var sWidth = Global.getSettings().screenWidth
        var sHeight = Global.getSettings().screenHeight

        rPlanet.scale = 1f

        GL11.glPushMatrix();
        //GL11.glTranslatef(sWidth / 2f, sHeight / 2f, 0.0F);
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glTranslatef(-rPlanet.location.x, -rPlanet.location.y, 0.0F);
        viewport.alphaMult = 1f

        //rPlanet.renderSphere(viewport)
        rPlanet.render(CombatEngineLayers.PLANET_LAYER, viewport)

        GL11.glPopMatrix()

    }

}