#include <jni.h>
#include <string>
#include <android/log.h>
#include <shadowhook.h>
#include <vector>
#include <unistd.h>
#include <dlfcn.h>
#include <link.h>
#include <sys/mman.h>
#include <cstring>
#include <set>
#include <fcntl.h>

#define LOG_TAG "🔓ULTIMATE_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// --- SSL Types ---
typedef void (*SSL_CTX_set_verify_t)(void *ctx, int mode, int (*callback)(int, void *));
typedef void (*SSL_set_verify_t)(void *ssl, int mode, int (*callback)(int, void *));
typedef long (*SSL_get_verify_result_t)(void *ssl);
typedef int (*X509_verify_cert_t)(void *ctx);
typedef void (*SSL_CTX_set_custom_verify_t)(void *ctx, int mode, int (*callback)(void *ssl, uint8_t *out_alert));

// --- SSL Proxies ---
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

void SSL_CTX_set_custom_verify_proxy(void *ctx, int mode, int (*callback)(void *ssl, uint8_t *out_alert)) {
    auto original = (SSL_CTX_set_custom_verify_t)shadowhook_get_prev_func((void *)SSL_CTX_set_custom_verify_proxy);
    if(original) original(ctx, 0, nullptr); // 0 = SSL_VERIFY_NONE
}

int ssl_verify_peer_cert_proxy_ret0(void *ssl) {
    return 0; // ok
}

int ssl_verify_peer_cert_proxy_ret1(void *ssl) {
    return 1; // true
}

// --- Anti-Debug / Anti-Detection Native Hooks ---
typedef void (*exit_t)(int status);
typedef void (*abort_t)();
typedef int (*ptrace_t)(int request, pid_t pid, void *addr, void *data);
typedef int (*open_t)(const char *pathname, int flags, ...);

void exit_proxy(int status) {
    LOGI("Intercepted native exit(%d) - blocked!", status);
}

void _exit_proxy(int status) {
    LOGI("Intercepted native _exit(%d) - blocked!", status);
}

void abort_proxy() {
    LOGI("Intercepted native abort() - blocked!");
}

int ptrace_proxy(int request, pid_t pid, void *addr, void *data) {
    LOGI("Intercepted native ptrace(request=%d) - returned 0 (success/untraced)", request);
    return 0;
}

int open_proxy(const char *pathname, int flags, int mode) {
    if (pathname != nullptr) {
        std::string path(pathname);
        if (path.find("/proc/self/maps") != std::string::npos ||
            path.find("/proc/self/status") != std::string::npos ||
            path.find("/proc/self/task") != std::string::npos ||
            path.find("/proc/net/unix") != std::string::npos ||
            path.find("su") != std::string::npos ||
            path.find("magisk") != std::string::npos ||
            path.find("frida") != std::string::npos ||
            path.find("xposed") != std::string::npos) {
            LOGI("Intercepted native open() for sensitive path: %s -> redirecting to /dev/null", pathname);
            auto original = (open_t)shadowhook_get_prev_func((void *)open_proxy);
            if (original) return original("/dev/null", flags, mode);
        }
    }
    auto original = (open_t)shadowhook_get_prev_func((void *)open_proxy);
    if (original) return original(pathname, flags, mode);
    return -1;
}

// --- Memory Scanner ---
struct FlutterPattern {
    std::string pattern_str;
    int retval;
};

void parse_pattern(const std::string& pat_str, std::vector<uint8_t>& values, std::vector<uint8_t>& masks) {
    values.clear();
    masks.clear();
    for (size_t i = 0; i < pat_str.length(); i++) {
        if (pat_str[i] == ' ') continue;
        
        char high = pat_str[i];
        char low = pat_str[i+1];
        i++; // skip next char
        
        uint8_t val = 0;
        uint8_t mask = 0;
        
        if (high != '?') {
            val |= (high >= 'A' ? (high & 0xDF) - 'A' + 10 : high - '0') << 4;
            mask |= 0xF0;
        }
        if (low != '?') {
            val |= (low >= 'A' ? (low & 0xDF) - 'A' + 10 : low - '0');
            mask |= 0x0F;
        }
        values.push_back(val);
        masks.push_back(mask);
    }
}

