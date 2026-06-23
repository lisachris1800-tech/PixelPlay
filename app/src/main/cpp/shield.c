#include <jni.h>
#include <string.h>
#include <android/log.h>

#define LOGT "Shield"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOGT, __VA_ARGS__)

static const unsigned char XK[8] = {0x4A,0x7D,0x2B,0x6F,0x1C,0x5E,0x3A,0x8F};

JNIEXPORT jbyteArray JNICALL
Java_com_pixelplay_app_Shield_decrypt(JNIEnv* env, jclass clz, jbyteArray jData) {
    jsize len = (*env)->GetArrayLength(env, jData);
    jbyte* data = (*env)->GetByteArrayElements(env, jData, NULL);
    for (jsize i = 0; i < len; i++) {
        data[i] ^= (jbyte)XK[i & 7];
    }
    (*env)->ReleaseByteArrayElements(env, jData, data, 0);
    LOGI("[+] decrypted %d bytes", len);
    return jData;
}
