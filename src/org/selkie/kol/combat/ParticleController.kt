package org.selkie.kol.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*
import kotlin.random.Random

class ParticleController : BaseCombatLayeredRenderingPlugin() {
    override fun init(entity: CombatEntityAPI?) {
        super.init(entity)
        PARTICLES.clear()
    }

    override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
        PARTICLES
            .toList()
            .filter { it.layers.contains(layer) }
            .onEach { renderParticle(it) }

        PARTICLES.removeAll { it.elapsedRatio() >= 1f }
    }

    fun renderParticle(particle: ParticleData) {
        particle.preDraw()
        particle.draw()
        particle.postDraw()

        if (Global.getCombatEngine().isPaused) return
        particle.x += particle.xVel * Global.getCombatEngine().elapsedInLastFrame
        particle.y += particle.yVel * Global.getCombatEngine().elapsedInLastFrame
        particle.angle += particle.aVel * Global.getCombatEngine().elapsedInLastFrame
    }

    override fun getRenderRadius(): Float {
        return 9999999f
    }

    override fun isExpired(): Boolean {
        return false
    }

    override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
        return EnumSet.allOf(CombatEngineLayers::class.java)
    }

    override fun cleanup() {
        PARTICLES.clear()
    }

    companion object {
        internal val DEFAULT_PARTICLE_RENDER_LAYER = EnumSet.of(CombatEngineLayers.CONTRAILS_LAYER)
        private var PARTICLES: MutableList<ParticleData> = mutableListOf()

        fun addParticle(particle: ParticleData) {
            PARTICLES.add(particle)
        }

        fun mergeColors(fromColor: Color, toColor: Color, ratio: Float): Color {
            return mergeColors(fromColor, toColor, ratio, ratio)
        }

        fun mergeColors(fromColor: Color, toColor: Color, ratio: Float, alphaRatio: Float): Color {
            val r = fromColor.red * (1 - ratio) + toColor.red * ratio
            val g = fromColor.green * (1 - ratio) + toColor.green * ratio
            val b = fromColor.blue * (1 - ratio) + toColor.blue * ratio
            val a = fromColor.alpha * (1 - alphaRatio) + toColor.alpha * alphaRatio
            return Color(r / 255f, g / 255f, b / 255f, a / 255f)
        }
    }
}

open class ParticleData(
    var sprite: SpriteAPI,
    var x: Float,
    var y: Float,
    val xVel: Float,
    val yVel: Float,
    var angle: Float,
    val aVel: Float,
    val startingTime: Float,
    val ttl: Float,
    val startingSize: Float,
    val endSize: Float,
    val startingColor: Color,
    val endColor: Color,
    val layers: EnumSet<CombatEngineLayers> = ParticleController.DEFAULT_PARTICLE_RENDER_LAYER,
    val spritesInRow: Int = 1,
    val spritesInColumn: Int = 1,
    var texXStart: Float = -1f,
    var texYStart: Float = -1f,
    var texXEnd: Float = -1f,
    var texYEnd: Float = -1f,
) {
    init {
        if (texXStart == -1f || texXEnd == -1f) {
            val randomColumn = Random.nextInt(0, spritesInColumn - 1).toFloat() / spritesInColumn
            texXStart = randomColumn
            texXEnd = randomColumn + (1f / spritesInColumn)
        }


        if (texYStart == -1f || texYEnd == -1f) {
            val randomRow = Random.nextInt(0, spritesInRow - 1).toFloat() / spritesInRow
            texYStart = randomRow
            texYEnd = randomRow + (1f / spritesInRow)
        }
    }

    open fun preDraw() {

    }

    open fun draw() {
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sprite.textureId)
        GL11.glBegin(GL11.GL_QUADS)

        val elapsed = elapsedRatio()
        val size = startingSize * elapsed + endSize * (1f - elapsed)
        val position = Vector2f(x, y)
        val angle = angle

        GL11.glColor4ub(
            getColor().red.toByte(),
            getColor().green.toByte(),
            getColor().blue.toByte(),
            (getColor().alpha * elapsed).toInt().toByte()
        )

        val texXStart = texXStart
        val texYStart = texYStart
        val texXEnd = texXEnd
        val texYEnd = texYEnd

        GL11.glTexCoord2f(texXStart, texYEnd) //lower left
        var vec = MathUtils.getPointOnCircumference(position, size, angle + 315f)
        GL11.glVertex2f(vec.getX(), vec.getY())

        GL11.glTexCoord2f(texXStart, texYStart) //upper left
        vec = MathUtils.getPointOnCircumference(position, size, angle + 225f)
        GL11.glVertex2f(vec.getX(), vec.getY())

        GL11.glTexCoord2f(texXEnd, texYStart) //upper right
        vec = MathUtils.getPointOnCircumference(position, size, angle + 135f)
        GL11.glVertex2f(vec.getX(), vec.getY())

        GL11.glTexCoord2f(texXEnd, texYEnd) //lower right
        vec = MathUtils.getPointOnCircumference(position, size, angle + 45f)
        GL11.glVertex2f(vec.getX(), vec.getY())

        GL11.glEnd()
    }

    open fun postDraw() {

    }

    fun elapsedRatio(): Float {
        val curTime = Global.getCombatEngine().getTotalElapsedTime(false)
        return ((curTime - startingTime) / ttl).coerceIn(0f..1f)
    }

    fun elapsedRatio(intervalUtil: IntervalUtil): Float {
        return (intervalUtil.elapsed / intervalUtil.intervalDuration).coerceIn(0f..1f)
    }

    open fun getColor(): Color {
        return ParticleController.mergeColors(startingColor, endColor, ((elapsedRatio() - 0.66f) * 3f).coerceIn(0f..1f))
    }
}

