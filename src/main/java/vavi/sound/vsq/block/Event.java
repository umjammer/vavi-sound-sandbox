/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.vsq.Block;
import vavi.sound.vsq.VSQ;
import vavi.util.Debug;


/**
 * Event. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class Event implements Block {
    /** */
    String id;

    /** */
    public String getId() {
        return id;
    }

    /** "Singer" or "Anote" */
    String type;

    // Singer

    /** */
    String iconHandle; // h#0000

    // Anote

    /** */
    int length; // 240
    /** */
    int note; // 56
    /** */
    int dynamics; // 64
    /** */
    int pmBendDepth; // 8
    /** */
    int pmBendLength; // 0
    /** */
    int pmbPortamentoUse; // 0
    /** */
    int demDecGainRate; // 50
    /** */
    int demAccent; // 50
    /** {@link Handle#getId()} */
    String lyricHandle; // h#0001
    /** {@link Handle#getId()} */
    String vibratoHandle; // h#0004
    /** */
    int vibratoDelay; // 160

    /** */
    public static Block newInstance(String label, List<String> params) {
        Event block = new Event();
        block.id = label;
        for (String param : params) {
            String[] pair = param.split("=");
            if ("Type".equals(pair[0])) {
                block.type = pair[1];
            } else if ("IconHandle".equals(pair[0])) {
                block.iconHandle = pair[1];
            } else if ("Length".equals(pair[0])) {
                block.length = Integer.parseInt(pair[1]);
            } else if ("Note#".equals(pair[0])) {
                block.note = Integer.parseInt(pair[1]);
            } else if ("Dynamics".equals(pair[0])) {
                block.dynamics = Integer.parseInt(pair[1]);
            } else if ("PMBendDepth".equals(pair[0])) {
                block.pmBendDepth = Integer.parseInt(pair[1]);
            } else if ("PMBendLength".equals(pair[0])) {
                block.pmBendLength = Integer.parseInt(pair[1]);
            } else if ("PMbPortamentoUse".equals(pair[0])) {
                block.pmbPortamentoUse = Integer.parseInt(pair[1]);
            } else if ("DEMdecGainRate".equals(pair[0])) {
                block.demDecGainRate = Integer.parseInt(pair[1]);
            } else if ("DEMaccent".equals(pair[0])) {
                block.demAccent = Integer.parseInt(pair[1]);
            } else if ("LyricHandle".equals(pair[0])) {
                block.lyricHandle = pair[1];
            } else if ("VibratoHandle".equals(pair[0])) {
                block.vibratoHandle = pair[1];
            } else if ("VibratoDelay".equals(pair[0])) {
                block.vibratoDelay = Integer.parseInt(pair[1]);
            } else {
Debug.println("unhandled param: " + pair[0]);
            }
        }
        return block;
    }

    /** */
    public MidiEvent[] toMidiEvents(VSQ context) throws InvalidMidiDataException {
        if ("Anote".equals(type)) {
            int track = context.getCurrentTrack();
            int channel = (track + 1) % 4;

            ShortMessage noteOnMessage = new ShortMessage();
            noteOnMessage.setMessage(ShortMessage.NOTE_ON, channel, note, dynamics);
            MidiEvent noteOnEvent = new MidiEvent(noteOnMessage, context.getCurrentTicks());

            ShortMessage noteOffMessage = new ShortMessage();
            noteOffMessage.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
            MidiEvent noteOffEvent = new MidiEvent(noteOnMessage, context.getCurrentTicks() + length);

            Handle handle = Handle.class.cast(context.findHandle(track, lyricHandle));
            byte[] data = MidiUtil.getEncodedMessage(handle.getLyric());
            MetaMessage metaMessage = new MetaMessage();
            metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), data, data.length);
            MidiEvent textEvent = new MidiEvent(metaMessage, context.getCurrentTicks());

            return new MidiEvent[] { textEvent, noteOnEvent, noteOffEvent };
        } else {
            return null;
        }
    }
}

/* */
