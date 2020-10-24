/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

import org.uva.emulation.Opl3SoundBank.Opl3Instrument;

import vavi.util.Debug;


/**
 * Opl3Synthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/03 umjammer initial version <br>
 */
public class Opl3Synthesizer implements Synthesizer {

    static Logger logger = Logger.getLogger(Opl3Synthesizer.class.getName());

    private static final String version = "0.0.1.";

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("OPL3 MIDI Synthesizer",
                            "Vavisoft",
                            "Software synthesizer for OPL3",
                            "Version " + version) {};

    // TODO != channel???
    private static final int MAX_CHANNEL = 16;

    private Opl3MidiChannel[] channels = new Opl3MidiChannel[MAX_CHANNEL];

    private VoiceStatus[] voiceStatus = new VoiceStatus[MAX_CHANNEL];

    private long timestump;

    boolean isOpen;

    // ----

    Adlib adlib;

    Opl3SoundBank soundBank = new Opl3SoundBank();

    /** default {@link Opl3SoundBank#midi_fm_instruments} */
    Opl3Instrument[] myinsbank = new Opl3Instrument[128];

    static class Percussion {
        int channel = -1;
        int note;
        int volume = 0;
    }

    // ch percussion?
    private Percussion[] percussions = new Percussion[18];

    // ----

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    public void open(Map<String, Object> info) throws MidiUnavailableException {
        for (int i = 0; i < MAX_CHANNEL; i++) {
            channels[i] = new Opl3MidiChannel(i);
            voiceStatus[i] = new VoiceStatus();
            voiceStatus[i].channel = i;
        }

        // constructor
        if (info == null || info.get("adlib.writer") == null) {
            adlib = new Adlib();
        } else {
            adlib = new Adlib((Adlib.Writer) info.get("adlib.writer"));
        }

        // rewind
        adlib.style = Adlib.MIDI_STYLE | Adlib.CMF_STYLE;
        adlib.mode = Adlib.MELODIC;

        for (int i = 0; i < 128; ++i) {
            myinsbank[i] = (Opl3Instrument) soundBank.getInstruments()[i];
        }

        for (int c = 0; c < 16; ++c) {
            channels[c].inum = 0;

            channels[c].setIns(myinsbank[channels[c].inum]);

            voiceStatus[c].volume = 127;
            channels[c].nshift = -25;
            voiceStatus[c].active = true;
        }

        for (int i = 0; i < 18; ++i) {
            percussions[i] = new Percussion();
        }

        int type = info == null || info.get("type") == null ? MidPlayer.FILE_MIDI : (int) info.get("type");
Debug.println("type: " + type);
        switch (type) {
        case MidPlayer.FILE_LUCAS:
            adlib.style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;
        case MidPlayer.FILE_MIDI:
            break;
        case MidPlayer.FILE_CMF:
            int tins = (int) info.get("cmf.tins");
            for (int i = 0; i < tins; ++i) {
                myinsbank[i] = (Opl3Instrument) info.get("cmf.myinsbank." + i);
            }

            for (int c = 0; c < 16; ++c) {
                channels[c].nshift = -13;
            }

            adlib.style = Adlib.CMF_STYLE;
            break;
        case MidPlayer.FILE_SIERRA:
            myinsbank = (Opl3Instrument[]) info.get("sierra.myinsbank");
            for (int c = 0; c < 16; ++c) {
                channels[c].nshift = -13;
                voiceStatus[c].active = (boolean) info.get("sierra." + c + ".on");
                channels[c].inum = (int) info.get("sierra." + c + ".inum");

                channels[c].setIns(myinsbank[channels[c].inum]);
            }

            adlib.style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE;
            break;
        case MidPlayer.FILE_ADVSIERRA:
            myinsbank = (Opl3Instrument[]) info.get("sierra.myinsbank");
            adlib.style = Adlib.SIERRA_STYLE | Adlib.MIDI_STYLE; // advanced sierra tunes use volume;
            break;
        case MidPlayer.FILE_OLDLUCAS:
            tins = (int) info.get("old_lucas.tins");
            for (int i = 0; i < tins; ++i) {
                myinsbank[i] = (Opl3Instrument) info.get("old_lucas.myinsbank." + i);
            }

            for (int c = 0; c < 16; ++c) {
                if (c < tins) {
                    channels[c].inum = c;

                    channels[c].setIns(myinsbank[channels[c].inum]);
                }
            }

            info.put("adlib.style", Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE);
            break;
        }

        adlib.reset();

//        AudioInputStream ais = new Opl3
        
        
        
        
        
        isOpen = true;
    }

    @Override
    public void open() throws MidiUnavailableException {
        open(null);
    }

    @Override
    public void close() {
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
        return new Opl3Receiver();
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
        return MAX_CHANNEL * 6;
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

    private class Opl3MidiChannel implements MidiChannel {

        private int channel;

        private int[] polyPressure = new int[127];
        private int pressure;
        private int pitchBend;
        private int[] control = new int[127];

        // instrument number
        int inum;
        // instrument for adlib
        int[] ins = new int[11];
        //
        int nshift;

        void setIns(Opl3Instrument instrument) {
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

                logger.fine(String.format(" [%d:%d:%d:%d]", channel, inum, noteNumber, velocity));
            } else {
                logger.fine("off");
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
            case 0x07:
                logger.fine(String.format("(pb:%d: %d %d)", channel, controller, value));
                voiceStatus[channel].volume = value;
                logger.fine("vol");
                break;
            case 0x67:
                logger.fine(String.format("\n\nhere:%d\n\n", value));
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
            setIns(myinsbank[inum]);
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
        private boolean isOpen;

        public Opl3Receiver() {
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
Debug.printf("%02X\n", command);
                    }
                } else if (message instanceof SysexMessage) {
                    SysexMessage sysexMessage = (SysexMessage) message;
                    byte[] data = sysexMessage.getData();
//Debug.print("sysex:\n" + StringUtil.getDump(data));

                    int pos = 1;
                    if (data[pos] == 0x7d && data[pos + 1] == 0x10 && data[pos + 2] < 16) {
                        adlib.style = Adlib.LUCAS_STYLE | Adlib.MIDI_STYLE;

                        int c = data[pos + 2];
                        channels[c].ins[0] = (data[pos + 4] << 4) + data[pos + 5];
                        channels[c].ins[2] = 0xff - ((data[pos + 6] << 4) + data[pos + 7] & 0x3f);
                        channels[c].ins[4] = 0xff - ((data[pos + 8] << 4) + data[pos + 9]);
                        channels[c].ins[6] = 0xff - ((data[pos + 10] << 4) + data[pos + 11]);
                        channels[c].ins[8] = (data[pos + 12] << 4) + data[pos + 13];
                        channels[c].ins[1] = (data[pos + 14] << 4) + data[pos + 15];
                        channels[c].ins[3] = 0xff - ((data[pos + 16] << 4) + data[pos + 17] & 0x3f);
                        channels[c].ins[5] = 0xff - ((data[pos + 18] << 4) + data[pos + 19]);
                        channels[c].ins[7] = 0xff - ((data[pos + 20] << 4) + data[pos + 21]);
                        channels[c].ins[9] = (data[pos + 22] << 4) + data[pos + 23];
                        channels[c].ins[10] = (data[pos + 24] << 4) + data[pos + 24];
                    }
                } else {
                    // TODO meta message
Debug.printf(message.getClass().getName());
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
