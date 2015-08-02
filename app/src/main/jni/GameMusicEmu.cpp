#include <jni.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

#include "gme.h"

#define N_ITEMS(x) sizeof(x) / sizeof(x[0])

#define RET_ARGS0(ret) "()" ret
#define RET_ARGS1(ret, arg1)       "(" arg1 ")" ret
#define RET_ARGS2(ret, arg1, arg2) "(" arg1 arg2 ")" ret

#ifndef GME_PACKAGE
#define GME_PACKAGE "io/github/nesouri/engine"
#endif

#define C_GameMusicEmu GME_PACKAGE "/GameMusicEmu"
#define C_EngineType   GME_PACKAGE "/EngineType"
#define C_TrackInfo    GME_PACKAGE "/TrackInfo"

#define T_GameMusicEmu "L" C_GameMusicEmu ";"
#define T_EngineType   "L" C_EngineType ";"
#define T_TrackInfo    "L" C_TrackInfo ";"

#define T_String       "Ljava/lang/String;"
#define T_ByteBuffer   "Ljava/nio/ByteBuffer;"
#define T_ByteArray    "[B"
#define T_Bool         "Z"
#define T_Int          "I"
#define T_Float        "F"
#define T_Void         "V"

static jfieldID getHandleField(JNIEnv *env, jobject obj)
{
        jclass c = env->GetObjectClass(obj);
        return env->GetFieldID(c, "nativeHandle", "J");
}

template <typename T>
static T *getHandle(JNIEnv *env, jobject obj)
{
        jlong handle = env->GetLongField(obj, getHandleField(env, obj));
        if (handle == 0) {
                jclass Exception = env->FindClass("java/lang/IllegalStateException");
                env->ThrowNew(Exception, "Trying to use non-allocated native handle");
        }
        return reinterpret_cast<T *>(handle);
}

template <typename T>
static void setHandle(JNIEnv *env, jobject obj, T *t)
{
        jlong handle = reinterpret_cast<jlong>(t);
        env->SetLongField(obj, getHandleField(env, obj), handle);
}

static void check(JNIEnv *env, gme_err_t err)
{
        if (err) {
                jclass Exception = env->FindClass("java/io/IOException");
                env->ThrowNew(Exception, err);
        }
}

static jobject create(JNIEnv *env, jclass c, Music_Emu *emu)
{
        jmethodID cnstrctr = env->GetMethodID(c, "<init>", "()V");
        jobject obj = env->NewObject(c, cnstrctr);
        setHandle(env, obj, emu);
        return obj;
}

static gme_type_t engineFromString(JNIEnv *env, jstring value)
{
        const char *engine = env->GetStringUTFChars(value, NULL);
        if (strcmp(engine, "AY") == 0)
                return gme_ay_type;
        if (strcmp(engine, "GBS") == 0)
                return gme_gbs_type;
        if (strcmp(engine, "GYM") == 0)
                return gme_gym_type;
        if (strcmp(engine, "HES") == 0)
                return gme_hes_type;
        if (strcmp(engine, "KSS") == 0)
                return gme_kss_type;
        if (strcmp(engine, "NSF") == 0)
                return gme_nsf_type;
        if (strcmp(engine, "NSFE") == 0)
                return gme_nsfe_type;
        if (strcmp(engine, "SAP") == 0)
                return gme_sap_type;
        if (strcmp(engine, "SPC") == 0)
                return gme_spc_type;
        if (strcmp(engine, "VGM") == 0)
                return gme_vgm_type;
        if (strcmp(engine, "VGZ") == 0)
                return gme_vgz_type;
        jclass Exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(Exception, "Engine enum not is sync with JNI bindings!");
        return 0;
}

/**
 * static GameMusicEmu GameMusicEmu::create(final EngineType type, final int sampleRate) throws IOException
 */
