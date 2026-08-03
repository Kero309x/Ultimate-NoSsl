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
#include <fcntl.h>
#include <sys/syscall.h>
#include <sys/mman.h>

#define LOG_TAG "🔓ULTIMATE_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// --- Pattern Structure ---
struct FlutterPattern {
    std::string pattern_str;
    int retval;
};

// --- Memory Scanner Helpers ---
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

// --- Direct Memory Patching ---
void apply_patch(void* addr, int retval) {
    if (!addr) return;
    size_t page_size = sysconf(_SC_PAGESIZE);
    void* page_start = (void*)((uintptr_t)addr & ~(page_size - 1));
    mprotect(page_start, page_size * 2, PROT_READ | PROT_WRITE | PROT_EXEC);
    
    // ARM64 instruction encoding:
    // mov w0, #retval; ret
    if (retval == 1) {
        // mov w0, #1 -> \x20\x00\x80\x52, ret -> \xc0\x03\x5f\xd6
        const char patch[] = "\x20\x00\x80\x52\xc0\x03\x5f\xd6";
        memcpy(addr, patch, 8);
    } else {
        // mov w0, #0 -> \x00\x00\x80\x52, ret -> \xc0\x03\x5f\xd6
        const char patch[] = "\x00\x00\x80\x52\xc0\x03\x5f\xd6";
        memcpy(addr, patch, 8);
    }
    
    // Clear instruction cache to let CPU execute the new instructions
    __builtin___clear_cache((char*)addr, (char*)addr + 8);
    
    mprotect(page_start, page_size * 2, PROT_READ | PROT_EXEC);
    LOGI("Successfully patched memory at %p to return %d", addr, retval);
}

// --- Active Hooking & Scanning Logic ---
static bool flutter_hooked = false;

// Direct /proc/self/maps reader bypassing open hooks
void scan_flutter_via_maps() {
    if (flutter_hooked) return;
    LOGI("Scanning Flutter via /proc/self/maps directly...");
    
    // Bypass open hooks
#ifdef __arm__
    int fd = syscall(__NR_open, "/proc/self/maps", O_RDONLY);
#else
    int fd = syscall(__NR_openat, AT_FDCWD, "/proc/self/maps", O_RDONLY);
#endif

    if (fd < 0) {
        LOGI("Failed to open /proc/self/maps directly via syscall");
        return;
    }

    std::string content;
    char buf[4096];
    ssize_t n;
    while ((n = read(fd, buf, sizeof(buf))) > 0) {
        content.append(buf, n);
    }
    close(fd);

    std::vector<FlutterPattern> patterns = {
        {"F? 0F 1C F8 F? 5? 01 A9 F? 5? 02 A9 F? ?? 03 A9 ?? ?? ?? ?? 68 1A 40 F9", 0},
        {"F? 43 01 D1 FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 13 00 40 F9 F4 03 00 AA 68 1A 40 F9", 0},
        {"FF 43 01 D1 FE 67 01 A9 ?? ?? 06 94 ?? 7? 06 94 68 1A 40 F9 15 15 41 F9 B5 00 00 B4 B6 4A 40 F9", 0},
        {"FF ?3 01 D1 F? ?? 01 A9 ?? ?? ?? 94 ?? ?? ?? 52 48 00 00 39 1A 50 40 F9 DA 02 00 B4 48 03 40 F9", 1},
        {"FF C3 01 D1 FD 7B ?? A9 ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? 68 1A 40 F9", 0},
        {"55 48 89 E5 41 57 41 56 41 55 41 54 53 48 8? EC", 0},
        {"2D E9 F? 4? D0 F8 00 80 81 46 D8 F8 18 00 D0 F8", 0}
    };

    size_t pos = 0;
    bool found_any = false;

    while (pos < content.length()) {
        size_t next_line = content.find('\n', pos);
        std::string line;
        if (next_line == std::string::npos) {
            line = content.substr(pos);
            pos = content.length();
        } else {
            line = content.substr(pos, next_line - pos);
            pos = next_line + 1;
        }

        if (line.empty()) continue;

        // Target: executable memory regions containing "libflutter.so" or "base.apk"
        if (line.find("r-xp") != std::string::npos) {
            if (line.find("libflutter.so") != std::string::npos || line.find("base.apk") != std::string::npos || line.find("flutter") != std::string::npos) {
                LOGI("Scanning map line: %s", line.c_str());

                // Parse address range: e.g. "7b41200000-7b41500000 r-xp ..."
                size_t dash = line.find('-');
                if (dash == std::string::npos) continue;
                size_t space = line.find(' ', dash);
                if (space == std::string::npos) continue;

                std::string start_str = line.substr(0, dash);
                std::string end_str = line.substr(dash + 1, space - dash - 1);

                try {
                    uintptr_t start = std::stoull(start_str, nullptr, 16);
                    uintptr_t end = std::stoull(end_str, nullptr, 16);

                    if (start >= end) continue;
                    uint8_t* start_ptr = (uint8_t*)start;
                    size_t size = end - start;

                    for (size_t p = 0; p < patterns.size(); p++) {
                        auto matches = find_all_patterns(start_ptr, size, patterns[p].pattern_str);
                        for (void* match : matches) {
                            apply_patch(match, patterns[p].retval);
                            found_any = true;
                        }
                    }
                } catch (...) {
                    // Ignore parsing exceptions
                }
            }
        }
    }

    if (found_any) {
        flutter_hooked = true;
    }
}

