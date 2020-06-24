#include <jni.h>
#include <string>
#include <android/log.h>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredCPU.h>
#include <malloc.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include "ObserverChain.h"
#include <vector>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>

#define log_print __android_log_print
static SuperpoweredAndroidAudioIO *audioIO;
static SuperpoweredAdvancedAudioPlayer *player;
static float *floatBuffer;
std::vector<ObserverChain *> store_Wlistener_vector;
static JavaVM *jvm = NULL;
static char* eof = "EOF";
JNIEnv *store_env;

// Called when the application is initialized. You can initialize SuperpoweredUSBSystem
// at any time btw. Although this function is marked __unused, it's due Android Studio's
// annoying warning only. It's definitely used.
__unused jint JNI_OnLoad (
        JavaVM * __unused vm,
        void * __unused reserved
) {
//    SuperpoweredUSBSystem::initialize(NULL, NULL, NULL, NULL, NULL);
    return JNI_VERSION_1_6;
}

// Called when the application is closed. You can destroy SuperpoweredUSBSystem at any
// time btw. Although this function is marked __unused, it's due Android Studio's annoying
// warning only. It's definitely used.
__unused void JNI_OnUnload (
        JavaVM * __unused vm,
        void * __unused reserved
) {
//    SuperpoweredUSBSystem::destroy();
}

void txtCallback(JNIEnv *env, const _jstring *message_) {
    if (!store_Wlistener_vector.empty()) {
        for (int i = 0; i < store_Wlistener_vector.size(); i++) {
            env->CallVoidMethod(store_Wlistener_vector[i]->store_Wlistener,
                                store_Wlistener_vector[i]->store_method, message_);
        }

    }
}
void test_string_callback_from_c(char *val) {
    __android_log_print(ANDROID_LOG_VERBOSE, "GetEnv:", " start Callback  to JNL [%d]  \n", val);
    JNIEnv *g_env;
    if (NULL == jvm) {
        __android_log_print(ANDROID_LOG_ERROR, "GetEnv:", "  No VM  \n");
        return;
    }
    //  double check it's all ok
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6; // set your JNI version
    args.name = NULL; // you might want to give the java thread a name
    args.group = NULL; // you might want to assign the java thread to a ThreadGroup

    int getEnvStat = jvm->GetEnv((void **) &g_env, JNI_VERSION_1_6);

    if (getEnvStat == JNI_EDETACHED) {
        __android_log_print(ANDROID_LOG_ERROR, "GetEnv:", " not attached\n");
        if (jvm->AttachCurrentThread(&g_env, &args) != 0) {
            __android_log_print(ANDROID_LOG_ERROR, "GetEnv:", " Failed to attach\n");
        } else __android_log_print(ANDROID_LOG_VERBOSE, "GetEnv:", " JNI_OK\n");
    } else if (getEnvStat == JNI_OK) {
        __android_log_print(ANDROID_LOG_VERBOSE, "GetEnv:", " JNI_OK\n");
    } else if (getEnvStat == JNI_EVERSION) {
        __android_log_print(ANDROID_LOG_ERROR, "GetEnv:", " version not supported\n");
    }

    jstring message = g_env->NewStringUTF(val);//

    txtCallback(g_env, message);

    if (g_env->ExceptionCheck()) {
        g_env->ExceptionDescribe();
    }

    if (getEnvStat == JNI_EDETACHED) {
        jvm->DetachCurrentThread();
    }
}

// This is called periodically by the audio engine.
static bool audioProcessing (
        void * __unused clientdata, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // sampling rate
) {
    if (player->process(floatBuffer, false, (unsigned int)numberOfFrames)) {
        SuperpoweredFloatToShortInt(floatBuffer, audio, (unsigned int)numberOfFrames);
        return true;
    } else {
        return false;
    }
}

// Called by the player.
static void playerEventCallback (
        void * __unused clientData,
        SuperpoweredAdvancedAudioPlayerEvent event,
        void *value
) {
    switch (event) {
        case SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess:
            break;
        case SuperpoweredAdvancedAudioPlayerEvent_LoadError:
            log_print(ANDROID_LOG_ERROR, "PlayerExample", "Open error: %s", (char *)value);
            break;
        case SuperpoweredAdvancedAudioPlayerEvent_EOF:
            player->pause(0.0,0);
            test_string_callback_from_c(eof);
            break;
        default:;
    };
}

extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_OpenFile(JNIEnv *env, jobject instance, jstring path_,
                                             jint offset, jint length) {
    const char *path = env->GetStringUTFChars(path_, 0);
    player->open(path, offset, length);
    env->ReleaseStringUTFChars(path_, path);
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_TogglePlayback(JNIEnv *env, jclass type) {
    player->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);  // prevent dropouts
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_StartAudio(JNIEnv *env, jobject instance, jint samplerate,
                                               jint buffersize) {

    // Allocate audio buffer.
    floatBuffer = (float *)malloc(sizeof(float) * 2 * buffersize);

    // Initialize player and pass callback function.
    player = new SuperpoweredAdvancedAudioPlayer (
            NULL,                           // clientData
            playerEventCallback,            // callback function
            (unsigned int)samplerate,       // sampling rate
            0                               // cachedPointCount
    );

    // Initialize audio with audio callback function.
    audioIO = new SuperpoweredAndroidAudioIO (
            samplerate,                     // sampling rate
            buffersize,                     // buffer size
            false,                          // enableInput
            true,                           // enableOutput
            audioProcessing,                // process callback function
            NULL,                           // clientData
            -1,                             // inputStreamType (-1 = default)
            SL_ANDROID_STREAM_MEDIA,        // outputStreamType (-1 = default)
            buffersize * 2                  // latencySamples
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_Cleanup(JNIEnv *env, jobject instance) {
    delete audioIO;
    delete player;
    free(floatBuffer);
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_onForeground(JNIEnv *env, jobject instance) {
    audioIO->onForeground();
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_onBackground(JNIEnv *env, jobject instance) {
    audioIO->onBackground();
}
extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_PlayService_OpenFile(JNIEnv *env, jclass type, jstring path_, jint offset,
                                            jint length) {
    const char *path = env->GetStringUTFChars(path_, 0);

    player->open(path, offset, length);

    env->ReleaseStringUTFChars(path_, path);
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_PlayService_TogglePlayback(JNIEnv *env, jclass type) {

    player->togglePlayback();
    SuperpoweredCPU::setSustainedPerformanceMode(player->playing);

}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_PlayService_onBackground(JNIEnv *env, jobject instance) {
    audioIO->onBackground();
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_PlayService_onForeground(JNIEnv *env, jobject instance) {
    audioIO->onForeground();
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_PlayService_Cleanup(JNIEnv *env, jobject instance) {
    delete audioIO;
    delete player;
    free(floatBuffer);
}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_nsubscribeListener(JNIEnv *env, jclass type,
                                                       jobject listener) {

    env->GetJavaVM(&jvm); //store jvm reference for later call
    store_env = env;
    jweak store_Wlistener = env->NewWeakGlobalRef(listener);
    jclass clazz = env->GetObjectClass(store_Wlistener);
    jmethodID store_method = env->GetMethodID(clazz, "onAcceptMessage", "(Ljava/lang/String;)V");
    jmethodID store_methodVAL = env->GetMethodID(clazz, "onAcceptMessageVal", "(I)V");
    ObserverChain *tmpt = new ObserverChain(store_Wlistener, store_method, store_methodVAL);
    store_Wlistener_vector.push_back(tmpt);
    __android_log_print(ANDROID_LOG_VERBOSE, "GetEnv:", " Subscribe to Listener  OK \n");
    if (NULL == store_method) return;

}extern "C"
JNIEXPORT void JNICALL
Java_ru_abch_carplayer_MainActivity_ndismissListener(JNIEnv *env, jclass type) {

    if (!store_Wlistener_vector.empty()) {
        for (int i = 0; i < store_Wlistener_vector.size(); i++) {
            env->DeleteWeakGlobalRef(store_Wlistener_vector[i]->store_Wlistener);
            store_Wlistener_vector[i]->store_method = NULL;
            store_Wlistener_vector[i]->store_methodVAL = NULL;
        }
        store_Wlistener_vector.clear();
    }

}