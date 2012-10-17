#include "tvqdnc.h"
#include "tvqenc.h"
#include "vavi_sound_twinvq_TwinVQ.h"

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncInitialize
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;Lvavi/sound/twinvq/TwinVQ$EncSpecificInfo;Lvavi/sound/twinvq/TwinVQ$Index;I)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncInitialize
  (JNIEnv *env, jobject obj, jobject arg1, jobject arg2, jobject arg3, jint arg4) {

    int result = TvqEncInitialize( headerInfo *setupInfo, encSpecificInfo *encInfo, INDEX *index, int dispErrorMessageBox );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncTerminate
 * Signature: (Lvavi/sound/twinvq/TwinVQ$Index;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncTerminate
  (JNIEnv *env, jobject obj, jobject arg1) {

    TvqEncTerminate(INDEX *index);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetVectorInfo
 * Signature: ([[I[[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetVectorInfo
  (JNIEnv *env, jobject obj, jobjectArray arg1, jobjectArray arg2) {

    TvqEncGetVectorInfo(int *bits0[], int *bits1[]);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncResetFrameCounter
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncResetFrameCounter
  (JNIEnv *env, jobject obj) {

    TvqEncResetFrameCounter();
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncodeFrame
 * Signature: ([FLvavi/sound/twinvq/TwinVQ$Index;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncodeFrame
  (JNIEnv *env, jobject obj, jfloatArray arg1, jobject arg2) {

    TvqEncodeFrame( float sig_in[], INDEX  *index );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncUpdateVectorInfo
 * Signature: (II[I[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncUpdateVectorInfo
  (JNIEnv *env, jobject obj, jint arg1, jint arg2, jintArray arg3, jintArray arg4) {

    TvqEncUpdateVectorInfo(int varbits, int *ndiv, int bits0[], int bits1[]);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncSetFrameCounter
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncSetFrameCounter
  (JNIEnv *env, jobject obj, jint position) {

    TvqEncSetFrameCounter((int) position);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetFrameSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetFrameSize
  (JNIEnv *env, jobject obj) {

    int frameSize = TvqEncGetFrameSize();
    return frameSize;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetNumChannels
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetNumChannels
  (JNIEnv *env, jobject obj) {

    int channels = TvqEncGetNumChannels();
    return channels;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetNumFixedBitsPerFrame
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetNumFixedBitsPerFrame
  (JNIEnv *evn, jobject obj) {

    int bits = TvqEncGetNumFixedBitsPerFrame();
    return bits;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetSetupInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetSetupInfo
  (JNIEnv *env, jobject obj, jobject arg1) {

    TvqEncGetSetupInfo( headerInfo *setupInfo );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetSamplingRate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetSamplingRate
  (JNIEnv *env, jobject obj) {

    float samplingRate = TvqEncGetSamplingRate();
    return samplingRate;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetBitRate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetBitRate
  (JNIEnv *env, jobject obj) {

    int bitRate = TvqEncGetBitRate();
    return bitRate;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetConfInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$ConfInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetConfInfo
  (JNIEnv *env, jobject obj, jobjectarg1) {

   TvqEncGetConfInfo( tvqConfInfo *cf ); // TODO
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetNumFrames
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetNumFrames
  (JNIEnv *env, jobject obj) {

    int frames = TvqEncGetNumFrames();
    return frames;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetVersionID
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetVersionID
  (JNIEnv *env, jobject obj, jint arg1, jstring arg2) {

    int result = TvqGetVersionID( int versionNum, char* versionString );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncCheckVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncCheckVersion
  (JNIEnv *env, jobject obj, jstring arg1) {

    int result = TvqEncCheckVersion( char *strTvqID );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqEncGetModuleVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqEncGetModuleVersion
  (JNIEnv *env, jobject obj, jstring arg1) {

    int result = TvqEncGetModuleVersion( char* versionString );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqInitialize
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;Lvavi/sound/twinvq/TwinVQ$Index;I)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqInitialize
  (JNIEnv *env, jobject obj, jobject arg1, jobject arg2, jint arg3) {

     int result = TvqInitialize( headerInfo *setupInfo, INDEX *index, int dispErrorMessageBox );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqTerminate
 * Signature: (Lvavi/sound/twinvq/TwinVQ$Index;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqTerminate
  (JNIEnv *env, jobject obj, jobject index) {

    TvqTerminate(INDEX *index);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetVectorInfo
 * Signature: ([[I[[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetVectorInfo
  (JNIEnv *env, jobject obj, jobjectArray bits0, jobjectArray bits1) {

    TvqGetVectorInfo(int *bits0[], int *bits1[]);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqResetFrameCounter
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqResetFrameCounter
  (JNIEnv *env, jobject obj) {

    TvqResetFrameCounter();
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqDecodeFrame
 * Signature: (Lvavi/sound/twinvq/TwinVQ$Index;[F)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqDecodeFrame
  (JNIEnv *env, jobject obj, jobject indexp, jfloatArray out) {

    TvqDecodeFrame(INDEX  *indexp, float out[]);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqWtypeToBtype
 * Signature: (I[I)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqWtypeToBtype
  (JNIEnv *env, jobject obj, jint w_type, jintArray btype) {

    int  TvqWtypeToBtype( int w_type, int *btype );
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqUpdateVectorInfo
 * Signature: (I[I[I[I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqUpdateVectorInfo
  (JNIEnv *env, jobject obj, jint varbits, jintArray ndiv, jintArray bits0, jintArray bits1) {

    TvqUpdateVectorInfo(int varbits, int *ndiv, int bits0[], int bits1[]);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqSetFrameCounter
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqSetFrameCounter
  (JNIEnv *env, jobject obj, jint position) {

    TvqSetFrameCounter((int) position);
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqCheckVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqCheckVersion
  (JNIEnv *env, jobject obj, jstring versionID) {

    int result = TvqCheckVersion(char *versionID); // TODO
}

/*
 * setup information
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetSetupInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$HeaderInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetSetupInfo
  (JNIEnv *env, jobject obj, jobject setupInfo) {

    TvqGetSetupInfo(headerInfo *setupInfo); // TODO
}

/*
 * configuration information
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetConfInfo
 * Signature: (Lvavi/sound/twinvq/TwinVQ$ConfInfo;)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetConfInfo
  (JNIEnv *env, jobject obj, jobject cf) {

     TvqGetConfInfo(tvqConfInfo *cf); // TODO
}

/*
 * frame size
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetFrameSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetFrameSize
  (JNIEnv *env, jobject obj) {

    int frameSize = TvqGetFrameSize();
    return (jint) frameSize;
}

/*
 * number of channels
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetNumChannels
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetNumChannels
  (JNIEnv *env, jobject obj) {

    int channels = TvqGetNumChannels();
    return (jint) channels;
}

/*
 * total bitrate
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetBitRate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetBitRate
  (JNIEnv *env, jobject obj) {

    int bitRate = TvqGetBitRate();
    return (jint) bitRate;
}

/*
 * sampling rate
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetSamplingRate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetSamplingRate
  (JNIEnv *env, jobject obj) {

    float samplingRate = TvqGetSamplingRate();
    return (jfloat) samplingRate;
}

/*
 * number of fixed bits per frame
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetNumFixedBitsPerFrame
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetNumFixedBitsPerFrame
  (JNIEnv *env, jobject obj) {

    int bits = TvqGetNumFixedBitsPerFrame();
    return bits;
}

/*
 * number of decoded frame
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetNumFrames
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetNumFrames
  (JNIEnv *env, jobject obj) {

    int frames = TvqGetNumFrames();
    return (jint) frames;
}

/*
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetModuleVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetModuleVersion
  (JNIEnv *env, jobject obj, jstring versionString) {

    int result = TvqGetModuleVersion(char* versionString); // TODO
}

/*
 * count number of used bits 
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqFbCountUsedBits
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqFbCountUsedBits
  (JNIEnv *env, jobject obj, jint nbit) {

    TvqFbCountUsedBits((int) nbit);
}

/*
 * query average bitrate for the tool
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetFbCurrentBitrate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetFbCurrentBitrate
  (JNIEnv *env, jobject obj) {

    float bitrate = TvqGetFbCurrentBitrate();
    return (jfloat) bitrate;
}

/*
 * query total number of used bits 
 * Class:     vavi_sound_twinvq_TwinVQ
 * Method:    TvqGetFbTotalBits
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_vavi_sound_twinvq_TwinVQ_TvqGetFbTotalBits
  (JNIEnv *env, jobject obj) {

    int result = TvqGetFbTotalBits();
    return (jint) result;
}

/* */
