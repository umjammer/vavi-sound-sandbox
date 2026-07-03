/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.karplusStrong;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.karplusStrong.GuitarString;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.midi.karplusStrong.KarplusStrongMidiDeviceProvider.version;


/**
 * KarplusStrongSynthesizer.
 * <p>
 * a software synthesizer that simulates plucked strings by the
 * karplus strong algorithm ({@link GuitarString}). since the algorithm
 * models a plucked string only, program change is ignored and
 * every channel sounds as a guitar string.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/07/04 umjammer initial version <br>
 */
public class KarplusStrongSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(KarplusStrongSynthesizer.class.getName());

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("KarplusStrong MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer by the karplus strong algorithm",
                            "Version " + version) {};

    private static final int MAX_CHANNEL = 16;

    /** {@link GuitarString} is hard coded as 44100 Hz */
    private static final float SAMPLE_RATE = 44100;

    /** 16 bit, mono, signed, little endian */
    private final AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    /** samples per rendering cycle */
    private static final int BLOCK_SIZE = 512;

    /** damping per sample applied after note off (~ 60 ms to -60 dB) */
    private static final double RELEASE_FACTOR = 0.9974;

    /** below this gain a released voice is discarded */
    private static final double GAIN_EPSILON = 0.0001;

    private final KarplusStrongMidiChannel[] channels = new KarplusStrongMidiChannel[MAX_CHANNEL];

    private final List<VoiceStatus> voiceStatuses = new ArrayList<>();

    private final KarplusStrongSoundbank soundBank = new KarplusStrongSoundbank();

    private long timestamp;

    private volatile boolean isOpen;

    private SourceDataLine line;

    /** master gain by sysex master volume */
    private volatile double masterGain = 1;

    /** a sounding karplus strong string */
    private static class Voice {
        final GuitarString string;
        final int channel;
        final int note;
        /** velocity gain, damped after note off */
        volatile double gain;
        volatile boolean released;
        Voice(GuitarString string, int channel, int note, double gain) {
            this.string = string;
            this.channel = channel;
            this.note = note;
            this.gain = gain;
        }
    }

    /** modified by the receiver thread, read by the rendering thread */
    private final List<Voice> voices = new CopyOnWriteArrayList<>();

    /** the rendering thread */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "KarplusStrong Renderer");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        return thread;
    });

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
            channels[i] = new KarplusStrongMidiChannel(i);
        }

        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
            line.addLineListener(event -> logger.log(Level.DEBUG, "Line: " + event.getType()));
            line.open(audioFormat);
            line.start();
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }

        timestamp = 0;
        isOpen = true;

        executor.submit(this::play);
    }

    /** mixes all sounding strings into the line */
    private void play() {
        byte[] buf = new byte[BLOCK_SIZE * 2];

        while (isOpen) {
            try {
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    double mix = 0;
                    for (Voice voice : voices) {
                        mix += voice.string.sample() * voice.gain;
                        voice.string.tic();
                        if (voice.released) {
                            voice.gain *= RELEASE_FACTOR;
                        }
                    }
                    mix *= masterGain;
                    // clip
                    if (mix > 1) mix = 1;
                    else if (mix < -1) mix = -1;
                    int sample = (int) (mix * Short.MAX_VALUE);
                    buf[i * 2] = (byte) sample;
                    buf[i * 2 + 1] = (byte) (sample >> 8);
                }
                voices.removeIf(v -> v.released && v.gain < GAIN_EPSILON);
                line.write(buf, 0, buf.length);
            } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
        }
    }

    /** midi note number to frequency in Hz */
    private static double midiToFrequency(int noteNumber) {
        return 440 * Math.pow(2, (noteNumber - 69) / 12.0);
    }

    @Override
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void close() {
        isOpen = false;
        for (int i = 0; i < receivers.size(); i++) receivers.get(i).close();
        executor.shutdown();
        if (line != null) {
            line.drain();
            line.close();
        }
        voices.clear();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public long getMicrosecondPosition() {
        return timestamp;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new KarplusStrongReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new MidiUnavailableException("No transmitter available");
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Collections.emptyList();
    }

    @Override
    public int getMaxPolyphony() {
        return 64;
    }

    @Override
    public long getLatency() {
        return (long) (BLOCK_SIZE / SAMPLE_RATE * 1_000_000);
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
        return soundbank instanceof KarplusStrongSoundbank;
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return soundBank;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return soundBank.getInstruments();
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return soundBank.getInstruments();
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private class KarplusStrongMidiChannel implements MidiChannel {

        private final int channel;

        private int program;
        private boolean mute;

        private final int[] polyPressure = new int[128];
        private int pressure;
        private int pitchBend = 8192;
        private final int[] control = new int[128];

        /** */
        public KarplusStrongMidiChannel(int channel) {
            this.channel = channel;
            control[7] = 100; // channel volume default
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            if (velocity == 0) {
                noteOff(noteNumber);
                return;
            }
            if (mute) {
                return;
            }
            // percussion is not a string, skip the rhythm channel
            if (channel == 9) {
                return;
            }

            if (voices.size() >= getMaxPolyphony()) {
                // steal the oldest voice
                voices.remove(0);
            }

            GuitarString string = new GuitarString(midiToFrequency(noteNumber));
            string.pluck();
            double gain = (velocity / 127.0) * (control[7] / 127.0);
            voices.add(new Voice(string, channel, noteNumber, gain));

            VoiceStatus voiceStatus = new VoiceStatus();
            voiceStatus.channel = channel;
            voiceStatus.program = program;
            voiceStatus.note = noteNumber;
            voiceStatus.volume = velocity;
            voiceStatus.active = true;
            voiceStatuses.add(voiceStatus);
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            for (Voice voice : voices) {
                if (voice.channel == channel && voice.note == noteNumber && !voice.released) {
                    voice.released = true;
                }
            }

            VoiceStatus voiceStatus = find(channel, noteNumber);
            if (voiceStatus != null) {
                voiceStatus.active = false;
                voiceStatuses.remove(voiceStatus);
            }
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
            polyPressure[noteNumber] = pressure;
        }

        @Override
        public int getPolyPressure(int noteNumber) {
            return polyPressure[noteNumber];
        }

        @Override
        public void setChannelPressure(int pressure) {
            this.pressure = pressure;
        }

        @Override
        public int getChannelPressure() {
            return pressure;
        }

        @Override
        public void controlChange(int controller, int value) {
            switch (controller) {
            case 7: // channel volume
logger.log(Level.DEBUG, "control change[%d]: vol(%02x): %d".formatted(channel, controller, value));
                break;
            case 120: // all sound off
            case 123: // all notes off
                for (Voice voice : voices) {
                    if (voice.channel == channel) {
                        voice.released = true;
                    }
                }
                break;
            default:
logger.log(Level.TRACE, "control change unhandled[%d]: (%02x): %d".formatted(channel, controller, value));
            }
            control[controller] = value;
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            // a string is a string, only one instrument exists
            this.program = program & 0x7f;
logger.log(Level.DEBUG, "program change[%d]: %d (ignored)".formatted(channel, this.program));
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
            return program;
        }

        @Override
        public void setPitchBend(int bend) {
            // GuitarString frequency is fixed by its buffer length, bend is not supported
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
            return mute;
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

    private class KarplusStrongReceiver implements MidiDeviceReceiver {
        @SuppressWarnings("hiding")
        private boolean isOpen;

        public KarplusStrongReceiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) throw new IllegalStateException("receiver is not open");

            timestamp = timeStamp;
            switch (message) {
            case ShortMessage shortMessage -> {
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
logger.log(Level.DEBUG, "unhandled short: %02X".formatted(command));
                }
            }
            case SysexMessage sysexMessage -> {
                byte[] data = sysexMessage.getData();
logger.log(Level.TRACE, "sysex: %02X\n%s".formatted(sysexMessage.getStatus(), StringUtil.getDump(data, 32)));
                switch (data[0]) {
                case 0x7f: // Universal Realtime
                    // Sub-ID, Sub-ID2
                    if (data[2] == 0x04 && data[3] == 0x01) { // Device Control / Master Volume
                        float gain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383f;
logger.log(Level.DEBUG, "sysex volume: gain: %4.2f".formatted(gain));
                        masterGain = gain;
                    }
                    break;
                default:
logger.log(Level.DEBUG, "sysex unhandled: %02x".formatted(data[0]));
                    break;
                }
            }
            case null, default ->
logger.log(Level.DEBUG, message.getClass().getName());
            }
        }

        @Override
        public void close() {
            receivers.remove(this);
            isOpen = false;
        }

        @Override
        public MidiDevice getMidiDevice() {
            return KarplusStrongSynthesizer.this;
        }
    }
}