JNIEXPORT jobject JNICALL GameMusicEmu_CreateFromEngineType(JNIEnv *env, jclass c, jobject engine, jint sampleRate)
{
        jclass engineClass = env->GetObjectClass(engine);
        jmethodID method = env->GetMethodID(engineClass, "toString", "()Ljava/lang/String;");
        jstring value = (jstring) env->CallObjectMethod(engine, method);
        Music_Emu *emu = gme_new_emu(engineFromString(env, value), sampleRate);
        if (!emu)
                check(env, "Out of Memory / Invalid Engine!");
        return create(env, c, emu);
}

/**
 * static GameMusicEmu GameMusicEmu::create(final String filename, final int sampleRate) throws IOException
 */
JNIEXPORT jobject JNICALL GameMusicEmu_CreateFromFilename(JNIEnv *env, jclass c, jstring filename, jint sampleRate)
{
        Music_Emu *emu;
        check(env, gme_open_file(env->GetStringUTFChars(filename, NULL), &emu, sampleRate));
        return create(env, c, emu);
}

/**
 * static GameMusicEmu GameMusicEmu::create(final ByteBuffer buffer, final int sampleRate) throws IOException
 */
JNIEXPORT jobject JNICALL GameMusicEmu_CreateFromByteBuffer(JNIEnv *env, jclass c, jobject buffer, jint sampleRate)
{
        Music_Emu *emu;
        void *bytes = env->GetDirectBufferAddress(buffer);
        jlong length = env->GetDirectBufferCapacity(buffer);
        check(env, gme_open_data(bytes, length, &emu, sampleRate));
        return create(env, c, emu);
}

/**
 * static GameMusicEmu GameMusicEmu::create(final byte[] buffer, final int sampleRate) throws IOException
 */
JNIEXPORT jobject JNICALL GameMusicEmu_CreateFromByteArray(JNIEnv *env, jclass c, jbyteArray buffer, jint sampleRate)
{
        Music_Emu *emu;
        jboolean isCopy;
        jbyte* bytes = env->GetByteArrayElements(buffer, &isCopy);
        jint length = env->GetArrayLength(buffer);
        check(env, gme_open_data(bytes, length, &emu, sampleRate));
        env->ReleaseByteArrayElements(buffer, bytes, 0);
        return create(env, c, emu);
}

/**
 * void GameMusicEmu::load(final String filename) throws IOException
 */
JNIEXPORT void JNICALL GameMusicEmu_LoadFromFilename(JNIEnv *env, jobject obj, jstring filename)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        check(env, gme_load_file(emu, env->GetStringUTFChars(filename, NULL)));
}

/**
 * void GameMusicEmu::load(final ByteBuffer buffer) throws IOException
 */
JNIEXPORT void JNICALL GameMusicEmu_LoadFromByteBuffer(JNIEnv *env, jobject obj, jobject buffer)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        void *bytes = env->GetDirectBufferAddress(buffer);
        jlong length = env->GetDirectBufferCapacity(buffer);
        check(env, gme_load_data(emu, bytes, length));
}

/**
 * void GameMusicEmu::load(final byte[] buffer) throws IOException
 */
JNIEXPORT void JNICALL GameMusicEmu_LoadFromByteArray(JNIEnv *env, jobject obj, jbyteArray buffer)
{
        jboolean isCopy;
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        jbyte* bytes = env->GetByteArrayElements(buffer, &isCopy);
        jint length = env->GetArrayLength(buffer);
        check(env, gme_load_data(emu, bytes, length));
        env->ReleaseByteArrayElements(buffer, bytes, 0);
}

/**
 * int GameMusicEmu::trackCount()
 */
JNIEXPORT jint JNICALL GameMusicEmu_TrackCount(JNIEnv *env, jobject obj)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        return gme_track_count(emu);
}

/**
 * void GameMusicEmu::track(final int track) throws IOException
 */
JNIEXPORT void JNICALL GameMusicEmu_StartTrack(JNIEnv *env, jobject obj, jint track)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        check(env, gme_start_track(emu, (int) track));
}

/**
 * TrackInfo GameMusicEmu::info(final int track) throws IOException
 */