open class GL14ParticleData(
    sprite: SpriteAPI,
    x: Float,
    y: Float,
    xVel: Float,
    yVel: Float,
    angle: Float,
    aVel: Float,
    startingTime: Float,
    ttl: Float,
    startingSize: Float,
    endSize: Float,
    startingColor: Color,
    endColor: Color,
    layers: EnumSet<CombatEngineLayers> = ParticleController.DEFAULT_PARTICLE_RENDER_LAYER,
    spritesInRow: Int = 1,
    spritesInColumn: Int = 1,
    texXStart: Float = -1f,
    texYStart: Float = -1f,
    texXEnd: Float = -1f,
    texYEnd: Float = -1f
) : ParticleData(
    sprite,
    x,
    y,
    xVel,
    yVel,
    angle,
    aVel,
    startingTime,
    ttl,
    startingSize,
    endSize,
    startingColor,
    endColor,
    layers,
    spritesInRow,
    spritesInColumn,
    texXStart,
    texYStart,
    texXEnd,
    texYEnd
) {

    init {
        if (texXStart == -1f || texXEnd == -1f) {
            val randomColumn = Random.nextInt(0, spritesInColumn - 1).toFloat() / spritesInColumn
            this.texXStart = randomColumn
            this.texXEnd = randomColumn + (1f / spritesInColumn)

            sprite.setTexWidth(1f / spritesInColumn)
            sprite.setTexX(texXStart)
        }

        if (texYStart == -1f || texYEnd == -1f) {
            val randomRow = Random.nextInt(0, spritesInRow - 1).toFloat() / spritesInRow
            this.texYStart = randomRow
            this.texYEnd = randomRow + (1f / spritesInRow)

            sprite.setTexHeight(1f / spritesInColumn)
            sprite.setTexY(texYStart)
        }

        sprite.setAdditiveBlend()
    }

    override fun draw() {
        val elapsed = elapsedRatio()
        val size = startingSize * elapsed + endSize * (1f - elapsed)
        val position = Vector2f(x, y)
        val angle = angle
        val color = getColor()

        sprite.angle = angle
        sprite.setSize(/* width = */ size, /* height = */ size)
        sprite.alphaMult = (color.alpha * elapsed) / 255f
        sprite.color = color
        sprite.renderAtCenter(position.x, position.y)
    }
}