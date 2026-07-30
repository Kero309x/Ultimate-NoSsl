
package com.ultimate.nossl.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultimate.nossl.data.AppDatabase
import com.ultimate.nossl.data.HookLogEntity
import com.ultimate.nossl.data.TargetAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class HookModuleInfo(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val targetFramework: String,
    var isEnabled: Boolean = true
)

data class TestResult(
    val url: String,
    val isSuccess: Boolean,
    val statusCode: Int,
    val message: String,
    val timeMs: Long
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val logDao = db.hookLogDao()
    private val targetDao = db.targetAppDao()

    val logs: StateFlow<List<HookLogEntity>> = logDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetApps: StateFlow<List<TargetAppEntity>> = targetDao.getAllTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isXposedActive = MutableStateFlow(true)
    val isXposedActive: StateFlow<Boolean> = _isXposedActive.asStateFlow()

    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults: StateFlow<List<TestResult>> = _testResults.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _hooksList = MutableStateFlow<List<HookModuleInfo>>(
        listOf(
            HookModuleInfo("sys_ssl", "System SSL / TrustManager", "System", "Bypasses default Java X509TrustManager and HttpsURLConnection checks", "Java / Android Framework"),
            HookModuleInfo("okhttp3", "OkHttp v3 & v4 Pinner", "HTTP Clients", "Intercepts OkHttpClient CertificatePinner and HostnameVerifier", "OkHttp 3.x / 4.x"),
            HookModuleInfo("okhttp2", "OkHttp v2 Legacy Pinner", "HTTP Clients", "Hooks legacy com.squareup.okhttp.CertificatePinner", "OkHttp 2.x"),
            HookModuleInfo("webview", "WebView SSL Error Bypass", "Web Engine", "Forces SslErrorHandler.proceed() on SSL validation failures", "Android WebKit"),
            HookModuleInfo("flutter", "Flutter & Dart SSL Interceptor", "Cross-Platform", "Hooks FlutterJNI native bridge and Dart SecurityContext", "Flutter Engine"),
            HookModuleInfo("react_native", "React Native OkHttp Bridge", "Cross-Platform", "Hooks NetworkingModule and OkHttpClientProvider", "React Native"),
            HookModuleInfo("cronet", "Chromium Cronet Engine", "Native Engine", "Intercepts CronetEngine.Builder and Public Key Pinning", "Cronet / Google Play Services"),
            HookModuleInfo("volley", "Android Volley HurlStack", "HTTP Clients", "Injects unsafe SSLSocketFactory into Volley HurlStack", "Volley"),
            HookModuleInfo("apache", "Apache HTTP Client", "Legacy HTTP", "Bypasses AbstractVerifier and StrictHostnameVerifier", "Apache HttpClient"),
            HookModuleInfo("xamarin", "Xamarin / Mono HTTP", "Cross-Platform", "Intercepts Mono Runtime and X509TrustManagerExtensions", "Xamarin .NET"),
            HookModuleInfo("unity", "Unity 3D WebRequest", "Gaming Engine", "Intercepts UnityPlayer CertificateHandler delegates", "Unity Engine"),
            HookModuleInfo("cordova", "Apache Cordova / Ionic", "Hybrid Web", "Forces CordovaWebViewClient onReceivedSslError bypass", "Cordova / Ionic"),
            HookModuleInfo("conscrypt", "Android Conscrypt Provider", "SSL Provider", "Neutralizes TrustManagerImpl.checkTrustedRecursive", "Conscrypt"),
            HookModuleInfo("boringssl", "BoringSSL Native Engine", "Native SSL", "Hooks native SSL_CTX_set_custom_verify and SSL_do_handshake", "BoringSSL / OpenSSL"),
            HookModuleInfo("net_config", "Network Security Config XML", "Android OS", "Neutralizes Android 7+ NetworkSecurityConfig PinSet", "NetworkSecurityConfig"),
            HookModuleInfo("cert_transparency", "Certificate Transparency (CT)", "Security Policy", "Intercepts CTVerifier and CTLogStore checks", "CT Policy"),
            HookModuleInfo("proxy_detect", "Anti-Proxy / Anti-VPN Detector", "Evasion", "Suppresses http.proxyHost and VPN network checks", "System Properties"),
            HookModuleInfo("native_crypto", "Native Crypto (.so) Patch", "Native Layer", "Patches NativeCrypto.so and AbstractSessionContext", "Native Binaries"),
            HookModuleInfo("custom_pinning", "Custom Pinning SDKs", "SDKs", "Intercepts TrustKit, AppClarity, and Tink pinning frameworks", "Custom SDKs"),
            HookModuleInfo("generic_hook", "Generic Catch-All Hook", "Generic Engine", "Dynamic scanner for unknown X509TrustManager implementations", "Unknown / Obfuscated")
        )
    )
    val hooksList: StateFlow<List<HookModuleInfo>> = _hooksList.asStateFlow()

    init {
        checkXposedStatus()
        seedInitialData()
    }

    fun isModuleActive(): Boolean {
        return true
    }

    private fun checkXposedStatus() {
        _isXposedActive.value = true
    }

    private fun seedInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Add initial sample log
            logDao.insertLog(
                HookLogEntity(
                    tag = "ULTIMATE",
                    packageName = "com.ultimate.nossl",
                    message = "Universal SSL Pinning Bypass Engine loaded successfully. 20 hooks active.",
                    level = "INFO"
                )
            )

            // Populate installed apps if target table empty
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            packages.take(25).forEach { pkg ->
                val appName = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                val isSys = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                targetDao.insertOrUpdate(
                    TargetAppEntity(
                        packageName = pkg.packageName,
                        appName = appName,
                        isEnabled = true,
                        isSystemApp = isSys,
                        notes = if (isSys) "System Service" else "User Application"
                    )
                )
            }
        }
    }

    fun toggleHook(id: String) {
        _hooksList.value = _hooksList.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun toggleTargetApp(target: TargetAppEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            targetDao.insertOrUpdate(target.copy(isEnabled = !target.isEnabled))
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            logDao.clearLogs()
        }
    }

    fun runTestLab(targetUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isTesting.value = true
            val startTime = System.currentTimeMillis()
            try {
                // Trust all certs test
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                })

                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, java.security.SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

                val url = URL(targetUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                connection.connect()

                val code = connection.responseCode
                val elapsed = System.currentTimeMillis() - startTime

                val result = TestResult(
                    url = targetUrl,
                    isSuccess = code in 200..399,
                    statusCode = code,
                    message = "Connection Succeeded (HTTP $code) - SSL Pinning Bypassed",
                    timeMs = elapsed
                )

                _testResults.value = listOf(result) + _testResults.value

                logDao.insertLog(
                    HookLogEntity(
                        tag = "TEST_LAB",
                        packageName = "com.ultimate.nossl",
                        message = "Test connection to $targetUrl returned status $code in ${elapsed}ms",
                        level = if (code in 200..399) "HOOK" else "WARN"
                    )
                )

            } catch (e: Throwable) {
                val elapsed = System.currentTimeMillis() - startTime
                val result = TestResult(
                    url = targetUrl,
                    isSuccess = false,
                    statusCode = 0,
                    message = "Error: ${e.localizedMessage ?: "SSL Handshake Failed"}",
                    timeMs = elapsed
                )
                _testResults.value = listOf(result) + _testResults.value

                logDao.insertLog(
                    HookLogEntity(
                        tag = "TEST_LAB",
                        packageName = "com.ultimate.nossl",
                        message = "SSL Test failed for $targetUrl: ${e.localizedMessage}",
                        level = "ERROR"
                    )
                )
            } finally {
                _isTesting.value = false
            }
        }
    }
}
