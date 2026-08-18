
package com.ultimate.nossl.utils

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object SSLFactory {

    class TrustAllManager : X509ExtendedTrustManager() {
        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = CertSynthesizer.DUMMY_CHAIN

        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?, socket: java.net.Socket?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?, socket: java.net.Socket?) {}
        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?, engine: SSLEngine?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?, engine: SSLEngine?) {}

        // Chromium / Conscrypt reflection signature
        @Suppress("UNUSED_PARAMETER")
        fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?, host: String?): List<X509Certificate> {
            return if (!chain.isNullOrEmpty()) chain.toList() else CertSynthesizer.DUMMY_CHAIN_LIST
        }

        // Conscrypt / X509TrustManagerExtensions signature
        @Suppress("UNUSED_PARAMETER")
        fun checkServerTrusted(chain: Array<X509Certificate>?, ocspData: ByteArray?, tlsSctData: ByteArray?, authType: String?, host: String?, clientAuth: Boolean): List<X509Certificate> {
            return if (!chain.isNullOrEmpty()) chain.toList() else CertSynthesizer.DUMMY_CHAIN_LIST
        }
    }

    val TRUST_ALL: X509TrustManager = TrustAllManager()

    val UNSAFE_SSL_CONTEXT: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(TRUST_ALL), SecureRandom())
        }
    }

    val UNSAFE_SOCKET_FACTORY: SSLSocketFactory by lazy {
        UNSAFE_SSL_CONTEXT.socketFactory
    }

    val UNSAFE_VERIFIER = HostnameVerifier { _, _ -> true }

    fun createEmptyTrustManagerArray(): Array<TrustManager> = arrayOf(TRUST_ALL)
}
