#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <shadowhook.h>
#include <unistd.h>
#include <dlfcn.h>
#include <link.h>
#include <cstring>
#include <fcntl.h>
#include <sys/syscall.h>
#include <sys/mman.h>
#include <mutex>

#define LOG_TAG "🔓ULTIMATE_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct FlutterPattern {
    std::string pattern_str;
    int retval;
};

static std::mutex g_scan_mutex;
static bool g_flutter_patched = false;

// --- Multi-architecture Memory Patching Helper ---
static void parse_pattern(const std::string& pat_str, std::vector<uint8_t>& values, std::vector<uint8_t>& masks) {
    values.clear();
    masks.clear();
    for (size_t i = 0; i < pat_str.length(); i++) {
        if (pat_str[i] == ' ') continue;
        
        char high = pat_str[i];
        if (i + 1 >= pat_str.length()) break;
        char low = pat_str[i + 1];
        i++; // skip next char
        
        uint8_t val = 0;
        uint8_t mask = 0;
        
        if (high != '?') {
            val |= (high >= 'a' ? high - 'a' + 10 : (high >= 'A' ? high - 'A' + 10 : high - '0')) << 4;
            mask |= 0xF0;
        }
        if (low != '?') {
            val |= (low >= 'a' ? low - 'a' + 10 : (low >= 'A' ? low - 'A' + 10 : low - '0'));
            mask |= 0x0F;
        }
        values.push_back(val);
        masks.push_back(mask);
    }
}

