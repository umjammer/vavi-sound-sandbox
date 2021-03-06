/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class vavi_sound_twinvq_TwinVQ */

#ifndef _Included_vavi_sound_twinvq_TwinVQ
#define _Included_vavi_sound_twinvq_TwinVQ
#ifdef __cplusplus
extern "C" {
#endif
#undef vavi_sound_twinvq_TwinVQ_TVQ_UNKNOWN_VERSION
#define vavi_sound_twinvq_TwinVQ_TVQ_UNKNOWN_VERSION -1L
#undef vavi_sound_twinvq_TwinVQ_V2
#define vavi_sound_twinvq_TwinVQ_V2 0L
#undef vavi_sound_twinvq_TwinVQ_V2PP
#define vavi_sound_twinvq_TwinVQ_V2PP 1L
#undef vavi_sound_twinvq_TwinVQ_N_VERSIONS
#define vavi_sound_twinvq_TwinVQ_N_VERSIONS 2L
#undef vavi_sound_twinvq_TwinVQ_N_BTYPE
#define vavi_sound_twinvq_TwinVQ_N_BTYPE 3L
#undef vavi_sound_twinvq_TwinVQ_N_INTR_TYPE
#define vavi_sound_twinvq_TwinVQ_N_INTR_TYPE 4L
#undef vavi_sound_twinvq_TwinVQ_N_CH_MAX
#define vavi_sound_twinvq_TwinVQ_N_CH_MAX 2L
#undef vavi_sound_twinvq_TwinVQ_BUFSIZ
#define vavi_sound_twinvq_TwinVQ_BUFSIZ 1024L
#undef vavi_sound_twinvq_TwinVQ_KEYWORD_BYTES
#define vavi_sound_twinvq_TwinVQ_KEYWORD_BYTES 4L
#undef vavi_sound_twinvq_TwinVQ_VERSION_BYTES
#define vavi_sound_twinvq_TwinVQ_VERSION_BYTES 8L
#undef vavi_sound_twinvq_TwinVQ_ELEM_BYTES
#define vavi_sound_twinvq_TwinVQ_ELEM_BYTES 8L
#undef vavi_sound_twinvq_TwinVQ_ISSTMAX
#define vavi_sound_twinvq_TwinVQ_ISSTMAX 88L
/* Inaccessible static: stepSizeTable */
/* Inaccessible static: stateAdjustTable */
/* Inaccessible static: instance */
/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncInitialize
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;Lvavi/sound/twinvq/TwinVQ$EncSpecificInfo;Lvavi/sound/twinvq/TwinVQ$Index;I)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncInitialize
  (JNIEnv *, jobject, jobject, jobject, jobject, jint);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncTerminate
 * Signature: (Lvavi/sound/twinvq/TwinVQ$Index;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncTerminate
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetVectorInfo
 * Signature: ([[I[[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetVectorInfo
  (JNIEnv *, jobject, jobjectArray, jobjectArray);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncResetFrameCounter
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncResetFrameCounter
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncodeFrame
 * Signature: ([FLvavi/sound/twinvq/TwinVQ$Index;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncodeFrame
  (JNIEnv *, jobject, jfloatArray, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncUpdateVectorInfo
 * Signature: (II[I[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncUpdateVectorInfo
  (JNIEnv *, jobject, jint, jint, jintArray, jintArray);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncSetFrameCounter
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncSetFrameCounter
  (JNIEnv *, jobject, jint);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetFrameSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetFrameSize
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetNumChannels
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetNumChannels
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetNumFixedBitsPerFrame
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetNumFixedBitsPerFrame
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetSetupInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetSetupInfo
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetSamplingRate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetSamplingRate
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetBitRate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetBitRate
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetConfInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$ConfInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetConfInfo
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetNumFrames
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetNumFrames
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetVersionID
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetVersionID
  (JNIEnv *, jobject, jint, jstring);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncCheckVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncCheckVersion
  (JNIEnv *, jobject, jstring);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetModuleVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetModuleVersion
  (JNIEnv *, jobject, jstring);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqInitialize
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;Lvavi/sound/twinvq/TwinVQ$Index;I)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqInitialize
  (JNIEnv *, jobject, jobject, jobject, jint);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqTerminate
 * Signature: (Lvavi/sound/twinvq/TwinVQ$Index;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqTerminate
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetVectorInfo
 * Signature: ([[I[[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetVectorInfo
  (JNIEnv *, jobject, jobjectArray, jobjectArray);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqResetFrameCounter
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqResetFrameCounter
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqDecodeFrame
 * Signature: (Lvavi/sound/twinvq/TwinVQ$Index;[F)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqDecodeFrame
  (JNIEnv *, jobject, jobject, jfloatArray);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqWtypeToBtype
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqWtypeToBtype
  (JNIEnv *, jobject, jint, jintArray);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqUpdateVectorInfo
 * Signature: (I[I[I[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqUpdateVectorInfo
  (JNIEnv *, jobject, jint, jintArray, jintArray, jintArray);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqSetFrameCounter
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqSetFrameCounter
  (JNIEnv *, jobject, jint);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqCheckVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqCheckVersion
  (JNIEnv *, jobject, jstring);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetSetupInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetSetupInfo
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetConfInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$ConfInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetConfInfo
  (JNIEnv *, jobject, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetFrameSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetFrameSize
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetNumChannels
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetNumChannels
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetBitRate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetBitRate
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetSamplingRate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetSamplingRate
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetNumFixedBitsPerFrame
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetNumFixedBitsPerFrame
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetNumFrames
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetNumFrames
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetModuleVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetModuleVersion
  (JNIEnv *, jobject, jstring);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqFbCountUsedBits
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqFbCountUsedBits
  (JNIEnv *, jobject, jint);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetFbCurrentBitrate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetFbCurrentBitrate
  (JNIEnv *, jobject);

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetFbTotalBits
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetFbTotalBits
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
