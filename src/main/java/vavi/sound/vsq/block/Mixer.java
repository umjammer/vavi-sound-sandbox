/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.vsq.block;

import java.util.List;

import vavi.sound.vsq.Block;
import vavi.util.Debug;


/**
 * Mixer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class Mixer implements Block {

    /** */
    int tracks; // 1

    /** */
    int outputMode; // 0

    /** */
    int masterFeder; // 0
    /** */
    int masterPanpot; // 0
    /** */
    int masterMute; // 0

    /** */
    int[] feders; // 0
    /** */
    int[] panpots; // 0
    /** */
    int[] mutes; // 0
    /** */
    int[] solos; // 0

    /** */
    public static Block newInstance(String label, List<String> params) {
        Mixer block = new Mixer();
        for (String param : params) {
            String[] pair = param.split("=");
            if ("MasterFeder".equals(pair[0])) {
                block.masterFeder = Integer.parseInt(pair[1]);
            } else if ("MasterPanpot".equals(pair[0])) {
                block.masterPanpot = Integer.parseInt(pair[1]);
            } else if ("MasterMute".equals(pair[0])) {
                block.masterMute = Integer.parseInt(pair[1]);
            } else if ("OutputMode".equals(pair[0])) {
                block.outputMode = Integer.parseInt(pair[1]);
            } else if ("Tracks".equals(pair[0])) {
                block.tracks = Integer.parseInt(pair[1]);
                block.feders = new int[block.tracks];
                block.panpots = new int[block.tracks];
                block.mutes = new int[block.tracks];
                block.solos = new int[block.tracks];
            } else if (pair[0].startsWith("Feder")) {
                block.feders[Integer.parseInt(pair[0].substring(5))] = Integer.parseInt(pair[1]);
            } else if (pair[0].startsWith("Panpot")) {
                block.panpots[Integer.parseInt(pair[0].substring(6))] = Integer.parseInt(pair[1]);
            } else if (pair[0].startsWith("Mute")) {
                block.mutes[Integer.parseInt(pair[0].substring(4))] = Integer.parseInt(pair[1]);
            } else if (pair[0].startsWith("Solo")) {
                block.solos[Integer.parseInt(pair[0].substring(4))] = Integer.parseInt(pair[1]);
            } else {
Debug.println("unhandled param: " + pair[0]);
            }
        }
        return block;
    }
}
