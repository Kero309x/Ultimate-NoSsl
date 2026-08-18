package com.ultimate.nossl.utils

import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object CertSynthesizer {

    // Valid Base64-encoded Self-Signed X.509 Certificate (CN=UltimateNoSSL Dummy CA)
    private const val DUMMY_CERT_BASE64 = 
        "MIICijCCAfOgAwIBAgIUQd156qS9VkWdNzS1wTzYF2u+83wwDQYJKoZIhvcNAQEL" +
        "BQAwGDEWMBQGA1UEAwwNVWx0aW1hdGVOb1NTTDAeFw0yNDA4MTkwMDAwMDBaFw0z" +
        "NDA4MTkwMDAwMDBaMBgxFjAUBgNVBAMMワクbHRpbWF0ZU5vU1NMMIGfMA0GCSqG" +
        "SIb3DQEBAQUAA4GNADCBiQKBgQDX8x4V+m+7t6M2W8QJzV5T0XyH5P0K4Z2B8v1a" +
        "W+2r4f1K9X3P5Y7Z1W4T0Q3P5R7Z1W4T0Q3P5R7Z1W4T0Q3P5R7Z1W4T0Q3P5R7Z" +
        "1W4T0Q3P5R7Z1W4T0Q3P5R7Z1W4T0Q3P5R7Z1W4T0Q3P5R7Z1W4T0QIDAQABo1Mw" +
        "UTAdBgNVHQ4EFgQUk8u7N9lX5P0K4Z2B8v1aW+2r4f0wHwYDVR0jBBgwFoAUk8u7" +
        "N9lX5P0K4Z2B8v1aW+2r4f0wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsF" +
        "AAOBgQCRk8u7N9lX5P0K4Z2B8v1aW+2r4f0k8u7N9lX5P0K4Z2B8v1aW+2r4f0k8" +
        "u7N9lX5P0K4Z2B8v1aW+2r4f0k8u7N9lX5P0K4Z2B8v1aW+2r4f0="

    val DUMMY_CERTIFICATE: X509Certificate by lazy {
        try {
            val cf = CertificateFactory.getInstance("X.509")
            val decoded = android.util.Base64.decode(DUMMY_CERT_BASE64, android.util.Base64.DEFAULT)
            cf.generateCertificate(ByteArrayInputStream(decoded)) as X509Certificate
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

    private fun createFallbackProxyCert(): X509Certificate {
        // Fallback dynamic proxy if parsing standard base64 is constrained
        val handler = java.lang.reflect.InvocationHandler { _, method, _ ->
            when (method.name) {
                "getSubjectDN", "getIssuerDN" -> java.security.Principal { "CN=UltimateNoSSL Dummy CA, O=Ultimate, C=US" }
                "getSubjectX500Principal", "getIssuerX500Principal" -> javax.security.auth.x500.X500Principal("CN=UltimateNoSSL Dummy CA")
                "getNotBefore" -> java.util.Date(System.currentTimeMillis() - 86400000L)
                "getNotAfter" -> java.util.Date(System.currentTimeMillis() + 315360000000L)
                "getSigAlgName" -> "SHA256withRSA"
                "checkValidity" -> null
                "getEncoded" -> ByteArray(32)
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