package org.selkie.zea.combat.titles

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import java.util.*

class NullspaceTitleRenderer : BaseCombatLayeredRenderingPlugin() {

    @Transient
    var vignette = Global.getSettings().getSprite("graphics/kol/fx/kol_darkness_vignette.png")

    var interval = IntervalUtil(0.5f, 12f)
    var glitchDuration = 0f
    var noiseMult = 1f
    var lastNoise = 0f
    var shader = 0

    var layers = EnumSet.of(CombatEngineLayers.JUST_BELOW_WIDGETS)

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


        if (glitchDuration >= 0 && !Global.getCombatEngine().isPaused) {
            glitchDuration -= 1 * amount
        }

    }

    override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
        return layers
    }

    override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
        if (vignette == null) {
            vignette = Global.getSettings().getSprite("graphics/kol/fx/kol_darkness_vignette.png")
        }


        

        if (layer == CombatEngineLayers.JUST_BELOW_WIDGETS) {

            //Screen texture can be unloaded if graphicslib shaders are disabled, causing a blackscreen
            if (ShaderLib.getScreenTexture() != 0) {
                //Shader
                var t = Global.getCombatEngine().getTotalElapsedTime(true) / 1000f

                var noise = MathUtils.getRandomNumberInRange(0.3f, 1f)
                if (Global.getCombatEngine().isPaused) {
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
                GL20.glUniform3f(GL20.glGetUniformLocation(shader, "colorMult"), 1.2f, 1.1f, 1.2f)

                GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderLib.getScreenTexture());

                //Might Fix Incompatibilities with odd drivers
                GL20.glValidateProgram(shader)
                if (GL20.glGetProgrami(shader, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
                    ShaderLib.exitDraw()
                    return
                }

                GL11.glDisable(GL11.GL_BLEND);
                ShaderLib.screenDraw(ShaderLib.getScreenTexture(), GL13.GL_TEXTURE0 + 0)
                ShaderLib.exitDraw()

            }


            //Vignette
            var offset = 400f

            vignette.alphaMult = 1f
            vignette.setSize(viewport!!.visibleWidth + offset, viewport!!.visibleHeight + offset)
            vignette.render(viewport!!.llx - (offset / 2), viewport!!.lly - (offset / 2))

        }
    }
}