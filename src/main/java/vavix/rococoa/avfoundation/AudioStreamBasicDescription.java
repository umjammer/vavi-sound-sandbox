
package vavix.rococoa.avfoundation;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

import vavi.util.ByteUtil;


public class AudioStreamBasicDescription extends Structure {

    enum AudioFileTypeID {
        kAudioFileAIFFType("AIFF"),
        kAudioFileAIFCType("AIFC"),
        kAudioFileWAVEType("WAVE"),
        kAudioFileSoundDesigner2Type("Sd2f"),
        kAudioFileNextType("NeXT"),
        kAudioFileMP3Type("MPG3"),
        kAudioFileAC3Type("ac-3"),
        kAudioFileAAC_ADTSType("adts"),
        kAudioFileMPEG4Type("mp4f"),
        kAudioFileM4AType("m4af"),
        kAudioFileCAFType("caff");
        final int id;
        AudioFileTypeID(String id) {
            this.id = ByteUtil.readBeInt(id.getBytes(), 0);
        }
    }

    enum AudioFormatID {
        kAudioFormatLinearPCM("lpcm"),
        kAudioFormatAC3("ac-3"),
        kAudioFormat60958AC3("cac3"),
        kAudioFormatAppleIMA4("ima4"),
        kAudioFormatMPEG4AAC("aac "),
        kAudioFormatMPEG4CELP("celp"),
        kAudioFormatMPEG4HVXC("hvxc"),
        kAudioFormatMPEG4TwinVQ("twvq"),
        kAudioFormatMACE3("MAC3"),
        kAudioFormatMACE6("MAC6"),
        kAudioFormatULaw("ulaw"),
        kAudioFormatALaw("alaw"),
        kAudioFormatQDesign("QDMC"),
        kAudioFormatQDesign2("QDM2"),
        kAudioFormatQUALCOMM("Qclp"),
        kAudioFormatMPEGLayer1(".mp1"),
        kAudioFormatMPEGLayer2(".mp2"),
        kAudioFormatMPEGLayer3(".mp3"),
        kAudioFormatTimeCode("time"),
        kAudioFormatMIDIStream("midi"),
        kAudioFormatParameterValueStream("apvs"),
        kAudioFormatAppleLossless("alac"),
        kAudioFormatMPEG4AAC_HE("aach"),
        kAudioFormatMPEG4AAC_LD("aacl"),
        kAudioFormatMPEG4AAC_HE_V2("aacp"),
        kAudioFormatMPEG4AAC_Spatial("aacs"),
        kAudioFormatAMR("samr");
        final int id;
        AudioFormatID(String id) {
            this.id = ByteUtil.readBeInt(id.getBytes(), 0);
        }
    }

    enum AudioFormatFlag {
        kAudioFormatFlagIsFloat                      (1 << 0),
        kAudioFormatFlagIsBigEndian                  (1 << 1),
        kAudioFormatFlagIsSignedInteger              (1 << 2),
        kAudioFormatFlagIsPacked                     (1 << 3),
        kAudioFormatFlagIsAlignedHigh                (1 << 4),
        kAudioFormatFlagIsNonInterleaved             (1 << 5),
        kAudioFormatFlagIsNonMixable                 (1 << 6),
        kAudioFormatFlagsAreAllClear                 (1 << 31),

        kLinearPCMFormatFlagIsFloat                  (kAudioFormatFlagIsFloat.value),
        kLinearPCMFormatFlagIsBigEndian              (kAudioFormatFlagIsBigEndian.value),
        kLinearPCMFormatFlagIsSignedInteger          (kAudioFormatFlagIsSignedInteger.value),
        kLinearPCMFormatFlagIsPacked                 (kAudioFormatFlagIsPacked.value),
        kLinearPCMFormatFlagIsAlignedHigh            (kAudioFormatFlagIsAlignedHigh.value),
        kLinearPCMFormatFlagIsNonInterleaved         (kAudioFormatFlagIsNonInterleaved.value),
        kLinearPCMFormatFlagIsNonMixable             (kAudioFormatFlagIsNonMixable.value),
        kLinearPCMFormatFlagsAreAllClear             (kAudioFormatFlagsAreAllClear.value),

        kAppleLosslessFormatFlag_16BitSourceData    (1),
        kAppleLosslessFormatFlag_20BitSourceData    (2),
        kAppleLosslessFormatFlag_24BitSourceData    (3),
        kAppleLosslessFormatFlag_32BitSourceData    (4);
        final int value;
        AudioFormatFlag(int value) {
            this.value = value;
        }
    }

//    enum X {
//#if TARGET_RT_BIG_ENDIAN
//        kAudioFormatFlagsNativeEndian       = kAudioFormatFlagIsBigEndian,
//#else
//        kAudioFormatFlagsNativeEndian       (0),
//#endif
//        kAudioFormatFlagsCanonical (
//            AudioFormatFlag.kAudioFormatFlagIsFloat.value |
//            kAudioFormatFlagsNativeEndian |
//            kAudioFormatFlagIsPacked),
//        kAudioFormatFlagsNativeFloatPacked (
//            kAudioFormatFlagIsFloat |
//            kAudioFormatFlagsNativeEndian |
//            kAudioFormatFlagIsPacked);
//        long value;
//        X(long value) {
//            this.value = value;
//        }
//    }

//    public static class ByValue extends AudioStreamBasicDescription implements Structure.ByValue {}

    public AudioStreamBasicDescription() {
        setAlignType(ALIGN_GNUC);
    }

    protected List<String> getFieldOrder() {
        return Arrays.asList("mSampleRate",
                             "mFormatID",
                             "mFormatFlags",
                             "mBytesPerPacket",
                             "mFramesPerPacket",
                             "mBytesPerFrame",
                             "mChannelsPerFrame",
                             "mBitsPerChannel",
                             "mReserved");
    }

    public double mSampleRate;
    public int mFormatID;
    public int mFormatFlags;
    public int mBytesPerPacket;
    public int mFramesPerPacket;
    public int mBytesPerFrame;
    public int mChannelsPerFrame;
    public int mBitsPerChannel;
    public int mReserved;
}