static std::vector<void*> find_all_patterns(uint8_t* start, size_t size, const std::string& pat_str) {
    std::vector<void*> results;
    std::vector<uint8_t> values, masks;
    parse_pattern(pat_str, values, masks);
    if (size < values.size() || values.empty()) return results;
    
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

static bool apply_safe_patch(void* addr, int retval) {
    if (!addr) return false;

    const uint8_t* patch_bytes = nullptr;
    size_t patch_len = 0;

#if defined(__aarch64__)
    // ARM64: mov w0, #retval; ret
    static const uint8_t patch_arm64_ret1[] = { 0x20, 0x00, 0x80, 0x52, 0xc0, 0x03, 0x5f, 0xd6 };
    static const uint8_t patch_arm64_ret0[] = { 0x00, 0x00, 0x80, 0x52, 0xc0, 0x03, 0x5f, 0xd6 };
    patch_bytes = (retval == 1) ? patch_arm64_ret1 : patch_arm64_ret0;
    patch_len = sizeof(patch_arm64_ret1);

#elif defined(__arm__)
    // ARM32 Thumb-2: movs r0, #retval; bx lr
    static const uint8_t patch_arm_ret1[] = { 0x01, 0x20, 0x70, 0x47 };
    static const uint8_t patch_arm_ret0[] = { 0x00, 0x20, 0x70, 0x47 };
    patch_bytes = (retval == 1) ? patch_arm_ret1 : patch_arm_ret0;
    patch_len = sizeof(patch_arm_ret1);

#elif defined(__x86_64__)
    // x86_64: mov eax, retval; ret
    static const uint8_t patch_x64_ret1[] = { 0xb8, 0x01, 0x00, 0x00, 0x00, 0xc3 };
    static const uint8_t patch_x64_ret0[] = { 0x31, 0xc0, 0xc3 };
    if (retval == 1) {
        patch_bytes = patch_x64_ret1;
        patch_len = sizeof(patch_x64_ret1);
    } else {
        patch_bytes = patch_x64_ret0;
        patch_len = sizeof(patch_x64_ret0);
    }

#elif defined(__i386__)
    // x86: mov eax, retval; ret
    static const uint8_t patch_x86_ret1[] = { 0xb8, 0x01, 0x00, 0x00, 0x00, 0xc3 };
    static const uint8_t patch_x86_ret0[] = { 0x31, 0xc0, 0xc3 };
    if (retval == 1) {
        patch_bytes = patch_x86_ret1;
        patch_len = sizeof(patch_x86_ret1);
    } else {
        patch_bytes = patch_x86_ret0;
        patch_len = sizeof(patch_x86_ret0);
    }
#else
    LOGE("Unsupported architecture for native memory patch!");
    return false;
#endif

    size_t page_size = sysconf(_SC_PAGESIZE);
    uintptr_t page_start = (uintptr_t)addr & ~(page_size - 1);
    uintptr_t page_end = ((uintptr_t)addr + patch_len + page_size - 1) & ~(page_size - 1);
    size_t protect_len = page_end - page_start;

    // Strict W^X compliance: Make writable (PROT_READ | PROT_WRITE)
    if (mprotect((void*)page_start, protect_len, PROT_READ | PROT_WRITE) != 0) {
        LOGE("mprotect write failed at %p", addr);
        return false;
    }

    memcpy(addr, patch_bytes, patch_len);
    __builtin___clear_cache((char*)addr, (char*)addr + patch_len);

    // Restore executable permission (PROT_READ | PROT_EXEC)
    if (mprotect((void*)page_start, protect_len, PROT_READ | PROT_EXEC) != 0) {
        LOGW("mprotect exec restore warning at %p", addr);
    }

    LOGI("Successfully patched native function at %p (ret %d)", addr, retval);
    return true;
}

// --- Direct Memory Scanners for Flutter 2.x, 3.x, and Dart VM ---
static const std::vector<FlutterPattern>& get_patterns() {
    static const std::vector<FlutterPattern> patterns = {
#if defined(__aarch64__)
        // Flutter 3.19 - 3.24+ session_verify_cert_chain
        {"FF 03 05 D1 F8 5F 01 A9 F6 57 02 A9 F4 4F 03 A9 FD 7B 04 A9", 1},
        {"FF 83 01 D1 F8 5F 01 A9 F6 57 02 A9 F4 4F 03 A9 FD 7B 04 A9", 1},
        {"FF 43 01 D1 FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 13 00 40 F9 F4 03 00 AA 68 1A 40 F9", 0},
        {"FF 43 01 D1 FE 67 01 A9 ?? ?? 06 94 ?? 7? 06 94 68 1A 40 F9 15 15 41 F9 B5 00 00 B4 B6 4A 40 F9", 0},
        {"FF ?3 01 D1 F? ?? 01 A9 ?? ?? ?? 94 ?? ?? ?? 52 48 00 00 39 1A 50 40 F9 DA 02 00 B4 48 03 40 F9", 1},
        {"F? 0F 1C F8 F? 5? 01 A9 F? 5? 02 A9 F? ?? 03 A9 ?? ?? ?? ?? 68 1A 40 F9", 0},
        {"FF C3 01 D1 FD 7B ?? A9 ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? 68 1A 40 F9", 0},
        {"F4 03 00 AA ?? 00 00 94 ?? 00 00 34 ?? ?? 40 F9", 0},
        {"FD 7B 02 A9 FD 43 00 91 F4 03 00 AA 68 1A 40 F9", 0},
        {"F4 4F 02 A9 FD 7B 03 A9 FD C3 00 91", 0},
#elif defined(__arm__)
        {"2D E9 F? 4? D0 F8 00 80 81 46 D8 F8 18 00 D0 F8", 0},
        {"2D E9 ?? 4F ?? ?? ?? ?? ?? ?? 81 46", 0},
        {"2D E9 F0 4F D0 F8 00 80 81 46", 0},
#elif defined(__x86_64__)
        {"55 48 89 E5 41 57 41 56 41 55 41 54 53 48 8? EC", 0},
        {"55 48 89 E5 53 48 83 EC ?? ?? ?? ?? ?? ?? 48 8B", 1},
#endif
    };
    return patterns;
}

static void scan_flutter_via_maps() {
    std::lock_guard<std::mutex> lock(g_scan_mutex);

#ifdef __arm__
    int fd = syscall(__NR_open, "/proc/self/maps", O_RDONLY);
#else
    int fd = syscall(__NR_openat, AT_FDCWD, "/proc/self/maps", O_RDONLY);
#endif
    if (fd < 0) return;

    std::string content;
    char buf[4096];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf))) > 0) {
        content.append(buf, n);
    }
    close(fd);

    const auto& patterns = get_patterns();
    if (patterns.empty()) return;

    size_t pos = 0;
    while (pos < content.length()) {
        size_t next_line = content.find('\n', pos);
        std::string line = (next_line == std::string::npos) ? content.substr(pos) : content.substr(pos, next_line - pos);
        pos = (next_line == std::string::npos) ? content.length() : next_line + 1;

        if (line.empty()) continue;

        if (line.find("r-xp") != std::string::npos || line.find("r--p") != std::string::npos) {
            if (line.find("libflutter.so") != std::string::npos || line.find("flutter") != std::string::npos || line.find("base.apk") != std::string::npos) {
                size_t dash = line.find('-');
                if (dash == std::string::npos) continue;
                size_t space = line.find(' ', dash);
                if (space == std::string::npos) continue;

                try {
                    uintptr_t start = std::stoull(line.substr(0, dash), nullptr, 16);
                    uintptr_t end = std::stoull(line.substr(dash + 1, space - dash - 1), nullptr, 16);
                    if (start >= end) continue;

                    uint8_t* start_ptr = (uint8_t*)start;
                    size_t size = end - start;

                    for (const auto& pat : patterns) {
                        auto matches = find_all_patterns(start_ptr, size, pat.pattern_str);
                        for (void* match : matches) {
                            if (apply_safe_patch(match, pat.retval)) {
                                g_flutter_patched = true;
                            }
                        }
                    }
                } catch (...) { }
            }
        }
    }
}

