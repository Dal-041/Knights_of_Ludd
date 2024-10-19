package org.selkie.zea.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.util.IntervalUtil
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import java.util.*

class NullspaceVFXRenderer : LunaCampaignRenderingPlugin {

    @Transient
    var vignette = Global.getSettings().getSprite("graphics/kol/fx/kol_darkness_vignette.png")

    var interval = IntervalUtil(0.5f, 12f)

    var glitchDuration = 0f
    var noiseMult = 1f

    var lastNoise = 0f

    var shader = 0

    var layers = EnumSet.of(CampaignEngineLayers.ABOVE)

    init {
        shader = ShaderLib.loadShader(
            Global.getSettings().loadText("data/shaders/kolBaseVertex.shader"),
            Global.getSettings().loadText("data/shaders/kolNullspaceFragment.shader"))
        if (shader != 0) {
            GL20.glUseProgram(shader)

            GL20.glUniform1i(GL20.glGetUniformLocation(shader, "tex"), 0)

            GL20.glUseProgram(0)
        } else {
            var test = ""
        }
    }

    override fun isExpired(): Boolean {
        return false
    }

    override fun advance(amount: Float) {

        interval.advance(amount)
        if (interval.intervalElapsed()) {
            glitchDuration = MathUtils.getRandomNumberInRange(0.3f, 1.3f)
            noiseMult = MathUtils.getRandomNumberInRange(0.9f, 1.4f)
        }


        if (glitchDuration >= 0 && !Global.getSector().isPaused) {
            glitchDuration -= 1 * amount
        }

    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers> {
        return layers
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        if (vignette == null) {
            vignette = Global.getSettings().getSprite("graphics/kol/fx/kol_darkness_vignette.png")
        }


        

        if (layer == CampaignEngineLayers.ABOVE) {
            var playerfleet = Global.getSector().playerFleet
            if (playerfleet.containingLocation.name == "Nullspace") {


                //Shader
                var clock = Global.getSector().clock
                var t = clock.convertToSeconds(clock.elapsedDaysSinceGameStart()) / 1000f




                var noise = MathUtils.getRandomNumberInRange(0.3f, 1f)
                if (Global.getSector().isPaused) {
                    noise = lastNoise
                }

                if (glitchDuration <= 0) {
                    noise = 0f
                }

                lastNoise = noise

                noise *= noiseMult

                ShaderLib.beginDraw(shader);
                GL20.glUniform1f(GL20.glGetUniformLocation(shader, "iTime"), t / 8f)
                GL20.glUniform1f(GL20.glGetUniformLocation(shader, "noise"), noise)

                GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderLib.getScreenTexture());

                GL11.glDisable(GL11.GL_BLEND);
                ShaderLib.screenDraw(ShaderLib.getScreenTexture(), GL13.GL_TEXTURE0 + 0)
                ShaderLib.exitDraw()



                //Vignette
                var offset = 400f

                vignette.alphaMult = 1f
                vignette.setSize(viewport!!.visibleWidth + offset, viewport!!.visibleHeight + offset)
                vignette.render(viewport!!.llx - (offset / 2), viewport!!.lly - (offset / 2))

            }

        }
    }
}