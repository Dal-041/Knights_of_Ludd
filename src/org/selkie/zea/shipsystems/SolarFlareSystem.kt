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
import org.magiclib.kotlin.optColor
import org.selkie.kol.Utils
import org.selkie.zea.hullmods.ElysianBuiltin
import org.selkie.kol.plugins.KOL_ModPlugin
import java.io.IOException
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class SolarFlareSystem: BaseShipSystemScript() {

    var renderingPlugins: MutableList<Aurora> = mutableListOf()
    var tether: SolarTether? = null
    val allyCheckInterval = IntervalUtil(0.7f, 1.3f)
    var allies: List<ShipAPI> = listOf()

    companion object{
        val Range = 1000f
        val DropoffRange = 200f
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


        //ship.system.specAPI.specJson.optColor("auroraInnerColor", Color(235, 165, 20, 150))
        //ship.system.specAPI.specJson.optColor("auroraOuterColor", Color(235, 165, 20, 150))

        if (renderingPlugins.isEmpty()){
            renderingPlugins.add(Aurora(ship))
            renderingPlugins.add(Aurora(ship))
            engine.addLayeredRenderingPlugin(renderingPlugins[0])
            engine.addLayeredRenderingPlugin(renderingPlugins[1])

            renderingPlugins[1].radiusInner = ship.collisionRadius / 1.5f
            renderingPlugins[1].radiusOuter = ship.collisionRadius * 1.5f
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

        renderingPlugins[0].alphaMult = effectLevel
        renderingPlugins[0].thicknessMult = effectLevel

        renderingPlugins[1].alphaMult = effectLevel/3f
        renderingPlugins[1].thicknessMult = effectLevel*2f

        if (tether == null) {
            tether = SolarTether(ship)
            engine.addLayeredRenderingPlugin(tether!!)
        }
        tether!!.alphaMult = effectLevel

        val amount = engine.elapsedInLastFrame
        allyCheckInterval.advance(amount)

        // this is prob laggy to do everyframe
        if (allyCheckInterval.intervalElapsed()){
            allies = AIUtils.getNearbyAllies(ship, 2500f)
        }

        val tetherTargets = mutableListOf<Pair<ShipAPI, Float>>()
        allies.forEach { ally ->
            val rangeLevel = Utils.linMap(
                MathUtils.getDistance(ship, ally),
                Range,Range + DropoffRange,
                1f, 0f
            )
            if (rangeLevel <= 0f) return@forEach

            val coronalCaps = ally.getListeners(ElysianBuiltin.ElysianBuiltinListener::class.java)
            // fighters do not get visual bands
            if (ally.isAlive && !ally.isFighter && coronalCaps.isNotEmpty()) tetherTargets.add(ally to rangeLevel)

            coronalCaps.forEach { coronalCap ->
                val boost = when(ally.hullSize){
                    ShipAPI.HullSize.DEFAULT -> 0.5f
                    ShipAPI.HullSize.FIGHTER -> 0.5f
                    ShipAPI.HullSize.FRIGATE -> 0.4f
                    ShipAPI.HullSize.DESTROYER -> 0.3f
                    ShipAPI.HullSize.CRUISER -> 0.2f
                    ShipAPI.HullSize.CAPITAL_SHIP -> 0.1f
                }
                // boost is % per second, full effect at Range, dropoff to no effect at Range + DropoffRange
                coronalCap.capacitorAmount += ally.maxFlux * boost * Global.getCombatEngine().elapsedInLastFrame *
                        effectLevel * rangeLevel
            }
        }
        tether!!.targets = tetherTargets
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
        var radiusInner: Float = ship.collisionRadius/3f
        var radiusOuter: Float = ship.collisionRadius

        val flares = mutableListOf<Flare>()

        var thicknessMult: Float = 1f
        var alphaMult: Float = 1.0f


        // --- Internal animation state ---
        private var phaseAngle: Float = Random.nextFloat() * 360f // random initial angle

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

            val radStart: Float = radiusInner
            var radEnd: Float = radiusOuter

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

    // Wavy bands from the ship to every ally its field is charging. Fighters are not linked.
    class SolarTether(val ship: ShipAPI): BaseCombatLayeredRenderingPlugin() {
        companion object {
            const val TEXTURE_PATH = "graphics/fx/slipstream3.png"
        }

        // path sprites are not preloaded by vanilla, so load the texture before fetching the sprite
        val sprite: SpriteAPI = try {
            Global.getSettings().loadTexture(TEXTURE_PATH)
            Global.getSettings().getSprite(TEXTURE_PATH)
        } catch (e: IOException) {
            Global.getSettings().getSprite(TEXTURE_PATH)
        }
        var targets: List<Pair<ShipAPI, Float>> = emptyList()
        var alphaMult = 0f
        private var phase = 0f
        private var texScroll = 0f
        private var time = 0f

        // per-ally shape: amplitude and bend weight ease to a new random target, then hold.
        // Only amplitudes are moved, never frequencies, so a new target fades in instead of sliding the wave.
        private class Shape {
            var curve = 1f
            var bend = 0.2f
            val bendSign = if (Random.nextBoolean()) 1f else -1f
            var fromCurve = 1f
            var fromBend = 0.2f
            var toCurve = 1f
            var toBend = 0.2f
            var progress = 1f
            var duration = 6f
            var holdIn = MathUtils.getRandomNumberInRange(6f, 14f)
        }

        private val shapes = HashMap<String, Shape>()

        override fun advance(amount: Float) {
            // Culling uses the plugin entity, not the world-space vertices drawn below.
            entity?.location?.set(ship.location)
            phase = (phase + amount * 0.5f) % (Math.PI.toFloat() * 2f)
            texScroll = (texScroll + amount * 0.3f) % 1f
            time += amount

            shapes.values.forEach { shape ->
                if (shape.progress < 1f) {
                    shape.progress = min(1f, shape.progress + amount / shape.duration)
                    // smoothstep: the change starts and ends at zero speed, so it never reads as a snap
                    val s = shape.progress * shape.progress * (3f - 2f * shape.progress)
                    shape.curve = shape.fromCurve + (shape.toCurve - shape.fromCurve) * s
                    shape.bend = shape.fromBend + (shape.toBend - shape.fromBend) * s
                } else {
                    shape.holdIn -= amount
                    if (shape.holdIn <= 0f) {
                        shape.fromCurve = shape.curve
                        shape.fromBend = shape.bend
                        // a third of the time the tether settles into one gentle arc, otherwise it keeps its waves
                        if (Random.nextFloat() < 0.33f) {
                            shape.toBend = MathUtils.getRandomNumberInRange(0.8f, 1f)
                            shape.toCurve = MathUtils.getRandomNumberInRange(0.5f, 0.9f)
                        } else {
                            shape.toBend = MathUtils.getRandomNumberInRange(0f, 0.3f)
                            shape.toCurve = MathUtils.getRandomNumberInRange(0.85f, 1.2f)
                        }
                        shape.duration = MathUtils.getRandomNumberInRange(5f, 8f)
                        shape.holdIn = MathUtils.getRandomNumberInRange(6f, 14f)
                        shape.progress = 0f
                    }
                }
            }
            shapes.keys.retainAll(targets.map { it.first.id }.toSet())
        }

        override fun getRenderRadius(): Float = Range + DropoffRange

        override fun render(layer: CombatEngineLayers, viewport: ViewportAPI) {
            if (alphaMult <= 0f || targets.isEmpty() || !ship.isAlive) return
            if (sprite.getTextureId() == 0) return

            val from = ship.location
            GL11.glPushMatrix()
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            sprite.bindTexture()
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)

            for ((target, strength) in targets) {
                if (!target.isAlive) continue
                val to = target.location
                val dx = to.x - from.x
                val dy = to.y - from.y
                val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (length < 1f) continue

                val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
                val nx = -sin(angle)
                val ny = cos(angle)
                val shape = shapes.getOrPut(target.id) { Shape() }
                val amplitude = min(length * 0.08f, 50f) * shape.curve
                val rootPhase = phase + ((target.id.hashCode() and 0x7FFFFFFF) % 360) * 0.01f
                val flicker = 0.8f + 0.2f * sin(time * 7f + rootPhase)
                val a = alphaMult * strength * flicker

                // halo, body and core drift on their own waves, so the strands weave like a flare instead of stacking
                drawStrand(from.x, from.y, dx, dy, nx, ny, amplitude,
                    width = 104f, waves = 1.2f, phase = rootPhase * 0.6f, lateral = amplitude * 0.06f * shape.bendSign,
                    bend = shape.bend, bendSign = shape.bendSign,
                    r = 190, g = 80, b = 20, alpha = 0.05f * a, uScale = 0.8f)
                drawStrand(from.x, from.y, dx, dy, nx, ny, amplitude,
                    width = 76f, waves = 1.5f, phase = rootPhase * 0.7f, lateral = -amplitude * 0.05f * shape.bendSign,
                    bend = shape.bend, bendSign = shape.bendSign,
                    r = 205, g = 95, b = 22, alpha = 0.07f * a, uScale = 1f)
                drawStrand(from.x, from.y, dx, dy, nx, ny, amplitude,
                    width = 58f, waves = 1.8f, phase = rootPhase * 0.8f, lateral = 0f,
                    bend = shape.bend, bendSign = shape.bendSign,
                    r = 200, g = 90, b = 20, alpha = 0.1f * a, uScale = 1.2f)
                drawStrand(from.x, from.y, dx, dy, nx, ny, amplitude,
                    width = 30f, waves = 2.5f, phase = rootPhase, lateral = 0f,
                    bend = shape.bend, bendSign = shape.bendSign,
                    r = 230, g = 120, b = 25, alpha = 0.22f * a, uScale = 2f)
                drawStrand(from.x, from.y, dx, dy, nx, ny, amplitude,
                    width = 13f, waves = 4f, phase = rootPhase * 1.7f, lateral = amplitude * 0.12f * shape.bendSign,
                    bend = shape.bend, bendSign = shape.bendSign,
                    r = 240, g = 160, b = 40, alpha = 0.5f * a, uScale = 3f)
                drawStrand(from.x, from.y, dx, dy, nx, ny, amplitude,
                    width = 4f, waves = 5.5f, phase = -rootPhase * 1.3f, lateral = -amplitude * 0.2f * shape.bendSign,
                    bend = shape.bend, bendSign = shape.bendSign,
                    r = 255, g = 235, b = 190, alpha = 0.8f * a, uScale = 4f)
            }
            GL11.glPopMatrix()
        }

        private fun drawStrand(
            fromX: Float, fromY: Float, dx: Float, dy: Float,
            nx: Float, ny: Float, amplitude: Float,
            width: Float, waves: Float, phase: Float, lateral: Float,
            bend: Float, bendSign: Float,
            r: Int, g: Int, b: Int, alpha: Float, uScale: Float
        ) {
            val segments = 26
            GL11.glColor4ub(r.toByte(), g.toByte(), b.toByte(), (255f * alpha).toInt().coerceIn(0, 255).toByte())
            GL11.glBegin(GL11.GL_QUAD_STRIP)
            for (i in 0..segments) {
                val t = i / segments.toFloat()
                val taper = sin(Math.PI.toFloat() * t)
                // blend the scrolling wave and a static single arc; blending amplitudes keeps the shape smooth
                val waveTerm = sin(t * Math.PI.toFloat() * waves + phase) * (1f - bend)
                val bendTerm = sin(Math.PI.toFloat() * t) * bend * bendSign
                val wiggle = lateral + (waveTerm + bendTerm) * amplitude * taper
                val px = fromX + dx * t + nx * wiggle
                val py = fromY + dy * t + ny * wiggle
                val halfWidth = width * (0.3f + 0.7f * taper)
                // texture runs along the band (u) and across it (v), matching the sprite's streak axis
                val u = t * uScale + texScroll
                GL11.glTexCoord2f(u, 0f)
                GL11.glVertex2f(px + nx * halfWidth, py + ny * halfWidth)
                GL11.glTexCoord2f(u, 1f)
                GL11.glVertex2f(px - nx * halfWidth, py - ny * halfWidth)
            }
            GL11.glEnd()
        }

    }
}