std::vector<void*> find_all_patterns(uint8_t* start, size_t size, const std::string& pat_str) {
    std::vector<void*> results;
    std::vector<uint8_t> values, masks;
    parse_pattern(pat_str, values, masks);
    if (size < values.size()) return results;
    
    for (size_t i = 0; i <= size - values.size(); i++) {
        bool match = true;
        for (size_t j = 0; j < values.size(); j++) {
            if ((start[i + j] & masks[j]) != values[j]) {
                match = false;
                break;
            }
        }
        if (match) {
            results.push_back((void*)(start + i));
        }
    }
    return results;
}

static bool flutter_hooked = false;
static int dl_phdr_callback(struct dl_phdr_info *info, size_t size, void *data) {
    if (flutter_hooked) return 0;
    
    if (info->dlpi_name && strstr(info->dlpi_name, "libflutter.so")) {
        LOGI("Found libflutter.so in dl_iterate_phdr: %s at %p", info->dlpi_name, (void*)info->dlpi_addr);
        
        std::vector<FlutterPattern> patterns = {
            {"F? 0F 1C F8 F? 5? 01 A9 F? 5? 02 A9 F? ?? 03 A9 ?? ?? ?? ?? 68 1A 40 F9", 0},
            {"F? 43 01 D1 FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 13 00 40 F9 F4 03 00 AA 68 1A 40 F9", 0},
            {"FF 43 01 D1 FE 67 01 A9 ?? ?? 06 94 ?? 7? 06 94 68 1A 40 F9 15 15 41 F9 B5 00 00 B4 B6 4A 40 F9", 0},
            {"FF ?3 01 D1 F? ?? 01 A9 ?? ?? ?? 94 ?? ?? ?? 52 48 00 00 39 1A 50 40 F9 DA 02 00 B4 48 03 40 F9", 1},
            {"2D E9 F? 4? D0 F8 00 80 81 46 D8 F8 18 00 D0 F8", 0}
        };
        
        bool found_any = false;
        std::set<void*> hooked_addrs;
        
        for (int i = 0; i < info->dlpi_phnum; i++) {
            if (info->dlpi_phdr[i].p_type == PT_LOAD && (info->dlpi_phdr[i].p_flags & PF_X)) {
                uint8_t* start = (uint8_t*)(info->dlpi_addr + info->dlpi_phdr[i].p_vaddr);
                size_t seg_size = info->dlpi_phdr[i].p_memsz;
                
                for (size_t p = 0; p < patterns.size(); p++) {
                    auto matches = find_all_patterns(start, seg_size, patterns[p].pattern_str);
                    for (void* match : matches) {
                        if (hooked_addrs.find(match) != hooked_addrs.end()) continue;
                        void *stub = nullptr;
                        void *proxy = patterns[p].retval == 0 ? (void*)ssl_verify_peer_cert_proxy_ret0 : (void*)ssl_verify_peer_cert_proxy_ret1;
                        shadowhook_hook_func_addr(match, proxy, &stub);
                        hooked_addrs.insert(match);
                        found_any = true;
                    }
                }
            }
        }
        
        if (found_any) {
            flutter_hooked = true;
            return 1;
        }
    }
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_scanFlutterNative(JNIEnv *env, jobject thiz) {
    if (flutter_hooked) return;
    dl_iterate_phdr(dl_phdr_callback, nullptr);
}

void do_hooks() {
    // Global generic SSL hooks
    shadowhook_hook_sym_name(nullptr, "SSL_CTX_set_verify", (void *)SSL_CTX_set_verify_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "SSL_set_verify", (void *)SSL_set_verify_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "SSL_get_verify_result", (void *)SSL_get_verify_result_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "X509_verify_cert", (void *)X509_verify_cert_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    
    // Anti-debug / Anti-detection libc hooks
    shadowhook_hook_sym_name(nullptr, "exit", (void *)exit_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "_exit", (void *)_exit_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "abort", (void *)abort_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "ptrace", (void *)ptrace_proxy, nullptr);
    shadowhook_hook_sym_name(nullptr, "open", (void *)open_proxy, nullptr);
    
    // Attempt standard symbol hooks (for debug builds)
    shadowhook_hook_sym_name("libflutter.so", "ssl_verify_peer_cert", (void *)ssl_verify_peer_cert_proxy_ret0, nullptr);
    shadowhook_hook_sym_name("libflutter.so", "ssl_crypto_x509_session_verify_cert_chain", (void *)ssl_verify_peer_cert_proxy_ret1, nullptr);
    
    LOGI("Ultimate Native Hooks Deployed (SSL Core + Anti-Debug)");
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
