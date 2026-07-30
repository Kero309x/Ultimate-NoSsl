
package com.ultimate.nossl.utils

import de.robv.android.xposed.XposedHelpers

object ReflectionUtil {
    
    fun setField(obj: Any, fieldName: String, value: Any?) {
        try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(obj, value)
        } catch (e: Throwable) {
            Logger.e("setField $fieldName", e)
        }
    }

    fun getField(obj: Any, fieldName: String): Any? {
        return try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(obj)
        } catch (e: Throwable) {
            null
        }
    }

    fun callMethod(obj: Any, methodName: String, vararg args: Any?): Any? {
        return try {
            XposedHelpers.callMethod(obj, methodName, *args)
        } catch (e: Throwable) {
            null
        }
    }
}
