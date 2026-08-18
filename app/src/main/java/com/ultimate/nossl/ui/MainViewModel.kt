package com.ultimate.nossl.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultimate.nossl.data.AppDatabase
import com.ultimate.nossl.data.HookLogEntity
import com.ultimate.nossl.data.TargetAppEntity
import com.ultimate.nossl.utils.ModuleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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
    private val prefs = application.getSharedPreferences("ultimate_nossl_prefs", Context.MODE_PRIVATE)

    val logs: StateFlow<List<HookLogEntity>> = logDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetApps: StateFlow<List<TargetAppEntity>> = targetDao.getAllTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isXposedActive = MutableStateFlow(false)
    val isXposedActive: StateFlow<Boolean> = _isXposedActive.asStateFlow()

    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults: StateFlow<List<TestResult>> = _testResults.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _hooksList = MutableStateFlow<List<HookModuleInfo>>(
        listOf(
            HookModuleInfo("SystemSSL", "System SSL / TrustManager", "System", "Bypasses default Java X509TrustManager and HttpsURLConnection checks", "Java / Android Framework"),
            HookModuleInfo("OkHttp", "OkHttp v3 & v4 Pinner", "HTTP Clients", "Intercepts OkHttpClient CertificatePinner and HostnameVerifier", "OkHttp 3.x / 4.x"),
            HookModuleInfo("OkHttp2", "OkHttp v2 Legacy Pinner", "HTTP Clients", "Hooks legacy com.squareup.okhttp.CertificatePinner", "OkHttp 2.x"),
            HookModuleInfo("WebView", "WebView SSL Error Bypass", "Web Engine", "Forces SslErrorHandler.proceed() on SSL validation failures", "Android WebKit"),
            HookModuleInfo("Flutter", "Flutter & Dart SSL Interceptor", "Cross-Platform", "Hooks FlutterJNI native bridge and Dart SecurityContext", "Flutter Engine"),
            HookModuleInfo("ReactNative", "React Native OkHttp & Hermes Engine", "Cross-Platform", "Hooks NetworkingModule, Hermes JS executor, and OkHttpClientProvider", "React Native / Hermes"),
            HookModuleInfo("Cronet", "Chromium Cronet Engine", "Native Engine", "Intercepts CronetEngine.Builder and Public Key Pinning", "Cronet / Google Play Services"),
            HookModuleInfo("Volley", "Android Volley HurlStack", "HTTP Clients", "Injects unsafe SSLSocketFactory into Volley HurlStack", "Volley"),
            HookModuleInfo("ApacheHttp", "Apache HTTP Client", "Legacy HTTP", "Bypasses AbstractVerifier and StrictHostnameVerifier", "Apache HttpClient"),
            HookModuleInfo("Xamarin", "Xamarin / Mono HTTP", "Cross-Platform", "Intercepts Mono Runtime and X509TrustManagerExtensions", "Xamarin .NET"),
            HookModuleInfo("Unity", "Unity 3D WebRequest", "Gaming Engine", "Intercepts UnityPlayer CertificateHandler delegates", "Unity Engine"),
            HookModuleInfo("Cordova", "Apache Cordova / Ionic", "Hybrid Web", "Forces CordovaWebViewClient onReceivedSslError bypass", "Cordova / Ionic"),
            HookModuleInfo("Conscrypt", "Android Conscrypt Provider", "SSL Provider", "Neutralizes TrustManagerImpl.checkTrustedRecursive", "Conscrypt"),
            HookModuleInfo("BoringSSL", "BoringSSL Native Engine", "Native SSL", "Hooks native SSL_CTX_set_custom_verify and X509_verify_cert", "BoringSSL / OpenSSL"),
            HookModuleInfo("NetworkSecurity", "Network Security Config XML", "Android OS", "Neutralizes Android 7+ NetworkSecurityConfig PinSet", "NetworkSecurityConfig"),
            HookModuleInfo("CertificateTransparency", "Certificate Transparency (CT)", "Security Policy", "Intercepts CTVerifier and CTLogStore checks", "CT Policy"),
            HookModuleInfo("ProxyDetection", "Anti-Proxy / Anti-VPN Detector", "Evasion", "Suppresses VPN network checks without breaking proxy routing", "System Properties"),
            HookModuleInfo("NativeCrypto", "Native Crypto (.so) Patch", "Native Layer", "Patches NativeCrypto.so and AbstractSessionContext", "Native Binaries"),
            HookModuleInfo("ModernHttp", "Retrofit & Fuel Managers", "HTTP Clients", "Injects unsafe SSL into Retrofit.Builder and FuelManager", "Retrofit / Fuel"),
            HookModuleInfo("gRPC", "gRPC Channels (OkHttp / Netty)", "Protocols", "Intercepts OkHttpChannelBuilder and NettyChannelBuilder SSL factories", "gRPC Framework"),
            HookModuleInfo("WebSocket", "WebSocket Secure (WSS)", "Protocols", "Intercepts Java-WebSocket and nv-websocket-client SSL factories", "WebSocket WSS"),
            HookModuleInfo("Ktor", "Ktor HTTP Client (KMP)", "Protocols", "Injects unsafe SSL into Ktor OkHttp, CIO, and AndroidClientEngine", "Ktor Multiplatform"),
            HookModuleInfo("GenericHook", "Generic Catch-All Hook", "Generic Engine", "Dynamic scanner for unknown X509TrustManager implementations", "Unknown / Obfuscated")
        ).map { hook ->
            val isEnabled = prefs.getBoolean("hook_${hook.id}", true)
            hook.copy(isEnabled = isEnabled)
        }
    )
    val hooksList: StateFlow<List<HookModuleInfo>> = _hooksList.asStateFlow()

    init {
        checkXposedStatus()
        seedInitialData()
    }

    fun isModuleActive(): Boolean {
        return ModuleStatus.isModuleActive()
    }

    private fun checkXposedStatus() {
        _isXposedActive.value = isModuleActive()
    }

    private fun seedInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            logDao.insertLog(
                HookLogEntity(
                    tag = "ULTIMATE",
                    packageName = "com.ultimate.nossl",
                    message = "Universal SSL Pinning Bypass Engine initialized.",
                    level = "INFO"
                )
            )
            val pm = getApplication<Application>().packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            packages.take(25).forEach { pkg ->
                val appName = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                val isSys = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                val isEnabled = prefs.getBoolean("app_${pkg.packageName}", true)
                targetDao.insertOrUpdate(
                    TargetAppEntity(
                        packageName = pkg.packageName,
                        appName = appName,
                        isEnabled = isEnabled,
                        isSystemApp = isSys,
                        notes = if (isSys) "System Service" else "User Application"
                    )
                )
            }
        }
    }

    fun toggleHook(id: String) {
        _hooksList.value = _hooksList.value.map {
            if (it.id == id) {
                val newState = !it.isEnabled
                prefs.edit().putBoolean("hook_$id", newState).apply()
                it.copy(isEnabled = newState)
            } else it
        }
    }

    fun toggleTargetApp(target: TargetAppEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newState = !target.isEnabled
            prefs.edit().putBoolean("app_${target.packageName}", newState).apply()
            targetDao.insertOrUpdate(target.copy(isEnabled = newState))
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
                val url = URL(targetUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
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
