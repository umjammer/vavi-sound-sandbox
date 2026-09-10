/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import org.rococoa.ObjCClass;
import org.rococoa.cocoa.foundation.NSData;

import static vavix.rococoa.avfoundation.AudioToolbox.AudioUnitPropertyID;
import static vavix.rococoa.avfoundation.AudioToolbox.AudioUnitScope;


/**
 * @interface AVAudioUnitMIDIInstrument : AVAudioUnit
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/??/?? umjammer initial version <br>
 */
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
//logger.log(Level.TRACE, "sysex: %02X\n%s".formatted(midiData[0], StringUtil.getDump(midiData)));
        NSData data = NSData.dataWithBytes(midiData);
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

//#region MIDI 2.0

    /**
     * Sends one complete Universal MIDI Packet message to the instrument. CoreMIDI translates it to
     * whatever {@link #MIDIProtocol()} reports, so MIDI 2.0 messages reach a MIDI 1.0 only
     * instrument as well, though of course with their resolution cut down.
     *
     * @param eventList a scratch buffer owned by the caller, reusable across calls
     * @param words one complete UMP message, native endian
     * @return OSStatus, 0 on success
     */
    public int sendMIDIEventList(MIDIEventList eventList, int... words) {
        return AudioToolbox.instance.MusicDeviceMIDIEventList(audioUnit(), 0, eventList.packet(0, words));
    }

    /**
     * The MIDI protocol the instrument wants to be talked in.
     *
     * @return MIDIProtocolID, {@link MIDIEventList#kMIDIProtocol_1_0} unless the instrument says otherwise
     */
    public int MIDIProtocol() {
        Memory value = new Memory(Integer.BYTES);
        IntByReference size = new IntByReference(Integer.BYTES);
        int status = AudioToolbox.instance.AudioUnitGetProperty(audioUnit(),
                AudioUnitPropertyID.kAudioUnitProperty_AudioUnitMIDIProtocol.id,
                AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                0,
                value,
                size);
        return status == 0 ? value.getInt(0) : MIDIEventList.kMIDIProtocol_1_0;
    }

    /**
     * Tells the instrument which protocol the host would like to send in. It is free to ignore the
     * wish, ask {@link #MIDIProtocol()} for what it settled on. Has to be set before the audio unit
     * is initialized, which for an {@link AVAudioUnit} means before it is attached to an engine.
     *
     * @param protocol MIDIProtocolID
     * @return OSStatus, 0 on success
     */
    public int setHostMIDIProtocol(int protocol) {
        Memory value = new Memory(Integer.BYTES);
        value.setInt(0, protocol);
        return AudioToolbox.instance.AudioUnitSetProperty(audioUnit(),
                AudioUnitPropertyID.kAudioUnitProperty_HostMIDIProtocol.id,
                AudioUnitScope.kAudioUnitScope_Global.ordinal(),
                0,
                value,
                Integer.BYTES);
    }

//#endregion
}