static int dl_phdr_callback(struct dl_phdr_info *info, size_t size, void *data) {
    if (!info->dlpi_name) return 0;
    if (strstr(info->dlpi_name, "libflutter.so") || strstr(info->dlpi_name, "flutter")) {
        const auto& patterns = get_patterns();
        for (int i = 0; i < info->dlpi_phnum; i++) {
            if (info->dlpi_phdr[i].p_type == PT_LOAD && (info->dlpi_phdr[i].p_flags & PF_X)) {
                uint8_t* start = (uint8_t*)(info->dlpi_addr + info->dlpi_phdr[i].p_vaddr);
                size_t seg_size = info->dlpi_phdr[i].p_memsz;
                for (const auto& pat : patterns) {
                    auto matches = find_all_patterns(start, seg_size, pat.pattern_str);
                    for (void* match : matches) {
                        if (apply_safe_patch(match, pat.retval)) {
                            g_flutter_patched = true;
                        }
                    }
                }
            }
        }
    }
    return 0;
}

void patch_flutter_memory() {
    dl_iterate_phdr(dl_phdr_callback, nullptr);
    scan_flutter_via_maps();
}

// --- ShadowHook Native SSL Interceptors ---
typedef void (*SSL_CTX_set_custom_verify_t)(void *ctx, int mode, int (*callback)(void *ssl, uint8_t *out_alert));
typedef void (*SSL_set_custom_verify_t)(void *ssl, int mode, int (*callback)(void *ssl, uint8_t *out_alert));
typedef void (*SSL_CTX_set_verify_t)(void *ctx, int mode, int (*callback)(int, void *));
typedef void (*SSL_set_verify_t)(void *ssl, int mode, int (*callback)(int, void *));

static void* stub_ctx_custom_verify = nullptr;
static void* stub_ssl_custom_verify = nullptr;
static void* stub_ctx_set_verify = nullptr;
static void* stub_ssl_set_verify = nullptr;

static int custom_verify_always_ok(void *ssl, uint8_t *out_alert) {
    return 0; // SSL_VERIFY_OK
}

static void SSL_CTX_set_custom_verify_proxy(void *ctx, int mode, int (*callback)(void *ssl, uint8_t *out_alert)) {
    if (stub_ctx_custom_verify) {
        ((SSL_CTX_set_custom_verify_t)stub_ctx_custom_verify)(ctx, 0, custom_verify_always_ok);
    }
}

static void SSL_set_custom_verify_proxy(void *ssl, int mode, int (*callback)(void *ssl, uint8_t *out_alert)) {
    if (stub_ssl_custom_verify) {
        ((SSL_set_custom_verify_t)stub_ssl_custom_verify)(ssl, 0, custom_verify_always_ok);
    }
}

