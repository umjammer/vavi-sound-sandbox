/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.util.ArrayList;
import java.util.List;

import vavi.sound.vsq.Block;


/**
 * EventList.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class EventList implements Block {

    /** */
    public static class Pair {
        public final long tick;
        public final String id;
        public Pair(long tick, String id) {
            this.tick = tick;
            this.id = id;
        }
    }

    /** */
    private final List<Pair> events = new ArrayList<>(); // 0=ID#0000

    /** */
    public List<Pair> getEvents() {
        return events;
    }

    /** */
    public static Block newInstance(String label, List<String> params) {
        EventList block = new EventList();
        for (String param : params) {
            String[] pair = param.split("=");
            block.events.add(new Pair(Long.parseLong(pair[0]), pair[1]));
        }
        return block;
    }
}
