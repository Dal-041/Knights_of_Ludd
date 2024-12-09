package org.selkie.zea.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import org.selkie.kol.ReflectionUtils
import java.awt.Color
import java.util.*

class NullspaceGateTransferVFXRenderer(var gate: SectorEntityToken, var first: Boolean) : LunaCampaignRenderingPlugin {



    var interval = IntervalUtil(0.2f, 0.7f)
    var glitchDuration = 0f
    var noiseMult = 1f
    var lastNoise = 0f
    var shader = 0

    @Transient var backgroundSprite = Global.getSettings().getSprite("backgrounds", "zea_bg_dusk")
    @Transient var glowOverwrite = Global.getSettings().getSprite("fx", "nullspace_gate_glow_overwrite")

    var startDelay = 1.5f
    var fade = 0f

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

    fun isDone() = !Global.getSector().playerFleet.isInHyperspaceTransition || Global.getSector().playerFleet.containingLocation.name == "Nullspace"
    override fun isExpired(): Boolean {
       return isDone()
    }

    fun getFadeIn() = easeInOutSine(fade)

    fun easeInOutSine(x: Float): Float {
        return (-(Math.cos(Math.PI * x) - 1) / 2).toFloat();
    }

    override fun advance(amount: Float) {

        //Disable Speedup if this is the first ever jump to nullspace, i.e Triggered by the Gate-Glitch
        if (first) {
            var state = AppDriver.getInstance().currentState
            if (state is CampaignState) {
                ReflectionUtils.set("fastForward", state, false)
            }
        }


        startDelay -= 1f * amount
        if (startDelay >= 0) return

        fade += 0.20f * amount
        fade = MathUtils.clamp(fade, 0f, 1f)

        //Glitch Interval
        interval.advance(amount * getFadeIn())
        if (interval.intervalElapsed()) {
            glitchDuration = MathUtils.getRandomNumberInRange(0.2f, 0.75f)
            noiseMult = MathUtils.getRandomNumberInRange(0.9f, 1.4f)
        }

        if (glitchDuration >= 0 && !Global.getSector().isPaused) {
            glitchDuration -= 1 * amount
        }

    }

    var layers = EnumSet.of(CampaignEngineLayers.ABOVE_STATIONS, CampaignEngineLayers.ABOVE)
    override fun getActiveLayers(): EnumSet<CampaignEngineLayers> {
        return layers
    }

    override fun render(layer: CampaignEngineLayers, viewport: ViewportAPI) {
        if (isDone()) return

        if (backgroundSprite == null) {
            backgroundSprite = Global.getSettings().getSprite("backgrounds", "zea_bg_dusk")
            glowOverwrite = Global.getSettings().getSprite("fx", "nullspace_gate_glow_overwrite")
        }


        if (layer == CampaignEngineLayers.ABOVE_STATIONS) {
            var vWidth = viewport.visibleWidth
            var vHeight = viewport.visibleHeight

            var vx = viewport.llx
            var vy = viewport.lly

            var outerRadius = 75f
            var radius = outerRadius * getFadeIn()

            val spec = gate.customEntitySpec
            val scale: Float = spec.getSpriteWidth() / Global.getSettings().getSprite(spec.getSpriteName()).getWidth()

            startStencil(gate.location, radius, 100)

            backgroundSprite.setSize(vWidth, vWidth)
            backgroundSprite.color = Color(255, 255, 255)
            backgroundSprite.alphaMult = 1f /** getFadeIn()*/
            backgroundSprite.renderAtCenter(vx + vWidth / 2, vy + vHeight / 2)

            endStencil()
            renderBorder(gate.location, radius, Color(75, 0, 200, 150), 100)

            glowOverwrite.setSize(256f * scale, 256f * scale)
            var glowFade = MathUtils.clamp(fade * 6f, 0f, 1f)
            glowOverwrite.alphaMult = glowFade
            glowOverwrite.renderAtCenter(gate.location.x, gate.location.y)

            //renderBorder(gate.location, outerRadius, Color(75, 0, 200, (250 * getFadeIn()).toInt()), 100)
        }

        if (layer == CampaignEngineLayers.ABOVE) {
            //Screen texture can be unloaded if graphicslib shaders are disabled, causing a blackscreen
            if (ShaderLib.getScreenTexture() != 0) {
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
                GL20.glUniform3f(GL20.glGetUniformLocation(shader, "colorMult"), 1f - (0.1f * getFadeIn()), 1f - (0.4f * getFadeIn()), 1f - (0.1f * getFadeIn()))

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
        }


    }





    fun startStencil(loc: Vector2f, radius: Float, circlePoints: Int) {

        GL11.glClearStencil(0);
        GL11.glStencilMask(0xff);
        //set everything to 0
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        //disable drawing colour, enable stencil testing
        GL11.glColorMask(false, false, false, false); //disable colour
        GL11.glEnable(GL11.GL_STENCIL_TEST); //enable stencil

        // ... here you render the part of the scene you want masked, this may be a simple triangle or square, or for example a monitor on a computer in your spaceship ...
        //begin masking
        //put 1s where I want to draw
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xff); // Do not test the current value in the stencil buffer, always accept any value on there for drawing
        GL11.glStencilMask(0xff);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE); // Make every test succeed

        // <draw a quad that dictates you want the boundaries of the panel to be>

        GL11.glBegin(GL11.GL_POLYGON) // Middle circle

        val x = loc.x
        val y = loc.y

        for (i in 0..circlePoints) {

            val angle: Double = (2 * Math.PI * i / circlePoints)
            val vertX: Double = Math.cos(angle) * (radius)
            val vertY: Double = Math.sin(angle) * (radius)
            GL11.glVertex2d(x + vertX, y + vertY)
        }

        GL11.glEnd()

        //GL11.glRectf(x, y, x + width, y + height)

        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP); // Make sure you will no longer (over)write stencil values, even if any test succeeds
        GL11.glColorMask(true, true, true, true); // Make sure we draw on the backbuffer again.

        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF); // Now we will only draw pixels where the corresponding stencil buffer value equals 1
        //Ref 0 causes the content to not display in the specified area, 1 causes the content to only display in that area.

        // <draw the lines>

    }

    fun endStencil() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    fun renderBorder(loc: Vector2f, radius: Float, color: Color, circlePoints: Int) {
        var c = color
        GL11.glPushMatrix()

        GL11.glTranslatef(0f, 0f, 0f)
        GL11.glRotatef(0f, 0f, 0f, 1f)

        GL11.glDisable(GL11.GL_TEXTURE_2D)


        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)


        GL11.glColor4f(c.red / 255f,
            c.green / 255f,
            c.blue / 255f,
            c.alpha / 255f * (1f))

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val x = loc.x
        val y = loc.y


        for (i in 0..circlePoints) {
            val angle: Double = (2 * Math.PI * i / circlePoints)
            val vertX: Double = Math.cos(angle) * (radius)
            val vertY: Double = Math.sin(angle) * (radius)
            GL11.glVertex2d(x + vertX, y + vertY)
        }

        GL11.glEnd()
        GL11.glPopMatrix()
    }

}