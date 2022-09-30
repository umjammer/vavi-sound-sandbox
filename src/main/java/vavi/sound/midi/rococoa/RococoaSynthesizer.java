/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.Instrument;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;

import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.StringUtil;

import vavix.rococoa.avfoundation.AVAudioEngine;
import vavix.rococoa.avfoundation.AVAudioFormat;
import vavix.rococoa.avfoundation.AVAudioMixerNode;
import vavix.rococoa.avfoundation.AVAudioUnitEQ;
import vavix.rococoa.avfoundation.AVAudioUnitMIDIInstrument;
import vavix.rococoa.avfoundation.AudioComponentDescription;


/**
 * RococoaSynthesizer.
 * <p>
 * system property
 * <li> "vavi.sound.midi.rococoa.RococoaSynthesizer.audesc"
 * <p>
 * ex. "Ftcr:mc5p"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
public class RococoaSynthesizer implements Synthesizer {

    private static final String version = "1.0.4";

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("Rococoa MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer by MacOS",
                            "Version " + version) {};

    // TODO != channel???
    private static final int MAX_CHANNEL = 16;

    private AVAudioEngine engine;

    private AVAudioUnitMIDIInstrument midiSynth;

    private AVAudioUnitEQ equalizer;
    private AVAudioMixerNode mixer;

    private MidiChannel[] channels = new MidiChannel[MAX_CHANNEL];

    // TODO voice != channel ( = getMaxPolyphony())
    private VoiceStatus[] voiceStatus = new VoiceStatus[MAX_CHANNEL];

    private long timestump;

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
Debug.println("already open: " + hashCode());
            return;
        }

        for (int i = 0; i < MAX_CHANNEL; i++) {
            channels[i] = new RococoaMidiChannel(i);
            voiceStatus[i] = new VoiceStatus();
            voiceStatus[i].channel = i;
        }

        this.engine = AVAudioEngine.newInstance();
Debug.println(engine);

        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        String audesc = System.getProperty(getClass().getName() + ".audesc", "appl:dls ");
        String[] pair = audesc.split(":");
        // for example "Ftcr:mc5p", "NiSc:nK1v"
Debug.println("AudioUnit: " + pair[0] + ":" + pair[1]);
        description.componentSubType = ByteUtil.readBeInt(pair[1].getBytes());
        description.componentManufacturer = ByteUtil.readBeInt(pair[0].getBytes());
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        midiSynth = AVAudioUnitMIDIInstrument.init(description);
        if (midiSynth == null) {
            throw new MidiUnavailableException(audesc);
        }
Debug.println(midiSynth + ", " + midiSynth.name() + ", " + midiSynth.version() + ", " + midiSynth.manufacturerName());

Debug.println("output: " + engine.outputNode());
        this.equalizer = AVAudioUnitEQ.newInstance();
Debug.println("equalizer: " + equalizer);
        this.mixer = engine.mainMixerNode();
Debug.println("mixer: " + mixer);

        engine.attachNode(midiSynth);
        engine.attachNode(equalizer);
        AVAudioFormat format = engine.inputNode().outputFormatForBus(0);
Debug.println("format: " + format);
        engine.connect_to_format(midiSynth, equalizer, format);
        engine.connect_to_format(equalizer, engine.outputNode(), format);

        engine.prepare();

        mixer.setOutputVolume(0.1f);
Debug.printf("out volume: %3.2f%n", mixer.outputVolume());
        equalizer.setGlobalGain(-40);
Debug.printf("equalizer gain: %+3.0f%n", equalizer.globalGain());

        boolean r = engine.start();
Debug.println("stated: " + r + ", " + hashCode());
    }

    @Override
    public void close() {
        engine.stop();
    }

    @Override
    public boolean isOpen() {
        return engine != null && engine.running();
    }

    @Override
    public long getMicrosecondPosition() {
        return timestump;
    }

    @Override
    public int getMaxReceivers() {
        // TODO Auto-generated method stub
        return 1;
    }

    @Override
    public int getMaxTransmitters() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new RococoaReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Transmitter> getTransmitters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getMaxPolyphony() {
        return 40;
    }

    @Override
    public long getLatency() {
        return 0;
    }

    @Override
    public MidiChannel[] getChannels() {
        return channels;
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        return voiceStatus;
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        // TODO Auto-generated method stub
        return new Instrument[0];
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        // TODO Auto-generated method stub
        return new Instrument[0];
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        // TODO Auto-generated method stub

    }

    private class RococoaMidiChannel implements MidiChannel {

        private int channel;

        private int[] polyPressure = new int[128];
        private int pressure;
        private int pitchBend;
        private int[] control = new int[128];

        /** */
        public RococoaMidiChannel(int channel) {
            this.channel = channel;
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            midiSynth.startNote(noteNumber, velocity, channel);
            voiceStatus[channel].note = noteNumber;
            voiceStatus[channel].volume = velocity;
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            midiSynth.stopNote(noteNumber, channel); // TODO velocity
            voiceStatus[channel].note = 0;
            voiceStatus[channel].volume = velocity;
        }

        @Override
        public void noteOff(int noteNumber) {
            noteOff(noteNumber, 0);
        }

        @Override
        public void setPolyPressure(int noteNumber, int pressure) {
            midiSynth.sendPressure(noteNumber, pressure, channel);
            polyPressure[noteNumber] = pressure;
        }

        @Override
        public int getPolyPressure(int noteNumber) {
            return polyPressure[noteNumber];
        }

        @Override
        public void setChannelPressure(int pressure) {
            midiSynth.sendPressure(pressure, channel);
            this.pressure = pressure;
        }

        @Override
        public int getChannelPressure() {
            return pressure;
        }

        @Override
        public void controlChange(int controller, int value) {
            midiSynth.sendController(controller, value, channel);
            control[controller] = value;
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            midiSynth.sendProgramChange(program, channel);
            voiceStatus[channel].program = program;
        }

        @Override
        public void programChange(int bank, int program) {
            int bankMSB = bank >> 7;
            int bankLSB = bank & 0x7F;
            controlChange(0, bankMSB);
            controlChange(32, bankLSB);
            programChange(program);
        }

        @Override
        public int getProgram() {
            return voiceStatus[channel].program;
        }

        @Override
        public void setPitchBend(int bend) {
            midiSynth.sendPitchBend(bend, channel);
            pitchBend = bend;
        }

        @Override
        public int getPitchBend() {
            return pitchBend;
        }

        @Override
        public void resetAllControllers() {
            controlChange(121, 0);
        }

        @Override
        public void allNotesOff() {
            controlChange(123, 0);
        }

        @Override
        public void allSoundOff() {
            controlChange(120, 0);
        }

        @Override
        public boolean localControl(boolean on) {
            controlChange(122, on ? 127 : 0);
            return getController(122) >= 64;
        }

        @Override
        public void setMono(boolean on) {
            controlChange(on ? 126 : 127, 0);
        }

        @Override
        public boolean getMono() {
            return getController(126) == 0;
        }

        @Override
        public void setOmni(boolean on) {
            controlChange(on ? 125 : 124, 0);
        }

        @Override
        public boolean getOmni() {
            return getController(125) == 0;
        }

        @Override
        public void setMute(boolean mute) {
            // TODO Auto-generated method stub
            voiceStatus[channel].active = !mute;
        }

        @Override
        public boolean getMute() {
            // TODO Auto-generated method stub
            return voiceStatus[channel].active;
        }

        @Override
        public void setSolo(boolean soloState) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean getSolo() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private List<Receiver> receivers = new ArrayList<>();

    private class RococoaReceiver implements Receiver {
        private boolean isOpen;

        public RococoaReceiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            timestump = timeStamp;
            if (isOpen) {
                if (message instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) message;
                    int channel = shortMessage.getChannel();
                    int command = shortMessage.getCommand();
                    int data1 = shortMessage.getData1();
                    int data2 = shortMessage.getData2();
                    switch (command) {
                    case ShortMessage.NOTE_OFF:
                        channels[channel].noteOff(data1, data2);
                        break;
                    case ShortMessage.NOTE_ON:
                        channels[channel].noteOn(data1, data2);
                        break;
                    case ShortMessage.POLY_PRESSURE:
                        channels[channel].setPolyPressure(data1, data2);
                        break;
                    case ShortMessage.CONTROL_CHANGE:
//Debug.printf("control change: %02X, %d %d\n", command, data1, data2);
                        channels[channel].controlChange(data1, data2);
                        break;
                    case ShortMessage.PROGRAM_CHANGE:
//Debug.printf("program change: %02X, %d %d\n", command, data1, data2);
                        channels[channel].programChange(data1);
                        break;
                    case ShortMessage.CHANNEL_PRESSURE:
                        channels[channel].setChannelPressure(data1);
                        break;
                    case ShortMessage.PITCH_BEND:
                        channels[channel].setPitchBend(data1 | (data2 << 7));
                        break;
                    default:
Debug.printf("uncknown short: %02X\n", command);
                    }
                } else if (message instanceof SysexMessage) {
                    SysexMessage sysexMessage = (SysexMessage) message;
                    byte[] data = sysexMessage.getData();
Debug.printf("sysex: %02X\n%s", sysexMessage.getStatus(), StringUtil.getDump(data));
                    switch (data[0]) {
                    case 0x7f: // Universal Realtime
                        @SuppressWarnings("unused")
                        int c = data[1]; // 0x7f: Disregards channel
                        // Sub-ID, Sub-ID2
                        if (data[2] == 0x04 && data[3] == 0x01) { // Device Control / Master Volume
                                // TODO doesn't work
                                float gain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383f;
                                float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0) * 4;
Debug.printf("sysex volume: gain: %03.2f, dB: %-3.0f%n", gain, dB);
//                                equalizer.setGlobalGain(-90);
                        }
                        break;
                    default:
                        break;
                    }

                    midiSynth.sendMIDISysExEvent(sysexMessage.getMessage());
                } else if (message instanceof MetaMessage) {
                    MetaMessage metaMessage = (MetaMessage) message;
Debug.printf("meta: %02x", metaMessage.getType());
                    switch (metaMessage.getType()) {
                    case 0x2f:
                        break;
                    }
                } else {
                    assert false;
                }
            } else {
                throw new IllegalStateException("receiver is not open");
            }
        }

        @Override
        public void close() {
            receivers.remove(this);
            isOpen = false;
        }
    }
}

/* */
