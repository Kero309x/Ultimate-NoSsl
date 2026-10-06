package com.ultimate.nossl.utils

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object SafeReflection {
    private const val MAX_ENTRIES = 512

    private val classCache = ConcurrentHashMap<String, Class<*>>(256)
    private val methodCache = ConcurrentHashMap<String, Method>(512)
    private val fieldCache = ConcurrentHashMap<String, Field>(256)

    private fun <K, V> putBounded(map: ConcurrentHashMap<K, V>, key: K, value: V) {
        if (map.size >= MAX_ENTRIES) map.clear()
        map[key] = value
    }

    fun findClass(className: String, classLoader: ClassLoader?): Class<*>? {
        val key = "${classLoader?.hashCode() ?: 0}_$className"
        classCache[key]?.let { return it }

        return try {
            val clazz = Class.forName(className, false, classLoader ?: ClassLoader.getSystemClassLoader())
            putBounded(classCache, key, clazz)
            clazz
        } catch (ignored: Throwable) {
            null
        }
    }

    fun findMethod(clazz: Class<*>?, methodName: String, vararg paramTypes: Class<*>): Method? {
        if (clazz == null) return null
        val paramSid = paramTypes.joinToString(":") { it.name }
        val key = "${clazz.name}_${methodName}_$paramSid"
        methodCache[key]?.let { return it }

        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            try {
                val method = current.getDeclaredMethod(methodName, *paramTypes)
                method.isAccessible = true
                putBounded(methodCache, key, method)
                return method
            } catch (ignored: NoSuchMethodException) {
                current = current.superclass
            } catch (ignored: Throwable) {
                break
            }
        }
        return null
    }

    fun getField(target: Any?, fieldName: String): Any? {
        if (target == null) return null
        val clazz = target.javaClass
        val key = "${clazz.name}_$fieldName"
        var field = fieldCache[key]

        if (field == null) {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                try {
                    val f = current.getDeclaredField(fieldName)
                    f.isAccessible = true
                    putBounded(fieldCache, key, f)
                    field = f
                    break
                } catch (ignored: NoSuchFieldException) {
                    current = current.superclass
                } catch (ignored: Throwable) {
                    break
                }
            }
        }

        return try {
            field?.get(target)
        } catch (ignored: Throwable) {
            null
        }
    }

    fun setField(target: Any?, fieldName: String, value: Any?): Boolean {
        if (target == null) return false
        val clazz = target.javaClass
        val key = "${clazz.name}_$fieldName"
        var field = fieldCache[key]

        if (field == null) {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                try {
                    val f = current.getDeclaredField(fieldName)
                    f.isAccessible = true
                    putBounded(fieldCache, key, f)
                    field = f
                    break
                } catch (ignored: NoSuchFieldException) {
                    current = current.superclass
                } catch (ignored: Throwable) {
                    break
                }
            }
        }

        return try {
            field?.set(target, value)
            true
        } catch (ignored: Throwable) {
            false
        }
    }

    fun clearAll() {
        classCache.clear()
        methodCache.clear()
        fieldCache.clear()
    }
}
