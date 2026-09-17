package com.ultimate.nossl.utils

import java.math.BigInteger
import java.security.Principal
import java.security.cert.X509Certificate
import java.util.Date

object CertSynthesizer {

    val DUMMY_CERTIFICATE: X509Certificate by lazy { createDummyCert() }

    val DUMMY_CHAIN: Array<X509Certificate> by lazy {
        arrayOf(DUMMY_CERTIFICATE)
    }

    val DUMMY_CHAIN_LIST: List<X509Certificate> by lazy {
        listOf(DUMMY_CERTIFICATE)
    }

    private fun createDummyCert(): X509Certificate {
        val handler = java.lang.reflect.InvocationHandler { _, method, _ ->
            when (method.name) {
                "getSubjectDN", "getIssuerDN" -> Principal { "CN=UltimateNoSSL Dummy CA, O=UltimateNoSSL, C=US" }
                "getSubjectX500Principal", "getIssuerX500Principal" -> javax.security.auth.x500.X500Principal("CN=UltimateNoSSL Dummy CA")
                "getNotBefore" -> Date(System.currentTimeMillis() - 86400000L)
                "getNotAfter" -> Date(System.currentTimeMillis() + 315360000000L)
                "getSigAlgName" -> "SHA256withRSA"
                "checkValidity" -> null
                "getEncoded" -> ByteArray(32)
                "getSerialNumber" -> BigInteger.ONE
                "getVersion" -> 3
                "getPublicKey" -> null
                "getSignature" -> ByteArray(32)
                "getBasicConstraints" -> -1
                "toString" -> "X509Certificate [UltimateNoSSL Dummy Synthetic Certificate]"
                else -> null
            }
        }
        return java.lang.reflect.Proxy.newProxyInstance(
            X509Certificate::class.java.classLoader,
            arrayOf(X509Certificate::class.java),
            handler
        ) as X509Certificate
    }
}