JNIEXPORT jobject JNICALL GameMusicEmu_Info(JNIEnv *env, jobject obj, jint track)
{
        gme_info_t *info;
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        check(env, gme_track_info(emu, &info, track));
        jclass trackInfoClass = env->FindClass("com/github/nesouri/TrackInfo");
        jmethodID cnstrctr = env->GetMethodID(trackInfoClass, "<init>", "()V");
        jobject trackInfo = env->NewObject(trackInfoClass, cnstrctr);
        setHandle(env, trackInfo, info);
        return trackInfo;
}

/**
 * boolean GameMusicEmu::eof()
 */
JNIEXPORT jboolean JNICALL GameMusicEmu_EOF(JNIEnv *env, jobject obj)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        return gme_track_ended(emu) > 0 ? JNI_TRUE : JNI_FALSE;
}

/**
 * int GameMusicEmu::read(final ByteBuffer buffer) throws IOException
 */
JNIEXPORT jint JNICALL GameMusicEmu_ReadToByteBuffer(JNIEnv *env, jobject obj, jobject buffer)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        void *bytes = env->GetDirectBufferAddress(buffer);
        const size_t length = env->GetDirectBufferCapacity(buffer);
        check(env, gme_play(emu, length / sizeof(short), reinterpret_cast<short *>(bytes)));
        jclass byteBufferClass = env->FindClass("java/nio/Buffer");
        jmethodID method = env->GetMethodID(byteBufferClass, "position", "(I)Ljava/nio/Buffer;");
        env->CallObjectMethod(buffer, method, length);
        return length;
}

/**
 * int GameMusicEmu::read(final byte[] buffer) throws IOException
 */
JNIEXPORT jint JNICALL GameMusicEmu_ReadToByteArray(JNIEnv *env, jobject obj, jbyteArray buffer)
{
        jboolean isCopy;
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        jint length = env->GetArrayLength(buffer);
        jbyte* bytes = env->GetByteArrayElements(buffer, &isCopy);
        check(env, gme_play(emu, length / sizeof(short), reinterpret_cast<short *>(bytes)));
        env->ReleaseByteArrayElements(buffer, bytes, 0);
        return length;
}

/**
 * void GameMusicEmu::stereoDepth(final float stereo)
 */
JNIEXPORT void JNICALL GameMusicEmu_StereoDepth (JNIEnv *env, jobject obj, jfloat stereo)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        gme_set_stereo_depth(emu, stereo);
}

/**
 * void GameMusicEmu::enableAccuracy(final boolean enable)
 */
JNIEXPORT void JNICALL GameMusicEmu_EnableAccuracy(JNIEnv *env, jobject obj, jboolean enable)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        gme_enable_accuracy(emu, enable == JNI_TRUE ? 1 : 0);
}

/**
 * void GameMusicEmu::close()
 */
JNIEXPORT void JNICALL GameMusicEmu_Close(JNIEnv *env, jobject obj)
{
        Music_Emu *emu = getHandle<Music_Emu>(env, obj);
        gme_delete(emu);
}

/**
 * String TrackInfo::system()
 */
JNIEXPORT jstring JNICALL TrackInfo_System(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->system);
}

/**
 * String TrackInfo::game()
 */
JNIEXPORT jstring JNICALL TrackInfo_Game(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->game);
}

/**
 * String TrackInfo::song()
 */
JNIEXPORT jstring JNICALL TrackInfo_Song(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->song);
}

/**
 * String TrackInfo::author()
 */
JNIEXPORT jstring JNICALL TrackInfo_Author(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->author);
}

/**
 * String TrackInfo::copyright()
 */
JNIEXPORT jstring JNICALL TrackInfo_Copyright(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->copyright);
}

/**
 * String TrackInfo::comment()
 */
JNIEXPORT jstring JNICALL TrackInfo_Comment(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->comment);
}

/**
 * String TrackInfo::dumper()
 */
JNIEXPORT jstring JNICALL TrackInfo_Dumper(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        return env->NewStringUTF(info->dumper);
}

/**
 * void TrackInfo::close()
 */
JNIEXPORT void JNICALL TrackInfo_Close(JNIEnv *env, jobject obj)
{
        gme_info_t *info = getHandle<gme_info_t>(env, obj);
        gme_free_info(info);
        info = NULL;
        setHandle(env, obj, info);
}


