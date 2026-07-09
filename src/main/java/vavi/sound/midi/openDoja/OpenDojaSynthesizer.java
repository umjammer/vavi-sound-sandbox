/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javax.sound.midi.MetaMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import opendoja.audio.mld.Sampler;
import opendoja.audio.mld.SamplerProvider;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * OpenDojaSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/07/09 nsano initial version <br>
 */
public abstract class OpenDojaSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(OpenDojaSynthesizer.class.getName());

    private static final int MAX_CHANNEL = 16;
    private static final float SAMPLE_RATE = 48000.0f;
    private static final int BLOCK_SIZE = 1024;

    private final AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);

    private final OpenDojaMidiChannel[] channels = new OpenDojaMidiChannel[MAX_CHANNEL];
    private final List<VoiceStatus> voiceStatuses = new ArrayList<>();
    private final List<Receiver> receivers = new ArrayList<>();

    private final Object lock = new Object();

    private long timestamp;
    private volatile boolean isOpen;
    private SourceDataLine line;
    private Sampler sampler;
    private volatile double masterGain = 1.0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "OpenDoja Renderer");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        return thread;
    });

    protected OpenDojaSynthesizer() {
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new OpenDojaMidiChannel(i);
        }
    }

    protected abstract SamplerProvider createSamplerProvider();

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
            logger.log(Level.WARNING, "already open: " + hashCode());
            return;
        }

        synchronized (lock) {
            SamplerProvider provider = createSamplerProvider();
            sampler = provider.instance(SAMPLE_RATE);
            sampler.drumEnable(9, true);
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

    private void play() {
        float[] samples = new float[BLOCK_SIZE * 2];
        byte[] buf = new byte[BLOCK_SIZE * 4];

        while (isOpen) {
            try {
                synchronized (lock) {
                    if (sampler != null) {
                        sampler.render(samples, 0, BLOCK_SIZE, (float) masterGain, (float) masterGain, true, true);
                    } else {
                        java.util.Arrays.fill(samples, 0.0f);
                    }
                }

                int output = 0;
                for (int i = 0; i < BLOCK_SIZE * 2; i++) {
                    float sample = Math.max(-1.0f, Math.min(1.0f, samples[i]));
                    int value = Math.round(sample * Short.MAX_VALUE);
                    buf[output++] = (byte) (value & 0xFF);
                    buf[output++] = (byte) ((value >>> 8) & 0xFF);
                }

                line.write(buf, 0, buf.length);
            } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
        }
    }

    @Override
    public void close() {
        isOpen = false;
        for (Receiver receiver : new ArrayList<>(receivers)) {
            receiver.close();
        }
        executor.shutdown();
        if (line != null) {
            line.drain();
            line.close();
        }
        synchronized (lock) {
            if (sampler != null) {
                sampler.stopAll();
                sampler = null;
            }
        }
        voiceStatuses.clear();
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
        return new OpenDojaReceiver();
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
        return false;
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
        return null;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return new Instrument[0];
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return new Instrument[0];
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

    public class OpenDojaMidiChannel implements MidiChannel {

        private final int channel;
        private boolean mute = false;
        private boolean solo = false;
        private int program = 0;
        private int bank = 0;
        private int pitchBend = 8192;
        private final int[] control = new int[128];
        private final int[] polyPressure = new int[128];
        private int pressure = 0;

        public OpenDojaMidiChannel(int channel) {
            this.channel = channel;
            control[7] = 127; // default volume
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
            synchronized (lock) {
                if (sampler != null) {
                    sampler.keyOn(channel, noteNumber - 69, velocity / 127.0f);
                }
            }

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
            synchronized (lock) {
                if (sampler != null) {
                    sampler.keyOff(channel, noteNumber - 69);
                }
            }
            VoiceStatus voiceStatus = find(channel, noteNumber);
            if (voiceStatus != null) {
                voiceStatus.active = false;
                voiceStatuses.remove(voiceStatus);
            }
        }

        @Override
        public void noteOff(int noteNumber) {
            noteOff(noteNumber, 0);
        }

        private VoiceStatus find(int channel, int noteNumber) {
            return voiceStatuses.stream()
                .filter(vs -> vs.channel == channel && vs.note == noteNumber)
                .findFirst().orElse(null);
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
            control[controller] = value;
            switch (controller) {
                case 7 -> {
                    synchronized (lock) {
                        if (sampler != null) {
                            sampler.volume(channel, value / 127.0f);
                        }
                    }
                }
                case 10 -> {
                    synchronized (lock) {
                        if (sampler != null) {
                            sampler.panpot(channel, (value - 64) / 64.0f);
                        }
                    }
                }
                case 120 -> allSoundOff();
                case 123 -> allNotesOff();
            }
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            this.program = program;
            synchronized (lock) {
                if (sampler != null) {
                    sampler.programChange(channel, program);
                }
            }
        }

        @Override
        public void programChange(int bank, int program) {
            this.bank = bank;
            this.program = program;
            synchronized (lock) {
                if (sampler != null) {
                    sampler.bankChange(channel, bank);
                    sampler.programChange(channel, program);
                }
            }
        }

        @Override
        public int getProgram() {
            return program;
        }

        @Override
        public void setPitchBend(int bend) {
            this.pitchBend = bend;
            float range = 2.0f;
            float semitones = ((bend - 8192) / 8192.0f) * range;
            synchronized (lock) {
                if (sampler != null) {
                    sampler.pitchBend(channel, semitones);
                }
            }
        }

        @Override
        public int getPitchBend() {
            return pitchBend;
        }

        @Override
        public void resetAllControllers() {
            for (int i = 0; i < 128; i++) {
                control[i] = 0;
            }
            control[7] = 127;
            synchronized (lock) {
                if (sampler != null) {
                    sampler.volume(channel, 1.0f);
                    sampler.panpot(channel, 0.0f);
                    sampler.pitchBend(channel, 0.0f);
                }
            }
        }

        @Override
        public void allNotesOff() {
            for (VoiceStatus vs : new ArrayList<>(voiceStatuses)) {
                if (vs.channel == channel) {
                    noteOff(vs.note);
                }
            }
        }

        @Override
        public void allSoundOff() {
            synchronized (lock) {
                if (sampler != null) {
                    for (VoiceStatus vs : new ArrayList<>(voiceStatuses)) {
                        if (vs.channel == channel) {
                            sampler.keyOff(channel, vs.note - 69);
                        }
                    }
                }
            }
            voiceStatuses.removeIf(vs -> vs.channel == channel);
        }

        @Override
        public boolean localControl(boolean on) {
            return false;
        }

        @Override
        public void setMono(boolean on) {
        }

        @Override
        public boolean getMono() {
            return false;
        }

        @Override
        public void setOmni(boolean on) {
        }

        @Override
        public boolean getOmni() {
            return false;
        }

        @Override
        public void setMute(boolean mute) {
            this.mute = mute;
            if (mute) {
                allSoundOff();
            }
        }

        @Override
        public boolean getMute() {
            return mute;
        }

        @Override
        public void setSolo(boolean soloState) {
            this.solo = soloState;
        }

        @Override
        public boolean getSolo() {
            return solo;
        }
    }

    private class OpenDojaReceiver implements MidiDeviceReceiver {
        private boolean receiverOpen = true;

        public OpenDojaReceiver() {
            receivers.add(this);
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!receiverOpen) throw new IllegalStateException("receiver is not open");
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
                    synchronized (lock) {
                        if (sampler != null) {
                            sampler.sysEx(data);
                        }
                    }
                }
                case MetaMessage metaMessage -> {
                    logger.log(Level.DEBUG, "meta: %02x".formatted(metaMessage.getType()));
                }
                case null, default -> {
                    assert false;
                }
            }
        }

        @Override
        public void close() {
            receivers.remove(this);
            receiverOpen = false;
        }

        @Override
        public MidiDevice getMidiDevice() {
            return OpenDojaSynthesizer.this;
        }
    }
}
