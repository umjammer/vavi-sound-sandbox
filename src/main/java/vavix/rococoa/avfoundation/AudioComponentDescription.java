
package vavix.rococoa.avfoundation;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

import vavi.util.ByteUtil;


public class AudioComponentDescription extends Structure {

    public static final int kAudioUnitType_Output = ByteUtil.readBeInt("auou".getBytes());
    public static final int kAudioUnitType_MusicDevice = ByteUtil.readBeInt("aumu".getBytes());
    public static final int kAudioUnitType_MusicEffect = ByteUtil.readBeInt("aumf".getBytes());
    public static final int kAudioUnitType_FormatConverter = ByteUtil.readBeInt("aufc".getBytes());
    public static final int kAudioUnitType_Effect = ByteUtil.readBeInt("aufx".getBytes());
    public static final int kAudioUnitType_Mixer = ByteUtil.readBeInt("aumx".getBytes());
    public static final int kAudioUnitType_Panner = ByteUtil.readBeInt("aupn".getBytes());
    public static final int kAudioUnitType_Generator = ByteUtil.readBeInt("augn".getBytes());
    public static final int kAudioUnitType_OfflineEffect = ByteUtil.readBeInt("auol".getBytes());
    public static final int kAudioUnitType_MIDIProcessor = ByteUtil.readBeInt("aumi".getBytes());

    public static final int kAudioUnitType_RemoteEffect = ByteUtil.readBeInt("aurx".getBytes());
    public static final int kAudioUnitType_RemoteGenerator = ByteUtil.readBeInt("aurg".getBytes());
    public static final int kAudioUnitType_RemoteInstrument = ByteUtil.readBeInt("auri".getBytes());
    public static final int kAudioUnitType_RemoteMusicEffect = ByteUtil.readBeInt("aurm".getBytes());

    public static final int kAudioUnitSubType_MIDISynth = ByteUtil.readBeInt("msyn".getBytes());
    public static final int kAudioUnitSubType_Sampler = ByteUtil.readBeInt("samp".getBytes());
    public static final int kAudioUnitSubType_DLSSynth = ByteUtil.readBeInt("dls ".getBytes());

    public static final int kAudioUnitManufacturer_Apple = ByteUtil.readBeInt("appl".getBytes());

    public static class ByValue extends AudioComponentDescription implements Structure.ByValue {}

    public AudioComponentDescription() {
        setAlignType(ALIGN_GNUC);
    }

    protected List<String> getFieldOrder() {
        return Arrays.asList("componentType",
                             "componentSubType",
                             "componentManufacturer",
                             "componentFlags",
                             "componentFlagsMask");
    }

    public int componentType;
    public int componentSubType;
    public int componentManufacturer;
    public int componentFlags;
    public int componentFlagsMask;

    public ByValue byValue() {
        ByValue byValue = new ByValue();
        byValue.componentType = componentType;
        byValue.componentSubType = componentSubType;
        byValue.componentManufacturer = componentManufacturer;
        byValue.componentFlags = componentFlags;
        byValue.componentFlagsMask = componentFlagsMask;
        return byValue;
    }

    public String toString() {
        return new String(ByteUtil.getBeBytes(componentType)) + ":" +
                new String(ByteUtil.getBeBytes(componentSubType)) + ":" +
                new String(ByteUtil.getBeBytes(componentManufacturer)) + ", " +
                componentFlags + ", " + componentFlagsMask;
    }
}