// --- dl_iterate_phdr fallback scanner ---
static int dl_phdr_callback(struct dl_phdr_info *info, size_t size, void *data) {
    if (flutter_hooked) return 0;
    
    if (info->dlpi_name && (strstr(info->dlpi_name, "libflutter.so") || strstr(info->dlpi_name, "base.apk") || strstr(info->dlpi_name, "flutter"))) {
        LOGI("Found target library segment in dl_iterate_phdr: %s", info->dlpi_name);
        
        std::vector<FlutterPattern> patterns = {
            {"F? 0F 1C F8 F? 5? 01 A9 F? 5? 02 A9 F? ?? 03 A9 ?? ?? ?? ?? 68 1A 40 F9", 0},
            {"F? 43 01 D1 FE 67 01 A9 F8 5F 02 A9 F6 57 03 A9 F4 4F 04 A9 13 00 40 F9 F4 03 00 AA 68 1A 40 F9", 0},
            {"FF 43 01 D1 FE 67 01 A9 ?? ?? 06 94 ?? 7? 06 94 68 1A 40 F9 15 15 41 F9 B5 00 00 B4 B6 4A 40 F9", 0},
            {"FF ?3 01 D1 F? ?? 01 A9 ?? ?? ?? 94 ?? ?? ?? 52 48 00 00 39 1A 50 40 F9 DA 02 00 B4 48 03 40 F9", 1},
            {"FF C3 01 D1 FD 7B ?? A9 ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? ?? 68 1A 40 F9", 0},
            {"55 48 89 E5 41 57 41 56 41 55 41 54 53 48 8? EC", 0},
            {"2D E9 F? 4? D0 F8 00 80 81 46 D8 F8 18 00 D0 F8", 0}
        };
        
        bool found_any = false;
        
        for (int i = 0; i < info->dlpi_phnum; i++) {
            if (info->dlpi_phdr[i].p_type == PT_LOAD && (info->dlpi_phdr[i].p_flags & PF_X)) {
                uint8_t* start = (uint8_t*)(info->dlpi_addr + info->dlpi_phdr[i].p_vaddr);
                size_t seg_size = info->dlpi_phdr[i].p_memsz;
                
                for (size_t p = 0; p < patterns.size(); p++) {
                    auto matches = find_all_patterns(start, seg_size, patterns[p].pattern_str);
                    for (void* match : matches) {
                        apply_patch(match, patterns[p].retval);
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

// --- SSL Proxies for dynamic hooks ---
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
    return 0; // ssl_verify_ok
}

int ssl_verify_peer_cert_proxy_ret1(void *ssl) {
    return 1; // success
}

void patch_flutter_memory() {
    // Call our twin direct scanners
    dl_iterate_phdr(dl_phdr_callback, nullptr);
    if (!flutter_hooked) {
        scan_flutter_via_maps();
    }
}

void do_hooks() {
    shadowhook_hook_sym_name("libssl.so", "SSL_CTX_set_verify", (void *)SSL_CTX_set_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libssl.so", "SSL_set_verify", (void *)SSL_set_verify_proxy, nullptr);
    shadowhook_hook_sym_name("libssl.so", "SSL_get_verify_result", (void *)SSL_get_verify_result_proxy, nullptr);
    shadowhook_hook_sym_name("libcrypto.so", "X509_verify_cert", (void *)X509_verify_cert_proxy, nullptr);
    
    // Attempt to patch library memory directly
    patch_flutter_memory();
    
    // Also deploy fallback dynamic hooks if symbols happen to be exported
    shadowhook_hook_sym_name("libflutter.so", "ssl_verify_peer_cert", (void *)ssl_verify_peer_cert_proxy_ret0, nullptr);
    shadowhook_hook_sym_name("libflutter.so", "session_verify_cert_chain", (void *)ssl_verify_peer_cert_proxy_ret1, nullptr);
    shadowhook_hook_sym_name("libflutter.so", "ssl_crypto_x509_session_verify_cert_chain", (void *)ssl_verify_peer_cert_proxy_ret1, nullptr);
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
    patch_flutter_memory();
}
