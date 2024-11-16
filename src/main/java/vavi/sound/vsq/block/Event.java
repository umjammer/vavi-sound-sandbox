/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.lang.System.Logger.Level;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.vsq.Block;
import vavi.sound.vsq.VSQ;


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
            switch (pair[0]) {
                case "Type" -> block.type = pair[1];
                case "IconHandle" -> block.iconHandle = pair[1];
                case "Length" -> block.length = Integer.parseInt(pair[1]);
                case "Note#" -> block.note = Integer.parseInt(pair[1]);
                case "Dynamics" -> block.dynamics = Integer.parseInt(pair[1]);
                case "PMBendDepth" -> block.pmBendDepth = Integer.parseInt(pair[1]);
                case "PMBendLength" -> block.pmBendLength = Integer.parseInt(pair[1]);
                case "PMbPortamentoUse" -> block.pmbPortamentoUse = Integer.parseInt(pair[1]);
                case "DEMdecGainRate" -> block.demDecGainRate = Integer.parseInt(pair[1]);
                case "DEMaccent" -> block.demAccent = Integer.parseInt(pair[1]);
                case "LyricHandle" -> block.lyricHandle = pair[1];
                case "VibratoHandle" -> block.vibratoHandle = pair[1];
                case "VibratoDelay" -> block.vibratoDelay = Integer.parseInt(pair[1]);
                case null, default -> logger.log(Level.DEBUG, "unhandled param: " + pair[0]);
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

            Handle handle = (Handle) context.findHandle(track, lyricHandle);
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
