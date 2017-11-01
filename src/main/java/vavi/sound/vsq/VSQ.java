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
import vavi.util.Debug;


/**
 * VSQ. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080630 nsano initial version <br>
 * @see "http://www9.atwiki.jp/boare/pages/16.html"
 */
public class VSQ {

    /** */
    private List<Block>[] tracks;

    /**
     * @param sequence will be update 
     */
    @SuppressWarnings("unchecked")
    public VSQ(Sequence sequence) throws IOException {
        
        String[] data = getData(sequence);

        tracks = new List[data.length];

        for (int i = 0; i < data.length; i++) {
            tracks[i] = new ArrayList<>();
//Debug.println("track:" + i + "\n" + data[i]);
            Reader reader = new StringReader(data[i]);
            readBlocks(i, reader);
Debug.println("track[" + i + "]: " + tracks[i].size());
            
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

            EventList eventList = EventList.class.cast(findBlock(i, EventList.class));
            for (EventList.Pair pair : eventList.getEvents()) {
                if (!"EOS".equals(pair.id)) {
                    Event event = Event.class.cast(findEvent(i, pair.id));
                    currentTicks = pair.tick;
        
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
//Debug.println(ToStringBuilder.reflectionToString(block));
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
            if (Event.class.isInstance(block)) {
                if (Event.class.cast(block).getId().equals(id)) {
                    return block;
                }
            }
        }
        throw new NoSuchElementException(String.valueOf(id));
    }

    /** */
    public Block findHandle(int trackNumber, String id) {
        for (Block block : tracks[trackNumber]) {
            if (Handle.class.isInstance(block)) {
                if (Handle.class.cast(block).getId().equals(id)) {
                    return block;
                }
            }
        }
        throw new NoSuchElementException(String.valueOf(id));
    }

    /**
     * @throws IllegalArgumentException sequence has not "Voice#"
     */
    private String[] getData(Sequence sequence) throws IOException {
        Track[] tracks = sequence.getTracks();
Debug.println("tracks: " + tracks.length);
        String[] results = new String[tracks.length - 1];

        for (int t = 0; t < tracks.length; t++) {
            Track track = tracks[t];
            if (t > 0) { // track 0 is master track
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
Debug.println("events[" + t + "]: " + track.size());
                for (int e = 0; e < track.size(); e++) {
                    MidiEvent event = track.get(e);
                    MidiMessage message = event.getMessage();
//Debug.println("message: " + message);
                    if (MetaMessage.class.isInstance(message)) {
                        MetaMessage meta = MetaMessage.class.cast(message);
//Debug.println(meta.getType());
                        switch (meta.getType()) {
                        case 1:  // テキスト・イベント 127 bytes
                            byte[] data = meta.getData();
//Debug.println(new String(data));
                            int p = 0;
                            do {
                                p++;
                            } while (data[p] != ':');
                            do {
                                p++;
                            } while (data[p] != ':');
                            p++;
//Debug.println(new String(data).substring(p));
                            baos.write(data, p, data.length - p);
                            break;
                        case 3:  // トラック名 , 
                            String trackName = new String(meta.getData());
Debug.println("trackName[" + t + "]: " + trackName);
                            break;
                        default:
Debug.println("unhandled meta: " + meta.getType());
                            break;
                        }
                    }
                }
                results[t - 1] = baos.toString();
            }
        }

        return results;
    }
}

/* */
