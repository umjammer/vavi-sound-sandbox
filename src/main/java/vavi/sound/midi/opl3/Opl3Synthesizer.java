/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import javax.sound.midi.Instrument;
import javax.sound.midi.MetaMessage;
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

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vavi.sound.midi.opl3.Opl3Soundbank.Opl3Instrument;
import vavi.sound.opl3.Adlib;
import vavi.sound.opl3.MidPlayer;
import vavi.sound.opl3.MidPlayer.FileType;
import vavi.sound.opl3.Opl3Player;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * Opl3Synthesizer.
 * <p>
 * CC accepts
 * <pre>
 * 7 ... channel volume
 * 103 ... modify adlib parameter
 * </pre>
 * sysex accepts
 * <pre>
 * 0xf0 xx 0x7d 0x10 yy ... (yy: channel) program set change
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
public class Opl3Synthesizer implements Synthesizer {

    private static final Logger logger = getLogger(Opl3Synthesizer.class.getName());

    static {
        try {
            try (InputStream is = Opl3Synthesizer.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sandbox/pom.properties")) {
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
        new MidiDevice.Info("Adlib MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer for Adlib",
                            "Version " + version) {};

    // TODO != channel???
    private static final int MAX_CHANNEL = 16;

    private final Opl3MidiChannel[] channels = new Opl3MidiChannel[MAX_CHANNEL];

    // TODO voice != channel ( = getMaxPolyphony())
    private final List<VoiceStatus> voiceStatuses = new ArrayList<>();

    private long timestamp;

    private boolean isOpen;

    private SourceDataLine line;

    private final AudioFormat audioFormat = Opl3Player.opl3;

    // ----

    private FileType type;

    private Adlib adlib;

    private Opl3Soundbank soundBank;

    /** default {@link Adlib#midi_fm_instruments} */
    private Opl3Instrument[] instruments = new Opl3Instrument[128];

    public class Context {
        public Adlib adlib() { return Opl3Synthesizer.this.adlib; }
        public Opl3Instrument[] instruments() { return Opl3Synthesizer.this.instruments; }
        public void instruments(Opl3Instrument[] instruments) { Opl3Synthesizer.this.instruments = instruments; }
        public Opl3MidiChannel[] channels() { return Opl3Synthesizer.this.channels; }
    }

    // ----

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    public void open(FileType type, Adlib.Writer writer) throws MidiUnavailableException {
        if (isOpen()) {
logger.log(Level.WARNING, "already open: " + hashCode());
            return;
        }

        soundBank = (Opl3Soundbank) getDefaultSoundbank();

        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Opl3MidiChannel(i);
        }

        // constructor
        if (writer == null) {
            adlib = new Adlib();
        } else {
            adlib = new Adlib(writer);
        }

        // rewind
        adlib.style = Adlib.MIDI_STYLE | Adlib.CMF_STYLE;
        adlib.mode = Adlib.MELODIC;

        adlib.reset();

        for (int i = 0; i < 128; i++) {
            instruments[i] = (Opl3Instrument) soundBank.getInstruments()[i];
        }

        for (int c = 0; c < 16; c++) {
            channels[c].program = 0;

            channels[c].setIns(instruments[channels[c].program]);

            channels[c].nShift = -25;
        }

logger.log(Level.DEBUG, "type: " + type);
        this.type = type;
        type.midiTypeFile.init(new Context());

        //
        isOpen = true;

        if (adlib.isOplInternal()) {
            // when midi spi
            init();
            executor.submit(this::play);
        }
    }

    /** when midi spi */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MAX_PRIORITY);
        return thread;
    });

    /** when midi spi */
    private void init() throws MidiUnavailableException {
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
logger.log(Level.DEBUG, line.getClass().getName());
            line.addLineListener(event -> logger.log(Level.DEBUG, "Line: " + event.getType()));

            line.open(audioFormat);
            line.start();
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }

        timestamp = 0;
    }

    /** when midi spi */
    private void play() {
        byte[] buf = new byte[4 * (int) audioFormat.getSampleRate() / 10];
//logger.log(Level.TRACE, "buf: %d".formatted(buf.length));

        while (isOpen) {
            try {
                long msec = System.currentTimeMillis() - timestamp;
                timestamp = System.currentTimeMillis();
                msec = msec > 100 ? 100 : msec;
                int l = adlib.read(buf, 0, 4 * (int) (audioFormat.getSampleRate() * msec / 1000.0));
//logger.log(Level.TRACE, "adlib: %d".formatted(l));
                if (l > 0) {
                    line.write(buf, 0, l);
                }
                Thread.sleep(33); // TODO how to define?
            } catch (Exception e) {
                logger.log(Level.INFO, e.getMessage(), e);
            }
        }
    }

    @Override
    public void open() throws MidiUnavailableException {
        open(FileType.MIDI, null);
    }

    @Override
    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void close() {
        isOpen = false;
        for (int i = 0; i < receivers.size(); i++) receivers.get(i).close();
        line.drain();
        line.close();
        executor.shutdown();
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
        return new Opl3Receiver();
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
        return 18; // TODO OPL3 class said
    }

    @Override
    public long getLatency() {
        return 33;
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
        return soundbank instanceof Opl3Instrument;
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
        return new Opl3Soundbank(Adlib.midi_fm_instruments);
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return soundBank.getInstruments();
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return instruments;
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

    public class Opl3MidiChannel implements MidiChannel {

        private final int channel;

        private boolean mute = false;
        public int volume;
        private final int[] polyPressure = new int[128];
        private int pressure;
        private int pitchBend;
        private final int[] control = new int[128];

        // instrument number TODO public
        public int program;
        // instrument for adlib
        int[] ins = new int[11];
        //
        public int nShift; // TODO public

        public void setIns(Opl3Instrument instrument) { // TODO public
            int[] data = (int[]) instrument.getData();
            System.arraycopy(data, 0, ins, 0, 11);
        }

        /** */
        public Opl3MidiChannel(int channel) {
            this.channel = channel;
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            adlib.noteOn(noteNumber, velocity, channel, volume, program, ins, nShift, channels[channel].mute);
            //
            VoiceStatus voiceStatus = new VoiceStatus();
            voiceStatus.note = noteNumber;
            voiceStatus.active = true;
            voiceStatus.channel = channel;
            voiceStatus.program = program;
            voiceStatus.bank = 0;
            voiceStatus.volume = velocity;
            voiceStatuses.add(voiceStatus);

            this.pressure = velocity;
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            adlib.noteOff(noteNumber, channel);
            //
            VoiceStatus voiceStatus = find(channel, noteNumber);
            if (voiceStatus != null) {
                voiceStatus.active = false;
                voiceStatuses.remove(voiceStatus);
            }

            this.pressure = velocity;
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
                case 0x07 -> { // channel volume
                    this.volume = value;
logger.log(Level.DEBUG, "control change[%d]: vol(%02x): %d".formatted(channel, controller, value));
                }
                default ->
logger.log(Level.TRACE, "control change unhandled[%d]: (%02x): %d".formatted(channel, controller, value));
            }

            type.midiTypeFile.controlChange(channel, controller, value);

            //
            control[controller] = value;
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            this.program = program & 0x7f;
logger.log(Level.DEBUG, "instruments[" + this.program + "]: " + instruments[this.program]);
            setIns(instruments[this.program]);
logger.log(Level.DEBUG, "program change[%d]: %d".formatted(channel, this.program));
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
            //
            pitchBend = bend;
        }

        @Override
        public int getPitchBend() {
            return pitchBend;
        }

        @Override
        public void resetAllControllers() {
            controlChange(121, 0); // 0x79
        }

        @Override
        public void allNotesOff() {
            controlChange(123, 0); // 0x7b
        }

        @Override
        public void allSoundOff() {
            controlChange(120, 0); // 0x78
        }

        @Override
        public boolean localControl(boolean on) {
            controlChange(122, on ? 127 : 0); // 0x7a
            return getController(122) >= 64;
        }

        @Override
        public void setMono(boolean on) {
            controlChange(on ? 126 : 127, 0); // 0x7e, 0x7d
        }

        @Override
        public boolean getMono() {
            return getController(126) == 0;
        }

        @Override
        public void setOmni(boolean on) {
            controlChange(on ? 125 : 124, 0); // 0x7d, 0x7c
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

    private class Opl3Receiver implements MidiDeviceReceiver {
        @SuppressWarnings("hiding")
        private boolean isOpen;

        public Opl3Receiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) throw new IllegalStateException("receiver is not open");

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
                        case 0x7f -> { // Universal Realtime
                            int c = data[1]; // 0x7f: Disregards channel
                            // Sub-ID, Sub-ID2
                            if (data[2] == 0x04 && data[3] == 0x01) { // Device Control / Master Volume
                                float gain = ((data[4] & 0x7f) | ((data[5] & 0x7f) << 7)) / 16383f;
logger.log(Level.DEBUG, "sysex volume: gain: %4.2f".formatted(gain));
                                volume(line, gain);
                            }
                        }
                        case 0x7d -> { // test
                            switch (data[2]) {
                                case 0x10: // 7D 10 ch -- set an instrument to ch
                                    // TODO maybe for LUCAS only
if (type != FileType.LUCAS) {
 logger.log(Level.WARNING, "sysex: set LUCAS_STYLE for " + type);
}
                                    adlib.style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;

                                    int c = data[3];
                                    System.arraycopy(MidPlayer.fromSysex(data), 0, channels[c].ins, 0, 11);
logger.log(Level.DEBUG, "sysex lucas ins ch: %d".formatted(c));

                                    break;
                            }
                        }
                        default -> logger.log(Level.DEBUG, "sysex unhandled: %02x".formatted(data[1]));
                    }
                }
                case MetaMessage metaMessage -> {
logger.log(Level.DEBUG, "meta: %02x".formatted(metaMessage.getType()));
                    switch (metaMessage.getType()) {
                        case 0x2f -> {}
                    }
                }
                case null, default -> {
                    assert false;
                }
            }
        }

        @Override
        public void close() {
            receivers.remove(this);
            isOpen = false;
        }

        @Override
        public MidiDevice getMidiDevice() {
            return Opl3Synthesizer.this;
        }
    }
}
