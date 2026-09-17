package com.ultimate.nossl.utils

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

object CertSynthesizer {

    val DUMMY_CERTIFICATE: X509Certificate by lazy {
        try {
            generateDummyCert()
        } catch (e: Throwable) {
            createFallbackProxyCert()
        }
    }

    val DUMMY_CHAIN: Array<X509Certificate> by lazy {
        arrayOf(DUMMY_CERTIFICATE)
    }

    val DUMMY_CHAIN_LIST: List<X509Certificate> by lazy {
        listOf(DUMMY_CERTIFICATE)
    }

    private fun generateDummyCert(): X509Certificate {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val kp = kpg.generateKeyPair()

        val issuer = X500Principal("CN=UltimateNoSSL Dummy CA, O=UltimateNoSSL, C=US")
        val serial = BigInteger(128, SecureRandom())

        val builder = sun.security.x509.X509CertImpl(
            sun.security.x509.X509CertInfo().apply {
                set(sun.security.x509.X509CertInfo.VALIDITY, sun.security.x509.CertificateValidity(
                    Date(System.currentTimeMillis() - 86400000L),
                    Date(System.currentTimeMillis() + 315360000000L)
                ))
                set(sun.security.x509.X509CertInfo.SERIAL_NUMBER, sun.security.x509.CertificateSerialNumber(serial))
                set(sun.security.x509.X509CertInfo.SUBJECT, issuer)
                set(sun.security.x509.X509CertInfo.ISSUER, issuer)
                set(sun.security.x509.X509CertInfo.KEY, sun.security.x509.CertificateX509Key(kp.public))
                set(sun.security.x509.X509CertInfo.VERSION, sun.security.x509.CertificateVersion(sun.security.x509.CertificateVersion.V3))
                set(sun.security.x509.X509CertInfo.ALGORITHM_ID, sun.security.x509.CertificateAlgorithmId(
                    sun.security.x509.AlgorithmId.get("SHA256withRSA")
                ))
            }
        )
        builder.sign(kp.private, "SHA256withRSA")
        return builder
    }

    private fun createFallbackProxyCert(): X509Certificate {
        val handler = java.lang.reflect.InvocationHandler { _, method, _ ->
            when (method.name) {
                "getSubjectDN", "getIssuerDN" -> java.security.Principal { "CN=UltimateNoSSL Dummy CA, O=Ultimate, C=US" }
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
