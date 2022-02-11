/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import vavi.sound.midi.opl3.Opl3Soundbank.Opl3Instrument;
import vavi.sound.opl3.Adlib;
import vavi.sound.opl3.MidPlayer;
import vavi.sound.opl3.MidPlayer.FileType;
import vavi.sound.opl3.Opl3Player;
import vavi.util.Debug;


/**
 * Opl3Synthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
public class Opl3Synthesizer implements Synthesizer {

    private static Logger logger = Logger.getLogger(Opl3Synthesizer.class.getName());

    private static final String version = "1.0.4";

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("OPL3 MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer for OPL3",
                            "Version " + version) {};

    // TODO != channel???
    private static final int MAX_CHANNEL = 16;

    private Opl3MidiChannel[] channels = new Opl3MidiChannel[MAX_CHANNEL];

    // TODO voice != channel ( = getMaxPolyphony())
    private VoiceStatus[] voiceStatus = new VoiceStatus[MAX_CHANNEL];

    private long timestump;

    private boolean isOpen;

    private SourceDataLine line;

    private AudioFormat audioFormat = Opl3Player.opl3;

    // ----

    private Adlib adlib;

    private Opl3Soundbank soundBank = new Opl3Soundbank(Adlib.midi_fm_instruments);

    /** default {@link Adlib#midi_fm_instruments} */
    private Opl3Instrument[] instruments = new Opl3Instrument[128];

    private static class Percussion {
        int channel = -1;
        int note;
        int volume = 0;
    }

    // ch percussion?
    private Percussion[] percussions = new Percussion[18];

    public class Context {
        public Adlib adlib() { return Opl3Synthesizer.this.adlib; }
        public Opl3Instrument[] instruments() { return Opl3Synthesizer.this.instruments; }
        public void instruments(Opl3Instrument[] instruments) { Opl3Synthesizer.this.instruments = instruments; }
        public Opl3MidiChannel[] channels() { return Opl3Synthesizer.this.channels; }
        public VoiceStatus[] voiceStatus() { return Opl3Synthesizer.this.voiceStatus; }
    }

    // ----

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    public void open(FileType type, Adlib.Writer writer) throws MidiUnavailableException {
        if (isOpen()) {
Debug.println("already open: " + hashCode());
            return;
        }

        for (int i = 0; i < MAX_CHANNEL; i++) {
            channels[i] = new Opl3MidiChannel(i);
            voiceStatus[i] = new VoiceStatus();
            voiceStatus[i].channel = i;
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

        for (int i = 0; i < 128; i++) {
            instruments[i] = (Opl3Instrument) soundBank.getInstruments()[i];
        }

        for (int c = 0; c < 16; c++) {
            channels[c].inum = 0;

            channels[c].setIns(instruments[channels[c].inum]);

            voiceStatus[c].volume = 127;
            channels[c].nshift = -25;
            voiceStatus[c].active = true;
        }

        for (int i = 0; i < 18; i++) {
            percussions[i] = new Percussion();
        }

Debug.println("type: " + type);
        type.midiTypeFile.init(new Context());

        adlib.reset();

        //
        isOpen = true;

        if (adlib.isOplInternal()) {
            init();
            executor.submit(this::play);
        }
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MAX_PRIORITY);
        return thread;
    });

    /** */
    private void init() throws MidiUnavailableException {
        try {
            DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
            line = (SourceDataLine) AudioSystem.getLine(lineInfo);
Debug.println(line.getClass().getName());
            line.addLineListener(event -> { Debug.println("Line: " + event.getType()); });

            line.open(audioFormat);
            line.start();
        } catch (LineUnavailableException e) {
            throw (MidiUnavailableException) new MidiUnavailableException().initCause(e);
        }

        timestump = 0;
    }

    /** */
    private void play() {
        byte[] buf = new byte[4 * (int) audioFormat.getSampleRate() / 10];
//Debug.printf("buf: %d", buf.length);

        while (isOpen) {
            try {
                long msec = System.currentTimeMillis() - timestump;
                timestump = System.currentTimeMillis();
                msec = msec > 100 ? 100 : msec;
                int l = adlib.read(buf, 0, 4 * (int) (audioFormat.getSampleRate() * msec / 1000.0));
//Debug.printf("adlib: %d", l);
                if (l > 0) {
                    line.write(buf, 0, l);
                }
                Thread.sleep(33); // TODO how to define?
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void open() throws MidiUnavailableException {
        open(FileType.MIDI, null);
    }

    @Override
    public void close() {
        isOpen = false;
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
        return timestump;
    }

    @Override
    public int getMaxReceivers() {
        return 1;
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
        return null;
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
        return 330;
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
        return soundbank instanceof Opl3Instrument;
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
        return soundBank;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return instruments;
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return instruments;
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

    public class Opl3MidiChannel implements MidiChannel {

        private int channel;

        private int[] polyPressure = new int[128];
        private int pressure;
        private int pitchBend;
        private int[] control = new int[128];

        // instrument number TODO public
        public int inum;
        // instrument for adlib
        int[] ins = new int[11];
        //
        public int nshift; // TODO public

        public void setIns(Opl3Instrument instrument) { // TODO public
            int[] data = (int[]) instrument.getData();
            for (int i = 0; i < 11; ++i) {
                ins[i] = data[i];
            }
        }

        /** */
        public Opl3MidiChannel(int channel) {
            this.channel = channel;
        }

        @Override
        public void noteOn(int noteNumber, int velocity) {
            int numchan;
            if (adlib.mode == Adlib.RYTHM) {
                numchan = 6;
            } else {
                numchan = 9;
            }

            if (voiceStatus[channel].active) {
                for (int i = 0; i < 18; ++i) {
                    ++percussions[i].volume;
                }

                int voice;
                if (channel < 11 || adlib.mode == Adlib.MELODIC) {
                    boolean f = false;
                    voice = -1;
                    int onl = 0;

                    for (int i = 0; i < numchan; ++i) {
                        if (percussions[i].channel == -1 && percussions[i].volume > onl) {
                            onl = percussions[i].volume;
                            voice = i;
                            f = true;
                        }
                    }

                    if (voice == -1) {
                        onl = 0;

                        for (int i = 0; i < numchan; ++i) {
                            if (percussions[i].volume > onl) {
                                onl = percussions[i].volume;
                                voice = i;
                            }
                        }
                    }

                    if (!f) {
                        adlib.endNote(voice);
                    }
                } else {
                    voice = Adlib.percussion_map[channel - 11];
                }

                if (velocity != 0 && inum >= 0 && inum < 128) {
                    if (adlib.mode == Adlib.MELODIC || channel < 12) {
                        adlib.instrument(voice, ins);
                    } else {
                        adlib.percussion(channel, ins);
                    }

                    int nv;
                    if ((adlib.style & Adlib.MIDI_STYLE) != 0) {
                        nv = voiceStatus[channel].volume * velocity / 128;
                        if ((adlib.style & Adlib.LUCAS_STYLE) != 0) {
                            nv *= 2;
                        }

                        if (nv > 127) {
                            nv = 127;
                        }

                        nv = Adlib.my_midi_fm_vol_table[nv];
                        if ((adlib.style & Adlib.LUCAS_STYLE) != 0) {
                            nv = (int) ((float) Math.sqrt((nv)) * 11.0F);
                        }
                    } else {
                        nv = velocity;
                    }

                    adlib.playNote(voice, noteNumber + nshift, nv * 2);

                    percussions[voice].channel = channel;
                    percussions[voice].note = noteNumber;
                    percussions[voice].volume = 0;

                    if (adlib.mode == Adlib.RYTHM && channel >= 11) {
                        adlib.write(0xbd, adlib.read(0xbd) & ~(16 >> (channel - 11)));
                        adlib.write(0xbd, adlib.read(0xbd) | 16 >> (channel - 11));
                    }
                } else if (velocity == 0) { // same code as end note
                    for (int i = 0; i < 9; ++i) {
                        if (percussions[i].channel == channel && percussions[i].note == noteNumber) {
                            adlib.endNote(i);
                            percussions[i].channel = -1;
                        }
                    }
                } else { // i forget what this is for.
                    percussions[voice].channel = -1;
                    percussions[voice].volume = 0;
                }

                logger.fine(String.format("note on[%d]: %d %d %d", channel, inum, noteNumber, velocity));
            } else {
                logger.fine("note is off");
            }
            //
            voiceStatus[channel].note = noteNumber;
            this.pressure = velocity;
        }

        @Override
        public void noteOff(int noteNumber, int velocity) {
            for (int i = 0; i < 9; ++i) {
                if (percussions[i].channel == channel && percussions[i].note == noteNumber) {
                    adlib.endNote(i);
                    percussions[i].channel = -1;
                }
            }
            //
            voiceStatus[channel].note = 0;
            this.pressure = velocity;
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
            case 0x07: // channel volume
                voiceStatus[channel].volume = value;
                logger.fine(String.format("control change[%d]: vol(%02x): %d", channel, controller, value));
                break;
            case 0x67: // 103: undefined
                logger.fine(String.format("control change[%d]: (%02x): %d", channel, controller, value));
                if ((adlib.style & Adlib.CMF_STYLE) != 0) {
                    adlib.mode = value;
                    if (adlib.mode == Adlib.RYTHM) {
                        adlib.write(0xbd, adlib.read(0xbd) | (1 << 5));
                    } else {
                        adlib.write(0xbd, adlib.read(0xbd) & ~(1 << 5));
                    }
                }
                break;
            }
            //
            control[controller] = value;
        }

        @Override
        public int getController(int controller) {
            return control[controller];
        }

        @Override
        public void programChange(int program) {
            inum = program;
            setIns(instruments[inum]);
            //
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
            voiceStatus[channel].active = !mute;
        }

        @Override
        public boolean getMute() {
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

    private class Opl3Receiver implements Receiver {
        @SuppressWarnings("hiding")
        private boolean isOpen;

        public Opl3Receiver() {
            receivers.add(this);
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) {
                throw new IllegalStateException("receiver is not open");
            }

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
Debug.printf("unhandled short: %02X\n", command);
                }
            } else if (message instanceof SysexMessage) {
                SysexMessage sysexMessage = (SysexMessage) message;
                byte[] data = sysexMessage.getData();
//Debug.print("sysex:\n" + StringUtil.getDump(data));
Debug.printf(Level.FINE, "sysex: %02x %02x %02x", data[1], data[2], data[3]);

                if (data[1] == 0x7d && data[2] == 0x10 && data[3] < 16) {
                    adlib.style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;

                    int c = data[3];
                    channels[c].ins = MidPlayer.fromSysex(data);
                }
            } else if (message instanceof SysexMessage) {
                MetaMessage metaMessage = (MetaMessage) message;
Debug.printf("meta: %02x", metaMessage.getType());
                switch (metaMessage.getType()) {
                case 0x2f:
                    break;
                }
            } else {
                assert false;
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
