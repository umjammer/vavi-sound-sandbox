
package vavix.rococoa.avfoundation;

import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSData;


public abstract class AVAudioUnitMIDIInstrument extends AVAudioUnit {

    private static final _Class CLASS = org.rococoa.Rococoa.createClass("AVAudioUnitMIDIInstrument", _Class.class);

    public interface _Class extends ObjCClass {
        AVAudioUnitMIDIInstrument alloc();
    }

    // TODO wrong description terminates jvm (cannot catch NSException)
    public static AVAudioUnitMIDIInstrument init(AudioComponentDescription description) {
        AVAudioUnitMIDIInstrument audioUnit = CLASS.alloc();
        return audioUnit.initWithAudioComponentDescription(description.byValue());
    }

    public abstract AVAudioUnitMIDIInstrument initWithAudioComponentDescription(AudioComponentDescription.ByValue description);

    public void sendMIDIEvent(int midiStatus, int data1) {
        sendMIDIEvent_data1((byte) midiStatus, (byte) data1);
    }

    public abstract void sendMIDIEvent_data1(byte midiStatus, byte data1);

    public void sendMIDIEvent(int midiStatus, int data1, int data2) {
        sendMIDIEvent_data1_data2((byte) midiStatus, (byte) data1, (byte) data2);
    }

    public abstract void sendMIDIEvent_data1_data2(byte midiStatus, byte data1, byte data2);

    public void sendProgramChange(int program, int channel) {
        sendProgramChange_onChannel((byte) program, (byte) channel);
    }

    public abstract void sendProgramChange_onChannel(byte program, byte channel);

    public abstract void sendProgramChange_bankMSB_bankLSB_onChannel(byte program, byte bankMSB, byte bankLSB, byte channel);

    public void sendController(int controller, int value, int channel) {
        sendController_withValue_onChannel((byte) controller, (byte) value, (byte) channel);
    }

    public abstract void sendController_withValue_onChannel(byte controller, byte value, byte channel);

    public void startNote(int note, int velocity, int channel) {
        startNote_withVelocity_onChannel((byte) note, (byte) velocity, (byte) channel);
    }

    public abstract void startNote_withVelocity_onChannel(byte note, byte velocity, byte channel);

    public void stopNote(int note, int channel) {
        stopNote_onChannel((byte) note, (byte) channel);
    }

    public abstract void stopNote_onChannel(byte note, byte channel);

    /**
     * Sends a MIDI System Exclusive event to the instrument.
     * @param midiData should contain the complete SysEx data, including start
     *            (F0) and termination (F7) bytes.
     */
    public void sendMIDISysExEvent(byte[] midiData) {
//Debug.printf("sysex: %02X\n%s", midiData[0], StringUtil.getDump(midiData));
        NSData data = NSData.CLASS.dataWithBytes_length(midiData, midiData.length);
        sendMIDISysExEvent(data);
    }

    public abstract void sendMIDISysExEvent(NSData midiData);

    public void sendPitchBend(int pitchbend, int channel) {
        sendPitchBend_onChannel((short) pitchbend, (byte) channel);
    }

    public abstract void sendPitchBend_onChannel(short pitchbend, byte channel);

    public void sendPressure(int pressure, int channel) {
        sendPressure_onChannel((byte) pressure, (byte) channel);
    }

    public abstract void sendPressure_onChannel(short pressure, byte channel);

    public void sendPressure(int key, int value, int channel) {
        sendPressureForKey_withValue_onChannel((byte) key, (byte) value, (byte) channel);
    }

    public abstract void sendPressureForKey_withValue_onChannel(byte key, byte value, byte channel);
}