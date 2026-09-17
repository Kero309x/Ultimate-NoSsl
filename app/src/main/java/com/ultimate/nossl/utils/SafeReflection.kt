package com.ultimate.nossl.utils

import android.util.LruCache
import java.lang.reflect.Field
import java.lang.reflect.Method

object SafeReflection {

    private val classCache = LruCache<String, Class<*>>(256)
    private val methodCache = LruCache<String, Method>(512)
    private val fieldCache = LruCache<String, Field>(256)

    fun findClass(className: String, classLoader: ClassLoader?): Class<*>? {
        val key = "${classLoader?.hashCode() ?: 0}_$className"
        classCache.get(key)?.let { return it }

        return try {
            val clazz = Class.forName(className, false, classLoader ?: ClassLoader.getSystemClassLoader())
            classCache.put(key, clazz)
            clazz
        } catch (ignored: Throwable) {
            null
        }
    }

    fun findMethod(clazz: Class<*>?, methodName: String, vararg paramTypes: Class<*>): Method? {
        if (clazz == null) return null
        val paramSid = paramTypes.joinToString(":") { it.name }
        val key = "${clazz.name}_${methodName}_$paramSid"
        methodCache.get(key)?.let { return it }

        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            try {
                val method = current.getDeclaredMethod(methodName, *paramTypes)
                method.isAccessible = true
                methodCache.put(key, method)
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
        var field = fieldCache.get(key)

        if (field == null) {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                try {
                    val f = current.getDeclaredField(fieldName)
                    f.isAccessible = true
                    fieldCache.put(key, f)
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
        var field = fieldCache.get(key)

        if (field == null) {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                try {
                    val f = current.getDeclaredField(fieldName)
                    f.isAccessible = true
                    fieldCache.put(key, f)
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
        classCache.evictAll()
        methodCache.evictAll()
        fieldCache.evictAll()
    }
}
