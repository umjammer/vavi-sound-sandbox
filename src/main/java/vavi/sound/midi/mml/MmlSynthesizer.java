/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sound.midi.Instrument;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
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

import vavi.sound.midi.MidiUtil;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * MmlSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2024/12/18 umjammer initial version <br>
 */
public class MmlSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(MmlSynthesizer.class.getName());

    static {
        try {
            try (InputStream is = MmlSynthesizer.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sandbox/pom.properties")) {
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
    protected static final Info info =
        new Info("MML MIDI Synthesizer",
                            "vavi",
                            "Software synthesizer for MML",
                            "Version " + version) {};

    /** required gervill */
    private Synthesizer synthesizer;

    private MmlOscillator mmlOscillator;

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        synthesizer = MidiUtil.getDefaultSynthesizer(MmlMidiDeviceProvider.class);
logger.log(Level.DEBUG, "wrapped synthesizer: " + synthesizer.getClass().getName());
        synthesizer.open();
        synthesizer.unloadAllInstruments(synthesizer.getDefaultSoundbank());
        synthesizer.loadAllInstruments(mmlOscillator = new MmlOscillator());
    }

    @Override
    public void close() {
        synthesizer.close();
    }

    @Override
    public boolean isOpen() {
        if (synthesizer != null) {
            return synthesizer.isOpen();
        } else {
            return false;
        }
    }

    @Override
    public long getMicrosecondPosition() {
        return synthesizer.getMicrosecondPosition();
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
        return new MmlReceiver(synthesizer.getReceiver()); // TODO not works, infinite loop?
//        return synthesizer.getReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers; // TODO ditto
//        return synthesizer.getReceivers();
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return synthesizer.getTransmitter();
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return synthesizer.getTransmitters();
    }

    @Override
    public int getMaxPolyphony() {
        return synthesizer.getMaxPolyphony();
    }

    @Override
    public long getLatency() {
        return synthesizer.getLatency();
    }

    @Override
    public MidiChannel[] getChannels() {
        return synthesizer.getChannels();
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        return synthesizer.getVoiceStatus();
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
        return mmlOscillator;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return mmlOscillator.getInstruments();
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return mmlOscillator.getInstruments();
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
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

    private final List<Receiver> receivers = new ArrayList<>();

    private class MmlReceiver implements Receiver {
        final Receiver receiver;

        public MmlReceiver(Receiver receiver) {
            receivers.add(this);
            this.receiver = receiver;
logger.log(Level.DEBUG, "receiver: " + this.receiver);
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
try {
            if (message instanceof ShortMessage shortMessage) {
                int command = shortMessage.getCommand();
                int channel = shortMessage.getChannel();
                int data1 = shortMessage.getData1();
                int data2 = shortMessage.getData2();
logger.log(Level.TRACE, "short: %02x %d %02x %02x".formatted(command, channel, data1, data2));
                switch (command) {
                    case ShortMessage.PROGRAM_CHANGE -> mmlOscillator.programChange(channel, data1, data2);
                }
            } else if (message instanceof SysexMessage sysexMessage) {
                byte[] data = sysexMessage.getData();
//logger.log(Level.TRACE, "sysex: %02x %02x %02x".formatted(data[1], data[2], data[3]));

logger.log(Level.DEBUG, "sysex: %02X\n%s".formatted(sysexMessage.getStatus(), StringUtil.getDump(data)));
                switch (data[0]) {
                    case 0x7f -> { // realtime universal exclusive
                        switch (data[1]) {
                            case 0x7f -> { // device ID: all-call
                                if (data[2] == 0x04 && data[3] == 0x01) { // master volume
                                    logger.log(Level.DEBUG, "sysex: master volume: %02x %02x".formatted(data[4], data[5])); // TODO
                                }
                            }
                        }
                    }
                    default -> {}
                }
            } else if (message instanceof MetaMessage metaMessage) {
                int type = metaMessage.getType();
                byte[] data = metaMessage.getData();
logger.log(Level.DEBUG, "meta: %02x".formatted(type));
                switch (type) {
                    case 0x51 -> {
                        mmlOscillator.meta(type, data);
                    }
                    case 0x2f -> {}
                }
            } else {
                assert false;
            }
} catch (Throwable t) {
 logger.log(Level.DEBUG, t.getMessage(), t);
}
            this.receiver.send(message, timeStamp);
        }

        @Override
        public void close() {
            receivers.remove(this);
        }
    }
}
