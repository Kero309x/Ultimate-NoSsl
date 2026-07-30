
package com.ultimate.nossl.utils

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object SSLFactory {
    
    class TrustAllManager : X509ExtendedTrustManager() {
        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?, socket: java.net.Socket?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?, socket: java.net.Socket?) {}
        override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?, engine: SSLEngine?) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?, engine: SSLEngine?) {}
        
        // This is the signature Chromium often looks for via reflection
        @Suppress("UNUSED_PARAMETER")
        fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?, host: String?): List<X509Certificate> {
            return chain?.toList() ?: ArrayList()
        }
    }

    val TRUST_ALL: X509TrustManager = TrustAllManager()

    val UNSAFE_SOCKET_FACTORY: SSLSocketFactory by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(TRUST_ALL), SecureRandom())
        }.socketFactory
    }

    val UNSAFE_VERIFIER = HostnameVerifier { _, _ -> true }

    fun createEmptyTrustManagerArray(): Array<TrustManager> = arrayOf(TRUST_ALL)
}
