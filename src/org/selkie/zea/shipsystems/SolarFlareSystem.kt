package org.selkie.zea.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Misc.ZERO
import org.dark.shaders.light.LightShader
import org.dark.shaders.light.StandardLight
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import org.selkie.kol.Utils
import org.selkie.zea.hullmods.ElysianBuiltin
import org.selkie.kol.plugins.KOL_ModPlugin
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class SolarFlareSystem: BaseShipSystemScript() {

    var renderingPlugin: Aurora? = null
    val allyCheckInterval = IntervalUtil(0.7f, 1.3f)
    var allies: List<ShipAPI> = listOf()

    companion object{
        val Range = 2000f
        val DropoffRange = 500f
        val SpeedDebuff = -70f // in percentage
    }


    override fun apply(
        stats: MutableShipStatsAPI?,
        id: String?,
        state: ShipSystemStatsScript.State?,
        effectLevel: Float
    ) {
        val ship = stats!!.entity as ShipAPI
        val engine = Global.getCombatEngine()
        if (renderingPlugin == null){
            renderingPlugin = Aurora(ship)
            engine.addLayeredRenderingPlugin(renderingPlugin)
        }

        val speedDebuff = SpeedDebuff * effectLevel
        stats.maxSpeed.modifyPercent("SolarFlareSystem", speedDebuff)

        if (effectLevel > 0 && ship == engine.playerShip){
            engine.maintainStatusForPlayerShip(
                "SolarFlareSystem",
                ship.system.specAPI.iconSpriteName,
                "Solar Tunnel",
                speedDebuff.roundToInt().toString() + "% lower top speed",
                true
            )
        }

        renderingPlugin!!.alphaMult = effectLevel
        renderingPlugin!!.thicknessMult = effectLevel

        val amount = engine.elapsedInLastFrame
        allyCheckInterval.advance(amount)

        // this is prob laggy to do everyframe
        if (allyCheckInterval.intervalElapsed()){
            allies = AIUtils.getNearbyAllies(ship, 2500f)
        }

        allies.forEach { ally ->
            val coronalCaps = ally.getListeners(ElysianBuiltin.ElysianBuiltinListener::class.java)
            coronalCaps.forEach { coronalCap ->
                val boost = when(ally.hullSize){
                    ShipAPI.HullSize.DEFAULT -> 0.5f
                    ShipAPI.HullSize.FIGHTER -> 0.5f
                    ShipAPI.HullSize.FRIGATE -> 0.4f
                    ShipAPI.HullSize.DESTROYER -> 0.3f
                    ShipAPI.HullSize.CRUISER -> 0.2f
                    ShipAPI.HullSize.CAPITAL_SHIP -> 0.1f
                }
                val rangeLevel = Utils.linMap(
                    MathUtils.getDistance(ship, ally),
                    Range,Range + DropoffRange,
                    1f, 0f
                )
                // boost is % per second, full effect at Range, dropoff to no effect at Range + DropoffRange
                coronalCap.capacitorAmount += ally.maxFlux * boost * Global.getCombatEngine().elapsedInLastFrame *
                        effectLevel * rangeLevel
            }
        }
    }

    // never called
    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {}

    class Aurora(val ship: ShipAPI): BaseCombatLayeredRenderingPlugin() {

        // --- Data class for manually defining flares ---
        data class Flare(
            val angle: Float,
            val arc: Float,
            val thicknessMult: Float,
            val thicknessFlat: Float,
            var intensity: Float,
            val highlightColor: Color
        )

        lateinit var pluginEntity: CombatEntityAPI

        // --- Public properties with defaults from StarCoronaTerrainPlugin.java ---
        val sprite: SpriteAPI = Global.getSettings().getSprite("terrain", "aurora")
        var outerRadius = ship.collisionRadius

        /** The list of flares to render. You can add or remove from this list directly. */
        val flares = mutableListOf<Flare>()

        var thicknessMult: Float = 1f
        /** Overall transparency multiplier. */
        var alphaMult: Float = 1.0f


        // --- Internal animation state ---
        private var phaseAngle: Float = 0f

        private val PARTICLE_INTERVAL = IntervalUtil(1f, 1f)

        override fun advance(amount: Float) {
            // This is the animation logic from AuroraRenderer.advance()
            phaseAngle = (phaseAngle + amount * 60f) % 360f

            if (alphaMult == 0f) return

            Global.getCombatEngine().addSmoothParticle(
                ship.location,
                ZERO,
                ship.collisionRadius * 4f,
                0.25f,
                0.1f * amount,
                Color(255, 125, 25, (75 * alphaMult).toInt())
            )


            if (KOL_ModPlugin.hasGraphicsLib) {
                PARTICLE_INTERVAL.advance(amount)
                if (PARTICLE_INTERVAL.intervalElapsed()) {
                    val light = StandardLight(Vector2f(), Vector2f(), Vector2f(), ship)
                    light.size = ship.collisionRadius / 1.1f
                    light.intensity = 0.33f
                    light.setLifetime(0.66f)
                    light.autoFadeOutTime = 1f
                    light.setColor(Color(255, 125, 25))
                    light.location = ship.location
                    LightShader.addLight(light)
                }
            }

            if (entity != null){
                entity.location.set(ship.location)
            }
        }

        override fun getRenderRadius(): Float {
            return ship.collisionRadius
        }

        override fun render(layer: CombatEngineLayers, viewport: ViewportAPI) {
            if (alphaMult <= 0) return

            val bandWidthInTexture: Float = 256f
            var bandIndex: Float

            val radStart: Float = ship.collisionRadius - 100f
            var radEnd: Float = ship.collisionRadius

            if (radEnd < radStart + 10f) radEnd = radStart + 10f

            val circ = (Math.PI * 2f * (radStart + radEnd) / 2f).toFloat()
            val pixelsPerSegment = 50f
            val segments = Math.round(circ / pixelsPerSegment).toFloat()

            val startRad = Math.toRadians(0.0).toFloat()
            val endRad = Math.toRadians(360.0).toFloat()
            val spanRad = abs(endRad - startRad)
            val anglePerSegment = spanRad / segments


            GL11.glPushMatrix()
            GL11.glTranslatef(ship.location.x, ship.location.y, 0f)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            sprite.bindTexture()
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)

            val thickness = (radEnd - radStart) * 1f
            val radius = radStart

            var texProgress = 0f
            val texHeight: Float = sprite.textureHeight
            val imageHeight: Float = sprite.height
            var texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness

            val totalTex = max(1f, Math.round(texPerSegment * segments).toFloat())
            texPerSegment = totalTex / segments

            val texWidth: Float = sprite.textureWidth
            val imageWidth: Float = sprite.width

            for (iter in 0..1) {
                if (iter == 0) {
                    bandIndex = 1f
                } else {
                    bandIndex = 0f
                }

                val leftTX = bandIndex * texWidth * bandWidthInTexture / imageWidth
                val rightTX = (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f

                GL11.glBegin(GL11.GL_QUAD_STRIP)
                var i = 0f
                while (i < segments + 1) {
                    val segIndex = i % segments.toInt()


                    //float phaseAngleRad = (float) Math.toRadians(phaseAngle + segIndex * 10) + (segIndex * anglePerSegment * 10f);
                    val phaseAngleRad: Float
                    if (iter == 0) {
                        phaseAngleRad = Math.toRadians(phaseAngle.toDouble()).toFloat() + (segIndex * anglePerSegment * 10f)
                    } else { //if (iter == 1) {
                        phaseAngleRad = Math.toRadians(-phaseAngle.toDouble()).toFloat() + (segIndex * anglePerSegment * 5f)
                    }


                    var angle = Math.toDegrees((segIndex * anglePerSegment).toDouble()).toFloat()
                    if (iter == 1) angle += 180f

                    var blockerMax = 100000f

                    val pulseSin = sin(phaseAngleRad.toDouble()).toFloat()
                    var pulseMax: Float = thickness * 0.2f

                    if (pulseMax > blockerMax * 0.5f) {
                        pulseMax = blockerMax * 0.5f
                    }
                    val pulseAmount = pulseSin * pulseMax
                    var pulseInner = pulseAmount * 0.1f
                    pulseInner *= 0.1f

                    val r = radius

                    val thicknessFlat: Float = 50f

                    val theta = anglePerSegment * segIndex

                    val cos = cos(theta.toDouble()).toFloat()
                    val sin = sin(theta.toDouble()).toFloat()

                    var rInner = r - pulseInner
                    if (rInner < r * 0.9f) rInner = r * 0.9f

                    var rOuter = (r + thickness * thicknessMult - pulseAmount + thicknessFlat)

                    val x1 = cos * rInner
                    val y1 = sin * rInner
                    var x2 = cos * rOuter
                    var y2 = sin * rOuter

                    x2 += (cos(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                    y2 += (sin(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()

                    val color: Color = Color(235, 165, 20, 150)
                    var alpha: Float = 1f
                    GL11.glColor4ub(
                        color.red.toByte(),
                        color.green.toByte(),
                        color.blue.toByte(),
                        (color.alpha.toFloat() * alphaMult * alpha).toInt().toByte()
                    )

                    GL11.glTexCoord2f(leftTX, texProgress)
                    GL11.glVertex2f(x1, y1)
                    GL11.glTexCoord2f(rightTX, texProgress)
                    GL11.glVertex2f(x2, y2)

                    texProgress += texPerSegment * 1f
                    i++
                }
                GL11.glEnd()

                GL11.glRotatef(180f, 0f, 0f, 1f)
            }
            GL11.glPopMatrix()
        }
    }
}