static void SSL_CTX_set_verify_proxy(void *ctx, int mode, int (*callback)(int, void *)) {
    if (stub_ctx_set_verify) {
        ((SSL_CTX_set_verify_t)stub_ctx_set_verify)(ctx, 0, nullptr);
    }
}

static void SSL_set_verify_proxy(void *ssl, int mode, int (*callback)(int, void *)) {
    if (stub_ssl_set_verify) {
        ((SSL_set_verify_t)stub_ssl_set_verify)(ssl, 0, nullptr);
    }
}

static long SSL_get_verify_result_proxy(void *ssl) {
    return 0; // X509_V_OK
}

static int X509_verify_cert_proxy(void *ctx) {
    return 1; // 1 = Valid / Verified
}

static int ssl_verify_peer_cert_ret0(void *ssl) { return 0; }
static int session_verify_cert_chain_ret1(void *ssl) { return 1; }

static void apply_native_ssl_hooks() {
    stub_ctx_custom_verify = shadowhook_hook_sym_name("libssl.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    stub_ssl_custom_verify = shadowhook_hook_sym_name("libssl.so", "SSL_set_custom_verify", (void *)SSL_set_custom_verify_proxy, nullptr);
    stub_ctx_set_verify = shadowhook_hook_sym_name("libssl.so", "SSL_CTX_set_verify", (void *)SSL_CTX_set_verify_proxy, nullptr);
    stub_ssl_set_verify = shadowhook_hook_sym_name("libssl.so", "SSL_set_verify", (void *)SSL_set_verify_proxy, nullptr);

    shadowhook_hook_sym_name("libssl.so", "SSL_get_verify_result", (void *)SSL_get_verify_result_proxy, nullptr);
    shadowhook_hook_sym_name("libcrypto.so", "X509_verify_cert", (void *)X509_verify_cert_proxy, nullptr);

    // Flutter exported symbols if present
    shadowhook_hook_sym_name("libflutter.so", "ssl_verify_peer_cert", (void *)ssl_verify_peer_cert_ret0, nullptr);
    shadowhook_hook_sym_name("libflutter.so", "session_verify_cert_chain", (void *)session_verify_cert_chain_ret1, nullptr);
    shadowhook_hook_sym_name("libflutter.so", "ssl_crypto_x509_session_verify_cert_chain", (void *)session_verify_cert_chain_ret1, nullptr);

    // Cronet / BoringSSL variants
    shadowhook_hook_sym_name("libcronet.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libcronet.so", "SSL_set_custom_verify", (void *)SSL_set_custom_verify_proxy, nullptr);

    // Meta / Facebook / Instagram native network engines (Liger, Proxygen, Fizz)
    shadowhook_hook_sym_name("libliger.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libliger.so", "SSL_set_custom_verify", (void *)SSL_set_custom_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libproxygen.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libfb.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libfizz.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);

    patch_flutter_memory();
}

// --- Dynamic dlopen interceptor to catch late-loaded libraries ---
typedef void* (*dlopen_t)(const char* filename, int flags);
static void* stub_dlopen = nullptr;

static void* dlopen_proxy(const char* filename, int flags) {
    void* result = nullptr;
    if (stub_dlopen) {
        result = ((dlopen_t)stub_dlopen)(filename, flags);
    }
    if (filename && result) {
        if (strstr(filename, "flutter") || strstr(filename, "cronet") || 
            strstr(filename, "ssl") || strstr(filename, "crypto") ||
            strstr(filename, "liger") || strstr(filename, "proxygen") || 
            strstr(filename, "fb") || strstr(filename, "fizz")) {
            LOGI("dlopen intercepted target library: %s", filename);
            apply_native_ssl_hooks();
        }
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_initNative(JNIEnv *env, jobject thiz) {
    static bool initialized = false;
    if (initialized) return;

    if (shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false) == 0) {
        LOGI("ShadowHook core initialized successfully.");
        apply_native_ssl_hooks();
        stub_dlopen = shadowhook_hook_sym_name("libdl.so", "dlopen", (void *)dlopen_proxy, nullptr);
        initialized = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_scanFlutterNative(JNIEnv *env, jclass clazz) {
    patch_flutter_memory();
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_00024Companion_scanFlutterNative(JNIEnv *env, jobject thiz) {
    patch_flutter_memory();
}
