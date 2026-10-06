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
#include <cstdint>

#define LOG_TAG "🔓ULTIMATE_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace nossl {

// --- Diagnostics ---
class Diagnostics {
public:
    static void report(const std::string& msg) {
        LOGI("%s", msg.c_str());
    }
    static void reportError(const std::string& msg) {
        LOGE("%s", msg.c_str());
    }
    static void reportWarning(const std::string& msg) {
        LOGW("%s", msg.c_str());
    }
};

// --- Signature System ---
struct Signature {
    std::string id;
    std::string architecture;
    std::string library;
    std::string version_constraints;
    std::string pattern_str;
    int expected_retval;
    int confidence; // 0-100
};

class SignatureDatabase {
public:
    static const std::vector<Signature>& getFlutterSignatures() {
        static const std::vector<Signature> signatures = {
#if defined(__aarch64__)
            {"FL_3_24_AARCH64_01", "arm64", "libflutter.so", ">=3.19", "FF 03 05 D1 F8 5F 01 A9 F6 57 02 A9 F4 4F 03 A9 FD 7B 04 A9", 1, 90},
            {"FL_3_24_AARCH64_02", "arm64", "libflutter.so", ">=3.19", "FF 83 01 D1 F8 5F 01 A9 F6 57 02 A9 F4 4F 03 A9 FD 7B 04 A9", 1, 90},
            {"FL_3_AARCH64_03", "arm64", "libflutter.so", ">=3.0", "FF 43 01 D1 FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 13 00 40 F9 F4 03 00 AA 68 1A 40 F9", 0, 85},
            {"FL_3_AARCH64_04", "arm64", "libflutter.so", ">=3.0", "FF 43 01 D1 FE 67 01 A9 ?? ?? 06 94 ?? 7? 06 94 68 1A 40 F9 15 15 41 F9 B5 00 00 B4 B6 4A 40 F9", 0, 80},
            {"FL_3_AARCH64_05", "arm64", "libflutter.so", ">=3.0", "FF ?3 01 D1 F? ?? 01 A9 ?? ?? ?? 94 ?? ?? ?? 52 48 00 00 39 1A 50 40 F9 DA 02 00 B4 48 03 40 F9", 1, 80},
            {"FL_2_AARCH64_01", "arm64", "libflutter.so", ">=2.0", "F? 0F 1C F8 F? 5? 01 A9 F? 5? 02 A9 F? ?? 03 A9 ?? ?? ?? ?? 68 1A 40 F9", 0, 75},
            {"FL_2_AARCH64_02", "arm64", "libflutter.so", ">=2.0", "FF C3 01 D1 FD 7B ?? A9 ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? 68 1A 40 F9", 0, 75},
            {"FL_2_AARCH64_03", "arm64", "libflutter.so", ">=2.0", "F4 03 00 AA ?? 00 00 94 ?? 00 00 34 ?? ?? 40 F9", 0, 70},
            {"FL_2_AARCH64_04", "arm64", "libflutter.so", ">=2.0", "FD 7B 02 A9 FD 43 00 91 F4 03 00 AA 68 1A 40 F9", 0, 70},
            {"FL_1_AARCH64_01", "arm64", "libflutter.so", "<2.0", "F4 4F 02 A9 FD 7B 03 A9 FD C3 00 91", 0, 60}
#elif defined(__arm__)
            {"FL_ARM32_01", "arm32", "libflutter.so", "Any", "2D E9 F? 4? D0 F8 00 80 81 46 D8 F8 18 00 D0 F8", 0, 85},
            {"FL_ARM32_02", "arm32", "libflutter.so", "Any", "2D E9 ?? 4F ?? ?? ?? ?? ?? ?? 81 46", 0, 70},
            {"FL_ARM32_03", "arm32", "libflutter.so", "Any", "2D E9 F0 4F D0 F8 00 80 81 46", 0, 75}
#elif defined(__x86_64__)
            {"FL_X64_01", "x64", "libflutter.so", "Any", "55 48 89 E5 41 57 41 56 41 55 41 54 53 48 8? EC", 0, 80},
            {"FL_X64_02", "x64", "libflutter.so", "Any", "55 48 89 E5 53 48 83 EC ?? ?? ?? ?? ?? ?? 48 8B", 1, 80}
#endif
        };
        return signatures;
    }
};

// --- Memory Management ---
class MemoryManager {
public:
    static bool makeWritable(void* addr, size_t len) {
        size_t page_size = sysconf(_SC_PAGESIZE);
        uintptr_t page_start = (uintptr_t)addr & ~(page_size - 1);
        uintptr_t page_end = ((uintptr_t)addr + len + page_size - 1) & ~(page_size - 1);
        return mprotect((void*)page_start, page_end - page_start, PROT_READ | PROT_WRITE) == 0;
    }

