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
 * Handle. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080628 nsano initial version <br>
 */
public class Handle implements Block {

    /** */
    String id;

    /** */
    public String getId() {
        return id;
    }

    /** */
    String l0; // "れ","4 e",0.000000,64,0,0

    /** */
    int language; // 0
    /** */
    int program; // 0    

    /** */
    String iconID; // $04040001;
    /** */
    String ids; // normal
    /** */
    int original; // 1
    /** */
    String caption; // [Normal] Type 1
    /** */
    int length; // 6080;

    /** */
    int startDepth; // 64
    /** */
    int depthBPNum; // 0
    /** */
    String depthBPX; // 0.606300,0.612500 ...
    /** */
    String depthBPY;

    /** */
    int startRate; // 50
    /** */
    int rateBPNum; // 0
    /** */
    String rateBPX;
    /** */
    String rateBPY;

    /** */
    public static Block newInstance(String label, List<String> params) {
        Handle block = new Handle();
        block.id = label;
        for (String param : params) {
            String[] pair = param.split("=");
            if ("L0".equals(pair[0])) {
                block.l0 = pair[1];
            } else if ("IconID".equals(pair[0])) {
                block.iconID = pair[1];
            } else if ("IDS".equals(pair[0])) {
                block.ids = pair[1];
            } else if ("Original".equals(pair[0])) {
                block.original = Integer.parseInt(pair[1]);
            } else if ("Caption".equals(pair[0])) {
                block.caption = pair.length == 2 ? pair[1] : null;
            } else if ("Length".equals(pair[0])) {
                block.length = Integer.parseInt(pair[1]);
            } else if ("StartDepth".equals(pair[0])) {
                block.startDepth = Integer.parseInt(pair[1]);
            } else if ("DepthBPNum".equals(pair[0])) {
                block.depthBPNum = Integer.parseInt(pair[1]);
            } else if ("StartRate".equals(pair[0])) {
                block.startRate = Integer.parseInt(pair[1]);
            } else if ("RateBPNum".equals(pair[0])) {
                block.rateBPNum = Integer.parseInt(pair[1]);
            } else if ("Language".equals(pair[0])) {
                block.language = Integer.parseInt(pair[1]);
            } else if ("OpusTest".equals(pair[0])) {
                block.program = Integer.parseInt(pair[1]);
            } else if ("DepthBPX".equals(pair[0])) {
                block.depthBPX = pair[1];
            } else if ("DepthBPY".equals(pair[0])) {
                block.depthBPY = pair[1];
            } else if ("RateBPX".equals(pair[0])) {
                block.rateBPX = pair[1];
            } else if ("RateBPY".equals(pair[0])) {
                block.rateBPY = pair[1];
            } else {
Debug.println("unhandled param: " + pair[0]);
            }
        }
        return block;
    }

    public String getLyric() {
//Debug.println("l0: " + l0.split(",")[0].replace("\"", ""));
        return l0.split(",")[0].replace("\"", "");
    }
}

/* */
