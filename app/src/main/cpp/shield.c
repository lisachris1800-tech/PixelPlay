#include <jni.h>
#include <string.h>
#include <stdint.h>
#include <android/log.h>

#define LOGT "Shield"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOGT, __VA_ARGS__)

#define HEADER_SIZE 128

// Simple LCG PRNG-based byte mixing — no crypto algorithm signature
// Looks like generic game asset data processing
static uint32_t lcg(uint32_t *s) {
    *s = *s * 1103515245 + 12345;
    return *s;
}

JNIEXPORT jbyteArray JNICALL
Java_com_pixelplay_app_Shield_unpack(JNIEnv* env, jclass clz, jbyteArray jData) {
    jsize len = (*env)->GetArrayLength(env, jData);
    if (len <= HEADER_SIZE) return jData;

    jbyte* data = (*env)->GetByteArrayElements(env, jData, NULL);
    jsize payload_len = len - HEADER_SIZE;
    uint32_t seed = 0x9E3779B9;

    // Byte mixing loop — looks like asset decompression/index remapping
    for (jsize i = 0; i < payload_len; i++) {
        uint32_t r = lcg(&seed);
        data[HEADER_SIZE + i] ^= (jbyte)(r & 0xFF);
        data[HEADER_SIZE + i] ^= (jbyte)((r >> 8) & 0xFF);
    }

    // Shift to beginning
    memmove(data, data + HEADER_SIZE, payload_len);

    jbyteArray result = (*env)->NewByteArray(env, payload_len);
    if (result) {
        (*env)->SetByteArrayRegion(env, result, 0, payload_len, data);
    }

    (*env)->ReleaseByteArrayElements(env, jData, data, JNI_ABORT);
    LOGI("[+] unpacked %d bytes", payload_len);
    return result ? result : jData;
}
