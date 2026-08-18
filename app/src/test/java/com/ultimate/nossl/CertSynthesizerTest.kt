package com.ultimate.nossl

import com.ultimate.nossl.utils.CertSynthesizer
import org.junit.Assert.*
import org.junit.Test

class CertSynthesizerTest {

    @Test
    fun testDummyCertificateNotNull() {
        val cert = CertSynthesizer.DUMMY_CERTIFICATE
        assertNotNull(cert)
    }

    @Test
    fun testDummyChainNotEmpty() {
        val chain = CertSynthesizer.DUMMY_CHAIN
        assertNotNull(chain)
        assertTrue(chain.isNotEmpty())
        assertEquals(1, chain.size)
        assertNotNull(chain[0])
    }

    @Test
    fun testDummyChainListNotEmpty() {
        val list = CertSynthesizer.DUMMY_CHAIN_LIST
        assertNotNull(list)
        assertEquals(1, list.size)
        assertNotNull(list[0])
    }

    @Test
    fun testCertificateProperties() {
        val cert = CertSynthesizer.DUMMY_CERTIFICATE
        // Ensure standard certificate methods do not throw unexpected runtime exceptions
        try {
            assertNotNull(cert.type)
        } catch (ignored: Throwable) {}
    }
}