package org.selkie.kol

import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

object ReflectionUtils {

    private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
    private val setFieldHandle = MethodHandles.lookup().findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
    private val getFieldHandle = MethodHandles.lookup().findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
    private val getFieldTypeHandle = MethodHandles.lookup().findVirtual(fieldClass, "getType", MethodType.methodType(Class::class.java))
    private val getFieldNameHandle = MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
    private val setFieldAccessibleHandle = MethodHandles.lookup().findVirtual(fieldClass,"setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

    private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
    private val getMethodNameHandle = MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
    private val invokeMethodHandle = MethodHandles.lookup().findVirtual(methodClass, "invoke", MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java))

    internal val getMethodReturnHandle = MethodHandles.lookup().findVirtual(methodClass, "getReturnType", MethodType.methodType(Class::class.java))
    private val getMethodParametersHandle = MethodHandles.lookup().findVirtual(methodClass, "getParameterTypes", MethodType.methodType(arrayOf<Class<*>>().javaClass))

    @JvmStatic
    fun set(fieldName: String, instanceToModify: Any, newValue: Any?) {
        var field: Any? = null

        try {
            field = instanceToModify.javaClass.getField(fieldName)
        } catch (_: Exception) {
        }
        if (field == null) {
            field = instanceToModify.javaClass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        setFieldHandle.invoke(field, instanceToModify, newValue)
    }

    @JvmStatic
    fun setFieldOfType(type: Class<*>, instanceToModify: Any, newValue: Any?)
    {
        var decFieldsA: Array<Any> = instanceToModify.javaClass.declaredFields as Array<Any>
        var fields: MutableList<Any> = decFieldsA.toMutableList()
        var nonDecFieldsA: Array<Any> = instanceToModify.javaClass.fields as Array<Any>
        var nonDecFields: MutableList<Any> = nonDecFieldsA.toMutableList()

        fields.addAll(nonDecFields)

        for (field: Any in fields)
        {
            setFieldAccessibleHandle.invoke(field, true)
            var fieldType: Class<*> = getFieldTypeHandle.invoke(field) as Class<*>
            if (fieldType == type) {
                setFieldHandle.invoke(field, instanceToModify, newValue)
            }
        }
    }

    @JvmStatic
    fun get(fieldName: String, instanceToGetFrom: Any): Any? {
        var field: Any? = null

        try {
            field = instanceToGetFrom.javaClass.getField(fieldName)
        } catch (_: Exception) {
        }
        if (field == null) {
            field = instanceToGetFrom.javaClass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }
    @JvmStatic
    fun getFromSuper(fieldName: String, instanceToGetFrom: Any): Any? {
        var field: Any? = null

        try {
            field = instanceToGetFrom.javaClass.superclass.getField(fieldName)
        } catch (_: Exception) {
        }
        if (field == null) {
            field = instanceToGetFrom.javaClass.superclass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun hasMethodOfName(name: String, instance: Any, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods() as Array<out Any>

        return if (!contains) {
            instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    fun getMethodOfReturnType(instance: Any, clazz: Class<*>): String? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods() as Array<out Any>

        return instancesOfMethods.firstOrNull { getMethodReturnHandle.invoke(it) == clazz }
                ?.let { getMethodNameHandle.invoke(it) as String }
    }

    fun hasVariableOfName(name: String, instance: Any): Boolean {
        val instancesOfFields: Array<out Any> = instance.javaClass.getDeclaredFields() as Array<out Any>
        return instancesOfFields.any { getFieldNameHandle.invoke(it) == name }
    }

    fun getFieldsOfType(instance: Any, clazz: Class<*>): List<String> {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredFields() as Array<out Any>

        return instancesOfMethods.filter { getFieldTypeHandle.invoke(it) == clazz }
                .map { getFieldNameHandle.invoke(it) as String }
    }

    fun getConstructor(clazz: Class<*>, vararg arguments: Class<*>) =
            MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(Void.TYPE, arguments))

    fun instantiate(clazz: Class<*>, vararg arguments: Any?): Any? {
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }

        val constructorHandle = getConstructor(clazz, *args.toTypedArray())

        return constructorHandle.invokeWithArguments(arguments.toList())
    }

    fun createNewInstanceFromExisting(existingObject: Any, vararg arguments: Any?): Any {
        // Get the class of the existing object
        val clazz = existingObject.javaClass

        // Extract argument types for the constructor
        val argumentClasses = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }.toTypedArray()

        // Find and invoke the constructor
        return try {
            val constructorHandle = getConstructor(clazz, *argumentClasses)
            constructorHandle.invokeWithArguments(arguments.toList())
        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to create a new instance for class ${clazz.name}: ${e.message}", e)
        }
    }


    fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false): Any? {
        val method: Any?

        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        method = if (!declared) {
            clazz.getMethod(methodName, *methodType.parameterArray()) as Any?
        } else {
            clazz.getDeclaredMethod(methodName, *methodType.parameterArray()) as Any?
        }

        return invokeMethodHandle.invoke(method, instance, arguments)
    }

    fun invokeStatic(methodName: String, clazz: Class<*>, vararg arguments: Any?, declared: Boolean = false): Any? {
        val method: Any?
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        method = if (!declared) {
            clazz.getMethod(methodName, *methodType.parameterArray())
        } else {
            clazz.getDeclaredMethod(methodName, *methodType.parameterArray())
        }

        return invokeMethodHandle.invoke(method, null, arguments)
    }

    fun findFieldWithMethodReturnType(instance: Any, clazz: Class<*>): ReflectedField? {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields as Array<out Any>

        return instancesOfFields.map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
                .firstOrNull { (fieldObj, fieldClass) ->
                    ((fieldClass!! as Class<Any>).declaredMethods as Array<Any>).any { methodObj ->
                        getMethodReturnHandle.invoke(
                                methodObj
                        ) == clazz
                    }
                }?.let { (fieldObj, fieldClass) ->
                    return ReflectedField(fieldObj)
                }
    }

    fun findFieldWithMethodName(instance: Any, methodName: String): ReflectedField? {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields as Array<out Any>

        return instancesOfFields.map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
                .firstOrNull { (fieldObj, fieldClass) ->
                    hasMethodOfNameInClass(methodName, fieldClass as Class<Any>)
                }?.let { (fieldObj, fieldClass) ->
                    return ReflectedField(fieldObj)
                }
    }

    fun hasMethodOfNameInClass(name: String, instance: Class<Any>, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.getDeclaredMethods() as Array<out Any>

        return if (!contains) {
            instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    fun getMethodArguments(method: String, instance: Any): Array<Class<*>>? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.declaredMethods as Array<out Any>
        instancesOfMethods.firstOrNull { getMethodNameHandle.invoke(it) == method }?.let {
            return getMethodParametersHandle.invoke(it) as Array<Class<*>>
        }
        return null
    }

    fun getMethodWithArguments(instance: Any, argumentsClass: Array<Class<*>>): String? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.declaredMethods as Array<out Any>
        for(method in instancesOfMethods){
            if (argumentsClass.contentEquals(invoke("getParameterTypes", method) as Array<Class<*>>)){
                return getMethodNameHandle.invoke(method) as String
            }
        }
        return null
    }

    fun findFieldsOfType(instance: Any, clazz: Class<*>): List<ReflectedField> {
        val instancesOfFields: Array<out Any> = instance.javaClass.declaredFields as Array<out Any>

        return instancesOfFields.map { fieldObj -> fieldObj to getFieldTypeHandle.invoke(fieldObj) }
                .filter { (fieldObj, fieldClass) ->
                    fieldClass == clazz
                }.map { (fieldObj, fieldClass) -> ReflectedField(fieldObj) }
    }

    class ReflectedField(val field: Any) {
        fun get(instance: Any?): Any? {
            setFieldAccessibleHandle.invoke(field, true)
            return getFieldHandle.invoke(field, instance)
        }

        fun set(instance: Any?, value: Any?) {
            setFieldHandle.invoke(field, instance, value)
        }
    }

    class ReflectedMethod(val method: Any) {
        fun invoke(instance: Any?, vararg arguments: Any?): Any? =
                invokeMethodHandle.invoke(method, instance, arguments)
    }

    fun UIPanelAPI.getChildrenCopy() : List<UIComponentAPI> {
        return ReflectionUtils.invoke("getChildrenCopy", this) as List<UIComponentAPI>
    }
}