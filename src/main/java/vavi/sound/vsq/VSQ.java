/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import vavi.sound.vsq.block.Event;
import vavi.sound.vsq.block.EventList;
import vavi.sound.vsq.block.Handle;

import static java.lang.System.getLogger;


/**
 * VSQ.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080630 nsano initial version <br>
 * @see "http://www9.atwiki.jp/boare/pages/16.html"
 */
public class VSQ {

    private static final Logger logger = getLogger(VSQ.class.getName());

    /** */
    private final List<Block>[] tracks;

    /**
     * @param sequence will be updated
     */
    @SuppressWarnings("unchecked")
    public VSQ(Sequence sequence) throws IOException {

        String[] data = getData(sequence);

        tracks = new List[data.length];

        for (int i = 0; i < data.length; i++) {
            tracks[i] = new ArrayList<>();
//logger.log(Level.TRACE, "track:" + i + "\n" + data[i]);
            Reader reader = new StringReader(data[i]);
            readBlocks(i, reader);
logger.log(Level.DEBUG, "track[" + i + "]: " + tracks[i].size());

        }
    }

    /** */
    private long currentTicks;

    /** */
    public long getCurrentTicks() {
        return currentTicks;
    }

    /** */
    private int currentTrack;

    /** */
    public int getCurrentTrack() {
        return currentTrack;
    }

    /** */
    public void convert1(Sequence sequence) throws InvalidMidiDataException {
        int tracks = sequence.getTracks().length - 1;

        for (int i = 0; i < tracks; i++) {
            Track track = sequence.createTrack();
            currentTrack = i;

            EventList eventList = (EventList) findBlock(i, EventList.class);
            for (EventList.Pair pair : eventList.getEvents()) {
                if (!"EOS".equals(pair.id())) {
                    Event event = (Event) findEvent(i, pair.id());
                    currentTicks = pair.tick();

                    //
                    MidiEvent[] midiEvents = event.toMidiEvents(this);
                    if (midiEvents != null) {
                        for (MidiEvent midiEvent : midiEvents) {
                            track.add(midiEvent);
                        }
                    }
                }
            }
        }
    }

    /** */
    private void readBlocks(int trackNumber, Reader reader) {
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(reader);
        String label = null;
        List<String> params = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("[")) {
                if (label == null) {
                    label = line.substring(1, line.length() - 1);
                } else {
                    Block block = Block.Factory.getBlock(label, params);
//logger.log(Level.TRACE, ToStringBuilder.reflectionToString(block));
                    tracks[trackNumber].add(block);
                    label = line.substring(1, line.length() - 1);
                    params.clear();
                }
            } else {
                params.add(line);
            }
        }
    }

    /** */
    public Block findBlock(int trackNumber, Class<? extends Block> blockClass) {
        for (Block block : tracks[trackNumber]) {
            if (blockClass.isInstance(block)) {
                return block;
            }
        }
        throw new NoSuchElementException(blockClass.getName());
    }

    /** */
    public Block findEvent(int trackNumber, String id) {
        for (Block block : tracks[trackNumber]) {
            if (block instanceof Event) {
                if (((Event) block).getId().equals(id)) {
                    return block;
                }
            }
        }
        throw new NoSuchElementException(String.valueOf(id));
    }

    /** */
    public Block findHandle(int trackNumber, String id) {
        for (Block block : tracks[trackNumber]) {
            if (block instanceof Handle) {
                if (((Handle) block).getId().equals(id)) {
                    return block;
                }
            }
        }
        throw new NoSuchElementException(String.valueOf(id));
    }

    /**
     * @throws IllegalArgumentException sequence has not "Voice#"
     */
    private static String[] getData(Sequence sequence) throws IOException {
        Track[] tracks = sequence.getTracks();
logger.log(Level.DEBUG, "tracks: " + tracks.length);
        String[] results = new String[tracks.length - 1];

        // for text, "DM:###:###..."
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int t = 0; t < tracks.length; t++) {
            Track track = tracks[t];
            if (t > 0) { // track 0 is master track
logger.log(Level.DEBUG, "events[" + t + "]: " + track.size());
                for (int e = 0; e < track.size(); e++) {
                    MidiEvent event = track.get(e);
                    MidiMessage message = event.getMessage();
//logger.log(Level.TRACE, "message: " + message);
                    if (message instanceof MetaMessage meta) {
//logger.log(Level.TRACE, meta.getType());
                        switch (meta.getType()) {
                        case 1:  // text event 127 bytes
                            byte[] data = meta.getData();
//logger.log(Level.TRACE, new String(data));
                            if (data[0] == 'D' && data[1] == 'M') {
                                int p = 0;
                                do {
                                    p++;
                                } while (data[p] != ':');
                                do {
                                    p++;
                                } while (data[p] != ':');
                                p++;
//logger.log(Level.TRACE, new String(data).substring(p));
                                baos.write(data, p, data.length - p);
                            } else {
logger.log(Level.DEBUG, new String(data, Charset.forName("MS932")));
                            }
                            break;
                        case 3:  // track name,
                            String trackName = new String(meta.getData());
logger.log(Level.DEBUG, "trackName[" + t + "]: " + trackName);
                            break;
                        default:
logger.log(Level.DEBUG, "unhandled meta: " + meta.getType());
                            break;
                        }
                    }
                }
                results[t - 1] = baos.toString();
            }
        }

logger.log(Level.DEBUG, baos.toString("MS932"));

        return results;
    }
}
