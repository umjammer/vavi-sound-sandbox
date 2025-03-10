/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sound.midi.Instrument;
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

import com.jsyn.JSyn;
import com.jsyn.midi.MidiSynthesizer;
import com.jsyn.unitgen.LineOut;
import com.jsyn.util.MultiChannelSynthesizer;
import com.jsyn.util.VoiceDescription;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * JSynSynthesizer.
 *
 * TODO delegate default synthesizer
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
public class JSynSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(JSynSynthesizer.class.getName());

    static {
        try {
            try (InputStream is = JSynSynthesizer.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sandbox/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String version;

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("JSyn MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer powered by JSyn",
                            "Version " + version) {};

    // TODO != channel???
    private static final int MAX_CHANNEL = 16;

    private com.jsyn.Synthesizer synth;
    private MultiChannelSynthesizer multiSynth;
    private MidiSynthesizer midiSynthesizer;
    private LineOut lineOut;

    private final JSynSoundbank soundBank = new JSynSoundbank();

    private final MidiChannel[] channels = new MidiChannel[MAX_CHANNEL];

    private final List<VoiceStatus> voiceStatuses = new ArrayList<>();

    private long timestamp;

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
logger.log(Level.WARNING, "already open: " + hashCode());
            return;
        }

        for (int i = 0; i < channels.length; i++) {
            channels[i] = new JSynMidiChannel(i);
        }

        synth = JSyn.createSynthesizer();

        synth.add(lineOut = new LineOut());

        this.multiSynth = new MultiChannelSynthesizer();

        // voice setting for each channel
        VoiceDescription voice1 = (VoiceDescription) soundBank.getInstruments()[0].getData();
        VoiceDescription drums = (VoiceDescription) soundBank.getInstruments()[0].getData();
        multiSynth.setup(synth, 0, 9, 6, voice1);
        multiSynth.setup(synth, 9, 1, 6, drums);
        multiSynth.setup(synth, 10, 6, 6, voice1);

        // volume default (0.75) is too small
        multiSynth.setMasterAmplitude(100);

        multiSynth.getOutput().connect(0, lineOut.input, 0); // channel 0
        multiSynth.getOutput().connect(1, lineOut.input, 1); // channel 1

        midiSynthesizer = new MidiSynthesizer(multiSynth);

        synth.start();
        lineOut.start();
    }

    @Override
    public void close() {
        lineOut.stop();
        synth.stop();
    }

    @Override
    public boolean isOpen() {
        return synth != null && synth.isRunning();
    }

    @Override
    public long getMicrosecondPosition() {
        return timestamp;
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
        return new JSynReceiver();
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
        return MAX_CHANNEL;
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
        return voiceStatuses.toArray(VoiceStatus[]::new);
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

    private class JSynMidiChannel implements MidiChannel {

        private final int channel;

        private int note;
        private int volume;
        private int program;
        private boolean mute;

        private final int[] polyPressure = new int[128];
        private int pressure;
        private int pitchBend;
        private final int[] control = new int[128];

        /** */
        public JSynMidiChannel(int channel) {
            this.channel = channel;
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            VoiceStatus voiceStatus = new VoiceStatus();
            voiceStatus.channel = channel;
            voiceStatus.program = program;
            voiceStatus.note = noteNumber;
            voiceStatus.volume = velocity;
            voiceStatus.active = true;
            voiceStatuses.add(voiceStatus);

            midiSynthesizer.noteOn(channel, noteNumber, velocity);

            this.note = noteNumber;
            this.volume = velocity;
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            midiSynthesizer.noteOff(channel, noteNumber, velocity);

            VoiceStatus voiceStatus = find(channel, noteNumber);
            if (voiceStatus != null) voiceStatuses.remove(voiceStatus);

            this.note = 0;
            this.volume = velocity;
        }

        private VoiceStatus find(int channel, int noteNumber) {
            return voiceStatuses.stream().filter(vs -> vs.channel == channel && vs.note == noteNumber).findFirst().orElse(null);
        }

        @Override
        public void noteOff(int noteNumber) {
            noteOff(noteNumber, 0);
        }

        @Override
        public void setPolyPressure(int noteNumber, int pressure) {
            midiSynthesizer.polyphonicAftertouch(channel, noteNumber, pressure);
            polyPressure[noteNumber] = pressure;
        }

        @Override
        public int getPolyPressure(int noteNumber) {
            return polyPressure[noteNumber];
        }

        @Override
        public void setChannelPressure(int pressure) {
            midiSynthesizer.channelPressure(channel, pressure);
            this.pressure = pressure;
        }

        @Override
        public int getChannelPressure() {
            return pressure;
        }

        @Override
        public void controlChange(int controller, int value) {
            midiSynthesizer.controlChange(channel, controller, value);
            control[controller] = value;
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            midiSynthesizer.programChange(channel, program);
            this.program = program;
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
            return this.program;
        }

        @Override
        public void setPitchBend(int bend) {
            midiSynthesizer.pitchBend(channel, bend);
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
            this.mute = mute;
        }

        @Override
        public boolean getMute() {
            return this.mute;
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

    private final List<Receiver> receivers = new ArrayList<>();

    private class JSynReceiver implements Receiver {
        private boolean isOpen;

        public JSynReceiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            timestamp = timeStamp;
            if (isOpen) {
                if (message instanceof ShortMessage shortMessage) {
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
                        channels[channel].controlChange(data1, data2);
                        break;
                    case ShortMessage.PROGRAM_CHANGE:
                        channels[channel].programChange(data1);
                        break;
                    case ShortMessage.CHANNEL_PRESSURE:
                        channels[channel].setChannelPressure(data1);
                        break;
                    case ShortMessage.PITCH_BEND:
                        channels[channel].setPitchBend(data1 | (data2 << 7));
                        break;
                    default:
logger.log(Level.DEBUG, "%02X".formatted(command));
                    }
                } else if (message instanceof SysexMessage sysexMessage) {
                    byte[] data = sysexMessage.getData();
logger.log(Level.DEBUG, "sysex: %02X\n%s".formatted(sysexMessage.getStatus(), StringUtil.getDump(data)));
                    switch (data[0]) {
                    case 0x7f: // Universal Realtime
                        @SuppressWarnings("unused")
                        int c = data[1]; // 0x7f: Disregards channel
                        // Sub-ID, Sub-ID2
                        if (data[2] == 0x04 && data[3] == 0x01) { // Device Control / Master Volume
                            float gain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383f;
logger.log(Level.DEBUG, "sysex volume: gain: %4.0f%n".formatted(gain * 100));
                            multiSynth.setMasterAmplitude(gain * 100);
                            break;
                        }
                        break;
                    default:
                        break;
                    }
                } else {
                    // TODO meta message
logger.log(Level.DEBUG, message.getClass().getName());
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
