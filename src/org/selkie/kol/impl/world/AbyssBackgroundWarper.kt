package org.selkie.kol.impl.world

import com.fs.graphics.Sprite
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.campaign.BackgroundAndStars
import com.fs.starfarer.campaign.WarpingSpriteRenderer
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class AbyssBackgroundWarper(var system: LocationAPI, var chunks: Int, var speedMod: Float) : WarpingSpriteRenderer(chunks, chunks) {

    private val verticesWide = chunks
    private val verticesTall = chunks
    private val vertices = Array(verticesWide) { arrayOfNulls<Vertex>(verticesTall) }

    var overwriteColor: Color? = null

    companion object {
        private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
        private val setFieldHandle = MethodHandles.lookup().findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
        private val getFieldHandle = MethodHandles.lookup().findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
        private val getFieldNameHandle = MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
        private val setFieldAccessibleHandle = MethodHandles.lookup().findVirtual(fieldClass,"setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

        private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
        private val getMethodNameHandle = MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
        private val invokeMethodHandle = MethodHandles.lookup().findVirtual(methodClass, "invoke", MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java))
    }

    init {
        var background = invoke("getBackground", system) as BackgroundAndStars
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


        var spritePath = get("textureId", sprite) as String
        var spriteAPI: SpriteAPI? = Global.getSettings().getSprite(spritePath) ?: return

        var width = invoke("getWidth", sprite) as Float
        var heigth = invoke("getHeight", sprite) as Float

        GL11.glPushMatrix()
        spriteAPI!!.bindTexture()

        var var5 = spriteAPI.color
        if (overwriteColor != null) var5 = overwriteColor
        GL11.glColor4ub(var5.red.toByte(),
            var5.green.toByte(),
            var5.blue.toByte(),
            (var5.alpha.toFloat() * spriteAPI.alphaMult).toInt().toByte())
        GL11.glTranslatef(bgOffsetX, bgOffsetY, 0.0f)
        GL11.glEnable(3553)
        if (disableBlend) {
            GL11.glDisable(3042)
        }

        var var6 = width
        var var7 = heigth
        var var8 = spriteAPI.textureWidth - 0.001f
        var var9 = spriteAPI.textureHeight - 0.001f
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

    fun set(fieldName: String, instanceToModify: Any, newValue: Any?)
    {
        var field: Any? = null
        try {  field = instanceToModify.javaClass.getField(fieldName) } catch (e: Throwable) {
            try {  field = instanceToModify.javaClass.getDeclaredField(fieldName) } catch (e: Throwable) { }
        }

        setFieldAccessibleHandle.invoke(field, true)
        setFieldHandle.invoke(field, instanceToModify, newValue)
    }

    fun get(fieldName: String, instanceToGetFrom: Any): Any? {
        var field: Any? = null
        try {  field = instanceToGetFrom.javaClass.getField(fieldName) } catch (e: Throwable) {
            try {  field = instanceToGetFrom.javaClass.getDeclaredField(fieldName) } catch (e: Throwable) { }
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false) : Any?
    {
        var method: Any? = null

        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)


        if (!declared) {
            method = clazz.getMethod(methodName, *methodType.parameterArray())
        }
        else  {
            method = clazz.getDeclaredMethod(methodName, *methodType.parameterArray())
        }

        return invokeMethodHandle.invoke(method, instance, arguments)
    }

}