package org.selkie.kol.world

import com.fs.graphics.Sprite
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.campaign.BackgroundAndStars
import com.fs.starfarer.campaign.WarpingSpriteRenderer
import org.lwjgl.opengl.GL11
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.math.cos
import kotlin.math.sin

//Lukas04
class BackgroundWarper(var system: LocationAPI, var chunks: Int, var speedMod: Float) : WarpingSpriteRenderer(chunks, chunks) {

    private val verticesWide = chunks
    private val verticesTall = chunks
    private val vertices = Array(verticesWide) { arrayOfNulls<Vertex>(verticesTall) }
/*
    init {

        val background = invoke("getBackground", system) as BackgroundAndStars
        set("warpngRenderer", background, this)

        for (var3 in 0 until verticesWide) {
            for (var4 in 0 until verticesTall) {
                this.vertices[var3][var4] = Vertex()
            }
        }
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        for (var2 in 0 until this.verticesWide) {
            for (var3 in 0 until this.verticesTall) {
                this.vertices[var2][var3]!!.advance(amount * speedMod)
            }
        }
    }

    override fun renderNoBlendOrRotate(sprite: Sprite, bgOffsetX: Float, bgOffsetY: Float, disableBlend: Boolean) {

        val spritePath = get("textureId", sprite) as String
        val spriteAPI: SpriteAPI? = Global.getSettings().getSprite(spritePath) ?: return

        spriteAPI!!.bindTexture()
        GL11.glPushMatrix()

        val var5 = spriteAPI.color
        GL11.glColor4ub(var5.red.toByte(),
            var5.green.toByte(),
            var5.blue.toByte(),
            (var5.alpha.toFloat() * spriteAPI.alphaMult).toInt().toByte())
        GL11.glTranslatef(bgOffsetX, bgOffsetY, 0.0f)
        GL11.glEnable(3553)
        if (disableBlend) {
            GL11.glDisable(3042)
        }
        val var6 = spriteAPI.width
        val var7 = spriteAPI.height
        val var8 = spriteAPI.textureWidth - 0.001f
        val var9 = spriteAPI.textureHeight - 0.001f
        val var10 = var6 / (this.verticesWide - 1).toFloat()
        val var11 = var7 / (this.verticesTall - 1).toFloat()
        val var12 = var8 / (this.verticesWide - 1).toFloat()
        val var13 = var9 / (this.verticesTall - 1).toFloat()
        var var14 = 0.0f
        while (var14 < (this.verticesWide - 1).toFloat()) {
            GL11.glBegin(8)
            var var15 = 0.0f
            while (var15 < this.verticesTall.toFloat()) {
                var var16 = var10 * var14
                var var17 = var11 * var15
                var var18 = var10 * (var14 + 1.0f)
                var var19 = var11 * var15
                val var20 = var12 * var14
                val var21 = var13 * var15
                val var22 = var12 * (var14 + 1.0f)
                val var23 = var13 * var15
                var var24: Float
                var var25: Float
                var var26: Float
                var var27: Float
                if (var14 != 0.0f && var14 != (this.verticesWide - 1).toFloat() && var15 != 0.0f && var15 != (this.verticesTall - 1).toFloat()) {
                    var24 = Math.toRadians(this.vertices[var14.toInt()][var15.toInt()]!!.theta.value.toDouble())
                        .toFloat()
                    var25 = this.vertices[var14.toInt()][var15.toInt()]!!.radius.value
                    var26 = Math.sin(var24.toDouble()).toFloat()
                    var27 = Math.cos(var24.toDouble()).toFloat()
                    var16 += var27 * var25
                    var17 += var26 * var25
                }
                if (var14 + 1.0f != 0.0f && var14 + 1.0f != (this.verticesWide - 1).toFloat() && var15 != 0.0f && var15 != (this.verticesTall - 1).toFloat()) {
                    var24 = Math.toRadians(this.vertices[var14.toInt() + 1][var15.toInt()]!!.theta.value.toDouble())
                        .toFloat()
                    var25 = this.vertices[var14.toInt() + 1][var15.toInt()]!!.radius.value
                    var26 = Math.sin(var24.toDouble()).toFloat()
                    var27 = Math.cos(var24.toDouble()).toFloat()
                    var18 += var27 * var25
                    var19 += var26 * var25
                }
                GL11.glTexCoord2f(var20, var21)
                GL11.glVertex2f(var16, var17)
                GL11.glTexCoord2f(var22, var23)
                GL11.glVertex2f(var18, var19)
                ++var15
            }
            GL11.glEnd()
            ++var14
        }
        GL11.glPopMatrix()
    }

    fun get(fieldName: String, instanceToGetFrom: Any): Any? {
        val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
        val getMethod = MethodHandles.lookup().findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
        val getNameMethod = MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
        val setAcessMethod = MethodHandles.lookup().findVirtual(fieldClass,"setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

        val instancesOfFields: Array<out Any> = instanceToGetFrom.javaClass.getDeclaredFields()
        for (obj in instancesOfFields)
        {
            setAcessMethod.invoke(obj, true)
            val name = getNameMethod.invoke(obj)
            if (name.toString() == fieldName)
            {
                return getMethod.invoke(obj, instanceToGetFrom)
            }
        }
        return null
    }

    fun set(fieldName: String, instanceToModify: Any, newValue: Any?)
    {
        val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
        val setMethod = MethodHandles.lookup().findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
        val getNameMethod = MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
        val setAcessMethod = MethodHandles.lookup().findVirtual(fieldClass,"setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

        val instancesOfFields: Array<out Any> = instanceToModify.javaClass.getDeclaredFields()
        for (obj in instancesOfFields)
        {
            setAcessMethod.invoke(obj, true)
            val name = getNameMethod.invoke(obj)
            if (name.toString() == fieldName)
            {
                setMethod.invoke(obj, instanceToModify, newValue)
            }
        }
    }

    fun invoke(methodName: String, instance: Any, vararg arguments: Any?) : Any?
    {
        val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
        val getNameMethod = MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
        val invokeMethod = MethodHandles.lookup().findVirtual(methodClass, "invoke", MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java))

        var foundMethod: Any? = null

        for (method in instance::class.java.methods as Array<Any>)
        {
            if (getNameMethod.invoke(method) == methodName)
            {
                foundMethod = method
            }
        }

        return invokeMethod.invoke(foundMethod, instance, arguments)
    }
*/
}