static JNINativeMethod GameMusicEmuMethods[] = {
        {"create",         RET_ARGS2(T_GameMusicEmu, T_EngineType, T_Int), (void *) GameMusicEmu_CreateFromEngineType},
        {"create",         RET_ARGS2(T_GameMusicEmu, T_String,     T_Int), (void *) GameMusicEmu_CreateFromFilename  },
        {"create",         RET_ARGS2(T_GameMusicEmu, T_ByteBuffer, T_Int), (void *) GameMusicEmu_CreateFromByteBuffer},
        {"create",         RET_ARGS2(T_GameMusicEmu, T_ByteArray,  T_Int), (void *) GameMusicEmu_CreateFromByteArray },
        {"load",           RET_ARGS1(T_Void,         T_String           ), (void *) GameMusicEmu_LoadFromFilename    },
        {"load",           RET_ARGS1(T_Void,         T_ByteBuffer       ), (void *) GameMusicEmu_LoadFromByteBuffer  },
        {"load",           RET_ARGS1(T_Void,         T_ByteArray        ), (void *) GameMusicEmu_LoadFromByteArray   },
        {"trackCount",     RET_ARGS0(T_Int                              ), (void *) GameMusicEmu_TrackCount          },
        {"track",          RET_ARGS1(T_Void,         T_Int              ), (void *) GameMusicEmu_StartTrack          },
        {"info",           RET_ARGS1(T_TrackInfo,    T_Int              ), (void *) GameMusicEmu_Info                },
        {"eof",            RET_ARGS0(T_Bool                             ), (void *) GameMusicEmu_EOF                 },
        {"read",           RET_ARGS1(T_Int,          T_ByteBuffer       ), (void *) GameMusicEmu_ReadToByteBuffer    },
        {"read",           RET_ARGS1(T_Int,          T_ByteArray        ), (void *) GameMusicEmu_ReadToByteArray     },
        {"stereoDepth",    RET_ARGS1(T_Void,         T_Float            ), (void *) GameMusicEmu_StereoDepth         },
        {"enableAccuracy", RET_ARGS1(T_Void,         T_Bool             ), (void *) GameMusicEmu_EnableAccuracy      },
        {"close",          RET_ARGS0(T_Void                             ), (void *) GameMusicEmu_Close               },
};

static JNINativeMethod TrackInfoMethods[] = {
        {"system",    RET_ARGS0(T_String), (void *) TrackInfo_System   },
        {"game",      RET_ARGS0(T_String), (void *) TrackInfo_Game     },
        {"song",      RET_ARGS0(T_String), (void *) TrackInfo_Song     },
        {"author",    RET_ARGS0(T_String), (void *) TrackInfo_Author   },
        {"copyright", RET_ARGS0(T_String), (void *) TrackInfo_Copyright},
        {"comment",   RET_ARGS0(T_String), (void *) TrackInfo_Comment  },
        {"dumper",    RET_ARGS0(T_String), (void *) TrackInfo_Dumper   },
        {"close",     RET_ARGS0(T_Void),   (void *) TrackInfo_Close    }
};

static int registerClass(JNIEnv *env, const char *className, JNINativeMethod *methods, int numMethods)
{
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL)
        return JNI_FALSE;
    if (env->RegisterNatives(clazz, methods, numMethods) < 0)
        return JNI_FALSE;
    return JNI_TRUE;
}


typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
        UnionJNIEnvToVoid uenv = {0};

        if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
                fprintf(stderr, "GetEnv failed");
                return -1;
        }

        JNIEnv *env = uenv.env;

        if (!registerClass(env, C_TrackInfo, TrackInfoMethods, N_ITEMS (TrackInfoMethods))) {
                fprintf(stderr, "register " C_TrackInfo " failed");
                return -1;
        }

        if (!registerClass(env, C_GameMusicEmu, GameMusicEmuMethods, N_ITEMS (GameMusicEmuMethods))) {
                fprintf(stderr, "register " C_GameMusicEmu " failed");
                return -1;
        }

        return JNI_VERSION_1_4;
}