    static bool makeExecutable(void* addr, size_t len) {
        size_t page_size = sysconf(_SC_PAGESIZE);
        uintptr_t page_start = (uintptr_t)addr & ~(page_size - 1);
        uintptr_t page_end = ((uintptr_t)addr + len + page_size - 1) & ~(page_size - 1);
        return mprotect((void*)page_start, page_end - page_start, PROT_READ | PROT_EXEC) == 0;
    }
    
    static bool validateRegion(void* addr, size_t len) {
        if (!addr || len == 0) return false;
        // In a real robust implementation, we would parse maps or use msync/mincore to verify memory is valid
        return true;
    }
};

// --- Signature Matcher ---
class SignatureMatcher {
public:
    static void parsePattern(const std::string& pat_str, std::vector<uint8_t>& values, std::vector<uint8_t>& masks) {
        values.clear();
        masks.clear();
        for (size_t i = 0; i < pat_str.length(); i++) {
            if (pat_str[i] == ' ') continue;
            char high = pat_str[i];
            if (i + 1 >= pat_str.length()) break;
            char low = pat_str[i + 1];
            i++;
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

    static std::vector<void*> findMatches(uint8_t* start, size_t size, const Signature& sig) {
        std::vector<void*> results;
        std::vector<uint8_t> values, masks;
        parsePattern(sig.pattern_str, values, masks);
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
};

// --- Validation ---
class Validation {
public:
    static bool validateTarget(void* addr, const Signature& sig) {
        // Here we could implement disassembly validation (e.g. using Capstone)
        // to verify that the matched bytes are indeed the start of a function.
        // For now, we rely on the high confidence of the signature pattern.
        return sig.confidence >= 70;
    }
};

// --- Patch Controller ---
class PatchController {
public:
    static bool applySafePatch(void* addr, const Signature& sig) {
        if (!addr || !Validation::validateTarget(addr, sig)) {
            Diagnostics::reportError("Validation failed for patch at " + std::to_string((uintptr_t)addr));
            return false;
        }

        const uint8_t* patch_bytes = nullptr;
        size_t patch_len = 0;
        int retval = sig.expected_retval;

#if defined(__aarch64__)
        static const uint8_t patch_arm64_ret1[] = { 0x20, 0x00, 0x80, 0x52, 0xc0, 0x03, 0x5f, 0xd6 };
        static const uint8_t patch_arm64_ret0[] = { 0x00, 0x00, 0x80, 0x52, 0xc0, 0x03, 0x5f, 0xd6 };
        patch_bytes = (retval == 1) ? patch_arm64_ret1 : patch_arm64_ret0;
        patch_len = sizeof(patch_arm64_ret1);
#elif defined(__arm__)
        static const uint8_t patch_arm_ret1[] = { 0x01, 0x20, 0x70, 0x47 };
        static const uint8_t patch_arm_ret0[] = { 0x00, 0x20, 0x70, 0x47 };
        patch_bytes = (retval == 1) ? patch_arm_ret1 : patch_arm_ret0;
        patch_len = sizeof(patch_arm_ret1);
#elif defined(__x86_64__) || defined(__i386__)
        static const uint8_t patch_x86_ret1[] = { 0xb8, 0x01, 0x00, 0x00, 0x00, 0xc3 };
        static const uint8_t patch_x86_ret0[] = { 0x31, 0xc0, 0xc3 };
        patch_bytes = (retval == 1) ? patch_x86_ret1 : patch_x86_ret0;
        patch_len = (retval == 1) ? sizeof(patch_x86_ret1) : sizeof(patch_x86_ret0);
#else
        Diagnostics::reportError("Unsupported architecture for patching!");
        return false;
#endif

        if (!MemoryManager::validateRegion(addr, patch_len)) return false;
        if (!MemoryManager::makeWritable(addr, patch_len)) return false;

        memcpy(addr, patch_bytes, patch_len);
        __builtin___clear_cache((char*)addr, (char*)addr + patch_len);

        if (!MemoryManager::makeExecutable(addr, patch_len)) {
            Diagnostics::reportWarning("makeExecutable failed after patching at " + std::to_string((uintptr_t)addr));
        }

        Diagnostics::report("Successfully patched signature " + sig.id + " at " + std::to_string((uintptr_t)addr));
        return true;
    }
};

// --- Memory Scanner ---
static std::mutex g_scan_mutex;
static bool g_flutter_patched = false;

class MemoryScanner {
public:
    static void scanFlutterViaMaps() {
        if (g_flutter_patched) return;
        std::lock_guard<std::mutex> lock(g_scan_mutex);
        if (g_flutter_patched) return;

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

        const auto& signatures = SignatureDatabase::getFlutterSignatures();
        size_t pos = 0;
        while (pos < content.length()) {
            size_t next_line = content.find('\n', pos);
            std::string line = (next_line == std::string::npos) ? content.substr(pos) : content.substr(pos, next_line - pos);
            pos = (next_line == std::string::npos) ? content.length() : next_line + 1;

            if (line.empty()) continue;

            if (line.find("r-xp") == std::string::npos) continue;

            if (line.find("libflutter.so") != std::string::npos || line.find("flutter") != std::string::npos) {
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

                    for (const auto& sig : signatures) {
                        auto matches = SignatureMatcher::findMatches(start_ptr, size, sig);
                        for (void* match : matches) {
                            if (PatchController::applySafePatch(match, sig)) {
                                g_flutter_patched = true;
                            }
                        }
                    }
                } catch (...) { }
            }
        }
    }
    
    static int dlPhdrCallback(struct dl_phdr_info *info, size_t size, void *data) {
        if (!info->dlpi_name) return 0;
        if (strstr(info->dlpi_name, "libflutter.so") || strstr(info->dlpi_name, "flutter")) {
            const auto& signatures = SignatureDatabase::getFlutterSignatures();
            for (int i = 0; i < info->dlpi_phnum; i++) {
                if (info->dlpi_phdr[i].p_type == PT_LOAD && (info->dlpi_phdr[i].p_flags & PF_X)) {
                    uint8_t* start = (uint8_t*)(info->dlpi_addr + info->dlpi_phdr[i].p_vaddr);
                    size_t seg_size = info->dlpi_phdr[i].p_memsz;
                    for (const auto& sig : signatures) {
                        auto matches = SignatureMatcher::findMatches(start, seg_size, sig);
                        for (void* match : matches) {
                            if (PatchController::applySafePatch(match, sig)) {
                                g_flutter_patched = true;
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }
};

// --- Library Loader & ShadowHook Integrations ---
class LibraryLoader {
private:
    static void* stub_ctx_custom_verify;
    static void* stub_ssl_custom_verify;
    static void* stub_ctx_set_verify;
    static void* stub_ssl_set_verify;

    static int custom_verify_always_ok(void *ssl, uint8_t *out_alert) { return 0; }
    
    typedef void (*SSL_CTX_set_custom_verify_t)(void *ctx, int mode, int (*callback)(void *ssl, uint8_t *out_alert));
    static void SSL_CTX_set_custom_verify_proxy(void *ctx, int mode, int (*callback)(void *ssl, uint8_t *out_alert)) {
        if (stub_ctx_custom_verify) ((SSL_CTX_set_custom_verify_t)stub_ctx_custom_verify)(ctx, 0, custom_verify_always_ok);
    }
    
    typedef void (*SSL_set_custom_verify_t)(void *ssl, int mode, int (*callback)(void *ssl, uint8_t *out_alert));
    static void SSL_set_custom_verify_proxy(void *ssl, int mode, int (*callback)(void *ssl, uint8_t *out_alert)) {
        if (stub_ssl_custom_verify) ((SSL_set_custom_verify_t)stub_ssl_custom_verify)(ssl, 0, custom_verify_always_ok);
    }

    typedef void (*SSL_CTX_set_verify_t)(void *ctx, int mode, int (*callback)(int, void *));
    static void SSL_CTX_set_verify_proxy(void *ctx, int mode, int (*callback)(int, void *)) {
        if (stub_ctx_set_verify) ((SSL_CTX_set_verify_t)stub_ctx_set_verify)(ctx, 0, nullptr);
    }

    typedef void (*SSL_set_verify_t)(void *ssl, int mode, int (*callback)(int, void *));
    static void SSL_set_verify_proxy(void *ssl, int mode, int (*callback)(int, void *)) {
        if (stub_ssl_set_verify) ((SSL_set_verify_t)stub_ssl_set_verify)(ssl, 0, nullptr);
    }

    static long SSL_get_verify_result_proxy(void *ssl) { return 0; }
    static int X509_verify_cert_proxy(void *ctx) { return 1; }
    static int ssl_verify_peer_cert_ret0(void *ssl) { return 0; }
    static int session_verify_cert_chain_ret1(void *ssl) { return 1; }

public:
    static void applyNativeSslHooks() {
        stub_ctx_custom_verify = shadowhook_hook_sym_name("libssl.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
        stub_ssl_custom_verify = shadowhook_hook_sym_name("libssl.so", "SSL_set_custom_verify", (void *)SSL_set_custom_verify_proxy, nullptr);
        stub_ctx_set_verify = shadowhook_hook_sym_name("libssl.so", "SSL_CTX_set_verify", (void *)SSL_CTX_set_verify_proxy, nullptr);
        stub_ssl_set_verify = shadowhook_hook_sym_name("libssl.so", "SSL_set_verify", (void *)SSL_set_verify_proxy, nullptr);

        shadowhook_hook_sym_name("libssl.so", "SSL_get_verify_result", (void *)SSL_get_verify_result_proxy, nullptr);
        shadowhook_hook_sym_name("libcrypto.so", "X509_verify_cert", (void *)X509_verify_cert_proxy, nullptr);

        shadowhook_hook_sym_name("libflutter.so", "ssl_verify_peer_cert", (void *)ssl_verify_peer_cert_ret0, nullptr);
        shadowhook_hook_sym_name("libflutter.so", "session_verify_cert_chain", (void *)session_verify_cert_chain_ret1, nullptr);
        shadowhook_hook_sym_name("libflutter.so", "ssl_crypto_x509_session_verify_cert_chain", (void *)session_verify_cert_chain_ret1, nullptr);

        shadowhook_hook_sym_name("libcronet.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
        shadowhook_hook_sym_name("libcronet.so", "SSL_set_custom_verify", (void *)SSL_set_custom_verify_proxy, nullptr);

        shadowhook_hook_sym_name("libliger.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
        shadowhook_hook_sym_name("libliger.so", "SSL_set_custom_verify", (void *)SSL_set_custom_verify_proxy, nullptr);
        shadowhook_hook_sym_name("libproxygen.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
        shadowhook_hook_sym_name("libfb.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
        shadowhook_hook_sym_name("libfizz.so", "SSL_CTX_set_custom_verify", (void *)SSL_CTX_set_custom_verify_proxy, nullptr);
    }
};

void* LibraryLoader::stub_ctx_custom_verify = nullptr;
void* LibraryLoader::stub_ssl_custom_verify = nullptr;
void* LibraryLoader::stub_ctx_set_verify = nullptr;
void* LibraryLoader::stub_ssl_set_verify = nullptr;

// --- Native Core ---
class NativeCore {
private:
    typedef void* (*dlopen_t)(const char* filename, int flags);
    static void* stub_dlopen;

    static void* dlopen_proxy(const char* filename, int flags) {
        void* result = nullptr;
        if (stub_dlopen) {
            result = ((dlopen_t)stub_dlopen)(filename, flags);
        }
        if (filename && result) {
            static const char* targets[] = {
                "libflutter.so", "libcronet.so", "libliger.so",
                "libproxygen.so", "libfb.so", "libfizz.so"
            };
            for (const char* t : targets) {
                if (strstr(filename, t)) {
                    Diagnostics::report("dlopen intercepted target library: " + std::string(filename));
                    LibraryLoader::applyNativeSslHooks();
                    if (strstr(filename, "libflutter.so")) {
                        MemoryScanner::scanFlutterViaMaps();
                    }
                    break;
                }
            }
        }
        return result;
    }

public:
    static void init() {
        if (shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false) == 0) {
            Diagnostics::report("ShadowHook core initialized successfully.");
            LibraryLoader::applyNativeSslHooks();
            dl_iterate_phdr(MemoryScanner::dlPhdrCallback, nullptr);
            MemoryScanner::scanFlutterViaMaps();
            stub_dlopen = shadowhook_hook_sym_name("libdl.so", "dlopen", (void *)dlopen_proxy, nullptr);
        } else {
            Diagnostics::reportError("ShadowHook initialization failed.");
        }
    }
};

void* NativeCore::stub_dlopen = nullptr;

} // namespace nossl


// --- JNI Endpoints ---

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_initNative(JNIEnv *env, jobject thiz) {
    static bool initialized = false;
    if (initialized) return;
    
    nossl::NativeCore::init();
    initialized = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_scanFlutterNative(JNIEnv *env, jclass clazz) {
    nossl::MemoryScanner::scanFlutterViaMaps();
}

extern "C" JNIEXPORT void JNICALL
Java_com_ultimate_nossl_UltimateHook_00024Companion_scanFlutterNative(JNIEnv *env, jobject thiz) {
    nossl::MemoryScanner::scanFlutterViaMaps();
}
