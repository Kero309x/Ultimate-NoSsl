#include <jni.h>
#include <string>
#include <android/log.h>
#include <shadowhook.h>
#include <vector>
#include <unistd.h>
#include <dlfcn.h>
#include <link.h>
#include <cstring>
#include <set>

#define LOG_TAG "🔓ULTIMATE_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// --- SSL Types & Proxies ---
typedef void (*SSL_CTX_set_verify_t)(void *ctx, int mode, int (*callback)(int, void *));
typedef void (*SSL_set_verify_t)(void *ssl, int mode, int (*callback)(int, void *));

void SSL_CTX_set_verify_proxy(void *ctx, int mode, int (*callback)(int, void *)) {
    auto original = (SSL_CTX_set_verify_t)shadowhook_get_prev_func((void *)SSL_CTX_set_verify_proxy);
    if(original) original(ctx, 0, nullptr);
}

void SSL_set_verify_proxy(void *ssl, int mode, int (*callback)(int, void *)) {
    auto original = (SSL_set_verify_t)shadowhook_get_prev_func((void *)SSL_set_verify_proxy);
    if(original) original(ssl, 0, nullptr);
}

long SSL_get_verify_result_proxy(void *ssl) {
    return 0; // X509_V_OK
}

int X509_verify_cert_proxy(void *ctx) {
    return 1; // Success
}

int ssl_verify_peer_cert_proxy_ret0(void *ssl) {
    return 0;
}

int ssl_verify_peer_cert_proxy_ret1(void *ssl) {
    return 1;
}

void do_hooks() {
    shadowhook_hook_sym_name("libssl.so", "SSL_CTX_set_verify", (void *)SSL_CTX_set_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libssl.so", "SSL_set_verify", (void *)SSL_set_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libssl.so", "SSL_get_verify_result", (void *)SSL_get_verify_result_proxy, nullptr);
    shadowhook_hook_sym_name("libcrypto.so", "X509_verify_cert", (void *)X509_verify_cert_proxy, nullptr);
    
    shadowhook_hook_sym_name("libflutter.so", "ssl_verify_peer_cert", (void *)ssl_verify_peer_cert_proxy_ret0, nullptr);
    shadowhook_hook_sym_name("libflutter.so", "ssl_crypto_x509_session_verify_cert_chain", (void *)ssl_verify_peer_cert_proxy_ret1, nullptr);
    
    LOGI("Ultimate Native Hooks Deployed (Safe & Crash-Free)");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_initNative(JNIEnv *env, jobject thiz) {
    static bool initialized = false;
    if (initialized) return;
    if (shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false) == 0) {
        do_hooks();
        initialized = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_scanFlutterNative(JNIEnv *env, jobject thiz) {
    // Safe no-op
}
