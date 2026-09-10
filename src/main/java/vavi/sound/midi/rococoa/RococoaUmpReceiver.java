/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

import java.io.Closeable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.List;

import vavi.sound.midi.ump.Ump;
import vavi.sound.midi.ump.UmpReceiver;
import vavi.util.ByteUtil;
import vavix.rococoa.avfoundation.AVAudioEngine;
import vavix.rococoa.avfoundation.AVAudioMixerNode;
import vavix.rococoa.avfoundation.AVAudioUnitComponent;
import vavix.rococoa.avfoundation.AVAudioUnitComponentManager;
import vavix.rococoa.avfoundation.AVAudioUnitMIDIInstrument;
import vavix.rococoa.avfoundation.AudioComponentDescription;
import vavix.rococoa.avfoundation.MIDIEventList;

import static java.lang.System.getLogger;


/**
 * Plays Universal MIDI Packets on an AVFoundation music device AudioUnit.
 * <p>
 * The AudioUnit is asked to speak MIDI 2.0, and whether it agrees or not decides what happens to the
 * MIDI 2.0 Channel Voice messages of a clip. When it does, they go out as they are, when it does
 * not, they are down converted to MIDI 1.0 here, because the DLS synthesizer of macOS is a MIDI 1.0
 * one and would otherwise stay silent.
 * <p>
 * system property
 * <li> {@code vavi.sound.midi.rococoa.RococoaUmpReceiver.audesc} ... au desc, default {@code "appl:dls "} </li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/09/11 nsano initial version <br>
 * @see vavi.sound.midi.ump.UmpPlayer
 */
public class RococoaUmpReceiver implements UmpReceiver, Closeable {

    private static final Logger logger = getLogger(RococoaUmpReceiver.class.getName());

    private final AVAudioEngine engine;

    private final AVAudioMixerNode mixer;

    private final AVAudioUnitMIDIInstrument midiSynth;

    /** the scratch buffer every message is marshalled through, so playing allocates nothing native */
    private final MIDIEventList eventList;

    /** whether the AudioUnit takes MIDI 2.0 Channel Voice messages as they are */
    private final boolean midi2;

    /** how many messages the AudioUnit refused */
    private int errorCount;

    public RococoaUmpReceiver() {
        this(System.getProperty(RococoaUmpReceiver.class.getName() + ".audesc", "appl:dls "));
    }

    /** @param audesc {@code "manufacturer:subtype"} of a music device, ex. {@code "appl:dls "} */
    public RococoaUmpReceiver(String audesc) {
        String[] pair = audesc.split(":");
        if (pair.length != 2) {
            throw new IllegalArgumentException("not a component spec: " + audesc);
        }
        AudioComponentDescription description = new AudioComponentDescription();
        description.componentType = AudioComponentDescription.kAudioUnitType_MusicDevice;
        description.componentManufacturer = ByteUtil.readBeInt(fourCC(pair[0]));
        description.componentSubType = ByteUtil.readBeInt(fourCC(pair[1]));
        description.componentFlags = 0;
        description.componentFlagsMask = 0;

        // AVAudioUnitMIDIInstrument#init aborts the jvm on an unknown description, so look it up first
        List<AVAudioUnitComponent> components = AVAudioUnitComponentManager.shared().components(description);
        if (components.isEmpty()) {
            throw new IllegalArgumentException("no such AudioUnit: " + audesc);
        }
        midiSynth = AVAudioUnitMIDIInstrument.init(components.getFirst().audioComponentDescription());
        if (midiSynth == null) {
            throw new IllegalStateException("cannot instantiate AudioUnit: " + audesc);
        }

        // has to be asked before the engine initializes the AudioUnit
        int status = midiSynth.setHostMIDIProtocol(MIDIEventList.kMIDIProtocol_2_0);
        int protocol = midiSynth.MIDIProtocol();
        midi2 = protocol == MIDIEventList.kMIDIProtocol_2_0;
        eventList = new MIDIEventList(protocol);
logger.log(Level.DEBUG, "%s: %s, talks MIDI %s (setHostMIDIProtocol: %d)"
        .formatted(audesc, midiSynth.name(), midi2 ? "2.0" : "1.0", status));

        engine = AVAudioEngine.newInstance();
        mixer = AVAudioMixerNode.newInstance();
        engine.attachNode(mixer);
        engine.attachNode(midiSynth);
        engine.connect_to_format(midiSynth, mixer, null);
        engine.connect_to_format(mixer, engine.mainMixerNode(), null);
        engine.prepare();
        if (!engine.start()) {
            throw new IllegalStateException("cannot start the engine for " + audesc);
        }
    }

    /** The values must be in the range of 0.0 to 1.0. */
    public void setVolume(float volume) {
        mixer.setOutputVolume(volume);
    }

    /** whether the AudioUnit takes MIDI 2.0 Channel Voice messages as they are */
    public boolean isMidi2() {
        return midi2;
    }

    /** how many messages the AudioUnit has refused so far */
    public int getErrorCount() {
        return errorCount;
    }

    public boolean isOpen() {
        return engine.running();
    }

    @Override
    public void send(int[] words) {
        int[] message = midi2 ? words : Ump.toMidi1(words);
        // a down conversion may have turned one message into several, or into none at all
        for (int i = 0; i < message.length; i += Ump.words(message[i])) {
            int[] one = Arrays.copyOfRange(message, i, i + Ump.words(message[i]));
            if (!isPlayable(one[0])) {
logger.log(Level.TRACE, "skip: " + Ump.toString(one));
                continue;
            }
            int status = midiSynth.sendMIDIEventList(eventList, one);
            if (status != 0) {
                errorCount++;
logger.log(Level.WARNING, "MusicDeviceMIDIEventList(%s): %d".formatted(Ump.toString(one), status));
            }
        }
    }

    /** a music device has no use for the clock, the metadata and the endpoint discovery of a clip */
    private static boolean isPlayable(int word) {
        return switch (Ump.messageType(word)) {
            case Ump.SYSTEM, Ump.MIDI1_CHANNEL_VOICE, Ump.SYSEX7, Ump.MIDI2_CHANNEL_VOICE, Ump.DATA128 -> true;
            default -> false;
        };
    }

    @Override
    public void close() {
        engine.stop();
    }

    private static byte[] fourCC(String value) {
        byte[] bytes = value.getBytes();
        if (bytes.length != 4) {
            throw new IllegalArgumentException("not a 4 character code: " + value);
        }
        return bytes;
    }
}
