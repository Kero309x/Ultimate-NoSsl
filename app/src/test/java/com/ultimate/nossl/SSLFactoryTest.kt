package com.ultimate.nossl

import com.ultimate.nossl.utils.SSLFactory
import org.junit.Assert.*
import org.junit.Test
import java.security.cert.X509Certificate

class SSLFactoryTest {

    @Test
    fun testTrustAllManagerDoesNotThrowOnCheck() {
        val tm = SSLFactory.TRUST_ALL
        assertNotNull(tm)
        
        tm.checkClientTrusted(null, null)
        tm.checkServerTrusted(null, null)
        assertNotNull(tm.acceptedIssuers)
        assertTrue(tm.acceptedIssuers.isNotEmpty())
    }

    @Test
    fun testExtendedTrustManagerMethods() {
        val tm = SSLFactory.TrustAllManager()
        
        tm.checkClientTrusted(emptyArray<X509Certificate>(), "RSA", null as java.net.Socket?)
        tm.checkServerTrusted(emptyArray<X509Certificate>(), "RSA", null as java.net.Socket?)
        tm.checkClientTrusted(emptyArray<X509Certificate>(), "RSA", null as javax.net.ssl.SSLEngine?)
        tm.checkServerTrusted(emptyArray<X509Certificate>(), "RSA", null as javax.net.ssl.SSLEngine?)
        
        val resultList = tm.checkServerTrusted(null, "RSA", "example.com")
        assertNotNull(resultList)
        assertTrue(resultList.isNotEmpty())
    }

    @Test
    fun testUnsafeVerifierAlwaysReturnsTrue() {
        val verifier = SSLFactory.UNSAFE_VERIFIER
        assertTrue(verifier.verify("example.com", null))
        assertTrue(verifier.verify("bank.internal.api", null))
        assertTrue(verifier.verify("", null))
    }

    @Test
    fun testUnsafeSocketFactoryCreation() {
        val factory = SSLFactory.UNSAFE_SOCKET_FACTORY
        assertNotNull(factory)
    }
}
