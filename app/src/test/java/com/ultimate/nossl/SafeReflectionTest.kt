package com.ultimate.nossl

import com.ultimate.nossl.utils.SafeReflection
import org.junit.Assert.*
import org.junit.Test

class SafeReflectionTest {

    @Test
    fun testFindClass() {
        val stringClass = SafeReflection.findClass("java.lang.String", javaClass.classLoader)
        assertNotNull(stringClass)
        assertEquals(String::class.java, stringClass)

        val nonExistent = SafeReflection.findClass("com.nonexistent.FakeClass", javaClass.classLoader)
        assertNull(nonExistent)
    }

    @Test
    fun testFindMethod() {
        val method = SafeReflection.findMethod(String::class.java, "substring", Int::class.javaPrimitiveType!!)
        assertNotNull(method)
        assertEquals("substring", method?.name)

        val nonExistentMethod = SafeReflection.findMethod(String::class.java, "fakeMethodNonExistent")
        assertNull(nonExistentMethod)
    }

    @Test
    fun testClearAllDoesNotCrash() {
        SafeReflection.clearAll()
    }
}